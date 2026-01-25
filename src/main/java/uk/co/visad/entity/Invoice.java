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
@Table(name = "invoices", indexes = {
        @Index(name = "idx_traveler_id", columnList = "traveler_id"),
        @Index(name = "idx_invoice_number", columnList = "invoice_number"),
        @Index(name = "idx_invoice_date", columnList = "invoice_date"),
        @Index(name = "idx_payment_status", columnList = "payment_status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traveler_id", nullable = false, columnDefinition = "INT")
    private Traveler traveler;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_type", length = 20)
    @Builder.Default
    private String invoiceType = "invoice";

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "customer_address", columnDefinition = "TEXT")
    private String customerAddress;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", columnDefinition = "ENUM('none','percentage','fixed')")
    @Builder.Default
    private DiscountType discountType = DiscountType.none;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO; // Calculated discount in GBP

    @Column(name = "discount_calculated", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountCalculated = BigDecimal.ZERO; // Actual discount value applied

    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", columnDefinition = "ENUM('none','inclusive','exclusive')")
    @Builder.Default
    private TaxType taxType = TaxType.none;

    @Column(name = "tax_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 3)
    @Builder.Default
    private String currency = "GBP";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", columnDefinition = "ENUM('Unpaid','Partial','Paid','Refunded','Cancelled')")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.Unpaid;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_outstanding", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountOutstanding = BigDecimal.ZERO;

    @Column(name = "visa_country", length = 255)
    private String visaCountry;

    @Column(name = "visa_type", length = 100)
    private String visaType;

    @Column(name = "package", length = 100)
    private String packageType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Column(name = "footer_message", columnDefinition = "TEXT")
    private String footerMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "sent_to", length = 255)
    private String sentTo;

    @Column(name = "sent_count")
    @Builder.Default
    private Integer sentCount = 0;

    // Relationships
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoicePayment> payments = new ArrayList<>();

    // Enums
    public enum DiscountType {
        none, percentage, fixed
    }

    public enum TaxType {
        none, inclusive, exclusive
    }

    public enum PaymentStatus {
        Unpaid, Partial, Paid, Refunded, Cancelled
    }
}
