package com.myenglishvocab.server.onboarding;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingApiTest {
    @Autowired MockMvc mvc;
    @Autowired OnboardingCatalog catalog;

    @Test
    void curated_content_has_ten_complete_words_in_each_track() {
        assertEquals(7, catalog.get().tracks().size());
        var allWords = catalog.get().tracks().stream().flatMap(track -> track.words().stream()).toList();
        assertEquals(70, allWords.stream().map(OnboardingCatalog.StarterWord::id).distinct().count());
        assertEquals(70, allWords.stream().map(word -> word.term().toLowerCase(java.util.Locale.ROOT)).distinct().count());
        for (var track : catalog.get().tracks()) {
            assertEquals(10, track.words().size());
            assertEquals(10, track.words().stream().map(OnboardingCatalog.StarterWord::id).distinct().count());
            assertEquals(2, track.words().stream().filter(w -> w.difficulty().equals("beginner")).count());
            assertEquals(3, track.words().stream().filter(w -> w.difficulty().equals("intermediate")).count());
            assertEquals(5, track.words().stream().filter(w -> w.difficulty().equals("advanced")).count());
            assertEquals(List.of("beginner", "beginner", "intermediate", "intermediate", "intermediate",
                            "advanced", "advanced", "advanced", "advanced", "advanced"),
                    track.words().stream().map(OnboardingCatalog.StarterWord::difficulty).toList());
            for (var word : track.words()) {
                assertFalse(word.term().isBlank());
                assertTrue(word.term().length() <= 100);
                assertFalse(word.definition().isBlank());
                assertTrue(word.definition().length() <= 150);
                assertFalse(word.exampleSentence().isBlank());
                assertTrue(word.exampleSentence().length() <= 1000);
                assertFalse(word.meaningOfExampleSentence().isBlank());
                assertTrue(word.meaningOfExampleSentence().length() <= 1000);
            }
        }
    }

    @Test
    void added_tracks_save_their_own_words_and_examples() throws Exception {
        for (var trackId : List.of("toefl", "opic", "business-email", "it-dev")) {
            var token = signupAndLogin();
            var word = catalog.findTrack(trackId).words().getLast();
            save(token, """
                    {"version":3,"trackId":"%s","wordIds":["%s"]}
                    """.formatted(trackId, word.id()), 1, 0);
            mvc.perform(get("/api/words").header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].term").value(word.term()))
                    .andExpect(jsonPath("$[0].definition").value(word.definition()))
                    .andExpect(jsonPath("$[0].exampleSentence").value(word.exampleSentence()))
                    .andExpect(jsonPath("$[0].meaningOfExampleSentence").value(word.meaningOfExampleSentence()));
        }
    }

    @Test
    void saves_selected_canonical_words_and_retries_without_duplicates() throws Exception {
        var token = signupAndLogin();
        mvc.perform(get("/api/onboarding/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.eligible").value(true));
        mvc.perform(get("/api/onboarding/catalog").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tracks.length()").value(7));
        save(token, """
                {"version":3,"trackId":"toeic","wordIds":["toeic-schedule","toeic-invoice"]}
                """, 2, 0);
        save(token, """
                {"version":3,"trackId":"toeic","wordIds":["toeic-schedule","toeic-invoice"]}
                """, 0, 2);
        mvc.perform(get("/api/words").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].term").value("schedule"))
                .andExpect(jsonPath("$[0].exampleSentence").value(catalog.findTrack("toeic").words().getFirst().exampleSentence()))
                .andExpect(jsonPath("$[0].meaningOfExampleSentence").isNotEmpty())
                .andExpect(jsonPath("$[0].level").value(0));
        mvc.perform(get("/api/onboarding/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.eligible").value(false));
        var other = signupAndLogin();
        mvc.perform(get("/api/words").header("Authorization", "Bearer " + other))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejects_invalid_selections_before_any_word_is_saved() throws Exception {
        var token = signupAndLogin();
        for (String body : List.of(
                """
                {"version":3,"trackId":"toeic","wordIds":["toeic-schedule","csat-evidence"]}
                """,
                """
                {"version":3,"trackId":"toeic","wordIds":["toeic-schedule","toeic-schedule"]}
                """,
                """
                {"version":1,"trackId":"toeic","wordIds":["toeic-schedule"]}
                """,
                """
                {"version":3,"trackId":"unknown","wordIds":[]}
                """,
                """
                {"version":3,"trackId":"toeic","wordIds":null}
                """)) {
            mvc.perform(post("/api/onboarding/complete").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/words").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
        save(token, """
                {"version":3,"trackId":"daily","wordIds":[]}
                """, 0, 0);
    }

    @Test
    void existing_words_are_preserved_including_their_content() throws Exception {
        var token = signupAndLogin();
        mvc.perform(post("/api/words").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term":"SCHEDULE","definition":"내가 적은 뜻"}
                                """))
                .andExpect(status().isCreated());
        save(token, """
                {"version":3,"trackId":"toeic","wordIds":["toeic-schedule"]}
                """, 0, 1);
        mvc.perform(get("/api/words").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].definition").value("내가 적은 뜻"));
    }

    @Test
    void concurrent_completions_do_not_duplicate_words() throws Exception {
        var token = signupAndLogin();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                var response = mvc.perform(post("/api/onboarding/complete")
                                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"version":3,"trackId":"daily","wordIds":["daily-order","daily-layover"]}
                                        """))
                        .andExpect(status().isOk()).andReturn();
                return JsonPath.<Integer>read(response.getResponse().getContentAsString(), "$.addedCount");
            })).toList();
            start.countDown();
            assertEquals(2, tasks.get(0).get(15, TimeUnit.SECONDS) + tasks.get(1).get(15, TimeUnit.SECONDS));
        }
        mvc.perform(get("/api/words").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void all_endpoints_require_authentication() throws Exception {
        mvc.perform(get("/api/onboarding/status")).andExpect(status().isForbidden());
        mvc.perform(get("/api/onboarding/catalog")).andExpect(status().isForbidden());
        mvc.perform(post("/api/onboarding/complete").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"version":3,"trackId":"daily","wordIds":[]}
                        """)).andExpect(status().isForbidden());
    }

    private void save(String token, String body, int added, int existing) throws Exception {
        mvc.perform(post("/api/onboarding/complete").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.addedCount").value(added))
                .andExpect(jsonPath("$.existingCount").value(existing));
    }

    private String signupAndLogin() throws Exception {
        String username = "on_" + UUID.randomUUID().toString().substring(0, 12);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password1","displayName":"튜토리얼 테스트"}
                                """.formatted(username))).andExpect(status().isCreated());
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password1"}
                                """.formatted(username))).andExpect(status().isOk()).andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }
}
