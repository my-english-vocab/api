package com.myenglishvocab.server.quiz.service;

import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.quiz.dto.CompleteQuizSetAttemptRequest;
import com.myenglishvocab.server.quiz.dto.QuizSetAttemptSummaryResponse;
import com.myenglishvocab.server.quiz.entity.QuizSetAttempt;
import com.myenglishvocab.server.quiz.repository.QuizSetAttemptRepository;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSetAttemptService {

    private final QuizSetAttemptRepository quizSetAttemptRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuizSetAttemptSummaryResponse> getMySummaries(Long userId) {
        return quizSetAttemptRepository.findSummariesByUserId(userId);
    }

    @Transactional
    public List<QuizSetAttemptSummaryResponse> complete(
            Long userId,
            int setNumber,
            CompleteQuizSetAttemptRequest request
    ) {
        if (setNumber < 1 || request.learnedCount() > request.wordCount()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (quizSetAttemptRepository.findByUserIdAndAttemptId(userId, request.attemptId()).isPresent()) {
            return quizSetAttemptRepository.findSummariesByUserId(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        QuizSetAttempt attempt = new QuizSetAttempt(
                user,
                request.attemptId(),
                setNumber,
                request.wordCount(),
                request.learnedCount()
        );
        quizSetAttemptRepository.save(attempt);

        log.info(
                "퀴즈 세트 완료 userId={} setNumber={} wordCount={} learnedCount={}",
                userId,
                setNumber,
                request.wordCount(),
                request.learnedCount()
        );

        return quizSetAttemptRepository.findSummariesByUserId(userId);
    }
}
