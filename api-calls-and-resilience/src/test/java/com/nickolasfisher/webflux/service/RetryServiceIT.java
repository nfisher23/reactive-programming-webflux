package com.nickolasfisher.webflux.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.swagger.models.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockserver.model.HttpRequest.request;

@ExtendWith(MockServerExtension.class)
public class RetryServiceIT {

    public static final int WEBCLIENT_TIMEOUT = 50;
    private final ClientAndServer clientAndServer;

    private RetryService retryService;
    private WebClient mockWebClient;

    public RetryServiceIT(ClientAndServer clientAndServer) {
        this.clientAndServer = clientAndServer;
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(tcpClient ->
                        tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, WEBCLIENT_TIMEOUT)
                                .doOnConnected(connection -> connection.addHandlerLast(
                                        new ReadTimeoutHandler(WEBCLIENT_TIMEOUT, TimeUnit.MILLISECONDS))
                                )
                );

        this.mockWebClient = WebClient.builder()
                .baseUrl("http://localhost:" + this.clientAndServer.getPort())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @BeforeEach
    public void setup() {
        this.retryService = new RetryService(mockWebClient);
    }

    @AfterEach
    public void clearExpectations() {
        this.clientAndServer.reset();
    }

    @Test
    public void retryOnTimeout() {
        AtomicInteger counter = new AtomicInteger();
        HttpRequest expectedRequest = request()
                .withPath("/locale/en_US/message")
                .withMethod(HttpMethod.GET.name());

        this.clientAndServer.when(
                expectedRequest
        ).respond(
                httpRequest -> {
                    if (counter.incrementAndGet() < 2) {
                        Thread.sleep(WEBCLIENT_TIMEOUT + 10);
                    }
                    return HttpResponse.response()
                            .withBody("{\"message\": \"hello\"}")
                            .withContentType(MediaType.APPLICATION_JSON);
                }
        );

        StepVerifier.create(retryService.getWelcomeMessageAndHandleTimeout("en_US"))
                .expectNextMatches(welcomeMessage -> "hello".equals(welcomeMessage.getMessage()))
                .verifyComplete();

        this.clientAndServer.verify(expectedRequest, VerificationTimes.exactly(3));
    }
}
