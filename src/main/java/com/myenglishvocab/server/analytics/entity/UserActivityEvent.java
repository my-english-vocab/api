package com.myenglishvocab.server.analytics.entity;

import com.myenglishvocab.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_activity_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActivityType eventType;

    @Column(length = 255)
    private String path;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    public UserActivityEvent(User user, ActivityType eventType, String path, Instant occurredAt) {
        this.user = user;
        this.eventType = eventType;
        this.path = path;
        this.occurredAt = occurredAt;
    }
}
