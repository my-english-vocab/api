package com.myenglishvocab.server.ai.dto;

public record GenerateExampleResponse(
        String definition,
        String exampleSentence,
        String meaningOfExampleSentence
) {
}
