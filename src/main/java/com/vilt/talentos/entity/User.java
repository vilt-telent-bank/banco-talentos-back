package com.vilt.talentos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.RESOURCE;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DomainStatus status = DomainStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private Instant verificationCodeExpiresAt;

    @Builder.Default
    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires")
    private Instant resetTokenExpires;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "refresh_token", unique = true)
    private String refreshToken;

    @Column(name = "refresh_token_expires")
    private Instant refreshTokenExpires;
}
