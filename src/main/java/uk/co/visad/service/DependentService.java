package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.dto.DependentDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependentService {

    private final DependentRepository dependentRepository;
    private final TravelerRepository travelerRepository;
    private final TravelerQuestionsRepository travelerQuestionsRepository;
    private final VisaUrlRepository visaUrlRepository;
    private final AuditService auditService;

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "name", "travelCountry", "visaCenter", "package", "visaType", "status", "whatsappContact",
            "appointmentRemarks", "visaLink", "note", "plannedTravelDate", "firstName", "lastName",
            "gender", "dob", "nationality", "passportNo", "passportIssue", "passportExpire",
            "contactNumber", "email", "priority", "username", "logins", "notes", "paymentStatus",
            "addressLine1", "addressLine2", "city", "stateProvince", "zipCode", "docDate", "country",
            "applicationFormLink", "applicationFormUsername", "applicationFormPassword", "title",
            "placeOfBirth", "countryOfBirth", "relationshipToMain");

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
            "hotelBookingReference", "hotelWebsite",
            // Bookings
            "hasBookings",
            // eVisa
            "evisaIssueDate", "evisaExpiryDate", "evisaNoDateSettled", "evisaDocument",
            // Share Code
            "shareCode", "shareCodeExpiryDate", "shareCodeDocument",
            // Documents
            "bookingDocument", "passportFront", "passportBack", "additionalNotes",
            // Progress
            "progressPercentage", "formComplete", "agreedToTerms");

    @Transactional
    public Long createDependent(Long travelerId) {
        Traveler traveler = travelerRepository.findById(travelerId)
                .orElseThrow(() -> new ResourceNotFoundException("Main traveler not found"));

        String username = getCurrentUsername();

        Dependent dependent = Dependent.builder()
                .traveler(traveler)
                .name("Full Name")
                .firstName("")
                .lastName("")
                .priority("Normal")
                .status("Wait App")
                .travelCountry(traveler.getTravelCountry())
                .visaCenter(traveler.getVisaCenter())
                .packageType(traveler.getPackage_())
                .visaType(traveler.getVisaType())
                .whatsappContact(traveler.getWhatsappContact())
                .plannedTravelDate(traveler.getPlannedTravelDate())
                .createdByUsername(username)
                .build();

        dependent = dependentRepository.save(dependent);

        // Update visa link for the new dependent
        updateVisaLink(dependent);

        auditService.logChange("dependent", dependent.getId(), dependent.getName(),
                "Created Co-Traveler", "For: " + traveler.getName(), "New Record");

        return dependent.getId();
    }

    @Transactional
    public void updateField(Long id, String field, String value) {
        String javaField = convertToJavaFieldName(field);

        if (QUESTIONS_FIELDS.contains(javaField)) {
            updateQuestionField(id, field, value);
            return;
        }

        if (!ALLOWED_FIELDS.contains(javaField) && !"packageType".equals(javaField)) {
            throw new BadRequestException("Invalid field: " + field);
        }

        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));

        String oldValue = getFieldValue(dependent, javaField);

        if ("plannedTravelDate".equals(javaField)) {
            updateTravelerQuestionsDate(id, "dependent", value);
        } else {
            setFieldValue(dependent, javaField, value);
        }

        dependent.setLastUpdatedByUsername(getCurrentUsername());
        dependent.setLastUpdatedAt(LocalDateTime.now());
        dependentRepository.save(dependent);

        // Update visa link if country or center changed
        if ("travelCountry".equals(javaField) || "visaCenter".equals(javaField)) {
            updateVisaLink(dependent);
        }

        if (!Objects.equals(oldValue, value)) {
            auditService.logChange("dependent", id, dependent.getName(), field, oldValue, value);
        }
    }

    @Transactional
    public void updateFields(Long id, Map<String, Object> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new BadRequestException("No updates provided");
        }

        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));

        boolean visaLinkNeedsUpdate = false;

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

            if (!ALLOWED_FIELDS.contains(javaField) && !"packageType".equals(javaField)) {
                throw new BadRequestException("Invalid field: " + field);
            }

            String oldValue = getFieldValue(dependent, javaField);

            if ("plannedTravelDate".equals(javaField)) {
                updateTravelerQuestionsDate(id, "dependent", value);
            } else {
                setFieldValue(dependent, javaField, value);
            }

            // Track if visa link needs update
            if ("travelCountry".equals(javaField) || "visaCenter".equals(javaField)) {
                visaLinkNeedsUpdate = true;
            }

            // Log change
            if (!Objects.equals(oldValue, value)) {
                auditService.logChange("dependent", id, dependent.getName(), field, oldValue, value);
            }
        }

        // Update metadata
        dependent.setLastUpdatedByUsername(getCurrentUsername());
        dependent.setLastUpdatedAt(LocalDateTime.now());
        dependentRepository.save(dependent);

        // Perform post-update actions
        if (visaLinkNeedsUpdate) {
            updateVisaLink(dependent);
        }
    }

    @Transactional
    public void deleteDependent(Long id) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));

        String name = dependent.getName();

        dependentRepository.delete(dependent);

        auditService.logChange("dependent", id, name, "Deleted Co-Traveler", "Exists", "Deleted");
    }

    @Transactional(readOnly = true)
    public DependentDto getDependentById(Long id) {
        Dependent dependent = dependentRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));

        TravelerQuestions questions = travelerQuestionsRepository
                .findByRecordIdAndRecordType(id, "dependent")
                .orElse(null);

        return mapToDto(dependent, questions);
    }

    @Transactional(readOnly = true)
    public List<DependentDto> getDependentsForTraveler(Long travelerId) {
        List<Dependent> dependents = dependentRepository.findByTraveler_Id(travelerId);

        // Batch fetch questions to avoid N+1
        List<Long> depIds = dependents.stream().map(Dependent::getId).collect(Collectors.toList());
        Map<Long, TravelerQuestions> questionsMap = travelerQuestionsRepository
                .findAllByRecordIdInAndRecordType(depIds, "dependent")
                .stream()
                .collect(Collectors.toMap(TravelerQuestions::getRecordId, tq -> tq));

        return dependents.stream()
                .map(d -> mapToDto(d, questionsMap.get(d.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get all dependents across the system
     * PHP equivalent: dependents.php?action=read_all
     */
    @Transactional(readOnly = true)
    public List<DependentDto> getAllDependents() {
        List<Dependent> dependents = dependentRepository.findAll();

        // Batch fetch questions to avoid N+1
        List<Long> depIds = dependents.stream().map(Dependent::getId).collect(Collectors.toList());
        Map<Long, TravelerQuestions> questionsMap = new HashMap<>();
        if (!depIds.isEmpty()) {
            questionsMap = travelerQuestionsRepository
                    .findAllByRecordIdInAndRecordType(depIds, "dependent")
                    .stream()
                    .collect(Collectors.toMap(TravelerQuestions::getRecordId, tq -> tq));
        }

        final Map<Long, TravelerQuestions> finalQuestionsMap = questionsMap;
        return dependents.stream()
                .map(d -> mapToDto(d, finalQuestionsMap.get(d.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void setLockStatus(Long id, boolean locked) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));

        TravelerQuestions tq = travelerQuestionsRepository
                .findByRecordIdAndRecordType(id, "dependent")
                .orElseGet(() -> {
                    TravelerQuestions newTq = new TravelerQuestions();
                    newTq.setRecordId(id);
                    newTq.setRecordType("dependent");
                    return newTq;
                });

        boolean oldLocked = Boolean.TRUE.equals(tq.getFormComplete());
        tq.setFormComplete(locked);
        travelerQuestionsRepository.save(tq);

        auditService.logChange("dependent", id, dependent.getName(),
                "Form Lock Status",
                oldLocked ? "Locked" : "Unlocked",
                locked ? "Locked" : "Unlocked");
    }

    // Helper methods
    private void updateVisaLink(Dependent dependent) {
        String country = dependent.getTravelCountry();
        String center = dependent.getVisaCenter();

        if (country != null) {
            country = country.split(" - ")[0];
        }
        if (center != null) {
            center = center.split(" - ")[0];
        }

        String url = "";
        String appFormUrl = "";

        if (country != null && center != null && !center.isEmpty()) {
            Optional<VisaUrl> specific = visaUrlRepository.findByCountryAndVisaCenter(country, center);
            if (specific.isPresent()) {
                url = specific.get().getUrl();
                appFormUrl = specific.get().getApplicationFormUrl();
            }
        }

        if (url.isEmpty() && country != null) {
            Optional<VisaUrl> general = visaUrlRepository.findByCountryWithNoVisaCenter(country);
            if (general.isPresent()) {
                url = general.get().getUrl();
                appFormUrl = general.get().getApplicationFormUrl();
            }
        }

        dependent.setVisaLink(url);
        dependent.setApplicationFormLink(appFormUrl);
        dependentRepository.save(dependent);
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

    private DependentDto mapToDto(Dependent d, TravelerQuestions questions) {
        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

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
        String hasBookings = null;
        LocalDate evisaIssueDate = null;
        LocalDate evisaExpiryDate = null;
        String evisaNoDateSettled = null;
        String evisaDocument = null;
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
                .publicUrlToken(d.getPublicUrlToken())
                .price(d.getPrice())
                .progressPercentage(progress)
                .createdByUsername(d.getCreatedByUsername())
                .lastUpdatedByUsername(d.getLastUpdatedByUsername())
                .createdAtFormatted(d.getCreatedAt() != null ? d.getCreatedAt().format(displayFormat) : null)
                .lastUpdatedAtFormatted(
                        d.getLastUpdatedAt() != null ? d.getLastUpdatedAt().format(displayFormat) : null)
                .createdAt(d.getCreatedAt())
                .lastUpdatedAt(d.getLastUpdatedAt())
                // Extended fields
                .travelDateFrom(travelDateFromQuestions)
                .occupationStatus(occupationStatus)
                .occupationTitle(occupationTitle)
                .companyName(companyName)
                .companyAddress1(companyAddress1)
                .companyAddress2(companyAddress2)
                .companyCity(companyCity)
                .companyState(companyState)
                .companyZip(companyZip)
                .companyPhone(companyPhone)
                .companyEmail(companyEmail)
                .maritalStatus(maritalStatus)
                .travelDateTo(travelDateTo)
                .primaryDestination(primaryDestination)
                .fingerprintsTaken(fingerprintsTaken)
                .hasCreditCard(hasCreditCard)
                .travelCoveredBy(travelCoveredBy)
                .hasStayBooking(hasStayBooking)
                .hotelName(hotelName)
                .hotelAddress1(hotelAddress1)
                .hotelAddress2(hotelAddress2)
                .hotelCity(hotelCity)
                .hotelState(hotelState)
                .hotelZip(hotelZip)
                .hotelContactNumber(hotelContactNumber)
                .hotelEmail(hotelEmail)
                .hotelBookingReference(hotelBookingReference)
                .hotelWebsite(hotelWebsite)
                .hasBookings(hasBookings)
                .evisaIssueDate(evisaIssueDate)
                .evisaExpiryDate(evisaExpiryDate)
                .evisaNoDateSettled(evisaNoDateSettled)
                .evisaDocument(evisaDocument)
                .shareCode(shareCode)
                .shareCodeExpiryDate(shareCodeExpiryDate)
                .shareCodeDocument(shareCodeDocument)
                .bookingDocument(bookingDocument)
                .passportFront(passportFront)
                .passportBack(passportBack)
                .additionalNotes(additionalNotes)
                .agreedToTerms(agreedToTerms)
                .formComplete(formComplete)
                .build();
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getUsername();
        }
        return "system";
    }

    private String convertToJavaFieldName(String field) {
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

    private String getFieldValue(Dependent dependent, String fieldName) {
        try {
            Field field = Dependent.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(dependent);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void setFieldValue(Dependent dependent, String fieldName, String value) {
        try {
            Field field = Dependent.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Class<?> type = field.getType();

            if (type == String.class) {
                field.set(dependent, value);
            } else if (type == LocalDate.class) {
                field.set(dependent, parseDate(value));
            } else if (type == Boolean.class || type == boolean.class) {
                field.set(dependent, Boolean.parseBoolean(value) || "1".equals(value));
            } else if (type == BigDecimal.class) {
                field.set(dependent, value != null && !value.isEmpty() ? new BigDecimal(value) : null);
            }
        } catch (NoSuchFieldException e) {
            if ("package".equals(fieldName)) {
                dependent.setPackageType(value);
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
                .findByRecordIdAndRecordType(id, "dependent")
                .orElseGet(() -> {
                    TravelerQuestions newTq = new TravelerQuestions();
                    newTq.setRecordId(id);
                    newTq.setRecordType("dependent");
                    return newTq;
                });

        setObjectField(tq, javaField, value);
        travelerQuestionsRepository.save(tq);
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
        }
    }
}
