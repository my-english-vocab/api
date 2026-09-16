package com.myenglishvocab.server.onboarding;

import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.service.ActivityService;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.user.repository.UserRepository;
import com.myenglishvocab.server.word.entity.Word;
import com.myenglishvocab.server.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final OnboardingCatalog catalog;
    private final WordRepository words;
    private final UserRepository users;
    private final ActivityService activities;

    @Transactional(readOnly = true)
    public boolean isEligible(Long userId) {
        return !words.existsByUserId(userId);
    }

    @Transactional
    public OnboardingController.CompleteResponse complete(Long userId, OnboardingController.CompleteRequest request) {
        var track = catalog.findTrack(request.trackId());
        var selected = new HashSet<>(request.wordIds());
        var available = track.words().stream().map(OnboardingCatalog.StarterWord::id).collect(Collectors.toSet());
        if (request.version() != catalog.get().version() || selected.size() != request.wordIds().size()
                || !available.containsAll(selected)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // Serialize tutorial saves for this account, including retries and multiple tabs.
        var user = users.findByIdForOnboarding(userId)
                .filter(u -> u.isActive())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var existing = words.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(word -> word.getTerm().strip().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        int added = 0;
        for (var entry : track.words()) {
            if (selected.contains(entry.id()) && existing.add(entry.term().toLowerCase(Locale.ROOT))) {
                words.save(Word.builder().user(user).term(entry.term()).definition(entry.definition())
                        .exampleSentence(entry.exampleSentence())
                        .meaningOfExampleSentence(entry.meaningOfExampleSentence()).build());
                activities.record(user, ActivityType.WORD_CREATED, null, Instant.now());
                added++;
            }
        }
        return new OnboardingController.CompleteResponse(added, selected.size() - added);
    }
}
