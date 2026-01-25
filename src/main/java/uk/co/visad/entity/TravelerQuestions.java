package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "traveler_questions", indexes = {
        @Index(name = "idx_tq_record", columnList = "record_id, record_type")
})
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelerQuestions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @Column(name = "record_id", nullable = false, columnDefinition = "INT")
    private Long recordId;

    @Column(name = "record_type", nullable = false, columnDefinition = "ENUM('traveler','dependent')")
    private String recordType; // 'traveler' or 'dependent'

    // Link to Traveler (when record_type = 'traveler')
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", referencedColumnName = "id", insertable = false, updatable = false, columnDefinition = "INT")
    private Traveler traveler;

    // Link to Dependent (when record_type = 'dependent')
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", referencedColumnName = "id", insertable = false, updatable = false, columnDefinition = "INT")
    private Dependent dependent;

    // Occupation fields
    @Column(name = "occupation_status", length = 100)
    private String occupationStatus;

    @Column(name = "occupation_title", length = 255)
    private String occupationTitle;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "company_address_1", length = 255)
    private String companyAddress1;

    @Column(name = "company_address_2", length = 255)
    private String companyAddress2;

    @Column(name = "company_city", length = 100)
    private String companyCity;

    @Column(name = "company_state", length = 100)
    private String companyState;

    @Column(name = "company_zip", length = 20)
    private String companyZip;

    @Column(name = "company_phone", length = 50)
    private String companyPhone;

    @Column(name = "company_email", length = 255)
    private String companyEmail;

    // Marital status
    @Column(name = "marital_status", length = 50)
    private String maritalStatus;

    // Travel Dates
    @Column(name = "travel_date_from")
    private LocalDate travelDateFrom;

    @Column(name = "travel_date_to")
    private LocalDate travelDateTo;

    @Column(name = "primary_destination", length = 255)
    private String primaryDestination;

    @Column(name = "fingerprints_taken", length = 10)
    private String fingerprintsTaken;

    @Column(name = "has_credit_card", length = 10)
    private String hasCreditCard;

    @Column(name = "travel_covered_by", length = 50)
    private String travelCoveredBy;

    // Accommodation (Mapped to hotel_* columns in DB)
    @Column(name = "has_stay_booking", length = 10)
    private String hasStayBooking;

    @Column(name = "hotel_name", length = 255)
    private String hotelName;

    @Column(name = "hotel_address_1", length = 255)
    private String hotelAddress1;

    @Column(name = "hotel_address_2", length = 255)
    private String hotelAddress2;

    @Column(name = "hotel_city", length = 100)
    private String hotelCity;

    @Column(name = "hotel_state", length = 100)
    private String hotelState;

    @Column(name = "hotel_zip", length = 20)
    private String hotelZip;

    @Column(name = "hotel_contact_number", length = 50)
    private String hotelContactNumber;

    @Transient
    @Column(name = "hotel_email", length = 255)
    private String hotelEmail;

    @Column(name = "hotel_booking_reference", length = 100)
    private String hotelBookingReference;

    @Transient
    @Column(name = "hotel_website", length = 255)
    private String hotelWebsite;

    @Column(name = "has_bookings", length = 10)
    private String hasBookings;

    @Column(name = "agreed_to_terms")
    @Builder.Default
    private Boolean agreedToTerms = false;

    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    @Column(name = "last_question_index")
    private Integer lastQuestionIndex;

    @Column(name = "form_complete")
    @Builder.Default
    private Boolean formComplete = false;

    // eVisa fields
    @Column(name = "evisa_issue_date")
    private LocalDate evisaIssueDate;

    @Column(name = "evisa_expiry_date")
    private LocalDate evisaExpiryDate;

    @Column(name = "evisa_no_date_settled", length = 10)
    private String evisaNoDateSettled;

    @Column(name = "evisa_document_path", length = 255)
    private String evisaDocument; // Mapped to evisa_document_path column

    @Column(name = "share_code", length = 50)
    private String shareCode;

    @Column(name = "share_code_expiry_date")
    private LocalDate shareCodeExpiryDate;

    @Column(name = "share_code_document_path", length = 255)
    private String shareCodeDocument;

    @Column(name = "booking_documents_path", length = 255)
    private String bookingDocument;

    @Column(name = "passport_front", length = 255)
    private String passportFront;

    @Column(name = "passport_back", length = 255)
    private String passportBack;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    // Timestamps
    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    // Helper method to check if form is locked
    public boolean isLocked() {
        return Boolean.TRUE.equals(formComplete);
    }
}
