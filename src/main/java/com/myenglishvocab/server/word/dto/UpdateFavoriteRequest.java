package com.myenglishvocab.server.word.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateFavoriteRequest(
        @NotNull Boolean favorite
) {
}
