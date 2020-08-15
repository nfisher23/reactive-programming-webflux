package com.nickolasfisher.testing.service;

import com.nickolasfisher.testing.dto.DownstreamResponseDTO;
import com.nickolasfisher.testing.dto.PersonDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class MyService {

    private final WebClient webClient;

    public MyService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<DownstreamResponseDTO> getAllPeople() {
        return this.webClient.get()
                .uri("/legacy/persons")
                .retrieve()
                .bodyToFlux(DownstreamResponseDTO.class);
    }
}
