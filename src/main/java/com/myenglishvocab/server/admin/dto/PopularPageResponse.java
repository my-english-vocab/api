package com.myenglishvocab.server.admin.dto;

public record PopularPageResponse(
        String path,
        long viewCount,
        long userCount
) {
}
