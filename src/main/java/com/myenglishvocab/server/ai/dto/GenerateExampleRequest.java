package com.myenglishvocab.server.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateExampleRequest(
        @NotBlank @Size(max = 100) String term,
        @Size(max = 150) String definition
) {
}
