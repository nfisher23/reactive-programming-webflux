package com.nickolasfisher.testing.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickolasfisher.testing.dto.DownstreamResponseDTO;
import io.swagger.models.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpResponse.response;

public class MyServiceTest {

    private ClientAndServer mockServer;

    private MyService myService;

    private static final ObjectMapper serializer = new ObjectMapper();

    @BeforeEach
    public void setupMockServer() {
        mockServer = ClientAndServer.startClientAndServer(2001);
        myService = new MyService(WebClient.builder()
                .baseUrl("http://localhost:" + mockServer.getLocalPort()).build());
    }

    @AfterEach
    public void tearDownServer() {
        mockServer.stop();
    }

    @Test
    public void testTheThing() throws JsonProcessingException {
        String responseBody = getDownstreamResponseDTOAsString();
        mockServer.when(
                request()
                    .withMethod(HttpMethod.GET.name())
                    .withPath("/legacy/persons")
        ).respond(
                response()
                    .withStatusCode(HttpStatus.OK.value())
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody(responseBody)
        );

        List<DownstreamResponseDTO> responses = myService.getAllPeople().collectList().block();

        assertEquals(1, responses.size());
        assertEquals("first", responses.get(0).getFirstName());
        assertEquals("last", responses.get(0).getLastName());

        mockServer.verify(
                request().withMethod(HttpMethod.GET.name())
                    .withPath("/legacy/persons")
        );
    }

    private String getDownstreamResponseDTOAsString() throws JsonProcessingException {
        DownstreamResponseDTO downstreamResponseDTO = new DownstreamResponseDTO();

        downstreamResponseDTO.setLastName("last");
        downstreamResponseDTO.setFirstName("first");
        downstreamResponseDTO.setSsn("123-12-1231");
        downstreamResponseDTO.setDeepesetFear("alligators");

        return serializer.writeValueAsString(Arrays.asList(downstreamResponseDTO));
    }

    @Test
    public void retriesOnFailure() throws JsonProcessingException {
        String responseBody = getDownstreamResponseDTOAsString();

        AtomicInteger counter = new AtomicInteger(0);
        mockServer.when(
                request()
                    .withMethod(HttpMethod.GET.name())
                    .withPath("/legacy/persons")
        ).respond(
                new ExpectationResponseCallback() {
                    @Override
                    public HttpResponse handle(HttpRequest httpRequest) throws Exception {
                        int attempt = counter.incrementAndGet();
                        if (attempt >= 2) {
                            return response().
                                    withBody(responseBody)
                                    .withContentType(MediaType.APPLICATION_JSON)
                                    .withStatusCode(HttpStatus.OK.value());
                        } else {
                            return response().withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                        }
                    }
                }
        );

        List<DownstreamResponseDTO> responses = myService.getAllPeople().collectList().block();

        assertEquals(1, responses.size());
        assertEquals("first", responses.get(0).getFirstName());
        assertEquals("last", responses.get(0).getLastName());

        mockServer.verify(
                request().withMethod(HttpMethod.GET.name())
                        .withPath("/legacy/persons")
        );
    }
}
