package com.nickolasfisher.webflux;

import com.nickolasfisher.webflux.model.FirstCallDTO;
import com.nickolasfisher.webflux.model.SecondCallDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CombiningCallsService {

    private final WebClient serviceAWebClient;

    public CombiningCallsService(@Qualifier("service-a-web-client") WebClient serviceAWebClient) {
        this.serviceAWebClient = serviceAWebClient;
    }

    public Mono<SecondCallDTO> sequentialCalls(Integer key) {
        return this.serviceAWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/first/endpoint/{param}").build(key))
                .retrieve()
                .bodyToMono(FirstCallDTO.class)
                .zipWhen(firstCallDTO ->
                    serviceAWebClient.get().uri(
                            uriBuilder ->
                                    uriBuilder.path("/second/endpoint/{param}")
                                            .build(firstCallDTO.getFieldFromFirstCall()))
                            .retrieve()
                            .bodyToMono(SecondCallDTO.class),
                    (firstCallDTO, secondCallDTO) -> secondCallDTO
                );
    }
}
