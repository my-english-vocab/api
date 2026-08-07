package com.myenglishvocab.server.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "ai.timeout.response=100ms")
@ActiveProfiles("test")
class RestClientTimeoutTest {

    @Autowired
    private RestClient.Builder restClientBuilder;

    private HttpServer slowServer;

    @BeforeEach
    void setUp() throws IOException {
        slowServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        slowServer.createContext("/slow", exchange -> {
            try {
                Thread.sleep(500);
                byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        slowServer.start();
    }

    @AfterEach
    void tearDown() {
        slowServer.stop(0);
    }

    @Test
    void 응답이_timeout보다_늦으면_요청이_실패한다() {
        String url = "http://localhost:%d/slow".formatted(slowServer.getAddress().getPort());
        RestClient restClient = restClientBuilder.build();

        assertThrows(ResourceAccessException.class, () ->
                restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class)
        );
    }
}
