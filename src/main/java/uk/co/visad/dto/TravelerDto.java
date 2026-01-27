package uk.co.visad.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelerDto {

    private Long id;

    // Personal Information
    private String name;
    private String firstName;
    private String lastName;
    private String title;
    private String gender;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dob;

    private String placeOfBirth;
    private String countryOfBirth;
    private String nationality;

    // Contact Information
    private String email;
    private String contactNumber;
    private String whatsappContact;

    // Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String zipCode;
    private String country;

    // Passport Information
    private String passportNo;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate passportIssue;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate passportExpire;

    // Visa Information
    private String travelCountry;
    private String visaCenter;
    private String visaType;
    private String visaLink;
    private String applicationFormLink;
    private String applicationFormUsername;
    private String applicationFormPassword;

    @JsonProperty("package")
    private String packageType;

    // Status and Priority
    private String status;
    private String priority;
    private String paymentStatus;

    @JsonProperty("details_verified")
    private Boolean detailsVerified;

    // Dates
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate plannedTravelDate;

    @JsonProperty("planned_travel_date_raw")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedTravelDateRaw;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate docDate;

    @JsonProperty("doc_date_raw")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate docDateRaw;

    // Notes
    private String note;
    private String notes;
    private String appointmentRemarks;

    // Account
    private String username;
    private String logins;

    // Family
    private Boolean isFamily;
    private String relationshipToMain;

    // Public URL
    private String publicUrlToken;

    // Pricing
    private BigDecimal price;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal refundAmount;

    // Questionnaire Data
    private Integer progressPercentage;

    // Occupation
    private String occupationStatus;
    private String occupationTitle;
    private String companyName;
    private String companyAddress1;
    private String companyAddress2;
    private String companyCity;
    private String companyState;
    private String companyZip;
    private String companyPhone;
    private String companyEmail;

    // Additional Personal
    private String maritalStatus;

    // Travel Plans
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate travelDateFrom;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate travelDateTo;

    private String primaryDestination;
    private String fingerprintsTaken;

    // Financial
    private String hasCreditCard;
    private String travelCoveredBy;

    // Accommodation
    private String hasStayBooking;
    private String hotelName;
    private String hotelAddress1;
    private String hotelAddress2;
    private String hotelCity;
    private String hotelState;
    private String hotelZip;
    private String hotelContactNumber;
    private String hotelEmail;
    private String hotelBookingReference;
    private String hotelWebsite;

    private String stayType;

    // Business Visit - Inviting Company
    private String invitingCompanyName;
    private String invitingCompanyContactPerson;
    private String invitingCompanyPhone;

    @JsonProperty("inviting_company_address_1")
    private String invitingCompanyAddress1;

    @JsonProperty("inviting_company_address_2")
    private String invitingCompanyAddress2;

    private String invitingCompanyCity;
    private String invitingCompanyState;
    private String invitingCompanyZip;

    // Family/Friends Visit - Inviting Person
    private String invitingPersonFirstName;
    private String invitingPersonSurname;
    private String invitingPersonEmail;
    private String invitingPersonPhone;
    private String invitingPersonPhoneCode;
    private String invitingPersonRelationship;

    @JsonProperty("inviting_person_address_1")
    private String invitingPersonAddress1;

    @JsonProperty("inviting_person_address_2")
    private String invitingPersonAddress2;

    private String invitingPersonCity;
    private String invitingPersonState;
    private String invitingPersonZip;

    // Bookings & Docs
    private String hasBookings;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate evisaIssueDate;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate evisaExpiryDate;

    private String evisaNoDateSettled;
    private String evisaDocument;
    private String schengenVisaImage;
    private List<String> evisaDocumentLinks;

    private String shareCode;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate shareCodeExpiryDate;

    private String shareCodeDocument;
    private List<String> shareCodeDocumentLinks;
    private String bookingDocument;
    private List<String> bookingDocumentLinks;
    private String passportFront;
    private List<String> passportFrontLinks;
    private String passportBack;
    private List<String> passportBackLinks;
    private String additionalNotes;
    private Boolean agreedToTerms;
    private Boolean formComplete;

    // Invoice Data
    private InvoiceDto savedInvoice;

    // Audit Fields
    private String createdByUsername;
    private String lastUpdatedByUsername;

    @JsonFormat(pattern = "dd/MM/yy HH:mm")
    private String createdAtFormatted;

    @JsonFormat(pattern = "dd/MM/yy HH:mm")
    private String lastUpdatedAtFormatted;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    // Dependents
    private List<DependentDto> dependents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateFieldRequest {
        private Long id;
        private String field;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceDto {
        private Long id;
        private Long travelerId;
        private String invoiceNumber;
        private BigDecimal subtotal;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal discountAmount;
        private BigDecimal total;
        private String itemsJson;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveInvoiceRequest {
        private Long id;
        private BigDecimal subtotal;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal discountAmount;
        private BigDecimal total;
        private String itemsJson;
    }
}
