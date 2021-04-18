package com.nickolasfisher.reactiveredis;


import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

@Testcontainers
public class StringTypesTest {

    @Container
    public static GenericContainer genericContainer = new GenericContainer(
            DockerImageName.parse("redis:5.0.3-alpine")
    ).withExposedPorts(6379);

    private RedisClient redisClient;

    @BeforeEach
    public void setupRedisClient() {
        redisClient = RedisClient.create("redis://" + genericContainer.getHost() + ":" + genericContainer.getMappedPort(6379));
    }

    @AfterEach
    public void removeAllDataFromRedis() {
        redisClient.connect().reactive().flushall().block();
    }

    @Test
    public void setAndGet() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        // vanilla get and set
        StepVerifier.create(redisReactiveCommands.set("some-key-1", "some-value-1"))
                .expectNextMatches(response -> "OK".equals(response)).verifyComplete();

        StepVerifier.create(redisReactiveCommands.get("some-key-1"))
                .expectNextMatches(response -> "some-value-1".equals(response))
                .verifyComplete();

        // adding an additional argument like nx will cause it to return nothing if it doesn't get set
        StepVerifier.create(redisReactiveCommands.set("some-key-1", "some-value-2", new SetArgs().nx()))
                .verifyComplete();

        // prove the value is the same
        StepVerifier.create(redisReactiveCommands.get("some-key-1"))
                .expectNextMatches(response -> "some-value-1".equals(response))
                .verifyComplete();
    }

    @Test
    public void setNx() throws Exception {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        StepVerifier.create(redisReactiveCommands.setnx("key-1", "value-1"))
                .expectNextMatches(success -> success)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.setnx("key-1", "value-1"))
                .expectNextMatches(success -> !success)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.setex("key-1", 1, "value-2"))
                .expectNextMatches(response -> "OK".equals(response))
                .verifyComplete();

        // key-1 expires in 1 second
        Thread.sleep(1500);

        StepVerifier.create(redisReactiveCommands.get("key-1"))
                // no value
                .verifyComplete();
    }

    @Test
    public void append() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        StepVerifier.create(redisReactiveCommands.set("key-10", "value-10"))
                .expectNextMatches(response -> "OK".equals(response))
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.append("key-10", "-more-stuff"))
                // length of new value is returned
                .expectNextMatches(response -> 19L == response)
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.get("key-10"))
                .expectNextMatches(response ->
                        "value-10-more-stuff".equals(response))
                .verifyComplete();
    }

    @Test
    public void incrBy() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        StepVerifier.create(redisReactiveCommands.set("key-counter", "7"))
                .expectNextMatches(response -> "OK".equals(response))
                .verifyComplete();

        StepVerifier.create(redisReactiveCommands.incrby("key-counter", 8L))
                .expectNextMatches(val -> 15 == val)
                .verifyComplete();
    }

    @Test
    public void mget() {
        RedisReactiveCommands<String, String> redisReactiveCommands = redisClient.connect().reactive();

        StepVerifier.create(redisReactiveCommands.mset(Map.of(
                "key-1", "val-1",
                "key-2", "val-2",
                "key-3", "val-3"
        )))
                .expectNextMatches(response -> "OK".equals(response))
                .verifyComplete();

        Flux<KeyValue<String, String>> mgetValuesFlux = redisReactiveCommands.mget("key-1", "key-2", "key-3");
        StepVerifier.create(mgetValuesFlux.collectList())
                .expectNextMatches(collectedValues ->
                        collectedValues.size() == 3
                            && collectedValues.stream()
                                .anyMatch(stringStringKeyValue ->
                                        stringStringKeyValue.getKey().equals("key-1")
                                                && stringStringKeyValue.getValue().equals("val-1")
                                )
                )
                .verifyComplete();
    }

}
