package com.nickolasfisher.webfluxcontext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.nickolasfisher.webfluxcontext.WebContextFilter.X_CUSTOM_HEADER;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(new ExchangeFilterFunction() {
                    @Override
                    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction exchangeFunction) {
                        return Mono.subscriberContext()
                                .flatMap(context -> {
                                    String[] wat = context.get(X_CUSTOM_HEADER);
                                    ClientRequest clientReq = ClientRequest.from(clientRequest)
                                            .header(X_CUSTOM_HEADER, wat)
                                            .build();

                                    return exchangeFunction.exchange(clientReq);
                                });
                    }
                })
                .baseUrl("http://localhost:9000")
                .build();
    }
}
