package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_items", indexes = {
        @Index(name = "idx_invoice_id", columnList = "invoice_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false, columnDefinition = "INT")
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", columnDefinition = "ENUM('main','co-traveler')")
    @Builder.Default
    private ItemType itemType = ItemType.main;

    @Column(name = "traveler_id")
    private Integer travelerId;

    @Column(name = "dependent_id")
    private Integer dependentId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "traveler_name", length = 255)
    private String travelerName;

    @Column(name = "package", length = 100)
    private String packageType;

    @Column(name = "visa_type", length = 100)
    private String visaType;

    @Column(name = "visa_country", length = 255)
    private String visaCountry;

    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_discount_type", columnDefinition = "ENUM('none','percentage','fixed')")
    @Builder.Default
    private Invoice.DiscountType itemDiscountType = Invoice.DiscountType.none;

    @Column(name = "item_discount_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal itemDiscountValue = BigDecimal.ZERO;

    @Column(name = "item_discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal itemDiscountAmount = BigDecimal.ZERO;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    public enum ItemType {
        main, co_traveler {
            @Override
            public String toString() {
                return "co-traveler";
            }
        }
    }
}
