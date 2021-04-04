package com.nickolasfisher.reactiveredis;

import com.nickolasfisher.reactiveredis.service.RedisDataService;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import redis.embedded.RedisServer;
import java.net.ServerSocket;

public class RedisDataServiceTest {
    private static RedisServer redisServer;

    private static int getOpenPort() {
        try {
            int port = -1;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
            return port;
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static int port = getOpenPort();

    private RedisDataService redisDataService;

    @BeforeAll
    public static void setupRedisServer() throws Exception {
        redisServer = new RedisServer(port);
        redisServer.start();
    }

    @BeforeEach
    public void setupRedisClient() {
        RedisClient redisClient = RedisClient.create("redis://localhost:" + port);
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

    @AfterAll
    public static void teardownRedisServer() {
        redisServer.stop();
    }
}
