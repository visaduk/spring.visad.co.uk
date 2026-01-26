package uk.co.visad.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.dto.locker.LockerDtos.*;
import uk.co.visad.entity.Dependent;
import uk.co.visad.entity.Traveler;
import uk.co.visad.entity.TravelerQuestions;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.exception.UnauthorizedException;
import uk.co.visad.repository.DependentRepository;
import uk.co.visad.repository.TravelerQuestionsRepository;
import uk.co.visad.repository.TravelerRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main Business Logic Service for Locker (Public Portal)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LockerService {

    private final TravelerRepository travelerRepository;
    private final DependentRepository dependentRepository;
    private final TravelerQuestionsRepository travelerQuestionsRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");

    /**
     * Verify token and password, return applicant data
     */
    public ApplicantDataDTO verifyAndGetData(String token, String password) {
        log.debug("Verifying token: {}", token);

        // Find traveler record (Main Applicant)
        Traveler traveler = travelerRepository.findByPublicUrlToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid token"));

        // Verify password (DOB in DDMMYYYY format)
        if (traveler.getDob() == null) {
            throw new UnauthorizedException("DOB not set for applicant");
        }
        String expectedPassword = traveler.getDob().format(DOB_FORMATTER);
        String normalizedPassword = password != null ? password.trim() : "";

        log.info("Password Check - Token: {}, DOB: {}, Expected: '{}', Received: '{}'",
                token, traveler.getDob(), expectedPassword, normalizedPassword);

        if (!expectedPassword.equals(normalizedPassword)) {
            log.warn("Password mismatch for token: {}", token);
            throw new UnauthorizedException("Invalid password");
        }

        // Get questions data
        TravelerQuestions questions = travelerQuestionsRepository
                .findByRecordIdAndRecordType(traveler.getId(), "traveler")
                .orElseGet(() -> createDefaultQuestions(traveler));

        // Get co-travelers (Dependents)
        List<Dependent> dependents = dependentRepository.findByTraveler_Id(traveler.getId());

        // Build response DTO
        return ApplicantDataDTO.builder()
                .personal(mapTravelerToDTO(traveler))
                .questions(mapQuestionsToDTO(questions, traveler))
                .coTravelers(mapDependentsToDTO(dependents))
                .build();
    }

    /**
     * Update single personal field
     */
    public void updatePersonalField(String token, String field, String value) {
        RecordWrapper record = findRecordByToken(token);

        // Check if form is locked
        if (record.questions != null && Boolean.TRUE.equals(record.questions.getFormComplete())) {
            throw new IllegalStateException("Application is locked");
        }

        // Update field
        updatePersonalFieldByName(record, field, value);

        if (record.isTraveler) {
            travelerRepository.save(record.traveler);
        } else {
            dependentRepository.save(record.dependent);
        }

        log.info("Updated personal field: token={}, field={}", token, field);
    }

    /**
     * Update multiple question fields
     */
    public void updateQuestionFields(String token, Map<String, Object> data) {
        RecordWrapper record = findRecordByToken(token);

        // If questions don't exist, create them
        if (record.questions == null) {
            record.questions = createDefaultQuestions(record);
        }
        TravelerQuestions questions = record.questions;

        // Check if form is locked
        if (Boolean.TRUE.equals(questions.getFormComplete())) {
            // Allow updating last_question_index even if locked
            if (data.size() == 1 && data.containsKey("last_question_index")) {
                // TravelerQuestions doesn't have lastQuestionIndex? Ignoring for now.
                // Log debug or just return.
                updateQuestionFieldByName(record, "last_question_index", data.get("last_question_index"));
                travelerQuestionsRepository.save(questions);
                return;
            }
            throw new IllegalStateException("Application is locked");
        }

        // Update fields
        data.forEach((field, value) -> updateQuestionFieldByName(record, field, value));

        // Save the entity that was updated (Traveler/Dependent or Questions)
        // Since we might update both, save both if needed.
        // Optimally, we check what changed, but safely saving both is fine.
        travelerQuestionsRepository.save(questions);
        if (record.isTraveler) {
            travelerRepository.save(record.traveler);
        } else {
            dependentRepository.save(record.dependent);
        }

        log.info("Updated question fields: token={}, fields={}", token, data.keySet());
    }

    /**
     * Update progress percentage
     */
    public void updateProgress(String token, Integer percentage) {
        RecordWrapper record = findRecordByToken(token);
        if (record.questions == null) {
            record.questions = createDefaultQuestions(record);
        }
        TravelerQuestions questions = record.questions;

        questions.setProgressPercentage(percentage);
        travelerQuestionsRepository.save(questions);

        log.info("Updated progress: token={}, percentage={}", token, percentage);
    }

    /**
     * Mark application as complete
     */
    public void markApplicationComplete(String token) {
        RecordWrapper record = findRecordByToken(token);
        if (record.questions == null) {
            record.questions = createDefaultQuestions(record);
        }
        TravelerQuestions questions = record.questions;

        questions.setFormComplete(true);
        questions.setProgressPercentage(100);
        travelerQuestionsRepository.save(questions);

        log.info("Application marked complete: token={}", token);
    }

    /**
     * Get dependent data
     */
    public DependentDataDTO getDependentData(Long dependentId) {
        Dependent dependent = dependentRepository.findById(dependentId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));

        return DependentDataDTO.builder()
                .publicUrlToken(dependent.getPublicUrlToken())
                .firstName(dependent.getFirstName())
                .lastName(dependent.getLastName())
                .dobRaw(dependent.getDob() != null ? dependent.getDob().format(DOB_FORMATTER) : null)
                .build();
    }

    // Helper methods and inner classes

    private static class RecordWrapper {
        boolean isTraveler;
        Traveler traveler;
        Dependent dependent;
        TravelerQuestions questions;

        Long getId() {
            return isTraveler ? traveler.getId() : dependent.getId();
        }

        String getType() {
            return isTraveler ? "traveler" : "dependent";
        }

        String getFirstName() {
            return isTraveler ? traveler.getFirstName() : dependent.getFirstName();
        }

        String getLastName() {
            return isTraveler ? traveler.getLastName() : dependent.getLastName();
        }
    }

    private RecordWrapper findRecordByToken(String token) {
        RecordWrapper wrapper = new RecordWrapper();

        Optional<Traveler> travelerOpt = travelerRepository.findByPublicUrlToken(token);
        if (travelerOpt.isPresent()) {
            wrapper.isTraveler = true;
            wrapper.traveler = travelerOpt.get();
            wrapper.questions = travelerQuestionsRepository
                    .findByRecordIdAndRecordType(wrapper.traveler.getId(), "traveler")
                    .orElse(null);
            return wrapper;
        }

        Optional<Dependent> dependentOpt = dependentRepository.findByPublicUrlToken(token);
        if (dependentOpt.isPresent()) {
            wrapper.isTraveler = false;
            wrapper.dependent = dependentOpt.get();
            // Assuming dependents also have questions with record_type 'dependent'
            wrapper.questions = travelerQuestionsRepository
                    .findByRecordIdAndRecordType(wrapper.dependent.getId(), "dependent")
                    .orElse(null);
            return wrapper;
        }

        throw new UnauthorizedException("Invalid token");
    }

    private TravelerQuestions createDefaultQuestions(Traveler traveler) {
        TravelerQuestions questions = TravelerQuestions.builder()
                .recordId(traveler.getId())
                .recordType("traveler")
                .formComplete(false)
                .agreedToTerms(false)
                .progressPercentage(0)
                .build();
        return travelerQuestionsRepository.save(questions);
    }

    private TravelerQuestions createDefaultQuestions(RecordWrapper record) {
        TravelerQuestions questions = TravelerQuestions.builder()
                .recordId(record.getId())
                .recordType(record.getType())
                .formComplete(false)
                .agreedToTerms(false)
                .progressPercentage(0)
                .build();
        return travelerQuestionsRepository.save(questions);
    }

    private PersonalDTO mapTravelerToDTO(Traveler traveler) {
        return PersonalDTO.builder()
                .firstName(traveler.getFirstName())
                .lastName(traveler.getLastName())
                .dob(traveler.getDob() != null ? traveler.getDob().toString() : null)
                .nationality(traveler.getNationality())
                .passportNo(traveler.getPassportNo())
                .passportIssue(traveler.getPassportIssue() != null ? traveler.getPassportIssue().toString() : null)
                .passportExpire(traveler.getPassportExpire() != null ? traveler.getPassportExpire().toString() : null)
                .contactNumber(traveler.getContactNumber())
                .email(traveler.getEmail())
                .addressLine1(traveler.getAddressLine1())
                .addressLine2(traveler.getAddressLine2())
                .city(traveler.getCity())
                .stateProvince(traveler.getStateProvince())
                .zipCode(traveler.getZipCode())
                .visaType(traveler.getVisaType())
                .travelCountry(traveler.getTravelCountry())
                .visaCenter(traveler.getVisaCenter())
                .docDate(traveler.getDocDate() != null ? traveler.getDocDate().toString() : null)
                .gender(traveler.getGender())
                .build();
    }

    private QuestionsDTO mapQuestionsToDTO(TravelerQuestions questions, Traveler traveler) {
        // Map fields specifically, noting that some come from Traveler entity
        return QuestionsDTO.builder()
                // Fields from Traveler
                .placeOfBirth(traveler.getPlaceOfBirth())
                .countryOfBirth(traveler.getCountryOfBirth())

                // Fields from TravelerQuestions
                .maritalStatus(questions.getMaritalStatus())
                .travelCoveredBy(questions.getTravelCoveredBy())
                .occupationStatus(questions.getOccupationStatus())
                .occupationTitle(questions.getOccupationTitle())
                .companyName(questions.getCompanyName())
                .companyAddress1(questions.getCompanyAddress1())
                .companyAddress2(questions.getCompanyAddress2())
                .companyCity(questions.getCompanyCity())
                .companyState(questions.getCompanyState())
                .companyZip(questions.getCompanyZip())
                .companyPhone(questions.getCompanyPhone())
                .companyEmail(questions.getCompanyEmail())
                .hasCreditCard(questions.getHasCreditCard())
                .fingerprintsTaken(questions.getFingerprintsTaken())
                .schengenVisaImage(parseJsonArray(null)) // Not present in TravelerQuestions directly? TBD
                .travelDateFrom(questions.getTravelDateFrom() != null ? questions.getTravelDateFrom().toString() : null)
                .travelDateTo(questions.getTravelDateTo() != null ? questions.getTravelDateTo().toString() : null)
                .primaryDestination(questions.getPrimaryDestination())
                .hasStayBooking(questions.getHasStayBooking())
                .hasBookings(questions.getHasBookings())
                .hotelName(questions.getHotelName())
                .hotelAddress1(questions.getHotelAddress1())
                .hotelAddress2(questions.getHotelAddress2())
                .hotelCity(questions.getHotelCity())
                .hotelState(questions.getHotelState())
                .hotelZip(questions.getHotelZip())
                .hotelContactNumber(questions.getHotelContactNumber())
                .hotelEmail(questions.getHotelEmail())
                .hotelBookingReference(questions.getHotelBookingReference())
                .hotelWebsite(questions.getHotelWebsite())
                .stayType(questions.getStayType())
                .invitingCompanyName(questions.getInvitingCompanyName())
                .invitingCompanyContactPerson(questions.getInvitingCompanyContactPerson())
                .invitingCompanyPhone(questions.getInvitingCompanyPhone())
                .invitingCompanyAddress1(questions.getInvitingCompanyAddress1())
                .invitingCompanyAddress2(questions.getInvitingCompanyAddress2())
                .invitingCompanyCity(questions.getInvitingCompanyCity())
                .invitingCompanyState(questions.getInvitingCompanyState())
                .invitingCompanyZip(questions.getInvitingCompanyZip())
                .invitingPersonFirstName(questions.getInvitingPersonFirstName())
                .invitingPersonSurname(questions.getInvitingPersonSurname())
                .invitingPersonEmail(questions.getInvitingPersonEmail())
                .invitingPersonPhone(questions.getInvitingPersonPhone())
                .invitingPersonPhoneCode(questions.getInvitingPersonPhoneCode())
                .invitingPersonRelationship(questions.getInvitingPersonRelationship())
                .invitingPersonAddress1(questions.getInvitingPersonAddress1())
                .invitingPersonAddress2(questions.getInvitingPersonAddress2())
                .invitingPersonCity(questions.getInvitingPersonCity())
                .invitingPersonState(questions.getInvitingPersonState())
                .invitingPersonZip(questions.getInvitingPersonZip())
                .evisaIssueDate(questions.getEvisaIssueDate() != null ? questions.getEvisaIssueDate().toString() : null)
                .evisaExpiryDate(
                        questions.getEvisaExpiryDate() != null ? questions.getEvisaExpiryDate().toString() : null)
                .evisaNoDateSettled(questions.getEvisaNoDateSettled())
                .shareCode(questions.getShareCode())
                .shareCodeExpiryDate(
                        questions.getShareCodeExpiryDate() != null ? questions.getShareCodeExpiryDate().toString()
                                : null)
                .evisaDocumentPath(parseJsonArray(questions.getEvisaDocument()))
                .shareCodeDocumentPath(parseJsonArray(questions.getShareCodeDocument()))
                .bookingDocumentsPath(parseJsonArray(questions.getBookingDocument()))
                .lastQuestionIndex(questions.getLastQuestionIndex())
                .formComplete(Boolean.TRUE.equals(questions.getFormComplete()) ? "1" : "0")
                .progressPercentage(questions.getProgressPercentage())
                .build();
    }

    private List<CoTravelerDTO> mapDependentsToDTO(List<Dependent> dependents) {
        return dependents.stream()
                .map(dep -> CoTravelerDTO.builder()
                        .id(dep.getId())
                        .firstName(dep.getFirstName())
                        .lastName(dep.getLastName())
                        .gender(dep.getGender())
                        .dob(dep.getDob() != null ? dep.getDob().toString() : null)
                        .dobRaw(dep.getDob() != null ? dep.getDob().format(DOB_FORMATTER) : null)
                        .nationality(dep.getNationality())
                        .passportNo(dep.getPassportNo())
                        .contactNumber(dep.getContactNumber())
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty())
            return new ArrayList<>();
        // If it's a simple path string, handle it. If it's JSON array, parse it.
        // The previous system used JSON arrays for file paths?
        // TravelerQuestions has single String for documents mostly.
        // But DTO expects List<String>.
        // If the field contains a single path, return list of 1.
        try {
            if (json.startsWith("[")) {
                return objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } else {
                return Collections.singletonList(json);
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", json);
            return Collections.singletonList(json);
        }
    }

    private void updatePersonalFieldByName(RecordWrapper record, String field, String value) {
        if (record.isTraveler) {
            Traveler t = record.traveler;
            switch (field) {
                case "contact_number":
                    t.setContactNumber(value);
                    break;
                case "email":
                    t.setEmail(value);
                    break;
                case "address_line_1":
                    t.setAddressLine1(value);
                    break;
                case "address_line_2":
                    t.setAddressLine2(value);
                    break;
                case "city":
                    t.setCity(value);
                    break;
                case "state_province":
                    t.setStateProvince(value);
                    break;
                case "zip_code":
                    t.setZipCode(value);
                    break;
                case "place_of_birth":
                    t.setPlaceOfBirth(value);
                    break;
                case "country_of_birth":
                    t.setCountryOfBirth(value);
                    break;
                default:
                    log.warn("Unknown personal field: {}", field);
            }
        } else {
            Dependent d = record.dependent;
            switch (field) {
                case "contact_number":
                    d.setContactNumber(value);
                    break;
                case "email":
                    d.setEmail(value);
                    break;
                case "address_line_1":
                    d.setAddressLine1(value);
                    break;
                case "address_line_2":
                    d.setAddressLine2(value);
                    break;
                case "city":
                    d.setCity(value);
                    break;
                case "state_province":
                    d.setStateProvince(value);
                    break;
                case "zip_code":
                    d.setZipCode(value);
                    break;
                case "place_of_birth":
                    d.setPlaceOfBirth(value);
                    break;
                case "country_of_birth":
                    d.setCountryOfBirth(value);
                    break;
                default:
                    log.warn("Unknown personal field: {}", field);
            }
        }
    }

    private void updateQuestionFieldByName(RecordWrapper record, String field, Object value) {
        TravelerQuestions questions = record.questions;
        String strValue = value != null ? value.toString() : null;

        switch (field) {
            case "marital_status":
                questions.setMaritalStatus(strValue);
                break;

            // These belong to Traveler/Dependent
            case "place_of_birth":
                if (record.isTraveler)
                    record.traveler.setPlaceOfBirth(strValue);
                else
                    record.dependent.setPlaceOfBirth(strValue);
                break;
            case "country_of_birth":
                if (record.isTraveler)
                    record.traveler.setCountryOfBirth(strValue);
                else
                    record.dependent.setCountryOfBirth(strValue);
                break;

            case "travel_covered_by":
                questions.setTravelCoveredBy(strValue);
                break;
            case "occupation_status":
                questions.setOccupationStatus(strValue);
                break;
            case "occupation_title":
                questions.setOccupationTitle(strValue);
                break;
            case "company_name":
                questions.setCompanyName(strValue);
                break;
            case "company_address_1":
                questions.setCompanyAddress1(strValue);
                break;
            case "company_address_2":
                questions.setCompanyAddress2(strValue);
                break;
            case "company_city":
                questions.setCompanyCity(strValue);
                break;
            case "company_state":
                questions.setCompanyState(strValue);
                break;
            case "company_zip":
                questions.setCompanyZip(strValue);
                break;
            case "company_phone":
                questions.setCompanyPhone(strValue);
                break;
            case "company_email":
                questions.setCompanyEmail(strValue);
                break;

            // Map 'inviting_' prefixed fields to company fields
            case "stay_type":
                questions.setStayType(strValue);
                break;
            case "inviting_company_name":
                questions.setInvitingCompanyName(strValue);
                break;
            case "inviting_company_address_1":
                questions.setInvitingCompanyAddress1(strValue);
                break;
            case "inviting_company_address_2":
                questions.setInvitingCompanyAddress2(strValue);
                break;
            case "inviting_company_city":
                questions.setInvitingCompanyCity(strValue);
                break;
            case "inviting_company_state":
                questions.setInvitingCompanyState(strValue);
                break;
            case "inviting_company_zip":
                questions.setInvitingCompanyZip(strValue);
                break;
            case "inviting_company_phone":
                questions.setInvitingCompanyPhone(strValue);
                break;
            case "inviting_company_contact_person":
                questions.setInvitingCompanyContactPerson(strValue);
                break;
            case "inviting_person_first_name":
                questions.setInvitingPersonFirstName(strValue);
                break;
            case "inviting_person_surname":
                questions.setInvitingPersonSurname(strValue);
                break;
            case "inviting_person_email":
                questions.setInvitingPersonEmail(strValue);
                break;
            case "inviting_person_phone":
                questions.setInvitingPersonPhone(strValue);
                break;
            case "inviting_person_phone_code":
                questions.setInvitingPersonPhoneCode(strValue);
                break;
            case "inviting_person_relationship":
                questions.setInvitingPersonRelationship(strValue);
                break;
            case "inviting_person_address_1":
                questions.setInvitingPersonAddress1(strValue);
                break;
            case "inviting_person_address_2":
                questions.setInvitingPersonAddress2(strValue);
                break;
            case "inviting_person_city":
                questions.setInvitingPersonCity(strValue);
                break;
            case "inviting_person_state":
                questions.setInvitingPersonState(strValue);
                break;
            case "inviting_person_zip":
                questions.setInvitingPersonZip(strValue);
                break;
            case "has_credit_card":
                questions.setHasCreditCard(strValue);
                break;
            case "fingerprints_taken":
                questions.setFingerprintsTaken(strValue);
                break;
            case "travel_date_from":
                questions.setTravelDateFrom(strValue != null ? LocalDate.parse(strValue) : null);
                break;
            case "travel_date_to":
                questions.setTravelDateTo(strValue != null ? LocalDate.parse(strValue) : null);
                break;
            case "primary_destination":
                questions.setPrimaryDestination(strValue);
                break;
            case "has_stay_booking":
                questions.setHasStayBooking(strValue);
                break;
            case "hotel_name":
                questions.setHotelName(strValue);
                break;
            case "hotel_address_1":
                questions.setHotelAddress1(strValue);
                break;
            case "hotel_address_2":
                questions.setHotelAddress2(strValue);
                break;
            case "hotel_city":
                questions.setHotelCity(strValue);
                break;
            case "hotel_state":
                questions.setHotelState(strValue);
                break;
            case "hotel_zip":
                questions.setHotelZip(strValue);
                break;
            case "hotel_contact_number":
                questions.setHotelContactNumber(strValue);
                break;
            case "hotel_email":
                questions.setHotelEmail(strValue);
                break;
            case "hotel_booking_reference":
                questions.setHotelBookingReference(strValue);
                break;
            case "hotel_website":
                questions.setHotelWebsite(strValue);
                break;
            case "has_bookings":
                questions.setHasBookings(strValue);
                break;
            case "evisa_issue_date":
                questions.setEvisaIssueDate(strValue != null ? LocalDate.parse(strValue) : null);
                break;
            case "evisa_expiry_date":
                questions.setEvisaExpiryDate(strValue != null ? LocalDate.parse(strValue) : null);
                break;
            case "evisa_no_date_settled":
                questions.setEvisaNoDateSettled(strValue);
                break;
            case "share_code":
                questions.setShareCode(strValue);
                break;
            case "share_code_expiry_date":
                questions.setShareCodeExpiryDate(strValue != null ? LocalDate.parse(strValue) : null);
                break;
            case "last_question_index":
                if (value instanceof Number) {
                    questions.setLastQuestionIndex(((Number) value).intValue());
                } else if (value != null) {
                    try {
                        questions.setLastQuestionIndex(Integer.parseInt(value.toString()));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid last_question_index: {}", value);
                    }
                }
                break;
            case "agreed_to_terms":
                questions.setAgreedToTerms("1".equals(strValue));
                break;
            default:
                log.debug("Field {} not mapped, skipping", field);
        }
    }
}
