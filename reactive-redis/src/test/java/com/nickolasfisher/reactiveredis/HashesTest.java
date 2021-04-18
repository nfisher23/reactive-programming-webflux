package com.nickolasfisher.reactiveredis;

import io.lettuce.core.KeyValue;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

public class HashesTest extends BaseSetupAndTeardownRedis {

    @Test
    public void setSingleHashAndGetWholeHash() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        Mono<String> multiSetMono = redisReactiveCommands.hmset("hash-set-key", Map.of(
                "key-1", "value-1",
                "key-2", "value-2",
                "key-3", "value-3"
        ));

        StepVerifier.create(multiSetMono)
                .expectNextMatches(response -> "OK".equals(response))
                .verifyComplete();

        Mono<List<KeyValue<String, String>>> allKeyValuesMono = redisReactiveCommands.hgetall("hash-set-key").collectList();

        StepVerifier.create(allKeyValuesMono)
                .expectNextMatches(keyValues -> keyValues.size() == 3
                    && keyValues.stream()
                        .anyMatch(keyValue -> keyValue.getValue().equals("value-2")
                                && keyValue.getKey().equals("key-2"))
                )
                .verifyComplete();
    }

    @Test
    public void getAndSetSingleValueInHash() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        Mono<String> multiSetMono = redisReactiveCommands.hmset("hash-set-key", Map.of(
                "key-1", "value-1",
                "key-2", "value-2",
                "key-3", "value-3"
        ));

        StepVerifier.create(multiSetMono)
                .expectNextMatches(response -> "OK".equals(response))
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.hget("hash-set-key", "key-1"))
                .expectNextMatches(val -> val.equals("value-1"))
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.hset("hash-set-key", "key-2", "new-value-2"))
                // returns false if no new fields were added--in this case we're changing an existing field
                .expectNextMatches(response -> !response)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.hget("hash-set-key", "key-2"))
                .expectNextMatches(val -> "new-value-2".equals(val))
                .verifyComplete();

        // different value in the same hash is unchanged
        StepVerifier.create(redisReactiveCommands.hget("hash-set-key", "key-1"))
                .expectNextMatches(val -> "value-1".equals(val))
                .verifyComplete();
    }
}
