package com.myenglishvocab.server.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant lastLoginAt;

    private Instant lastActiveAt;

    private Instant withdrawnAt;

    @Builder
    public User(String username, String password, String displayName) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public void recordLogin(Instant now) {
        this.lastLoginAt = now;
        this.lastActiveAt = now;
    }

    public void recordActivity(Instant now) {
        this.lastActiveAt = now;
    }

    public void withdraw(String anonymizedUsername, String disabledPassword, Instant now) {
        this.username = anonymizedUsername;
        this.password = disabledPassword;
        this.displayName = "탈퇴 사용자";
        this.role = UserRole.USER;
        this.status = UserStatus.WITHDRAWN;
        this.lastActiveAt = now;
        this.withdrawnAt = now;
    }

}
