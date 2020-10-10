package com.nickolasfisher.webflux.service;

import com.nickolasfisher.webflux.model.WelcomeMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class CachingService {

    private final RetryService retryService;
    private final Mono<WelcomeMessage> cachedEnglishWelcomeMono;

    public CachingService(RetryService retryService) {
        this.retryService = retryService;
        this.cachedEnglishWelcomeMono = this.retryService.getWelcomeMessageAndHandleTimeout("en_US")
                .cache(welcomeMessage -> Duration.ofMinutes(5),
                        throwable -> Duration.ZERO,
                        () -> Duration.ZERO
                );
    }

    public Mono<WelcomeMessage> getEnglishLocaleWelcomeMessage() {
        return cachedEnglishWelcomeMono;
    }
}
