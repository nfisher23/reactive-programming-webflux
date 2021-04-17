package com.nickolasfisher.clusteredredis;

import io.lettuce.core.SetArgs;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Service
public class PostConstructExecutor {
    private final RedisClusterReactiveCommands<String, String> redisClusterReactiveCommands;

    public PostConstructExecutor(RedisClusterReactiveCommands<String, String> redisClusterReactiveCommands) {
        this.redisClusterReactiveCommands = redisClusterReactiveCommands;
    }

    @PostConstruct
    public void doStuffOnClusteredRedis() {
        SetArgs setArgs = new SetArgs();
        setArgs.ex(Duration.ofSeconds(10));
        Mono<String> command = Mono.empty();
        for (int i = 0; i < 10; i++) {
            command = command.then(redisClusterReactiveCommands.set("hello-" + i, "no " + i, setArgs));
        }
        command.block();
    }
}
