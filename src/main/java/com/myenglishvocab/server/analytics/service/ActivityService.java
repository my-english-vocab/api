package com.myenglishvocab.server.analytics.service;

import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.entity.UserActivityEvent;
import com.myenglishvocab.server.analytics.repository.UserActivityEventRepository;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.entity.UserStatus;
import com.myenglishvocab.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final UserActivityEventRepository activityEventRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(Long userId, ActivityType eventType) {
        record(userId, eventType, null);
    }

    @Transactional
    public void record(Long userId, ActivityType eventType, String path) {
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        record(user, eventType, path, Instant.now());
    }

    public void record(User user, ActivityType eventType, String path, Instant occurredAt) {
        user.recordActivity(occurredAt);
        activityEventRepository.save(new UserActivityEvent(user, eventType, path, occurredAt));
    }
}
