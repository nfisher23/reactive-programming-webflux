package com.nickolasfisher.clusteredredis;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.resource.ClientResources;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("redis-cluster")
public class LettuceConfig {
    private String host;
    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Bean
    public RedisClusterReactiveCommands<String, String> redisPrimaryReactiveCommands(RedisClusterClient redisClusterClient) {
        return redisClusterClient.connect().reactive();
    }

    @Bean
    public RedisClusterClient redisClient() {
        return RedisClusterClient.create(
                // adjust things like thread pool size with client resources
                ClientResources.builder().build(),
                "redis://" + this.getHost() + ":" + this.getPort()
        );
    }
}
