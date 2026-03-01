package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "locker_activities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockerActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "traveler_id", nullable = false)
    private Long travelerId;

    @Column(name = "traveler_name")
    private String travelerName;

    /** First 8 chars of the public URL token — for display only */
    @Column(length = 20)
    private String token;

    /** FORM_STARTED | VIEW_CHANGED | FILE_UPLOADED | FILE_DELETED | FORM_SUBMITTED */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
