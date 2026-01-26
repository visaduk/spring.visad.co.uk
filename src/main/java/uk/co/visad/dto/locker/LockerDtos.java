package uk.co.visad.dto.locker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Container class for all Locker-related DTOs.
 */
public class LockerDtos {

    /**
     * Token Verification Request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Password is required")
        private String password;
    }

    /**
     * Token-only Request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequest {
        @NotBlank(message = "Token is required")
        private String token;
    }

    /**
     * Complete Applicant Data DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicantDataDTO {
        private PersonalDTO personal;
        private QuestionsDTO questions;

        @JsonProperty("co_travelers")
        private List<CoTravelerDTO> coTravelers;
    }

    /**
     * Personal Information DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonalDTO {
        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String dob;
        private String nationality;

        @JsonProperty("passport_no")
        private String passportNo;

        @JsonProperty("passport_issue")
        private String passportIssue;

        @JsonProperty("passport_expire")
        private String passportExpire;

        @JsonProperty("contact_number")
        private String contactNumber;

        private String email;

        @JsonProperty("address_line_1")
        private String addressLine1;

        @JsonProperty("address_line_2")
        private String addressLine2;

        private String city;

        @JsonProperty("state_province")
        private String stateProvince;

        @JsonProperty("zip_code")
        private String zipCode;

        @JsonProperty("visa_type")
        private String visaType;

        @JsonProperty("travel_country")
        private String travelCountry;

        @JsonProperty("visa_center")
        private String visaCenter;

        @JsonProperty("doc_date")
        private String docDate;

        private String gender;
    }

    /**
     * Questions DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionsDTO {
        @JsonProperty("marital_status")
        private String maritalStatus;

        @JsonProperty("place_of_birth")
        private String placeOfBirth;

        @JsonProperty("country_of_birth")
        private String countryOfBirth;

        @JsonProperty("travel_covered_by")
        private String travelCoveredBy;

        @JsonProperty("occupation_status")
        private String occupationStatus;

        @JsonProperty("occupation_title")
        private String occupationTitle;

        @JsonProperty("company_name")
        private String companyName;

        @JsonProperty("company_address_1")
        private String companyAddress1;

        @JsonProperty("company_address_2")
        private String companyAddress2;

        @JsonProperty("company_city")
        private String companyCity;

        @JsonProperty("company_state")
        private String companyState;

        @JsonProperty("company_zip")
        private String companyZip;

        @JsonProperty("company_phone")
        private String companyPhone;

        @JsonProperty("company_email")
        private String companyEmail;

        @JsonProperty("has_credit_card")
        private String hasCreditCard;

        @JsonProperty("fingerprints_taken")
        private String fingerprintsTaken;

        @JsonProperty("has_bookings")
        private String hasBookings;

        @JsonProperty("schengen_visa_image")
        private List<String> schengenVisaImage;

        @JsonProperty("travel_date_from")
        private String travelDateFrom;

        @JsonProperty("travel_date_to")
        private String travelDateTo;

        @JsonProperty("primary_destination")
        private String primaryDestination;

        @JsonProperty("has_stay_booking")
        private String hasStayBooking;

        @JsonProperty("hotel_name")
        private String hotelName;

        @JsonProperty("hotel_address_1")
        private String hotelAddress1;

        @JsonProperty("hotel_address_2")
        private String hotelAddress2;

        @JsonProperty("hotel_city")
        private String hotelCity;

        @JsonProperty("hotel_state")
        private String hotelState;

        @JsonProperty("hotel_zip")
        private String hotelZip;

        @JsonProperty("hotel_contact_number")
        private String hotelContactNumber;

        @JsonProperty("hotel_email")
        private String hotelEmail;

        @JsonProperty("hotel_booking_reference")
        private String hotelBookingReference;

        @JsonProperty("hotel_website")
        private String hotelWebsite;

        @JsonProperty("stay_type")
        private String stayType;

        // Business
        @JsonProperty("inviting_company_name")
        private String invitingCompanyName;

        @JsonProperty("inviting_company_contact_person")
        private String invitingCompanyContactPerson;

        @JsonProperty("inviting_company_phone")
        private String invitingCompanyPhone;

        @JsonProperty("inviting_company_address_1")
        private String invitingCompanyAddress1;

        @JsonProperty("inviting_company_address_2")
        private String invitingCompanyAddress2;

        @JsonProperty("inviting_company_city")
        private String invitingCompanyCity;

        @JsonProperty("inviting_company_state")
        private String invitingCompanyState;

        @JsonProperty("inviting_company_zip")
        private String invitingCompanyZip;

        // Family/Friends
        @JsonProperty("inviting_person_first_name")
        private String invitingPersonFirstName;

        @JsonProperty("inviting_person_surname")
        private String invitingPersonSurname;

        @JsonProperty("inviting_person_email")
        private String invitingPersonEmail;

        @JsonProperty("inviting_person_phone")
        private String invitingPersonPhone;

        @JsonProperty("inviting_person_phone_code")
        private String invitingPersonPhoneCode;

        @JsonProperty("inviting_person_relationship")
        private String invitingPersonRelationship;

        @JsonProperty("inviting_person_address_1")
        private String invitingPersonAddress1;

        @JsonProperty("inviting_person_address_2")
        private String invitingPersonAddress2;

        @JsonProperty("inviting_person_city")
        private String invitingPersonCity;

        @JsonProperty("inviting_person_state")
        private String invitingPersonState;

        @JsonProperty("inviting_person_zip")
        private String invitingPersonZip;

        @JsonProperty("evisa_issue_date")
        private String evisaIssueDate;

        @JsonProperty("evisa_expiry_date")
        private String evisaExpiryDate;

        @JsonProperty("evisa_no_date_settled")
        private String evisaNoDateSettled;

        @JsonProperty("share_code")
        private String shareCode;

        @JsonProperty("share_code_expiry_date")
        private String shareCodeExpiryDate;

        @JsonProperty("evisa_document_path")
        private List<String> evisaDocumentPath;

        @JsonProperty("share_code_document_path")
        private List<String> shareCodeDocumentPath;

        @JsonProperty("booking_documents_path")
        private List<String> bookingDocumentsPath;

        @JsonProperty("last_question_index")
        private Integer lastQuestionIndex;

        @JsonProperty("form_complete")
        private String formComplete;

        @JsonProperty("progress_percentage")
        private Integer progressPercentage;
    }

    /**
     * Co-Traveler DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoTravelerDTO {
        private Long id;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        private String gender;
        private String dob;

        @JsonProperty("dob_raw")
        private String dobRaw;

        private String nationality;

        @JsonProperty("passport_no")
        private String passportNo;

        @JsonProperty("contact_number")
        private String contactNumber;
    }

    /**
     * Update Personal Field Request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePersonalRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Field is required")
        private String field;

        private String value;
    }

    /**
     * Update Questions Request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateQuestionsRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotNull(message = "Data is required")
        private Map<String, Object> data;
    }

    /**
     * Update Progress Request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProgressRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotNull(message = "Percentage is required")
        private Integer percentage;
    }

    /**
     * Delete File Request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteFileRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Field is required")
        @JsonProperty("db_field")
        private String dbField;

        @NotBlank(message = "Filename is required")
        private String filename;
    }

    /**
     * Dependent Token Request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependentTokenRequest {
        @NotNull(message = "Dependent ID is required")
        @JsonProperty("dependent_id")
        private Long dependentId;
    }

    /**
     * Dependent Data DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DependentDataDTO {
        @JsonProperty("public_url_token")
        private String publicUrlToken;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("dob_raw")
        private String dobRaw;
    }

    /**
     * File Upload Response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileUploadResponse {
        private List<String> filenames;
        private List<String> errors;
    }
}
