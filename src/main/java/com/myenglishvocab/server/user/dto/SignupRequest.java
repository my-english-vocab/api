package com.myenglishvocab.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 4, max = 20) String username,
        @NotBlank @Size(min = 4, max = 50) String password,
        @NotBlank @Size(min = 1, max = 30) String displayName
) {
}
