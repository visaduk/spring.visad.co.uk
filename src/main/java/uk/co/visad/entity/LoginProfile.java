package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "login_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @Column(name = "profile_name", nullable = false, length = 255)
    private String profileName;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String password;

    @Column(length = 50)
    @Builder.Default
    private String priority = "Normal";

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "pinned_by_username", length = 50)
    private String pinnedByUsername;
}
