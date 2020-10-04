package com.nickolasfisher.webflux.service;

import com.nickolasfisher.webflux.service.CombiningCallsService;
import io.swagger.models.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@ExtendWith(MockServerExtension.class)
public class CombiningCallsServiceIT {
    private CombiningCallsService combiningCallsService;

    private WebClient webClient;
    private ClientAndServer clientAndServer;

    public CombiningCallsServiceIT(ClientAndServer clientAndServer) {
        this.clientAndServer = clientAndServer;
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:" + clientAndServer.getPort())
                .build();
    }

    @BeforeEach
    public void setup() {
        combiningCallsService = new CombiningCallsService(webClient);
    }

    @AfterEach
    public void reset() {
        clientAndServer.reset();
    }

    @Test
    public void callsFirstAndUsesCallToGetSecond() {
        HttpRequest expectedFirstRequest = HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/first/endpoint/10");

        this.clientAndServer.when(
                expectedFirstRequest
        ).respond(
                HttpResponse.response()
                        .withBody("{\"fieldFromFirstCall\": 100}")
                        .withContentType(MediaType.APPLICATION_JSON)
        );

        HttpRequest expectedSecondRequest = HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/second/endpoint/100");

        this.clientAndServer.when(
                expectedSecondRequest
        ).respond(
                HttpResponse.response()
                        .withBody("{\"fieldFromSecondCall\": \"hello\"}")
                        .withContentType(MediaType.APPLICATION_JSON)
        );

        StepVerifier.create(this.combiningCallsService.sequentialCalls(10))
                .expectNextMatches(secondCallDTO -> "hello".equals(secondCallDTO.getFieldFromSecondCall()))
                .verifyComplete();

        this.clientAndServer.verify(expectedFirstRequest, VerificationTimes.once());
        this.clientAndServer.verify(expectedSecondRequest, VerificationTimes.once());
    }

    @Test
    public void mergedCalls_callsBothEndpointsAndMergesResults() {
        HttpRequest expectedFirstRequest = HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/first/endpoint/25");

        this.clientAndServer.when(
                expectedFirstRequest
        ).respond(
                HttpResponse.response()
                        .withBody("{\"fieldFromFirstCall\": 250}")
                        .withContentType(MediaType.APPLICATION_JSON)
        );

        HttpRequest expectedSecondRequest = HttpRequest.request()
                .withMethod(HttpMethod.GET.name())
                .withPath("/second/endpoint/45");

        this.clientAndServer.when(
                expectedSecondRequest
        ).respond(
                HttpResponse.response()
                        .withBody("{\"fieldFromSecondCall\": \"something\"}")
                        .withContentType(MediaType.APPLICATION_JSON)
        );

        StepVerifier.create(this.combiningCallsService.mergedCalls(25, 45))
                .expectNextMatches(mergedCallsDTO -> 250 == mergedCallsDTO.getFieldOne()
                        && "something".equals(mergedCallsDTO.getFieldTwo())
                )
                .verifyComplete();

        this.clientAndServer.verify(expectedFirstRequest, VerificationTimes.once());
        this.clientAndServer.verify(expectedSecondRequest, VerificationTimes.once());
    }

}
