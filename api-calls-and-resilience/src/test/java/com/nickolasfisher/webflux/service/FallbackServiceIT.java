package com.nickolasfisher.webflux.service;

import io.swagger.models.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
public class FallbackServiceIT {

    private WebClient webClient;
    private ClientAndServer clientAndServer;

    private FallbackService fallbackService;

    public FallbackServiceIT(ClientAndServer clientAndServer) {
        this.clientAndServer = clientAndServer;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:" + clientAndServer.getPort())
                .build();
    }

    @BeforeEach
    public void setup() {
        fallbackService = new FallbackService(webClient);
    }

    @AfterEach
    public void reset() {
        clientAndServer.reset();
    }

    @Test
    public void welcomeMessage_worksWhenNoErrors() {
        this.clientAndServer.when(
                request()
                        .withPath("/locale/en_US/message")
                        .withMethod(HttpMethod.GET.name())
        ).respond(
                HttpResponse.response()
                        .withBody("{\"message\": \"hello\"}")
                        .withContentType(MediaType.APPLICATION_JSON)
        );

        StepVerifier.create(fallbackService.getWelcomeMessageByLocale("en_US"))
                .expectNextMatches(welcomeMessage -> "hello".equals(welcomeMessage.getMessage()))
                .verifyComplete();
    }

    @Test
    public void welcomeMessage_fallsBackToEnglishWhenError() {
        this.clientAndServer.when(
                request()
                    .withPath("/locale/fr/message")
                    .withMethod(HttpMethod.GET.name())
        ).respond(
                HttpResponse.response()
                    .withStatusCode(503)
        );

        StepVerifier.create(fallbackService.getWelcomeMessageByLocale("fr"))
                .expectNextMatches(welcomeMessage -> "hello fallback!".equals(welcomeMessage.getMessage()))
                .verifyComplete();
    }
}
