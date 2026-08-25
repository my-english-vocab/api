package com.myenglishvocab.server.analytics.entity;

import com.myenglishvocab.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "account_lifecycle_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountLifecycleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String usernameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountLifecycleType eventType;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    public AccountLifecycleEvent(
            User user,
            String usernameSnapshot,
            AccountLifecycleType eventType,
            Instant occurredAt
    ) {
        this.user = user;
        this.usernameSnapshot = usernameSnapshot;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }
}
