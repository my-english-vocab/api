package com.myenglishvocab.server.ai.quota;

public interface AiUsageLimiter {

    /**
     * 계정당 하루 사용량 1회 차감. 한도 초과 시 예외.
     */
    void consume(Long userId);

    /**
     * 오늘 사용한 횟수(차감 없이 조회만). 없으면 0.
     */
    int getUsedCount(Long userId);
}
