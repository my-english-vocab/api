package com.myenglishvocab.server.user.controller;

import com.jayway.jsonpath.JsonPath;
import com.myenglishvocab.server.auth.cookie.AuthCookieFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 회원가입_로그인_me_성공_흐름() throws Exception {
        String username = "tester_" + System.currentTimeMillis() % 1_000_000_000L;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password1",
                                  "displayName": "테스터"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password1"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(cookie().exists(AuthCookieFactory.REFRESH_COOKIE_NAME))
                .andExpect(cookie().httpOnly(AuthCookieFactory.REFRESH_COOKIE_NAME, true))
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void 토큰_없이_me_호출하면_거부된다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 중복_회원가입이면_표준_에러응답() throws Exception {
        String username = "dup_" + System.currentTimeMillis() % 1_000_000_000L;
        String payload = """
                {
                  "username": "%s",
                  "password": "password1",
                  "displayName": "테스터"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_DUPLICATE_USERNAME"));
    }

    @Test
    void refresh_성공_후_이전_refresh는_재사용_불가() throws Exception {
        String username = "rf_" + System.currentTimeMillis() % 1_000_000_000L;
        signup(username);

        Cookie oldCookie = loginAndGetRefreshCookie(username);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(oldCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists(AuthCookieFactory.REFRESH_COOKIE_NAME))
                .andReturn();

        String newAccessToken = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.accessToken");
        Cookie newCookie = refreshResult.getResponse().getCookie(AuthCookieFactory.REFRESH_COOKIE_NAME);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(oldCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(newCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void logout_후_refresh는_무효화된다() throws Exception {
        String username = "lo_" + System.currentTimeMillis() % 1_000_000_000L;
        signup(username);
        Cookie refreshCookie = loginAndGetRefreshCookie(username);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthCookieFactory.REFRESH_COOKIE_NAME, 0));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 잘못된_refresh면_401_표준_에러응답() throws Exception {
        Cookie fake = new Cookie(AuthCookieFactory.REFRESH_COOKIE_NAME, "not-a-real-token");
        fake.setPath("/api/auth");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(fake))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REFRESH_TOKEN"));
    }

    private void signup(String username) throws Exception {
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
    }

    private Cookie loginAndGetRefreshCookie(String username) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "password1"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(AuthCookieFactory.REFRESH_COOKIE_NAME))
                .andReturn();

        return loginResult.getResponse().getCookie(AuthCookieFactory.REFRESH_COOKIE_NAME);
    }
}
