package com.nickolasfisher.reactiveredis;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

//https://redis.io/commands/#set
public class SimpleSetsTest extends BaseSetupAndTeardownRedis {

    @Test
    public void sAdd_and_sRem() {
        String setKey = "set-key-1";
        Mono<Long> saddMono = redisReactiveCommands.sadd(setKey, "value-1", "value-2");

        StepVerifier.create(saddMono)
                .expectNextMatches(numberOfElementsAdded -> 2L == numberOfElementsAdded)
                .verifyComplete();

        Mono<Long> saddOneRepeatingValueMono = redisReactiveCommands.sadd(setKey, "value-1", "value-3");

        StepVerifier.create(saddOneRepeatingValueMono)
                .expectNextMatches(numberOfElementsAdded -> 1L == numberOfElementsAdded)
                .verifyComplete();

        Mono<List<String>> smembersCollectionMono = redisReactiveCommands.smembers(setKey).collectList();

        StepVerifier.create(smembersCollectionMono)
                .expectNextMatches(setMembers -> setMembers.size() == 3 && setMembers.contains("value-3"))
                .verifyComplete();

        Mono<Long> sremValue3Mono = redisReactiveCommands.srem(setKey, "value-3");

        StepVerifier.create(sremValue3Mono)
                .expectNextMatches(numRemoved -> numRemoved == 1L)
                .verifyComplete();

        StepVerifier.create(smembersCollectionMono)
                .expectNextMatches(setMembers -> setMembers.size() == 2 && !setMembers.contains("value-3"));
    }

    @Test
    public void sisMember() {
        String setKey = "set-key-1";
        Mono<Long> saddMono = redisReactiveCommands.sadd(setKey, "value-1", "value-2");

        StepVerifier.create(saddMono)
                .expectNextMatches(numberOfElementsAdded -> 2L == numberOfElementsAdded)
                .verifyComplete();

        Mono<Boolean> shouldNotExistInSetMono = redisReactiveCommands.sismember(setKey, "value-3");

        StepVerifier.create(shouldNotExistInSetMono)
                .expectNext(false)
                .verifyComplete();

        Mono<Boolean> shouldExistInSetMono = redisReactiveCommands.sismember(setKey, "value-2");

        StepVerifier.create(shouldExistInSetMono)
                .expectNext(true)
                .verifyComplete();
    }
}
