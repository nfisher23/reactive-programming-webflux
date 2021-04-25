package com.nickolasfisher.reactiveredis;

import io.lettuce.core.Range;
import io.lettuce.core.ScoredValue;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ExpiringSortedSetEntriesTest extends BaseSetupAndTeardownRedis {

    @Test
    public void expireElementsPeriodically() throws Exception {
        String setKey = "values-set-key";

        addValueToSet(setKey, "first", Instant.now().plus(450, ChronoUnit.MILLIS).toEpochMilli());
        Thread.sleep(100);

        addValueToSet(setKey, "second", Instant.now().plus(150, ChronoUnit.MILLIS).toEpochMilli());
        Thread.sleep(100);

        addValueToSet(setKey, "third", Instant.now().plus(500, ChronoUnit.MILLIS).toEpochMilli());
        Thread.sleep(100);

        // expire everything based on score, or time to expire as epoch millisecond
        Mono<Long> expireOldEntriesMono = redisReactiveCommands.zremrangebyscore(setKey,
                Range.create(0, Instant.now().toEpochMilli())
        );

        StepVerifier.create(expireOldEntriesMono)
                .expectNext(1L).verifyComplete();

        // get all entries
        StepVerifier.create(redisReactiveCommands.zrevrangebyscore(setKey, Range.unbounded()))
                .expectNextMatches(val -> "third".equals(val))
                .expectNextMatches(val -> "first".equals(val))
                .verifyComplete();
    }

    private void addValueToSet(String setKey, String value, long epochMilliToExpire) {
        Mono<Long> addValueWithEpochMilliScore = redisReactiveCommands.zadd(
                setKey,
                ScoredValue.just(epochMilliToExpire, value)
        );

        StepVerifier.create(addValueWithEpochMilliScore)
                .expectNext(1L)
                .verifyComplete();
    }

}
