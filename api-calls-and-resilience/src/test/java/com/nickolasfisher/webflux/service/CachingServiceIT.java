package com.nickolasfisher.webflux.service;

import com.nickolasfisher.webflux.model.WelcomeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

public class CachingServiceIT {

    @Test
    public void englishLocaleWelcomeMessage_caches() {
        RetryService mockRetryService = Mockito.mock(RetryService.class);

        AtomicInteger counter = new AtomicInteger();
        Mockito.when(mockRetryService.getWelcomeMessageAndHandleTimeout("en_US"))
                .thenReturn(Mono.defer(() ->
                            Mono.just(new WelcomeMessage("count " + counter.incrementAndGet()))
                        )
                );

        CachingService cachingService = new CachingService(mockRetryService);

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

    @Test
    public void getCachedWelcomeMono_cachesSuccess() {
        RetryService mockRetryService = Mockito.mock(RetryService.class);

        AtomicInteger timesInvoked = new AtomicInteger(0);
        Mockito.when(mockRetryService.getWelcomeMessageAndHandleTimeout(anyString()))
                .thenAnswer(new Answer<Mono<WelcomeMessage>>() {
                    @Override
                    public Mono<WelcomeMessage> answer(InvocationOnMock invocation) throws Throwable {
                        String locale_arg = invocation.getArgument(0);
                        return Mono.defer(() -> {
                            timesInvoked.incrementAndGet();
                            return Mono.just(new WelcomeMessage("locale " + locale_arg));
                        });
                    }
                });

        CachingService cachingService = new CachingService(mockRetryService);

        for (int i = 0; i < 3; i++) {
            StepVerifier.create(cachingService.getCachedWelcomeMono("en"))
                    .expectNextMatches(welcomeMessage -> "locale en".equals(welcomeMessage.getMessage()))
                    .verifyComplete();
        }

        for (int i = 0; i < 5; i++) {
            StepVerifier.create(cachingService.getCachedWelcomeMono("ru"))
                    .expectNextMatches(welcomeMessage -> "locale ru".equals(welcomeMessage.getMessage()))
                    .verifyComplete();
        }

        assertEquals(2, timesInvoked.get());
    }

    @Test
    public void getCachedWelcomeMono_doesNotCacheFailure() {
        RetryService mockRetryService = Mockito.mock(RetryService.class);

        AtomicInteger timesInvoked = new AtomicInteger(0);
        Mockito.when(mockRetryService.getWelcomeMessageAndHandleTimeout(anyString()))
                .thenAnswer(new Answer<Mono<WelcomeMessage>>() {
                    @Override
                    public Mono<WelcomeMessage> answer(InvocationOnMock invocation) throws Throwable {
                        String locale_arg = invocation.getArgument(0);
                        return Mono.defer(() -> {
                            if (timesInvoked.incrementAndGet() > 1) {
                                return Mono.just(new WelcomeMessage("locale " + locale_arg));
                            } else {
                                return Mono.error(new RuntimeException());
                            }
                        });
                    }
                });

        CachingService cachingService = new CachingService(mockRetryService);

        StepVerifier.create(cachingService.getCachedWelcomeMono("en"))
                .verifyError();

        for (int i = 0; i < 3; i++) {
            StepVerifier.create(cachingService.getCachedWelcomeMono("en"))
                    .expectNextMatches(welcomeMessage -> "locale en".equals(welcomeMessage.getMessage()))
                    .verifyComplete();
        }

        assertEquals(2, timesInvoked.get());
    }
}
