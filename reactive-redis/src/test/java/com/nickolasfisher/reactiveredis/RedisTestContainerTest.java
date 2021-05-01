package com.nickolasfisher.reactiveredis;

import com.nickolasfisher.reactiveredis.model.Thing;
import com.nickolasfisher.reactiveredis.service.RedisDataService;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
public class RedisTestContainerTest {

    @Container
    public static GenericContainer genericContainer = new GenericContainer(
                DockerImageName.parse("redis:5.0.3-alpine")
            ).withExposedPorts(6379);

    private RedisDataService redisDataService;

    @BeforeEach
    public void setupRedisClient() {
        RedisClient redisClient = RedisClient.create("redis://" + genericContainer.getHost() + ":" + genericContainer.getMappedPort(6379));
        redisDataService = new RedisDataService(redisClient.connect().reactive(), redisClient.connect().reactive());
    }

    @Test
    public void canWriteAndReadThing() {
        Mono<Void> writeMono = redisDataService.writeThing(Thing.builder().id(1).value("hello-redis").build());

        StepVerifier.create(writeMono).verifyComplete();

        StepVerifier.create(redisDataService.getThing(1))
                .expectNextMatches(thing ->
                        thing.getId() == 1 &&
                                "hello-redis".equals(thing.getValue())
                )
                .verifyComplete();
    }
}
