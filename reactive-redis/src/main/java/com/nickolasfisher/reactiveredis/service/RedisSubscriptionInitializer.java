package com.nickolasfisher.reactiveredis.service;

import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class RedisSubscriptionInitializer {

    private final Logger LOG = LoggerFactory.getLogger(RedisSubscriptionInitializer.class);

    private final RedisPubSubReactiveCommands<String, String> redisPubSubReactiveCommands;

    public RedisSubscriptionInitializer(RedisPubSubReactiveCommands<String, String> redisPubSubReactiveCommands) {
        this.redisPubSubReactiveCommands = redisPubSubReactiveCommands;
    }

    @PostConstruct
    public void setupSubscriber() {
        redisPubSubReactiveCommands.subscribe("channel-1").subscribe();

        redisPubSubReactiveCommands.observeChannels().doOnNext(stringStringChannelMessage -> {
            if ("channel-1".equals(stringStringChannelMessage.getChannel())) {
                LOG.info("found message in channel 1: {}", stringStringChannelMessage.getMessage());
            }
        }).subscribe();
    }
}
