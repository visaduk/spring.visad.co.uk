package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dependents", indexes = {
        @Index(name = "idx_dependents_traveler_id", columnList = "traveler_id"),
        @Index(name = "idx_dependents_passport", columnList = "passport_no")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dependent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traveler_id", nullable = false, columnDefinition = "INT")
    private Traveler traveler;

    // Personal Information
    @Column(length = 100)
    private String name;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(length = 10)
    private String title;

    @Column(length = 10)
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "place_of_birth", length = 100)
    private String placeOfBirth;

    @Column(name = "country_of_birth", length = 100)
    private String countryOfBirth;

    @Column(length = 100)
    private String nationality;

    @Column(name = "relationship_to_main", length = 50)
    private String relationshipToMain;

    // Contact Information
    @Column(length = 150)
    private String email;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(name = "whatsapp_contact", length = 30)
    private String whatsappContact;

    // Address
    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(length = 100)
    private String country;

    // Passport Information
    @Column(name = "passport_no", length = 50)
    private String passportNo;

    @Column(name = "passport_issue")
    private LocalDate passportIssue;

    @Column(name = "passport_expire")
    private LocalDate passportExpire;

    // Visa Information
    @Column(name = "travel_country", length = 100)
    private String travelCountry;

    @Column(name = "visa_center", length = 150)
    private String visaCenter;

    @Column(name = "visa_type", length = 100)
    private String visaType;

    @Column(name = "visa_link", length = 500)
    private String visaLink;

    @Column(name = "application_form_link", length = 500)
    private String applicationFormLink;

    @Column(name = "application_form_username", length = 100)
    private String applicationFormUsername;

    @Column(name = "application_form_password", length = 100)
    private String applicationFormPassword;

    @Column(name = "package", length = 100)
    private String packageType;

    // Status and Priority
    @Column(length = 50)
    @Builder.Default
    private String status = "Wait App";

    @Column(length = 20)
    @Builder.Default
    private String priority = "Normal";

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    // Dates
    @Column(name = "planned_travel_date")
    private LocalDate plannedTravelDate;

    @Column(name = "doc_date")
    private LocalDate docDate;

    // Notes
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "appointment_remarks", columnDefinition = "TEXT")
    private String appointmentRemarks;

    // Account Information
    @Column(length = 100)
    private String username;

    @Column(columnDefinition = "TEXT")
    private String logins;

    // Public URL Token
    @Column(name = "public_url_token", length = 20)
    private String publicUrlToken;

    // Pricing
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    // Audit Fields
    @Column(name = "created_by_username", length = 50)
    private String createdByUsername;

    @Column(name = "last_updated_by_username", length = 50)
    private String lastUpdatedByUsername;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    // Relationship to TravelerQuestions


    // Helper Methods
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return name != null ? name : "";
    }

    public Long getTravelerId() {
        return traveler != null ? traveler.getId() : null;
    }

    @PrePersist
    public void prePersist() {
        if (publicUrlToken == null) {
            publicUrlToken = java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        if (status == null) {
            status = "Wait App";
        }
        if (priority == null) {
            priority = "Normal";
        }
    }
}
