package com.nickolasfisher.webflux.service;

import com.nickolasfisher.webflux.model.WelcomeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

public class CachingServiceIT {

    @Test
    public void englishLocaleWelcomeMessage_caches() {
        RetryService mockFallbackService = Mockito.mock(RetryService.class);

        AtomicInteger counter = new AtomicInteger();
        Mockito.when(mockFallbackService.getWelcomeMessageAndHandleTimeout("en_US"))
                .thenReturn(Mono.defer(() ->
                            Mono.just(new WelcomeMessage("count " + counter.incrementAndGet()))
                        )
                );

        CachingService cachingService = new CachingService(mockFallbackService);

        StepVerifier.create(cachingService.getEnglishLocaleWelcomeMessage())
                .expectNextMatches(welcomeMessage -> "count 1".equals(welcomeMessage.getMessage()))
                .verifyComplete();

        StepVerifier.create(cachingService.getEnglishLocaleWelcomeMessage())
                .expectNextMatches(welcomeMessage -> "count 1".equals(welcomeMessage.getMessage()))
                .verifyComplete();
    }

    @Test
    public void cachesSuccessOnly() {
        RetryService mockRetryService = Mockito.mock(RetryService.class);

        AtomicInteger counter = new AtomicInteger();
        Mockito.when(mockRetryService.getWelcomeMessageAndHandleTimeout("en_US"))
                .thenReturn(Mono.defer(() -> {
                            if (counter.incrementAndGet() > 1) {
                                return Mono.just(new WelcomeMessage("count " + counter.get()));
                            } else {
                                return Mono.error(new RuntimeException());
                            }
                        })
                );

        CachingService cachingService = new CachingService(mockRetryService);

        StepVerifier.create(cachingService.getEnglishLocaleWelcomeMessage())
                .expectError()
                .verify();

        StepVerifier.create(cachingService.getEnglishLocaleWelcomeMessage())
                .expectNextMatches(welcomeMessage -> "count 2".equals(welcomeMessage.getMessage()))
                .verifyComplete();

        // previous result should be cached
        StepVerifier.create(cachingService.getEnglishLocaleWelcomeMessage())
                .expectNextMatches(welcomeMessage -> "count 2".equals(welcomeMessage.getMessage()))
                .verifyComplete();
    }
}
