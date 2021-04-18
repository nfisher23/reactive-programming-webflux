package com.nickolasfisher.reactiveredis;

import io.lettuce.core.KeyValue;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertTrue;

public class ListsTest extends BaseSetupAndTeardownRedis {

    @Test
    public void addAndRemoveFromTheLeft() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        StepVerifier.create(redisReactiveCommands.lpush("list-key", "fourth-element", "third-element"))
                .expectNextMatches(sizeOfList -> 2L == sizeOfList)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.lpush("list-key","second-element", "first-element"))
                // pushes to the left of the same list
                .expectNextMatches(sizeOfList -> 4L == sizeOfList)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.lpop("list-key"))
                .expectNextMatches(poppedElement -> "first-element".equals(poppedElement))
                .verifyComplete();
    }

    @Test
    public void blockingGet() {
        RedisReactiveCommands<String, String> redisReactiveCommands1 = redisClient.connect().reactive();
        RedisReactiveCommands<String, String> redisReactiveCommands2 = redisClient.connect().reactive();

        long startingTime = Instant.now().toEpochMilli();
        StepVerifier.create(Mono.zip(
                    redisReactiveCommands1.blpop(1, "list-key").switchIfEmpty(Mono.just(KeyValue.empty("list-key"))),
                    Mono.delay(Duration.ofMillis(500)).then(redisReactiveCommands2.lpush("list-key", "an-element"))
                ).map(tuple -> tuple.getT1().getValue())
            )
            .expectNextMatches(value -> "an-element".equals(value))
            .verifyComplete();
        long endingTime = Instant.now().toEpochMilli();

        assertTrue(endingTime - startingTime > 400);
    }

    @Test
    public void getRange() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        StepVerifier.create(redisReactiveCommands.lpush("list-key", "third-element", "second-element", "first-element"))
                .expectNextMatches(sizeOfList -> 3L == sizeOfList)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.lrange("list-key", 0, 1))
                .expectNextMatches(first -> "first-element".equals(first))
                .expectNextMatches(second -> "second-element".equals(second))
                .verifyComplete();
    }
}
