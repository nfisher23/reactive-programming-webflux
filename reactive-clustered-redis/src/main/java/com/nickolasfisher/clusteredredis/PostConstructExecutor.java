package com.nickolasfisher.clusteredredis;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostConstructExecutor {

    private static final Logger LOG = Loggers.getLogger(PostConstructExecutor.class);

    private final RedisClusterReactiveCommands<String, String> redisClusterReactiveCommands;
    private final RedisClusterPubSubReactiveCommands<String, String> redisClusterPubSubReactiveCommands;

    public PostConstructExecutor(@Qualifier("redis-cluster-commands") RedisClusterReactiveCommands<String, String> redisClusterReactiveCommands,
                                 @Qualifier("redis-cluster-pub-sub") RedisClusterPubSubReactiveCommands<String, String> redisClusterPubSubReactiveCommands) {
        this.redisClusterReactiveCommands = redisClusterReactiveCommands;
        this.redisClusterPubSubReactiveCommands = redisClusterPubSubReactiveCommands;
    }

    @PostConstruct
    public void doStuffOnClusteredRedis() {
        subscribeToChannel();
        scriptLoad();
        setHellos();
        showMsetAcrossCluster();
        hashTagging();
        msetNxDifferentHashSlots();
        msetNxSameHashSlots();
    }

    private void subscribeToChannel() {
        List<String> channels = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            channels.add("channel-" + i);
        }
        redisClusterPubSubReactiveCommands.subscribe(channels.toArray(new String[0]))
                .subscribe();

        redisClusterPubSubReactiveCommands.observeChannels().doOnNext(channelAndMessage -> {
            LOG.info("channel {}, message {}", channelAndMessage.getChannel(), channelAndMessage.getMessage());
        }).subscribe();
    }

    private void scriptLoad() {
        LOG.info("starting script load");
        String hashOfScript = redisClusterReactiveCommands.scriptLoad("return redis.call('set',KEYS[1],ARGV[1],'ex',ARGV[2])")
                .block();

        redisClusterReactiveCommands.evalsha(hashOfScript, ScriptOutputType.BOOLEAN, new String[]{"foo1"}, "bar1", "10").blockLast();

        redisClusterReactiveCommands.evalsha(hashOfScript, ScriptOutputType.BOOLEAN, new String[] {"foo2"}, "bar2", "10").blockLast();
        redisClusterReactiveCommands.evalsha(hashOfScript, ScriptOutputType.BOOLEAN, new String[] {"foo4"}, "bar4", "10").blockLast();
    }

    private void msetNxDifferentHashSlots() {
        Mono<Boolean> successMono = redisClusterReactiveCommands.msetnx(
            Map.of(
                "key-1", "value-1",
                "key-2", "value-2",
                "key-3", "value-3",
                "key-4", "value-4"
            )
        );

        Boolean wasSuccessful = successMono.block();

        LOG.info("msetnx success response: {}", wasSuccessful);
    }

    private void msetNxSameHashSlots() {
        Mono<Boolean> successMono = redisClusterReactiveCommands.msetnx(
                Map.of(
                        "{same-hash-slot}.key-1", "value-1",
                        "{same-hash-slot}.key-2", "value-2",
                        "{same-hash-slot}.key-3", "value-3"
                )
        );

        Boolean wasSuccessful = successMono.block();

        LOG.info("msetnx success response: {}", wasSuccessful);
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