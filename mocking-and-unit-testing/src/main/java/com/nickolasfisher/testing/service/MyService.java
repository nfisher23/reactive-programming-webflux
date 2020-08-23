package com.nickolasfisher.testing.service;

import com.nickolasfisher.testing.dto.DownstreamResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

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
                .bodyToFlux(DownstreamResponseDTO.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(250))
                            .minBackoff(Duration.ofMillis(100))
                );
    }
}
