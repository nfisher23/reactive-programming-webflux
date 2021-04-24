package com.nickolasfisher.reactiveredis;

import io.lettuce.core.Range;
import io.lettuce.core.ScoredValue;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

// https://redis.io/commands/#sorted_set
public class SortedSetsTest extends BaseSetupAndTeardownRedis {

    @Test
    public void zAddAndUpdate() {
        String setKey = "set-key-1";
        Mono<Long> addOneHundredScoreMono = redisReactiveCommands.zadd(setKey, ScoredValue.just(100, "one hundred"));

        StepVerifier.create(addOneHundredScoreMono)
                .expectNextMatches(numAdded -> 1L == numAdded).verifyComplete();

        Mono<Double> getOneHundredScoreMono = redisReactiveCommands.zscore(setKey, "one hundred");

        StepVerifier.create(getOneHundredScoreMono)
                .expectNextMatches(score -> score < 100.01 && score > 99.99)
                .verifyComplete();

        Mono<Double> elementDoesNotExistMono = redisReactiveCommands.zscore(setKey, "not here");

        StepVerifier.create(elementDoesNotExistMono)
                .verifyComplete();

        Mono<Long> updateOneHundredScoreMono = redisReactiveCommands.zadd(setKey, ScoredValue.just(105, "one hundred"));

        StepVerifier.create(updateOneHundredScoreMono)
                // updated, not added, so 0
                .expectNextMatches(numAdded -> 0L == numAdded)
                .verifyComplete();

        StepVerifier.create(getOneHundredScoreMono)
                .expectNextMatches(score -> score < 105.01 && score > 104.99)
                .verifyComplete();
    }

    @Test
    public void zRange_Rank_AndScore() {
        String setKey = "set-key-1";
        Mono<Long> addOneHundredScoreMono = redisReactiveCommands.zadd(setKey, ScoredValue.just(100, "one hundred"));

        StepVerifier.create(addOneHundredScoreMono)
                .expectNextMatches(numAdded -> 1L == numAdded).verifyComplete();

        Mono<List<ScoredValue<String>>> allCollectedElementsMono = redisReactiveCommands
                .zrangebyscoreWithScores(setKey, Range.unbounded()).collectList();

        StepVerifier.create(allCollectedElementsMono)
                .expectNextMatches(allElements -> allElements.size() == 1
                                && allElements.stream().allMatch(
                        scoredValue -> scoredValue.getScore() == 100
                                && scoredValue.getValue().equals("one hundred")
                        )
                ).verifyComplete();

        Mono<Long> addFiftyMono = redisReactiveCommands.zadd(setKey, ScoredValue.just(50, "fifty"));

        StepVerifier.create(addFiftyMono)
                .expectNextMatches(numAdded -> 1L == numAdded)
                .verifyComplete();

        // by default, lowest score is at the front, or zero index
        StepVerifier.create(allCollectedElementsMono)
                .expectNextMatches(
                        allElements -> allElements.size() == 2
                                && allElements.get(0).equals(ScoredValue.just(50, "fifty"))
                                && allElements.get(1).equals(ScoredValue.just(100, "one hundred"))
                ).verifyComplete();
    }

    @Test
    public void zRevRangeByScore() {
        String setKey = "set-key-1";
        Mono<Long> addOneHundredScoreMono = redisReactiveCommands
                .zadd(
                        setKey,
                        ScoredValue.just(100, "first"),
                        ScoredValue.just(200, "second"),
                        ScoredValue.just(300, "third")
                );

        StepVerifier.create(addOneHundredScoreMono)
                .expectNextMatches(numAdded -> 3L == numAdded).verifyComplete();

        Mono<Long> removeElementsByScoreMono = redisReactiveCommands
                .zremrangebyscore(setKey, Range.create(90, 210));

        StepVerifier.create(removeElementsByScoreMono)
                .expectNext(2L)
                .verifyComplete();

        Mono<List<ScoredValue<String>>> allCollectedElementsMono = redisReactiveCommands
                .zrangebyscoreWithScores(setKey, Range.unbounded()).collectList();

        StepVerifier.create(allCollectedElementsMono)
                .expectNextMatches(allElements -> allElements.size() == 1
                                && allElements.stream().allMatch(
                        scoredValue -> scoredValue.getScore() == 300
                                && scoredValue.getValue().equals("third")
                        )
                ).verifyComplete();
    }
}
