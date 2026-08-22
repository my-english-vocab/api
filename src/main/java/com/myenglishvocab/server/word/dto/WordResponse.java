package com.myenglishvocab.server.word.dto;

import com.myenglishvocab.server.word.entity.Word;

import java.time.Instant;

public record WordResponse(
        Long id,
        String term,
        String definition,
        int level,
        boolean favorite,
        String exampleSentence,
        String meaningOfExampleSentence,
        Instant createdAt
) {
    public static WordResponse from(Word word) {
        return new WordResponse(
                word.getId(),
                word.getTerm(),
                word.getDefinition(),
                word.getLevel(),
                word.isFavorite(),
                word.getExampleSentence(),
                word.getMeaningOfExampleSentence(),
                word.getCreatedAt()
        );
    }
}
