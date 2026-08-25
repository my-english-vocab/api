package com.myenglishvocab.server.analytics.repository;

import com.myenglishvocab.server.admin.dto.PopularPageResponse;
import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.entity.UserActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, Long> {

    List<UserActivityEvent> findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            Instant from,
            Instant to
    );

    long countByEventType(ActivityType eventType);

    long countByEventTypeAndOccurredAtGreaterThanEqual(
            ActivityType eventType,
            Instant from
    );

    @Query("""
            SELECT COUNT(DISTINCT event.user.id)
            FROM UserActivityEvent event
            WHERE event.occurredAt >= :from
              AND event.occurredAt < :to
              AND event.user IS NOT NULL
            """)
    long countDistinctActiveUsers(Instant from, Instant to);

    @Query("SELECT MAX(event.occurredAt) FROM UserActivityEvent event")
    Instant findMostRecentActivityAt();

    @Query("""
            SELECT new com.myenglishvocab.server.admin.dto.PopularPageResponse(
                event.path,
                COUNT(event),
                COUNT(DISTINCT event.user.id)
            )
            FROM UserActivityEvent event
            WHERE event.eventType = com.myenglishvocab.server.analytics.entity.ActivityType.PAGE_VIEW
              AND event.occurredAt >= :from
              AND event.path IS NOT NULL
            GROUP BY event.path
            ORDER BY COUNT(event) DESC, event.path
            """)
    List<PopularPageResponse> findPopularPagesSince(Instant from, Pageable pageable);
}
