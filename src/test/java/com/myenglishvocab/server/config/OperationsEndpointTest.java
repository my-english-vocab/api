package com.myenglishvocab.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:operations-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "jwt.secret=test-secret-key-must-be-at-least-32-characters-long",
        "cors.allowed-origins=http://localhost:3000",
        "ai.provider=openai",
        "ai.openai.api-key=test-openai-key"
})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "prod"})
class OperationsEndpointTest {

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void 운영_프로필에서는_H2_console이_비활성화된다() {
        assertEquals("false", environment.getProperty("spring.h2.console.enabled"));
    }

    @Test
    void 운영_프로필에서는_Swagger_UI가_비활성화된다() {
        assertEquals("false", environment.getProperty("springdoc.swagger-ui.enabled"));
    }

    @Test
    void 운영_프로필에서는_API_docs가_비활성화된다() {
        assertEquals("false", environment.getProperty("springdoc.api-docs.enabled"));
    }

    @Test
    void open_in_view가_비활성화된다() {
        assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
    }

    @Test
    void 운영_프로필에서는_H2_console_경로가_공개되지_않는다() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 운영_프로필에서는_Swagger_경로가_공개되지_않는다() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 운영_프로필에서는_API_docs_경로가_공개되지_않는다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void actuator_health는_인증_없이_조회할_수_있다() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuator_health는_상세_구성요소를_노출하지_않는다() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void actuator_기본_경로는_인증_없이는_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isForbidden());
    }

    @Test
    void Spring_Security_기본_사용자_자동_설정이_제외된다() {
        assertThat(applicationContext.getBeansOfType(UserDetailsService.class)).isEmpty();
    }
}
