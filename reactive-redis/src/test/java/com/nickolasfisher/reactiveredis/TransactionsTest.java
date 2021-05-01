package com.nickolasfisher.reactiveredis;

import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

public class TransactionsTest extends BaseSetupAndTeardownRedis {

    @Test
    public void transactions() throws InterruptedException {
        RedisReactiveCommands<String, String> firstConnection =
                redisClient.connect().reactive();

        RedisReactiveCommands<String, String> secondConnection =
                redisClient.connect().reactive();

        StepVerifier.create(firstConnection.multi())
                .expectNext("OK")
                .verifyComplete();

        // This will block and never return, because lettuce is has issued it
        // but is waiting for the response from exec
//        StepVerifier.create(firstConnection.set("key-1", "value-1"))
//                .expectNext("OK")
//                .verifyComplete();

        firstConnection.set("key-1", "value-1")
            .subscribe(resp ->
                System.out.println(
                    "response from set within transaction: " + resp
                )
            );

        // no records yet, transaction not committed
        StepVerifier.create(secondConnection.get("key-1"))
                .verifyComplete();

        Thread.sleep(20);
        System.out.println("running exec");
        StepVerifier.create(firstConnection.exec())
                .expectNextMatches(tr -> {
                    System.out.println("exec responded");
                    return tr.size() == 1 && tr.get(0).equals("OK");
                })
                .verifyComplete();

        StepVerifier.create(secondConnection.get("key-1"))
                .expectNext("value-1")
                .verifyComplete();
    }

    @Test
    public void optLocking() {
        RedisReactiveCommands<String, String> firstConnection =
                redisClient.connect().reactive();

        RedisReactiveCommands<String, String> secondConnection =
                redisClient.connect().reactive();

        firstConnection.watch("key-1").subscribe();
        firstConnection.multi().subscribe();
        firstConnection.incr("key-1").subscribe();

        secondConnection.set("key-1", "10").subscribe();

        StepVerifier.create(firstConnection.exec())
                // transaction not committed
                .expectNextMatches(tr -> tr.wasDiscarded())
                .verifyComplete();

        StepVerifier.create(secondConnection.get("key-1"))
                .expectNextMatches(val -> "10".equals(val))
                .verifyComplete();
    }
}
