package com.myenglishvocab.server.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PageViewRequest(
        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "^/[^?#]*$", message = "path에는 경로만 입력할 수 있습니다.")
        String path
) {
}
