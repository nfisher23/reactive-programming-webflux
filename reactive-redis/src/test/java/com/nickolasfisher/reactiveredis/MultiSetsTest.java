package com.nickolasfisher.reactiveredis;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

//https://redis.io/commands/#set
public class MultiSetsTest extends BaseSetupAndTeardownRedis {

    @Test
    public void subtractingMultipleSets() {
        String firstSetKey = "first-set-key";
        String secondSetKey = "second-set-key";
        Mono<Long> setupFirstSetMono = redisReactiveCommands.sadd(firstSetKey, "value-1", "value-2");

        StepVerifier.create(setupFirstSetMono).expectNext(2L).verifyComplete();

        Mono<Long> setupSecondSetMono = redisReactiveCommands.sadd(secondSetKey, "value-1", "value-3");

        StepVerifier.create(setupSecondSetMono).expectNext(2L).verifyComplete();

        Mono<List<String>> subtractSecondFromFirstCollection = redisReactiveCommands.sdiff(firstSetKey, secondSetKey).collectList();

        StepVerifier.create(subtractSecondFromFirstCollection)
                .expectNextMatches(collection ->
                        collection.size() == 1
                        && collection.contains("value-2"))
                .verifyComplete();

        Mono<List<String>> subtractFirstFromSecondCollection = redisReactiveCommands.sdiff(secondSetKey, firstSetKey).collectList();

        StepVerifier.create(subtractFirstFromSecondCollection)
                .expectNextMatches(collection ->
                        collection.size() == 1
                                && collection.contains("value-3"))
                .verifyComplete();

        Mono<List<String>> originalSetUnchangedMono = redisReactiveCommands.smembers(firstSetKey).collectList();

        StepVerifier.create(originalSetUnchangedMono)
                .expectNextMatches(firstSetMembers ->
                        firstSetMembers.size() == 2
                        && firstSetMembers.contains("value-1")
                        && firstSetMembers.contains("value-2")
                ).verifyComplete();
    }

    @Test
    public void intersectingMultipleSets() {
        String firstSetKey = "first-set-key";
        String secondSetKey = "second-set-key";
        Mono<Long> setupFirstSetMono = redisReactiveCommands
                .sadd(firstSetKey, "value-1", "value-2");

        StepVerifier.create(setupFirstSetMono).expectNext(2L).verifyComplete();

        Mono<Long> setupSecondSetMono = redisReactiveCommands
                .sadd(secondSetKey, "value-1", "value-3");

        StepVerifier.create(setupSecondSetMono).expectNext(2L).verifyComplete();

        Mono<List<String>> intersectedSets = redisReactiveCommands
                .sinter(firstSetKey, secondSetKey).collectList();

        StepVerifier.create(intersectedSets)
                .expectNextMatches(collection ->
                    collection.size() == 1
                        && collection.contains("value-1")
                        && !collection.contains("value-2")
                )
                .verifyComplete();
    }

    @Test
    public void addingMultipleSetsTogether() {
        String firstSetKey = "first-set-key";
        String secondSetKey = "second-set-key";
        Mono<Long> setupFirstSetMono = redisReactiveCommands
                .sadd(firstSetKey, "value-1", "value-2");

        StepVerifier.create(setupFirstSetMono).expectNext(2L).verifyComplete();

        Mono<Long> setupSecondSetMono = redisReactiveCommands
                .sadd(secondSetKey, "value-1", "value-3");

        StepVerifier.create(setupSecondSetMono).expectNext(2L).verifyComplete();

        Mono<List<String>> unionedSets = redisReactiveCommands
                .sunion(firstSetKey, secondSetKey).collectList();

        StepVerifier.create(unionedSets)
                .expectNextMatches(collection ->
                    collection.size() == 3
                        && collection.contains("value-1")
                        && collection.contains("value-2")
                        && collection.contains("value-3")
                )
                .verifyComplete();
    }
}
