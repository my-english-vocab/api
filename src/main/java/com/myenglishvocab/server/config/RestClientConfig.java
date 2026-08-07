package com.myenglishvocab.server.config;

import com.myenglishvocab.server.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final AiProperties aiProperties;

    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(aiProperties.getTimeout().getConnect())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                aiProperties.getTimeout().getResponse()
        );

        return RestClient.builder().requestFactory(requestFactory);
    }
}
