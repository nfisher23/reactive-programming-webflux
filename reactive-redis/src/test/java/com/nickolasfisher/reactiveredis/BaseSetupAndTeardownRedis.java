package com.nickolasfisher.reactiveredis;

import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class BaseSetupAndTeardownRedis {

    @Container
    public static GenericContainer genericContainer = new GenericContainer(
            DockerImageName.parse("redis:5.0.3-alpine")
    ).withExposedPorts(6379);

    protected RedisClient redisClient;

    @BeforeEach
    public void setupRedisClient() {
        redisClient = RedisClient.create("redis://" + genericContainer.getHost() + ":" + genericContainer.getMappedPort(6379));
    }

    @AfterEach
    public void removeAllDataFromRedis() {
        redisClient.connect().reactive().flushall().block();
    }

}
