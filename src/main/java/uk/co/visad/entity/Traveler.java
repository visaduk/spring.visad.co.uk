package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travelers", indexes = {
        @Index(name = "idx_travelers_passport", columnList = "passport_no"),
        @Index(name = "idx_travelers_email", columnList = "email"),
        @Index(name = "idx_travelers_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Traveler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    // Personal Information (matching production DB)
    @Column(length = 10)
    private String title;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(length = 50)
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(length = 100)
    private String nationality;

    // Passport Information
    @Column(name = "passport_no", length = 100)
    private String passportNo;

    @Column(name = "passport_issue")
    private LocalDate passportIssue;

    @Column(name = "passport_expire")
    private LocalDate passportExpire;

    // Visa Information
    @Column(name = "travel_country", length = 255)
    private String travelCountry;

    @Column(name = "visa_center", length = 255)
    private String visaCenter;

    @Column(name = "visa_type", length = 100)
    private String visaType;

    @Column(name = "package", length = 100)
    private String package_;

    @Column(length = 100)
    @Builder.Default
    private String status = "Wait App";

    @Column(name = "planned_travel_date")
    private LocalDate plannedTravelDate;

    // Contact Information
    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(length = 255)
    private String email;

    @Column(name = "whatsapp_contact", length = 50)
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

    @Column(length = 255, columnDefinition = "VARCHAR(255) DEFAULT 'United Kingdom'")
    @Builder.Default
    private String country = "United Kingdom";

    // Public URL Token
    @Column(name = "public_url_token", length = 64, unique = true)
    private String publicUrlToken;

    // Priority and Pinning
    @Column(length = 255)
    @Builder.Default
    private String priority = "Normal";

    @Column(name = "is_family")
    @Builder.Default
    private Boolean isFamily = false;

    @Transient // Was @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    @Transient // Was @Column(name = "pinned_by_username", length = 50)
    private String pinnedByUsername;

    // Notes
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Audit Fields
    @Column(name = "created_by_username", length = 50)
    private String createdByUsername;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_by_username", length = 50)
    private String lastUpdatedByUsername;

    @LastModifiedDate
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    // Relationships
    @OneToMany(mappedBy = "traveler", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Dependent> dependents = new ArrayList<>();



    // Fields mapped to DB columns
    @Column(length = 100)
    private String name;

    @Column(name = "place_of_birth", length = 100)
    private String placeOfBirth;

    @Column(name = "country_of_birth", length = 100)
    private String countryOfBirth;

    @Column(name = "visa_link", length = 500)
    private String visaLink;

    @Column(name = "application_form_link", length = 500)
    private String applicationFormLink;

    @Column(name = "application_form_username", length = 100)
    private String applicationFormUsername;

    @Column(name = "application_form_password", length = 100)
    private String applicationFormPassword;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "appointment_remarks", columnDefinition = "TEXT")
    private String appointmentRemarks;

    @Column(length = 100)
    private String username;

    @Column(columnDefinition = "TEXT")
    private String logins;

    @Column(name = "relationship_to_main", length = 50)
    private String relationshipToMain;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_type", length = 20)
    private String discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "invoice_subtotal", precision = 10, scale = 2)
    private BigDecimal invoiceSubtotal;

    @Column(name = "invoice_discount_type", length = 20)
    private String invoiceDiscountType;

    @Column(name = "invoice_discount_value", precision = 10, scale = 2)
    private BigDecimal invoiceDiscountValue;

    @Column(name = "invoice_discount_amount", precision = 10, scale = 2)
    private BigDecimal invoiceDiscountAmount;

    @Column(name = "invoice_total", precision = 10, scale = 2)
    private BigDecimal invoiceTotal;

    @Column(name = "invoice_items_json", columnDefinition = "TEXT")
    private String invoiceItemsJson;

    @Column(name = "invoice_generated")
    @Builder.Default
    private Boolean invoiceGenerated = false;

    @Column(name = "invoice_generated_at")
    private LocalDateTime invoiceGeneratedAt;

    // Helper methods
    public void addDependent(Dependent dependent) {
        dependents.add(dependent);
        dependent.setTraveler(this);
    }

    public void removeDependent(Dependent dependent) {
        dependents.remove(dependent);
        dependent.setTraveler(null);
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return name != null ? name : "";
    }

    public String getInvoiceNumber() {
        return "INV-" + String.format("%04d", id);
    }

    @PrePersist
    public void prePersist() {
        if (publicUrlToken == null) {
            publicUrlToken = generateToken();
        }
        if (status == null) {
            status = "Wait App";
        }
        if (priority == null) {
            priority = "Normal";
        }
        if (country == null) {
            country = "United Kingdom";
        }
    }

    private String generateToken() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
