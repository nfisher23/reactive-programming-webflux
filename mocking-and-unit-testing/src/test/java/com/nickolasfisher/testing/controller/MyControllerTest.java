package com.nickolasfisher.testing.controller;

import com.nickolasfisher.testing.dto.DownstreamResponseDTO;
import com.nickolasfisher.testing.service.MyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class MyControllerTest {
    private MyService myServiceMock;

    private MyController myController;

    @BeforeEach
    public void setup() {
        myServiceMock = Mockito.mock(MyService.class);
        myController = new MyController(myServiceMock);
    }

    @Test
    public void verifyTransformsCorrectly() {
        DownstreamResponseDTO downstreamResponseDTO_1 = new DownstreamResponseDTO();
        downstreamResponseDTO_1.setFirstName("jack");
        downstreamResponseDTO_1.setLastName("attack");
        downstreamResponseDTO_1.setDeepesetFear("spiders");
        downstreamResponseDTO_1.setSsn("123-45-6789");

        DownstreamResponseDTO downstreamResponseDTO_2 = new DownstreamResponseDTO();
        downstreamResponseDTO_2.setFirstName("karen");
        downstreamResponseDTO_2.setLastName("cool");
        downstreamResponseDTO_2.setDeepesetFear("snakes");
        downstreamResponseDTO_2.setSsn("000-00-0000");

        Mockito.when(myServiceMock.getAllPeople())
                .thenReturn(Flux.just(downstreamResponseDTO_1, downstreamResponseDTO_2));

        StepVerifier.create(myController.getPersons())
                .expectNextMatches(personDTO -> personDTO.getLastName().equals(downstreamResponseDTO_1.getLastName())
                        && personDTO.getFirstName().equals(downstreamResponseDTO_1.getFirstName()))
                .expectNextMatches(personDTO -> personDTO.getLastName().equals(downstreamResponseDTO_2.getLastName())
                        && personDTO.getFirstName().equals(downstreamResponseDTO_2.getFirstName()))
                .verifyComplete();
    }
}
