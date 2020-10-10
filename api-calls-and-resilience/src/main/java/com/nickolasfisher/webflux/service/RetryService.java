package com.nickolasfisher.webflux.service;

import com.nickolasfisher.webflux.model.WelcomeMessage;
import io.netty.handler.timeout.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class RetryService {
    private final WebClient serviceAWebClient;

    public RetryService(@Qualifier("service-a-web-client") WebClient serviceAWebClient) {
        this.serviceAWebClient = serviceAWebClient;
    }

    public Mono<WelcomeMessage> getWelcomeMessageAndHandleTimeout(String locale) {
        return this.serviceAWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/locale/{locale}/message").build(locale))
                .retrieve()
                .bodyToMono(WelcomeMessage.class)
                .retryWhen(
                    Retry.backoff(2, Duration.ofMillis(25))
                            .filter(throwable -> throwable instanceof TimeoutException)
                );
    }
}
