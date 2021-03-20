package com.nickolasfisher.webflux.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nickolasfisher.webflux.model.WelcomeMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
public class CachingService {

    private final Cache<String, WelcomeMessage>
            WELCOME_MESSAGE_CACHE = Caffeine.newBuilder()
                                        .expireAfterWrite(Duration.ofMinutes(5))
                                        .maximumSize(1_000)
                                        .build();

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

    public Mono<WelcomeMessage> getCachedWelcomeMono(String locale) {
        Optional<WelcomeMessage> message = Optional.ofNullable(WELCOME_MESSAGE_CACHE.getIfPresent(locale));

        return message
                .map(Mono::just)
                .orElseGet(() ->
                        this.retryService.getWelcomeMessageAndHandleTimeout(locale)
                            .doOnNext(welcomeMessage -> WELCOME_MESSAGE_CACHE.put(locale, welcomeMessage))
                );
    }
}
