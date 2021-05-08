package com.nickolasfisher.reactiveredis;

import io.lettuce.core.Range;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.protocol.CommandArgs;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RedisStreamsTest extends BaseSetupAndTeardownRedis {

    @Test
    public void streamsEx() throws InterruptedException {
        StepVerifier.create(redisReactiveCommands
                .xadd("some-stream", Map.of("first", "1", "second", "2")))
                .expectNextMatches(resp -> resp.endsWith("-0"))
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.xlen("some-stream"))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.xrange("some-stream", Range.create("-", "+")))
                .expectNextMatches(streamMessage ->
                        streamMessage.getBody().get("first").equals("1") &&
                        streamMessage.getBody().get("second").equals("2")
                ).verifyComplete();

        AtomicInteger elementsSeen = new AtomicInteger(0);
        redisClient.connectPubSub().reactive()
                .xread(
                        new XReadArgs().block(2000),
                        XReadArgs.StreamOffset.from("some-stream", "0")
                )
                .subscribe(stringStringStreamMessage -> {
                    elementsSeen.incrementAndGet();
                });

        StepVerifier.create(redisReactiveCommands
                .xadd("some-stream", Map.of("third", "3", "fourth", "4")))
                .expectNextCount(1)
                .verifyComplete();

        Thread.sleep(500);

        assertEquals(2, elementsSeen.get());
    }
}
