package com.nickolasfisher.reactiveredis.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Bean("redis-primary-commands")
    public RedisStringReactiveCommands<String, String> redisPrimaryReactiveCommands(RedisClient redisClient) {
        return redisClient.connect().reactive();
    }

    @Bean("redis-primary-client")
    public RedisClient redisClient(RedisPrimaryConfig redisPrimaryConfig) {
        return RedisClient.create(
                // adjust things like thread pool size with client resources
                ClientResources.builder().build(),
                "redis://" + redisPrimaryConfig.getHost() + ":" + redisPrimaryConfig.getPort()
        );
    }

    @Bean("redis-replica-commands")
    public RedisStringReactiveCommands<String, String> redisReplicaReactiveCommands(RedisPrimaryConfig redisPrimaryConfig) {
        RedisURI redisPrimaryURI = RedisURI.builder()
                .withHost(redisPrimaryConfig.getHost())
                .withPort(redisPrimaryConfig.getPort())
                .build();

        RedisClient redisClient = RedisClient.create(
                redisPrimaryURI
        );

        StatefulRedisMasterReplicaConnection<String, String> primaryAndReplicaConnection = MasterReplica.connect(
                redisClient,
                StringCodec.UTF8,
                redisPrimaryURI
        );

        primaryAndReplicaConnection.setReadFrom(ReadFrom.REPLICA);

        return primaryAndReplicaConnection.reactive();
    }
}
