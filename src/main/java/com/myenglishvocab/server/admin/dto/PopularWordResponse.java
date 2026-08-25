package com.myenglishvocab.server.admin.dto;

public record PopularWordResponse(
        String term,
        long savedCount,
        long userCount
) {
}
