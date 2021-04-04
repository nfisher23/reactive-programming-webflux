package com.nickolasfisher.reactiveredis.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.internal.LettuceFactories;
import io.lettuce.core.resource.ClientResources;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "lettuce")
public class RedisConfig {

    private String host;
    private Integer port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Bean
    public RedisStringReactiveCommands<String, String> getRedis(RedisClient redisClient) {
        return redisClient.connect().reactive();
    }

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(
                // adjust things like thread pool size with client resources
                ClientResources.builder().build(),
                "redis://" + this.getHost() + ":" + this.getPort()
        );
    }
}
