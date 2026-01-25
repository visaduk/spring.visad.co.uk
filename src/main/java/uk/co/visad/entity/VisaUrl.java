package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "visa_urls", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "country", "visa_center" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "visa_center", length = 150)
    private String visaCenter;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "application_form_url", length = 500)
    private String applicationFormUrl;

    @Column(name = "is_uploaded_file")
    @Builder.Default
    private Boolean isUploadedFile = false;

    // Helper method to get display name
    public String getDisplayName() {
        if (visaCenter != null && !visaCenter.isEmpty()) {
            return country + " - " + visaCenter;
        }
        return country;
    }
}
