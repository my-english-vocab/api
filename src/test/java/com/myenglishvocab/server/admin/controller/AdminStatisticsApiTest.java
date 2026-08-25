package com.myenglishvocab.server.admin.controller;

import com.jayway.jsonpath.JsonPath;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.entity.UserRole;
import com.myenglishvocab.server.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminStatisticsApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void 관리자만_통계에_접근하고_가입_활동_탈퇴_새계정가입을_조회한다() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis() % 1_000_000_000L);
        String adminUsername = "adm_" + suffix;
        String userUsername = "usr_" + suffix;

        signup(adminUsername);
        User admin = userRepository.findByUsername(adminUsername).orElseThrow();
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        userRepository.saveAndFlush(admin);
        String adminToken = login(adminUsername);

        signup(userUsername);
        String userToken = login(userUsername);

        mockMvc.perform(get("/api/admin/statistics/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/analytics/page-views")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"path":"/words"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/words")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "term":"analytics",
                                  "definition":"분석"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/quiz/sets/1/attempts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attemptId":"%s",
                                  "wordCount":1,
                                  "learnedCount":1
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/statistics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAccounts").isNumber())
                .andExpect(jsonPath("$.newSignupsLast7Days").isNumber())
                .andExpect(jsonPath("$.wordUsersLast7Days").isNumber())
                .andExpect(jsonPath("$.dailyActiveUsers").isNumber())
                .andExpect(jsonPath("$.monthlyActiveUsers").isNumber())
                .andExpect(jsonPath("$.totalPageViews").isNumber())
                .andExpect(jsonPath("$.quizUsers").isNumber());

        mockMvc.perform(get("/api/admin/statistics/daily?days=7")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[6].date").isNotEmpty());

        mockMvc.perform(get("/api/admin/statistics/monthly?months=3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/admin/statistics/popular-words?limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/statistics/popular-pages?days=30&limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/statistics/users?limit=100")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/withdraw")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"password1"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/analytics/page-views")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"path":"/words"}
                                """))
                .andExpect(status().isForbidden());

        signup(userUsername);
        String newAccountToken = login(userUsername);
        assertThat(newAccountToken).isNotBlank();

        MvcResult lifecycleResult = mockMvc.perform(
                        get("/api/admin/statistics/account-lifecycle?limit=500")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String lifecycleJson = lifecycleResult.getResponse().getContentAsString();
        assertThat(lifecycleJson)
                .contains("WITHDRAWAL", "SIGNUP", userUsername)
                .doesNotContain("REJOIN");
    }

    @Test
    void 페이지_경로에_쿼리문자열을_보내면_거부한다() throws Exception {
        String username = "path_" + System.currentTimeMillis() % 1_000_000_000L;
        signup(username);
        String token = login(username);

        mockMvc.perform(post("/api/analytics/page-views")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"path":"/words?term=private"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_INPUT"));
    }

    private void signup(String username) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"password1",
                                  "displayName":"테스터"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"password1"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
