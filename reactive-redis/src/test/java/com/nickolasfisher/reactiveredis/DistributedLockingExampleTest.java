package com.nickolasfisher.reactiveredis;

import io.lettuce.core.SetArgs;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributedLockingExampleTest extends BaseSetupAndTeardownRedis {

    @Test
    public void distributedLocking() {
        AtomicInteger numTimesCalled = new AtomicInteger(0);
        Mono<Void> justLogItMono = Mono.defer(() -> {
            System.out.println("not doing anything, just logging");
            numTimesCalled.incrementAndGet();
            return Mono.empty();
        });

        StepVerifier.create(simpleDoIfLockAcquired("lock-123", justLogItMono))
                .verifyComplete();

        StepVerifier.create(simpleDoIfLockAcquired("lock-123", justLogItMono))
                .verifyComplete();

        StepVerifier.create(simpleDoIfLockAcquired("lock-123", justLogItMono))
                .verifyComplete();

        assertEquals(1, numTimesCalled.get());
    }

    public Mono<Void> simpleDoIfLockAcquired(String lockKey, Mono<Void> thingToDo) {
        return redisReactiveCommands.setnx(lockKey, "ACQUIRED")
                .flatMap(acquired -> {
                    if (acquired) {
                        System.out.println("lock acquired, returning mono");
                        return thingToDo;
                    } else {
                        System.out.println("lock not acquired, doing nothing");
                        return Mono.empty();
                    }
                });
    }

    @Test
    public void distributedLockingAndErrorHandling() {
        AtomicInteger numTimesCalled = new AtomicInteger(0);
        Mono<Void> errorMono = Mono.defer(() -> {
            System.out.println("returning an error");
            numTimesCalled.incrementAndGet();
            return Mono.error(new RuntimeException("ahhhh"));
        });

        Mono<Void> successMono = Mono.defer(() -> {
            System.out.println("returning success");
            numTimesCalled.incrementAndGet();
            return Mono.empty();
        });

        StepVerifier.create(doIfLockAcquiredAndHandleErrors("lock-123", errorMono))
                .verifyError();

        StepVerifier.create(doIfLockAcquiredAndHandleErrors("lock-123", errorMono))
                .verifyError();

        StepVerifier.create(doIfLockAcquiredAndHandleErrors("lock-123", errorMono))
                .verifyError();

        StepVerifier.create(doIfLockAcquiredAndHandleErrors("lock-123", successMono))
                .verifyComplete();

        // errors should cause the lock to be released
        assertEquals(4, numTimesCalled.get());

        // we should have finally succeeded, which means the lock is marked as processed
        StepVerifier.create(redisReactiveCommands.get("lock-123"))
                .expectNext("PROCESSED")
                .verifyComplete();
    }

    public Mono<Void> doIfLockAcquiredAndHandleErrors(String lockKey, Mono<Void> thingToDo) {
        SetArgs setArgs = new SetArgs().nx().ex(20);
        return redisReactiveCommands
                .set(lockKey, "PROCESSING", setArgs)
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("lock not acquired, doing nothing");
                    return Mono.empty();
                }))
                .flatMap(acquired -> {
                    if (acquired.equals("OK")) {
                        System.out.println("lock acquired, returning mono");
                        return thingToDo
                                .onErrorResume(throwable ->
                                        redisReactiveCommands
                                                .del(lockKey)
                                                .then(Mono.error(throwable))
                                )
                                .then(redisReactiveCommands.set(lockKey, "PROCESSED", new SetArgs().ex(200)).then());
                    }
                    return Mono.error(new RuntimeException("whoops!"));
                });
    }
}

