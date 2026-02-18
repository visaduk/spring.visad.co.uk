package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.DependentDto;
import uk.co.visad.dto.TravelerDto;
import uk.co.visad.entity.Dependent;
import uk.co.visad.entity.Traveler;
import uk.co.visad.entity.TravelerQuestions;
import uk.co.visad.entity.VisaUrl;
import uk.co.visad.exception.BadRequestException;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.repository.DependentRepository;
import uk.co.visad.repository.TravelerQuestionsRepository;
import uk.co.visad.repository.TravelerRepository;
import uk.co.visad.repository.VisaUrlRepository;
import uk.co.visad.security.UserPrincipal;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class TravelerService {

    private final TravelerRepository travelerRepository;
    private final DependentRepository dependentRepository;
    private final TravelerQuestionsRepository travelerQuestionsRepository;
    private final VisaUrlRepository visaUrlRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "name", "travelCountry", "visaCenter", "package", "visaType", "status", "whatsappContact",
            "appointmentRemarks", "visaLink", "note", "plannedTravelDate", "firstName", "lastName",
            "gender", "dob", "nationality", "passportNo", "passportIssue", "passportExpire",
            "contactNumber", "email", "priority", "username", "logins", "notes", "paymentStatus",
            "addressLine1", "addressLine2", "city", "stateProvince", "zipCode", "docDate", "isFamily",
            "publicUrlToken", "country", "applicationFormLink", "applicationFormUsername",
            "applicationFormPassword", "title", "placeOfBirth", "countryOfBirth", "relationshipToMain",
            "price", "discountType", "discountValue", "refundAmount");

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    // Fields that belong to the traveler_questions table
    private static final Set<String> QUESTIONS_FIELDS = Set.of(
            // Occupation
            "occupationStatus", "occupationTitle", "companyName", "companyAddress1", "companyAddress2",
            "companyCity", "companyState", "companyZip", "companyPhone", "companyEmail",
            // Personal
            "maritalStatus",
            // Travel
            "travelDateFrom", "travelDateTo", "primaryDestination", "fingerprintsTaken",
            // Financial
            "hasCreditCard", "travelCoveredBy",
            // Accommodation
            "hasStayBooking", "hotelName", "hotelAddress1", "hotelAddress2",
            "hotelCity", "hotelState", "hotelZip", "hotelContactNumber", "hotelEmail",
            "hotelBookingReference", "hotelWebsite", "stayType",
            // Business
            "invitingCompanyName", "invitingCompanyContactPerson", "invitingCompanyPhone",
            "invitingCompanyAddress1", "invitingCompanyAddress2", "invitingCompanyCity",
            "invitingCompanyState", "invitingCompanyZip",
            // Family/Friends
            "invitingPersonFirstName", "invitingPersonSurname", "invitingPersonEmail",
            "invitingPersonPhone", "invitingPersonPhoneCode", "invitingPersonRelationship",
            "invitingPersonAddress1", "invitingPersonAddress2", "invitingPersonCity",
            "invitingPersonState", "invitingPersonZip",
            // Bookings
            "hasBookings",
            // eVisa
            "evisaIssueDate", "evisaExpiryDate", "evisaNoDateSettled", "evisaDocument",
            "schengenVisaImage",
            // Share Code
            "shareCode", "shareCodeExpiryDate", "shareCodeDocument",
            // Documents
            "bookingDocument", "passportFront", "passportBack", "additionalNotes",
            // Progress
            "progressPercentage", "formComplete", "agreedToTerms");

    private static final Set<String> DATE_FIELDS = Set.of(
            "dob", "passportIssue", "passportExpire", "plannedTravelDate", "docDate");

    @Transactional
    public Long createTraveler() {
        System.out.println(">>> CHECKPOINT: createTraveler() called <<<");
        String username = getCurrentUsername();
        System.out.println(">>> CHECKPOINT: User identified as: " + username + " <<<");

        Traveler traveler = Traveler.builder()
                .name("Full Name")
                .firstName("")
                .lastName("")
                .priority("Normal")
                .status("Wait App")
                .createdByUsername(username)
                .build();

        traveler = travelerRepository.save(traveler);

        auditService.logChange("traveler", traveler.getId(), traveler.getName(),
                "Created Traveler", "", "New Record");

        return traveler.getId();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TravelerDto>> getAllTravelers(int page, int limit, boolean summary) {
        // Correct Sorting: createdAt DESC (primary), id DESC (tie-breaker)
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        if (summary) {
            // Fast path: use projection
            Page<uk.co.visad.repository.TravelerSummaryProjection> summaryPage = travelerRepository
                    .findAllProjectedBy(pageable);

            List<TravelerDto> dtos = summaryPage.getContent().stream()
                    .map(this::mapProjectionToDto)
                    .collect(Collectors.toList());

            ApiResponse.PaginationInfo pagination = ApiResponse.PaginationInfo.builder()
                    .page(page)
                    .limit(limit)
                    .totalRecords(summaryPage.getTotalElements())
                    .totalPages(summaryPage.getTotalPages())
                    .hasMore(page < summaryPage.getTotalPages())
                    .build();

            return ApiResponse.success(dtos, pagination);
        }

        // Full fetch: Two-Query Pattern (Critical Optimization)
        // 1. Fetch paginated Travelers ONLY (No Joins)
        Page<Traveler> travelerPage = travelerRepository.findAll(pageable);

        List<Traveler> travelers = travelerPage.getContent();
        List<Long> travelerIds = travelers.stream()
                .map(Traveler::getId)
                .collect(Collectors.toList());

        // 2 & 3. Batch Fetch: Dependents and Questions (Partitioned)
        Map<Long, List<Dependent>> dependentsMap = new HashMap<>();
        Map<Long, TravelerQuestions> questionsMap = new HashMap<>();
        Map<Long, TravelerQuestions> dependentQuestionsMap = new HashMap<>();

        if (!travelerIds.isEmpty()) {
            final int batchSize = 500;
            // Manual partitioning to avoid Guava dependency if not present
            for (int i = 0; i < travelerIds.size(); i += batchSize) {
                List<Long> batchIds = travelerIds.subList(i, Math.min(i + batchSize, travelerIds.size()));

                // Fetch Dependents for batch
                dependentRepository.findAllByTraveler_IdIn(batchIds)
                        .forEach(d -> dependentsMap.computeIfAbsent(d.getTraveler().getId(), k -> new ArrayList<>())
                                .add(d));

                // Fetch Traveler Questions for batch
                List<TravelerQuestions> batchQuestions = travelerQuestionsRepository
                        .findAllByRecordIdInAndRecordType(batchIds, "traveler");
                for (TravelerQuestions tq : batchQuestions) {
                    questionsMap.put(tq.getRecordId(), tq);
                }
            }

            // Fetch Dependent Questions (for deep tree)
            List<Dependent> allFetchedDependents = dependentsMap.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            List<Long> allDependentIds = allFetchedDependents.stream()
                    .map(Dependent::getId)
                    .collect(Collectors.toList());

            if (!allDependentIds.isEmpty()) {
                for (int i = 0; i < allDependentIds.size(); i += batchSize) {
                    List<Long> batchResultIds = allDependentIds.subList(i,
                            Math.min(i + batchSize, allDependentIds.size()));

                    travelerQuestionsRepository
                            .findAllByRecordIdInAndRecordType(batchResultIds, "dependent")
                            .forEach(tq -> dependentQuestionsMap.put(tq.getRecordId(), tq));
                }
            }
        }

        final Map<Long, List<Dependent>> finalDependentsMap = dependentsMap;
        final Map<Long, TravelerQuestions> finalDependentQuestionsMap = dependentQuestionsMap;

        // 4. In-Memory Stitching
        List<TravelerDto> dtos = travelers.stream()
                .map(t -> {

                    List<Dependent> myDependents = finalDependentsMap.getOrDefault(t.getId(),
                            new java.util.ArrayList<>());
                    return mapToDtoOptimized(t, myDependents, questionsMap.get(t.getId()), finalDependentQuestionsMap);
                })
                .collect(Collectors.toList());

        ApiResponse.PaginationInfo pagination = ApiResponse.PaginationInfo.builder()
                .page(page)
                .limit(limit)
                .totalRecords(travelerPage.getTotalElements())
                .totalPages(travelerPage.getTotalPages())
                .hasMore(page < travelerPage.getTotalPages())
                .build();

        return ApiResponse.success(dtos, pagination);
    }

    private TravelerDto mapProjectionToDto(uk.co.visad.repository.TravelerSummaryProjection p) {
        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

        String fullName = p.getName();
        if ((fullName == null || fullName.isEmpty()) && p.getFirstName() != null) {
            fullName = p.getFirstName() + (p.getLastName() != null ? " " + p.getLastName() : "");
        }

        return TravelerDto.builder()
                .id(p.getId())
                .name(fullName)
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .title(p.getTitle())
                .passportNo(p.getPassportNo())
                .travelCountry(p.getTravelCountry())
                .visaType(p.getVisaType())
                .visaCenter(p.getVisaCenter())
                .status(p.getStatus())
                .priority(p.getPriority())
                .paymentStatus(p.getPaymentStatus())
                .plannedTravelDate(p.getPlannedTravelDate())
                .plannedTravelDateRaw(p.getPlannedTravelDate())
                .createdByUsername(p.getCreatedByUsername())
                .email(p.getEmail())
                .contactNumber(p.getContactNumber())
                .createdAtFormatted(p.getCreatedAt() != null ? p.getCreatedAt().format(displayFormat) : null)
                .lastUpdatedAtFormatted(
                        p.getLastUpdatedAt() != null ? p.getLastUpdatedAt().format(displayFormat) : null)
                .createdAt(p.getCreatedAt())
                .lastUpdatedAt(p.getLastUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public TravelerDto getTravelerById(Long id) {
        Traveler traveler = travelerRepository.findByIdWithDependents(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        TravelerQuestions questions = travelerQuestionsRepository
                .findByRecordIdAndRecordType(id, "traveler")
                .orElse(null);

        return mapToDto(traveler, traveler.getDependents(), questions);
    }

    @Transactional
    public void updateField(Long id, String field, String value) {
        // Convert camelCase to snake_case field name if needed
        String javaField = convertToJavaFieldName(field);

        if (QUESTIONS_FIELDS.contains(javaField)) {
            updateQuestionField(id, field, value);
            return;
        }

        if (!ALLOWED_FIELDS.contains(javaField)) {
            throw new BadRequestException("Invalid field: " + field);
        }

        Traveler traveler = travelerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        String oldValue = getFieldValue(traveler, javaField);

        // Handle special fields
        if ("plannedTravelDate".equals(javaField)) {
            updateTravelerQuestionsDate(id, "traveler", value);
        } else {
            setFieldValue(traveler, javaField, value);
        }

        traveler.setLastUpdatedByUsername(getCurrentUsername());
        traveler.setLastUpdatedAt(LocalDateTime.now());
        travelerRepository.save(traveler);

        // Update visa link if country or center changed
        if ("travelCountry".equals(javaField) || "visaCenter".equals(javaField)) {
            updateVisaLink(traveler);
        }

        // Sync family address if needed
        if (Boolean.TRUE.equals(traveler.getIsFamily()) &&
                (javaField.startsWith("address") || "city".equals(javaField) ||
                        "stateProvince".equals(javaField) || "zipCode".equals(javaField)
                        || "country".equals(javaField))) {
            syncFamilyAddress(traveler);
        }

        if (!Objects.equals(oldValue, value)) {
            auditService.logChange("traveler", id, traveler.getName(), field, oldValue, value);
        }
        
        // Broadcast the real-time update
        broadcastDataUpdate(id, field, value);
    }

    @Transactional
    public void updateFields(Long id, Map<String, Object> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new BadRequestException("No updates provided");
        }

        Traveler traveler = travelerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        boolean visaLinkNeedsUpdate = false;
        boolean addressNeedsSync = false;

        // Process all updates
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";

            String javaField = convertToJavaFieldName(field);

            // Check if field belongs to questions table
            if (QUESTIONS_FIELDS.contains(javaField)) {
                // Update in questions table
                updateQuestionField(id, field, value);
                continue;
            }

            if (!ALLOWED_FIELDS.contains(javaField)) {
                throw new BadRequestException("Invalid field: " + field);
            }

            String oldValue = getFieldValue(traveler, javaField);

            // Handle special fields
            if ("plannedTravelDate".equals(javaField)) {
                updateTravelerQuestionsDate(id, "traveler", value);
            } else {
                setFieldValue(traveler, javaField, value);
            }

            // Track if visa link or address sync needed
            if ("travelCountry".equals(javaField) || "visaCenter".equals(javaField)) {
                visaLinkNeedsUpdate = true;
            }

            if (Boolean.TRUE.equals(traveler.getIsFamily()) &&
                    (javaField.startsWith("address") || "city".equals(javaField) ||
                            "stateProvince".equals(javaField) || "zipCode".equals(javaField)
                            || "country".equals(javaField))) {
                addressNeedsSync = true;
            }

            // Log change
            if (!Objects.equals(oldValue, value)) {
                auditService.logChange("traveler", id, traveler.getName(), field, oldValue, value);
            }
            
            // Broadcast the real-time update
            broadcastDataUpdate(id, field, value);
        }

        // Update metadata
        traveler.setLastUpdatedByUsername(getCurrentUsername());
        traveler.setLastUpdatedAt(LocalDateTime.now());
        travelerRepository.save(traveler);

        // Perform post-update actions
        if (visaLinkNeedsUpdate) {
            updateVisaLink(traveler);
        }

        if (addressNeedsSync) {
            syncFamilyAddress(traveler);
        }
    }

    @Transactional
    public void deleteTraveler(Long id) {
        Traveler traveler = travelerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        String name = traveler.getName();

        // Delete dependents first
        dependentRepository.deleteByTraveler_Id(id);

        // Delete traveler
        travelerRepository.delete(traveler);

        auditService.logChange("traveler", id, name, "Deleted Traveler", "Exists", "Deleted");
    }

    @Transactional(readOnly = true)
    public TravelerDto findByPassport(String passportNo) {
        // Search in travelers
        Optional<Traveler> traveler = travelerRepository.findByPassportNo(passportNo);
        if (traveler.isPresent()) {
            Traveler t = traveler.get();
            TravelerQuestions questions = travelerQuestionsRepository
                    .findByRecordIdAndRecordType(t.getId(), "traveler")
                    .orElse(null);
            return mapToDto(t, null, questions);
        }

        // Search in dependents
        Optional<Dependent> dependent = dependentRepository.findByPassportNo(passportNo);
        if (dependent.isPresent()) {
            // Return as a DTO (simplified)
            return TravelerDto.builder()
                    .id(dependent.get().getId())
                    .passportNo(dependent.get().getPassportNo())
                    .firstName(dependent.get().getFirstName())
                    .lastName(dependent.get().getLastName())
                    .build();
        }

        return null;
    }

    @Transactional
    public void setLockStatus(Long id, boolean locked) {
        Traveler traveler = travelerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        TravelerQuestions tq = travelerQuestionsRepository
                .findByRecordIdAndRecordType(id, "traveler")
                .orElseGet(() -> {
                    TravelerQuestions newTq = new TravelerQuestions();
                    newTq.setRecordId(id);
                    newTq.setRecordType("traveler");
                    return newTq;
                });

        boolean oldLocked = Boolean.TRUE.equals(tq.getFormComplete());
        tq.setFormComplete(locked);
        travelerQuestionsRepository.save(tq);

        auditService.logChange("traveler", id, traveler.getName(),
                "Form Lock Status",
                oldLocked ? "Locked" : "Unlocked",
                locked ? "Locked" : "Unlocked");
    }

    @Transactional
    public TravelerDto.InvoiceDto saveInvoice(TravelerDto.SaveInvoiceRequest request) {
        Traveler traveler = travelerRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        traveler.setInvoiceSubtotal(request.getSubtotal());
        traveler.setInvoiceDiscountType(request.getDiscountType());
        traveler.setInvoiceDiscountValue(request.getDiscountValue());
        traveler.setInvoiceDiscountAmount(request.getDiscountAmount());
        traveler.setInvoiceTotal(request.getTotal());
        traveler.setInvoiceItemsJson(request.getItemsJson());
        traveler.setInvoiceGenerated(true);
        traveler.setInvoiceGeneratedAt(LocalDateTime.now());
        traveler.setDiscountType(request.getDiscountType());
        traveler.setDiscountValue(request.getDiscountValue());

        travelerRepository.save(traveler);

        return TravelerDto.InvoiceDto.builder()
                .id(traveler.getId())
                .travelerId(traveler.getId())
                .invoiceNumber(traveler.getInvoiceNumber())
                .subtotal(traveler.getInvoiceSubtotal())
                .discountType(traveler.getInvoiceDiscountType())
                .discountValue(traveler.getInvoiceDiscountValue())
                .discountAmount(traveler.getInvoiceDiscountAmount())
                .total(traveler.getInvoiceTotal())
                .build();
    }

    @Transactional(readOnly = true)
    public TravelerDto.InvoiceDto getInvoice(Long travelerId) {
        Traveler traveler = travelerRepository.findById(travelerId)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));

        if (!Boolean.TRUE.equals(traveler.getInvoiceGenerated())) {
            throw new ResourceNotFoundException("Invoice not found");
        }

        return TravelerDto.InvoiceDto.builder()
                .id(traveler.getId())
                .travelerId(travelerId)
                .invoiceNumber(traveler.getInvoiceNumber())
                .subtotal(traveler.getInvoiceSubtotal())
                .discountType(traveler.getInvoiceDiscountType())
                .discountValue(traveler.getInvoiceDiscountValue())
                .discountAmount(traveler.getInvoiceDiscountAmount())
                .total(traveler.getInvoiceTotal())
                .itemsJson(traveler.getInvoiceItemsJson())
                .createdAt(traveler.getInvoiceGeneratedAt())
                .build();
    }

    /**
     * Save invoices for all travelers that don't have saved invoices yet
     * PHP equivalent: travelers.php?action=save_all_invoices
     */
    @Transactional
    public Map<String, Object> saveAllInvoices() {
        // Find all travelers without saved invoices
        List<Traveler> travelersWithoutInvoices = travelerRepository.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getInvoiceGenerated()))
                .collect(Collectors.toList());

        int savedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Traveler traveler : travelersWithoutInvoices) {
            try {
                // Get dependents for this traveler
                List<Dependent> dependents = dependentRepository.findByTraveler_Id(traveler.getId());

                // Calculate invoice data
                BigDecimal subtotal = BigDecimal.ZERO;
                List<Map<String, Object>> items = new ArrayList<>();

                // Add main traveler
                BigDecimal mainPrice = traveler.getPrice() != null ? traveler.getPrice() : BigDecimal.ZERO;
                subtotal = subtotal.add(mainPrice);
                items.add(Map.of(
                        "type", "main",
                        "id", traveler.getId(),
                        "name", (traveler.getFirstName() != null ? traveler.getFirstName() : "") + " " +
                                (traveler.getLastName() != null ? traveler.getLastName() : ""),
                        "service", traveler.getPackage_() != null ? traveler.getPackage_() : "Full Support",
                        "price", mainPrice));

                // Add dependents
                for (Dependent dep : dependents) {
                    BigDecimal depPrice = dep.getPrice() != null ? dep.getPrice() : BigDecimal.ZERO;
                    subtotal = subtotal.add(depPrice);
                    items.add(Map.of(
                            "type", "co-traveler",
                            "id", dep.getId(),
                            "name", (dep.getFirstName() != null ? dep.getFirstName() : "") + " " +
                                    (dep.getLastName() != null ? dep.getLastName() : ""),
                            "service", dep.getPackageType() != null ? dep.getPackageType() : "Full Support",
                            "price", depPrice));
                }

                // Calculate discount
                BigDecimal discountAmount = BigDecimal.ZERO;
                String discountType = traveler.getDiscountType() != null ? traveler.getDiscountType() : "none";
                BigDecimal discountValue = traveler.getDiscountValue() != null ? traveler.getDiscountValue()
                        : BigDecimal.ZERO;

                if ("percentage".equals(discountType) && discountValue.compareTo(BigDecimal.ZERO) > 0) {
                    discountAmount = subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 2,
                            BigDecimal.ROUND_HALF_UP);
                } else if ("fixed".equals(discountType)) {
                    discountAmount = discountValue;
                }

                BigDecimal total = subtotal.subtract(discountAmount);

                // Convert items to JSON
                String itemsJson;
                try {
                    itemsJson = objectMapper.writeValueAsString(items);
                } catch (Exception e) {
                    log.error("Failed to serialize invoice items for traveler {}", traveler.getId(), e);
                    continue;
                }

                // Save invoice data
                traveler.setInvoiceSubtotal(subtotal);
                traveler.setInvoiceDiscountType(discountType);
                traveler.setInvoiceDiscountValue(discountValue);
                traveler.setInvoiceDiscountAmount(discountAmount);
                traveler.setInvoiceTotal(total);
                traveler.setInvoiceItemsJson(itemsJson);
                traveler.setInvoiceGenerated(true);
                traveler.setInvoiceGeneratedAt(now);

                travelerRepository.save(traveler);
                savedCount++;

            } catch (Exception e) {
                log.error("Failed to save invoice for traveler {}", traveler.getId(), e);
            }
        }

        return Map.of(
                "saved_count", savedCount,
                "message", "Saved " + savedCount + " invoices successfully");
    }

    // Helper methods
    private void updateVisaLink(Traveler traveler) {
        String country = traveler.getTravelCountry();
        String center = traveler.getVisaCenter();

        if (country != null) {
            country = country.split(" - ")[0];
        }
        if (center != null) {
            center = center.split(" - ")[0];
        }

        String url = "";
        String appFormUrl = "";

        // Try specific match first
        if (country != null && center != null && !center.isEmpty()) {
            Optional<VisaUrl> specific = visaUrlRepository.findByCountryAndVisaCenter(country, center);
            if (specific.isPresent()) {
                url = specific.get().getUrl();
                appFormUrl = specific.get().getApplicationFormUrl();
            }
        }

        // Try general match
        if (url.isEmpty() && country != null) {
            Optional<VisaUrl> general = visaUrlRepository.findByCountryWithNoVisaCenter(country);
            if (general.isPresent()) {
                url = general.get().getUrl();
                appFormUrl = general.get().getApplicationFormUrl();
            }
        }

        traveler.setVisaLink(url);
        traveler.setApplicationFormLink(appFormUrl);
        travelerRepository.save(traveler);
    }

    private void syncFamilyAddress(Traveler traveler) {
        List<Dependent> dependents = dependentRepository.findByTraveler_Id(traveler.getId());
        for (Dependent dep : dependents) {
            dep.setAddressLine1(traveler.getAddressLine1());
            dep.setAddressLine2(traveler.getAddressLine2());
            dep.setCity(traveler.getCity());
            dep.setStateProvince(traveler.getStateProvince());
            dep.setZipCode(traveler.getZipCode());
            dep.setCountry(traveler.getCountry());
        }
        dependentRepository.saveAll(dependents);
    }

    private void updateTravelerQuestionsDate(Long recordId, String recordType, String value) {
        TravelerQuestions tq = travelerQuestionsRepository
                .findByRecordIdAndRecordType(recordId, recordType)
                .orElseGet(() -> {
                    TravelerQuestions newTq = new TravelerQuestions();
                    newTq.setRecordId(recordId);
                    newTq.setRecordType(recordType);
                    return newTq;
                });

        LocalDate date = parseDate(value);
        tq.setTravelDateFrom(date);
        travelerQuestionsRepository.save(tq);
    }

    private void broadcastDataUpdate(Long travelerId, String field, Object value) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "DATA_UPDATE");
            update.put("targetType", "TRAVELER");
            update.put("targetId", travelerId);
            update.put("field", field);
            update.put("value", value);
            update.put("updatedBy", getCurrentUsername());
            
            messagingTemplate.convertAndSend("/topic/presence", update);
        } catch (Exception e) {
            log.error("Failed to broadcast data update for traveler {}", travelerId, e);
        }
    }

    private TravelerDto mapToDto(Traveler t, List<Dependent> dependents, TravelerQuestions questions) {
        // For single traveler fetch, we can lazy load dependent questions or fetch them
        // if needed.
        // But mapToDtoOptimized now expects a Map.
        Map<Long, TravelerQuestions> depQuestionsMap = new HashMap<>();
        if (dependents != null && !dependents.isEmpty()) {
            List<Long> depIds = dependents.stream().map(Dependent::getId).collect(Collectors.toList());
            if (!depIds.isEmpty()) {
                depQuestionsMap = travelerQuestionsRepository
                        .findAllByRecordIdInAndRecordType(depIds, "dependent")
                        .stream()
                        .collect(Collectors.toMap(TravelerQuestions::getRecordId, q -> q));
            }
        }
        return mapToDtoOptimized(t, dependents, questions, depQuestionsMap);
    }

    private TravelerDto mapToDtoOptimized(Traveler t, List<Dependent> dependents, TravelerQuestions questions,
            Map<Long, TravelerQuestions> dependentQuestionsMap) {

        TravelerDto.InvoiceDto savedInvoice = null;
        if (Boolean.TRUE.equals(t.getInvoiceGenerated())) {
            savedInvoice = TravelerDto.InvoiceDto.builder()
                    .id(t.getId())
                    .travelerId(t.getId())
                    .invoiceNumber(t.getInvoiceNumber())
                    .subtotal(t.getInvoiceSubtotal())
                    .discountType(t.getInvoiceDiscountType())
                    .discountValue(t.getInvoiceDiscountValue())
                    .discountAmount(t.getInvoiceDiscountAmount())
                    .total(t.getInvoiceTotal())
                    .itemsJson(t.getInvoiceItemsJson())
                    .createdAt(t.getInvoiceGeneratedAt())
                    .build();
        }

        List<DependentDto> depDtos = null;
        if (dependents != null) {
            depDtos = dependents.stream()
                    .map(d -> mapDependentToDto(d,
                            dependentQuestionsMap != null ? dependentQuestionsMap.get(d.getId()) : null))
                    .collect(Collectors.toList());
        }

        // Get progress from traveler questions if available
        Integer progress = null;
        LocalDate travelDateFromQuestions = null;
        String occupationStatus = null;
        String occupationTitle = null;
        String companyName = null;
        String companyAddress1 = null;
        String companyAddress2 = null;
        String companyCity = null;
        String companyState = null;
        String companyZip = null;
        String companyPhone = null;
        String companyEmail = null;
        String maritalStatus = null;
        LocalDate travelDateTo = null;
        String primaryDestination = null;
        String fingerprintsTaken = null;
        String hasCreditCard = null;
        String travelCoveredBy = null;
        String hasStayBooking = null;
        String hotelName = null;
        String hotelAddress1 = null;
        String hotelAddress2 = null;
        String hotelCity = null;
        String hotelState = null;
        String hotelZip = null;
        String hotelContactNumber = null;
        String hotelEmail = null;
        String hotelBookingReference = null;
        String hotelWebsite = null;
        String stayType = null;
        String invitingCompanyName = null;
        String invitingCompanyContactPerson = null;
        String invitingCompanyPhone = null;
        String invitingCompanyAddress1 = null;
        String invitingCompanyAddress2 = null;
        String invitingCompanyCity = null;
        String invitingCompanyState = null;
        String invitingCompanyZip = null;
        String invitingPersonFirstName = null;
        String invitingPersonSurname = null;
        String invitingPersonEmail = null;
        String invitingPersonPhone = null;
        String invitingPersonPhoneCode = null;
        String invitingPersonRelationship = null;
        String invitingPersonAddress1 = null;
        String invitingPersonAddress2 = null;
        String invitingPersonCity = null;
        String invitingPersonState = null;
        String invitingPersonZip = null;
        String hasBookings = null;
        LocalDate evisaIssueDate = null;
        LocalDate evisaExpiryDate = null;
        String evisaNoDateSettled = null;
        String evisaDocument = null;
        String schengenVisaImage = null;
        String shareCode = null;
        LocalDate shareCodeExpiryDate = null;
        String shareCodeDocument = null;
        String bookingDocument = null;
        String passportFront = null;
        String passportBack = null;
        String additionalNotes = null;
        Boolean agreedToTerms = null;
        Boolean formComplete = null;

        if (questions != null) {
            progress = questions.getProgressPercentage();
            travelDateFromQuestions = questions.getTravelDateFrom();
            occupationStatus = questions.getOccupationStatus();
            occupationTitle = questions.getOccupationTitle();
            companyName = questions.getCompanyName();
            companyAddress1 = questions.getCompanyAddress1();
            companyAddress2 = questions.getCompanyAddress2();
            companyCity = questions.getCompanyCity();
            companyState = questions.getCompanyState();
            companyZip = questions.getCompanyZip();
            companyPhone = questions.getCompanyPhone();
            companyEmail = questions.getCompanyEmail();
            maritalStatus = questions.getMaritalStatus();
            travelDateTo = questions.getTravelDateTo();
            primaryDestination = questions.getPrimaryDestination();
            fingerprintsTaken = questions.getFingerprintsTaken();
            schengenVisaImage = questions.getSchengenVisaImage();
            hasCreditCard = questions.getHasCreditCard();
            travelCoveredBy = questions.getTravelCoveredBy();
            hasStayBooking = questions.getHasStayBooking();
            hotelName = questions.getHotelName();
            hotelAddress1 = questions.getHotelAddress1();
            hotelAddress2 = questions.getHotelAddress2();
            hotelCity = questions.getHotelCity();
            hotelState = questions.getHotelState();
            hotelZip = questions.getHotelZip();
            hotelContactNumber = questions.getHotelContactNumber();
            hotelEmail = questions.getHotelEmail();
            hotelBookingReference = questions.getHotelBookingReference();
            hotelWebsite = questions.getHotelWebsite();
            stayType = questions.getStayType();
            invitingCompanyName = questions.getInvitingCompanyName();
            invitingCompanyContactPerson = questions.getInvitingCompanyContactPerson();
            invitingCompanyPhone = questions.getInvitingCompanyPhone();
            invitingCompanyAddress1 = questions.getInvitingCompanyAddress1();
            invitingCompanyAddress2 = questions.getInvitingCompanyAddress2();
            invitingCompanyCity = questions.getInvitingCompanyCity();
            invitingCompanyState = questions.getInvitingCompanyState();
            invitingCompanyZip = questions.getInvitingCompanyZip();
            invitingPersonFirstName = questions.getInvitingPersonFirstName();
            invitingPersonSurname = questions.getInvitingPersonSurname();
            invitingPersonEmail = questions.getInvitingPersonEmail();
            invitingPersonPhone = questions.getInvitingPersonPhone();
            invitingPersonPhoneCode = questions.getInvitingPersonPhoneCode();
            invitingPersonRelationship = questions.getInvitingPersonRelationship();
            invitingPersonAddress1 = questions.getInvitingPersonAddress1();
            invitingPersonAddress2 = questions.getInvitingPersonAddress2();
            invitingPersonCity = questions.getInvitingPersonCity();
            invitingPersonState = questions.getInvitingPersonState();
            invitingPersonZip = questions.getInvitingPersonZip();
            hasBookings = questions.getHasBookings();
            evisaIssueDate = questions.getEvisaIssueDate();
            evisaExpiryDate = questions.getEvisaExpiryDate();
            evisaNoDateSettled = questions.getEvisaNoDateSettled();
            evisaDocument = questions.getEvisaDocument();
            shareCode = questions.getShareCode();
            shareCodeExpiryDate = questions.getShareCodeExpiryDate();
            shareCodeDocument = questions.getShareCodeDocument();
            bookingDocument = questions.getBookingDocument();
            passportFront = questions.getPassportFront();
            passportBack = questions.getPassportBack();
            additionalNotes = questions.getAdditionalNotes();
            agreedToTerms = questions.getAgreedToTerms();
            formComplete = questions.getFormComplete();
        }

        // Generate Links
        String token = t.getPublicUrlToken();
        List<String> evisaLinks = generateFileLinks(evisaDocument, token);
        List<String> shareCodeLinks = generateFileLinks(shareCodeDocument, token);
        List<String> bookingLinks = generateFileLinks(bookingDocument, token);
        List<String> passportFrontLinks = generateFileLinks(passportFront, token);
        List<String> passportBackLinks = generateFileLinks(passportBack, token);

        LocalDate plannedTravelDate = travelDateFromQuestions != null ? travelDateFromQuestions
                : t.getPlannedTravelDate();

        // Check if details are verified based on notes
        boolean detailsVerified = t.getNotes() != null && t.getNotes().contains("VERIFIED");

        return TravelerDto.builder().id(t.getId()).invoiceNumber(t.getInvoiceNumber()).name(t.getName()).firstName(t.getFirstName())
                .lastName(t.getLastName()).detailsVerified(detailsVerified).title(t.getTitle()).gender(t.getGender())
                .dob(t.getDob()).placeOfBirth(t.getPlaceOfBirth()).countryOfBirth(t.getCountryOfBirth())
                .nationality(t.getNationality()).email(t.getEmail()).contactNumber(t.getContactNumber())
                .whatsappContact(t.getWhatsappContact()).addressLine1(t.getAddressLine1())
                .addressLine2(t.getAddressLine2()).city(t.getCity()).stateProvince(t.getStateProvince())
                .zipCode(t.getZipCode()).country(t.getCountry()).passportNo(t.getPassportNo())
                .passportIssue(t.getPassportIssue()).passportExpire(t.getPassportExpire())
                .travelCountry(t.getTravelCountry()).visaCenter(t.getVisaCenter()).visaType(t.getVisaType())
                .visaLink(t.getVisaLink()).applicationFormLink(t.getApplicationFormLink())
                .applicationFormUsername(t.getApplicationFormUsername())
                .applicationFormPassword(t.getApplicationFormPassword()).packageType(t.getPackage_())
                .status(t.getStatus()).priority(t.getPriority()).paymentStatus(t.getPaymentStatus())
                .plannedTravelDate(plannedTravelDate)
                // Map extended fields
                .progressPercentage(progress).occupationStatus(occupationStatus).occupationTitle(occupationTitle)
                .companyName(companyName).companyAddress1(companyAddress1).companyAddress2(companyAddress2)
                .companyCity(companyCity).companyState(companyState).companyZip(companyZip).companyPhone(companyPhone)
                .companyEmail(companyEmail).maritalStatus(maritalStatus).travelDateFrom(plannedTravelDate) // Map
                                                                                                           // plannedTravelDate
                                                                                                           // to
                                                                                                           // travelDateFrom
                                                                                                           // as
                                                                                                           // well
                .travelDateTo(travelDateTo).primaryDestination(primaryDestination).fingerprintsTaken(fingerprintsTaken)
                .hasCreditCard(hasCreditCard).travelCoveredBy(travelCoveredBy).hasStayBooking(hasStayBooking)
                .hotelName(hotelName).hotelAddress1(hotelAddress1).hotelAddress2(hotelAddress2).hotelCity(hotelCity)
                .hotelState(hotelState).hotelZip(hotelZip).hotelContactNumber(hotelContactNumber).hotelEmail(hotelEmail)
                .hotelBookingReference(hotelBookingReference).hotelWebsite(hotelWebsite)
                .stayType(stayType).invitingCompanyName(invitingCompanyName)
                .invitingCompanyContactPerson(invitingCompanyContactPerson).invitingCompanyPhone(invitingCompanyPhone)
                .invitingCompanyAddress1(invitingCompanyAddress1).invitingCompanyAddress2(invitingCompanyAddress2)
                .invitingCompanyCity(invitingCompanyCity).invitingCompanyState(invitingCompanyState)
                .invitingCompanyZip(invitingCompanyZip)
                .invitingPersonFirstName(invitingPersonFirstName).invitingPersonSurname(invitingPersonSurname)
                .invitingPersonEmail(invitingPersonEmail).invitingPersonPhone(invitingPersonPhone)
                .invitingPersonPhoneCode(invitingPersonPhoneCode).invitingPersonRelationship(invitingPersonRelationship)
                .invitingPersonAddress1(invitingPersonAddress1).invitingPersonAddress2(invitingPersonAddress2)
                .invitingPersonCity(invitingPersonCity).invitingPersonState(invitingPersonState)
                .invitingPersonZip(invitingPersonZip)
                .hasBookings(hasBookings)
                .evisaIssueDate(evisaIssueDate).evisaExpiryDate(evisaExpiryDate).evisaNoDateSettled(evisaNoDateSettled)
                .evisaDocument(evisaDocument).schengenVisaImage(schengenVisaImage).evisaDocumentLinks(evisaLinks)
                .shareCode(shareCode)
                .shareCodeExpiryDate(shareCodeExpiryDate).shareCodeDocument(shareCodeDocument)
                .shareCodeExpiryDate(shareCodeExpiryDate).shareCodeDocument(shareCodeDocument)
                .shareCodeDocumentLinks(shareCodeLinks).bookingDocument(bookingDocument)
                .bookingDocumentLinks(bookingLinks).passportFront(passportFront).passportFrontLinks(passportFrontLinks)
                .passportBack(passportBack).passportBackLinks(passportBackLinks).additionalNotes(additionalNotes)
                .agreedToTerms(agreedToTerms).formComplete(formComplete).plannedTravelDateRaw(plannedTravelDate)
                .docDate(t.getDocDate()).docDateRaw(t.getDocDate()).note(t.getNote()).notes(t.getNotes())
                .appointmentRemarks(t.getAppointmentRemarks()).username(t.getUsername()).logins(t.getLogins())
                .isFamily(t.getIsFamily()).relationshipToMain(t.getRelationshipToMain())
                .publicUrlToken(t.getPublicUrlToken()).price(t.getPrice()).discountType(t.getDiscountType())
                .discountValue(t.getDiscountValue()).refundAmount(t.getRefundAmount()).progressPercentage(progress)
                .savedInvoice(savedInvoice).createdByUsername(t.getCreatedByUsername())
                .lastUpdatedByUsername(t.getLastUpdatedByUsername())
                .createdAtFormatted(t.getCreatedAt() != null ? t.getCreatedAt().format(DISPLAY_FORMAT) : null)
                .lastUpdatedAtFormatted(
                        t.getLastUpdatedAt() != null ? t.getLastUpdatedAt().format(DISPLAY_FORMAT) : null)
                .createdAt(t.getCreatedAt()).lastUpdatedAt(t.getLastUpdatedAt()).dependents(depDtos).build();
    }

    private DependentDto mapDependentToDto(Dependent d, TravelerQuestions questions) {

        return DependentDto.builder()
                .id(d.getId())
                .travelerId(d.getTravelerId())
                .name(d.getName())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .title(d.getTitle())
                .gender(d.getGender())
                .dob(d.getDob())
                .placeOfBirth(d.getPlaceOfBirth())
                .countryOfBirth(d.getCountryOfBirth())
                .nationality(d.getNationality())
                .relationshipToMain(d.getRelationshipToMain())
                .email(d.getEmail())
                .contactNumber(d.getContactNumber())
                .whatsappContact(d.getWhatsappContact())
                .addressLine1(d.getAddressLine1())
                .addressLine2(d.getAddressLine2())
                .city(d.getCity())
                .stateProvince(d.getStateProvince())
                .zipCode(d.getZipCode())
                .country(d.getCountry())
                .passportNo(d.getPassportNo())
                .passportIssue(d.getPassportIssue())
                .passportExpire(d.getPassportExpire())
                .travelCountry(d.getTravelCountry())
                .visaCenter(d.getVisaCenter())
                .visaType(d.getVisaType())
                .visaLink(d.getVisaLink())
                .applicationFormLink(d.getApplicationFormLink())
                .applicationFormUsername(d.getApplicationFormUsername())
                .applicationFormPassword(d.getApplicationFormPassword())
                .packageType(d.getPackageType())
                .status(d.getStatus())
                .priority(d.getPriority())
                .paymentStatus(d.getPaymentStatus())
                .plannedTravelDate(d.getPlannedTravelDate())
                .plannedTravelDateRaw(d.getPlannedTravelDate())
                .docDate(d.getDocDate())
                .docDateRaw(d.getDocDate())
                .note(d.getNote())
                .notes(d.getNotes())
                .appointmentRemarks(d.getAppointmentRemarks())
                .username(d.getUsername())
                .logins(d.getLogins())
                .logins(d.getLogins())
                .schengenVisaImage(questions != null ? questions.getSchengenVisaImage() : null)
                .publicUrlToken(d.getPublicUrlToken())
                .price(d.getPrice())
                .createdByUsername(d.getCreatedByUsername())
                .lastUpdatedByUsername(d.getLastUpdatedByUsername())
                .createdAtFormatted(d.getCreatedAt() != null ? d.getCreatedAt().format(DISPLAY_FORMAT) : null)
                .lastUpdatedAtFormatted(
                        d.getLastUpdatedAt() != null ? d.getLastUpdatedAt().format(DISPLAY_FORMAT) : null)
                .createdAt(d.getCreatedAt())
                .lastUpdatedAt(d.getLastUpdatedAt())
                .build();
    }

    private List<String> generateFileLinks(String jsonFiles, String token) {
        if (jsonFiles == null || jsonFiles.isEmpty() || token == null || jsonFiles.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            List<String> files;
            if (jsonFiles.trim().startsWith("[")) {
                files = objectMapper.readValue(jsonFiles, new TypeReference<List<String>>() {
                });
            } else {
                files = Collections.singletonList(jsonFiles);
            }

            return files.stream()
                    .map(file -> "/api/download_file?token=" + token + "&file=" + file)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse file list for links: {}", jsonFiles);
            return Collections.emptyList();
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getUsername();
        }
        return "system";
    }

    private String convertToJavaFieldName(String field) {
        // Validate input
        if (field == null || field.isEmpty()) {
            throw new BadRequestException("Field name cannot be null or empty");
        }

        // Convert snake_case to camelCase
        if (field.contains("_")) {
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = false;
            for (char c : field.toCharArray()) {
                if (c == '_') {
                    capitalizeNext = true;
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }
        return field;
    }

    private String getFieldValue(Traveler traveler, String fieldName) {
        try {
            Field field = Traveler.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(traveler);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void setFieldValue(Traveler traveler, String fieldName, String value) {
        try {
            Field field = Traveler.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Class<?> type = field.getType();

            if (type == String.class) {
                field.set(traveler, value);
            } else if (type == LocalDate.class) {
                field.set(traveler, parseDate(value));
            } else if (type == Boolean.class || type == boolean.class) {
                field.set(traveler, Boolean.parseBoolean(value) || "1".equals(value));
            } else if (type == BigDecimal.class) {
                field.set(traveler, value != null && !value.isEmpty() ? new BigDecimal(value) : null);
            } else if (type == Integer.class || type == int.class) {
                field.set(traveler, value != null && !value.isEmpty() ? Integer.parseInt(value) : null);
            }
        } catch (NoSuchFieldException e) {
            // Handle package field specially
            if ("package".equals(fieldName)) {
                traveler.setPackage_(value);
            }
        } catch (Exception e) {
            log.error("Error setting field {} to value {}: {}", fieldName, value, e.getMessage());
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            // Try different date formats
            if (value.contains("/")) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else {
                return LocalDate.parse(value);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void updateQuestionField(Long id, String field, String value) {
        String javaField = convertToJavaFieldName(field);

        TravelerQuestions tq = travelerQuestionsRepository
                .findByRecordIdAndRecordType(id, "traveler")
                .orElseGet(() -> {
                    TravelerQuestions newTq = new TravelerQuestions();
                    newTq.setRecordId(id);
                    newTq.setRecordType("traveler");
                    return newTq;
                });

        setObjectField(tq, javaField, value);
        travelerQuestionsRepository.save(tq);

        // Simple audit
        // auditService.logChange("traveler", id, "Traveler", "Question: " + field, "",
        // value);
    }

    @Transactional
    public String uploadQuestionFile(Long id, String category, org.springframework.web.multipart.MultipartFile file)
            throws java.io.IOException {
        // Map frontend category to entity field name
        String javaField = convertCategoryToJavaField(category);

        // Get or create TravelerQuestions
        TravelerQuestions tq = travelerQuestionsRepository
                .findByRecordIdAndRecordType(id, "traveler")
                .orElseGet(() -> {
                    TravelerQuestions newTq = new TravelerQuestions();
                    newTq.setRecordId(id);
                    newTq.setRecordType("traveler");
                    return newTq;
                });

        // Create upload directory structure:
        // uploads/documents/client_documents/YYYY/MM/
        LocalDateTime now = LocalDateTime.now();
        String yearMonth = String.format("%d/%02d", now.getYear(), now.getMonthValue());
        String uploadDir = "uploads/documents/client_documents/" + yearMonth + "/";
        java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = java.util.UUID.randomUUID().toString() + extension;
        java.nio.file.Path filePath = uploadPath.resolve(filename);

        // Save file to disk
        java.nio.file.Files.copy(file.getInputStream(), filePath);

        // Build relative path for storage
        String relativePath = yearMonth + "/" + filename;

        // Get existing files array
        String currentValue = getQuestionFieldValue(tq, javaField);
        List<String> files = new ArrayList<>();

        if (currentValue != null && !currentValue.isEmpty()) {
            try {
                if (currentValue.trim().startsWith("[")) {
                    files = objectMapper.readValue(currentValue, new TypeReference<List<String>>() {
                    });
                } else {
                    files.add(currentValue);
                }
            } catch (Exception e) {
                log.warn("Failed to parse existing files, starting fresh: {}", e.getMessage());
            }
        }

        // Add new file
        files.add(relativePath);

        // Save back as JSON
        String jsonValue = objectMapper.writeValueAsString(files);
        setObjectField(tq, javaField, jsonValue);
        travelerQuestionsRepository.save(tq);

        log.info("Uploaded file {} to field {} for traveler {}", filename, javaField, id);
        return relativePath;
    }

    private String convertCategoryToJavaField(String category) {
        // Map frontend category names to entity field names
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("evisa_document_path", "evisaDocument");
        categoryMap.put("share_code_document_path", "shareCodeDocument");
        categoryMap.put("booking_documents_path", "bookingDocument");
        categoryMap.put("schengen_visa_image", "schengenVisaImage");
        categoryMap.put("passport_front", "passportFront");
        categoryMap.put("passport_back", "passportBack");

        String javaField = categoryMap.get(category);
        if (javaField == null) {
            throw new BadRequestException("Unknown category: " + category);
        }
        return javaField;
    }

    private String getQuestionFieldValue(TravelerQuestions tq, String fieldName) {
        try {
            Field field = TravelerQuestions.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(tq);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Error getting field {} from TravelerQuestions: {}", fieldName, e.getMessage());
            return null;
        }
    }

    @Transactional
    public void deleteQuestionFile(Long id, String field) {
        updateQuestionField(id, field, "");
    }

    private void setObjectField(Object target, String fieldName, String value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Class<?> type = field.getType();

            if (type == String.class) {
                field.set(target, value);
            } else if (type == LocalDate.class) {
                field.set(target, parseDate(value));
            } else if (type == Boolean.class || type == boolean.class) {
                field.set(target, Boolean.parseBoolean(value) || "1".equals(value));
            } else if (type == BigDecimal.class) {
                field.set(target, value != null && !value.isEmpty() ? new BigDecimal(value) : null);
            } else if (type == Integer.class || type == int.class) {
                field.set(target, value != null && !value.isEmpty() ? Integer.parseInt(value) : null);
            }
        } catch (Exception e) {
            log.error("Error setting field {} on {} to value {}: {}", fieldName, target.getClass().getSimpleName(),
                    value, e.getMessage());
            // throw new BadRequestException("Invalid field: " + fieldName);
        }
    }
}
