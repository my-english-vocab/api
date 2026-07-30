package com.myenglishvocab.server.word.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWordRequest(
        @NotBlank @Size(max = 100) String term,
        @NotBlank @Size(max = 150) String definition,
        @Size(max = 1000) String exampleSentence,
        @Size(max = 1000) String meaningOfExampleSentence
) {
}
