package com.myenglishvocab.server.word.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WordApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 단어_CRUD와_mark_learned_흐름() throws Exception {
        String username = "wd_" + System.currentTimeMillis() % 1_000_000_000L;
        String token = signupAndLogin(username);

        MvcResult createResult = mockMvc.perform(post("/api/words")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "term": "apple",
                                  "definition": "사과",
                                  "exampleSentence": "I like apples.",
                                  "meaningOfExampleSentence": "나는 사과를 좋아한다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.term").value("apple"))
                .andExpect(jsonPath("$.level").value(0))
                .andReturn();

        int wordId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/words")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].term").value("apple"));

        mockMvc.perform(get("/api/words/" + wordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(wordId));

        mockMvc.perform(put("/api/words/" + wordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "term": "apple",
                                  "definition": "사과(수정)",
                                  "exampleSentence": "I like apples.",
                                  "meaningOfExampleSentence": "나는 사과를 좋아한다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.definition").value("사과(수정)"))
                .andExpect(jsonPath("$.level").value(0));

        mockMvc.perform(post("/api/words/" + wordId + "/mark-learned")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1));

        mockMvc.perform(delete("/api/words/" + wordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/words/" + wordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORD_NOT_FOUND"));
    }

    @Test
    void 다른_사용자의_단어는_404로_숨긴다() throws Exception {
        String owner = "ow_" + System.currentTimeMillis() % 1_000_000_000L;
        String ownerToken = signupAndLogin(owner);

        MvcResult createResult = mockMvc.perform(post("/api/words")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "term": "secret",
                                  "definition": "비밀"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        int wordId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        String other = "ot_" + System.currentTimeMillis() % 1_000_000_000L;
        String otherToken = signupAndLogin(other);

        mockMvc.perform(get("/api/words/" + wordId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORD_NOT_FOUND"));

        mockMvc.perform(put("/api/words/" + wordId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "term": "hack",
                                  "definition": "해킹"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORD_NOT_FOUND"));

        mockMvc.perform(delete("/api/words/" + wordId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORD_NOT_FOUND"));
    }

    @Test
    void 토큰_없이_단어_API_호출하면_거부된다() throws Exception {
        mockMvc.perform(get("/api/words"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 잘못된_입력이면_400_표준_에러응답() throws Exception {
        String token = signupAndLogin("iv_" + System.currentTimeMillis() % 1_000_000_000L);

        mockMvc.perform(post("/api/words")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "term": "",
                                  "definition": "사과"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
    }

    private String signupAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password1",
                                  "displayName": "테스터"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password1"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
    }
}
