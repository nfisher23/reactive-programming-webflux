package com.nickolasfisher.reactiveredis;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class PubSubTest extends BaseSetupAndTeardownRedis {

    @Test
    public void publishAndSubscribe() throws Exception {
        StatefulRedisPubSubConnection<String, String> pubSubConnection =
                redisClient.connectPubSub();

        AtomicBoolean messageReceived = new AtomicBoolean(false);
        RedisPubSubReactiveCommands<String, String> reactivePubSubCommands = pubSubConnection.reactive();
        reactivePubSubCommands.subscribe("some-channel").subscribe();

        reactivePubSubCommands.observeChannels()
                .doOnNext(stringStringChannelMessage -> messageReceived.set(true))
                .subscribe();

        Thread.sleep(25);

        redisClient.connectPubSub()
                .reactive()
                .publish("some-channel", "some-message")
                .subscribe();

        Thread.sleep(25);

        Assertions.assertTrue(messageReceived.get());
    }
}
