package com.myenglishvocab.server.analytics.repository;

import com.myenglishvocab.server.analytics.entity.AccountLifecycleEvent;
import com.myenglishvocab.server.analytics.entity.AccountLifecycleType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AccountLifecycleEventRepository extends JpaRepository<AccountLifecycleEvent, Long> {

    long countByEventType(AccountLifecycleType eventType);

    long countByEventTypeAndOccurredAtGreaterThanEqual(
            AccountLifecycleType eventType,
            Instant from
    );

    List<AccountLifecycleEvent> findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            Instant from,
            Instant to
    );

    List<AccountLifecycleEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
