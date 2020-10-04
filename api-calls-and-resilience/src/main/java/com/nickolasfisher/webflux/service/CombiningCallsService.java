package com.nickolasfisher.webflux.service;

import com.nickolasfisher.webflux.model.FirstCallDTO;
import com.nickolasfisher.webflux.model.MergedCallsDTO;
import com.nickolasfisher.webflux.model.SecondCallDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.function.Function;

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

    public Mono<MergedCallsDTO> mergedCalls(Integer firstEndpointParam, Integer secondEndpointParam) {
        Mono<FirstCallDTO> firstCallDTOMono = this.serviceAWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/first/endpoint/{param}").build(firstEndpointParam))
                .retrieve()
                .bodyToMono(FirstCallDTO.class);

        Mono<SecondCallDTO> secondCallDTOMono = this.serviceAWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/second/endpoint/{param}").build(secondEndpointParam))
                .retrieve()
                .bodyToMono(SecondCallDTO.class);

        // nothing has been subscribed to, those calls above are not waiting for anything and are not subscribed to, yet

        // zipping the monos will invoke the callback in "map" once both of them have completed, merging the results
        // into a tuple.
        return Mono.zip(firstCallDTOMono, secondCallDTOMono)
                .map(objects -> {
                    MergedCallsDTO mergedCallsDTO = new MergedCallsDTO();

                    mergedCallsDTO.setFieldOne(objects.getT1().getFieldFromFirstCall());
                    mergedCallsDTO.setFieldTwo(objects.getT2().getFieldFromSecondCall());

                    return mergedCallsDTO;
                });
    }
}
