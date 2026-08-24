package com.myenglishvocab.server.quiz.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuizSetAttemptApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 세트_완료를_기록하고_attemptId가_같으면_중복_집계하지_않는다() throws Exception {
        String token = signupAndLogin("qs_" + System.currentTimeMillis() % 1_000_000_000L);
        UUID attemptId = UUID.randomUUID();
        String request = """
                {
                  "attemptId": "%s",
                  "wordCount": 20,
                  "learnedCount": 14
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/quiz/sets/2/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].setNumber").value(2))
                .andExpect(jsonPath("$[0].completedCount").value(1))
                .andExpect(jsonPath("$[0].lastCompletedAt").isNotEmpty());

        mockMvc.perform(post("/api/quiz/sets/2/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].completedCount").value(1));

        mockMvc.perform(get("/api/quiz/sets/attempts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].setNumber").value(2))
                .andExpect(jsonPath("$[0].completedCount").value(1));
    }

    @Test
    void 외웠어요_개수가_전체_단어보다_많으면_400을_반환한다() throws Exception {
        String token = signupAndLogin("qi_" + System.currentTimeMillis() % 1_000_000_000L);

        mockMvc.perform(post("/api/quiz/sets/1/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attemptId": "%s",
                                  "wordCount": 12,
                                  "learnedCount": 13
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
    }

    @Test
    void 세트_번호가_1보다_작으면_400을_반환한다() throws Exception {
        String token = signupAndLogin("qn_" + System.currentTimeMillis() % 1_000_000_000L);

        mockMvc.perform(post("/api/quiz/sets/0/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attemptId": "%s",
                                  "wordCount": 20,
                                  "learnedCount": 10
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
    }

    @Test
    void 토큰이_없으면_완료_기록을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/quiz/sets/attempts"))
                .andExpect(status().isForbidden());
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
