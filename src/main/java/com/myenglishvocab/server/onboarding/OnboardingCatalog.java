package com.myenglishvocab.server.onboarding;

import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

/** Curated content is shared by the preview and save APIs; clients submit IDs only. */
@Component
public class OnboardingCatalog {
    private final Catalog catalog;

    public OnboardingCatalog() throws IOException {
        try (var stream = new ClassPathResource("onboarding/catalog.json").getInputStream()) {
            catalog = JsonMapper.builder().build().readValue(stream, Catalog.class);
        }
    }

    public Catalog get() {
        return catalog;
    }

    public Track findTrack(String id) {
        return catalog.tracks().stream().filter(track -> track.id().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    public record Catalog(int version, List<Track> tracks) {}
    public record Track(String id, String title, String subtitle, String description, List<StarterWord> words) {}
    public record StarterWord(String id, String difficulty, String term, String definition,
                              String exampleSentence, String meaningOfExampleSentence) {}
}
