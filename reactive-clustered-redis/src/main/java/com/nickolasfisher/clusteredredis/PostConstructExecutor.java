package com.nickolasfisher.clusteredredis;

import io.lettuce.core.SetArgs;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class PostConstructExecutor {

    private static final Logger LOG = Loggers.getLogger(PostConstructExecutor.class);

    private final RedisClusterReactiveCommands<String, String> redisClusterReactiveCommands;

    public PostConstructExecutor(RedisClusterReactiveCommands<String, String> redisClusterReactiveCommands) {
        this.redisClusterReactiveCommands = redisClusterReactiveCommands;
    }

    @PostConstruct
    public void doStuffOnClusteredRedis() {
        setHellos();
        showMsetAcrossCluster();
        hashTagging();
    }

    private void setHellos() {
        SetArgs setArgs = new SetArgs();
        setArgs.ex(Duration.ofSeconds(10));
        Mono<String> command = Mono.empty();
        for (int i = 0; i < 10; i++) {
            command = command.then(redisClusterReactiveCommands.set("hello-" + i, "no " + i, setArgs));
        }
        command.block();
    }

    private void showMsetAcrossCluster() {
        LOG.info("starting mset");

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put("key" + i, "value" + i);
        }

        // can follow with MONITOR to see the MSETs for just that node written, under the hood lettuce breaks
        // up the map, gets the hash slot and sends it to that node for you.
        redisClusterReactiveCommands
                .mset(map)
                .block();
        LOG.info("done with mset");
    }

    private void hashTagging() {
       for (int i = 0; i < 10; i++) {
           String candidateKey = "not-hashtag." + i;
           Long keySlotNumber = redisClusterReactiveCommands.clusterKeyslot(candidateKey).block();
           LOG.info("key slot number for {} is {}", candidateKey, keySlotNumber);
           redisClusterReactiveCommands.set(candidateKey, "value").block();
       }

        for (int i = 0; i < 10; i++) {
            String candidateHashTaggedKey = "{some:hashtag}." + i;
            Long keySlotNumber = redisClusterReactiveCommands.clusterKeyslot(candidateHashTaggedKey).block();
            LOG.info("key slot number for {} is {}", candidateHashTaggedKey, keySlotNumber);
            redisClusterReactiveCommands.set(candidateHashTaggedKey, "value").block();
        }
    }
}