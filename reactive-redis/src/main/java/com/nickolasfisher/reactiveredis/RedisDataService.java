package com.nickolasfisher.reactiveredis;

import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import reactor.core.publisher.Mono;

public class RedisDataService {

    private final RedisStringReactiveCommands<String, String> redisStringReactiveCommands;

    public RedisDataService(RedisStringReactiveCommands<String, String> redisStringReactiveCommands) {
        this.redisStringReactiveCommands = redisStringReactiveCommands;
    }

    public Mono<Void> writeThing(Thing thing) {
        return this.redisStringReactiveCommands
                .set(thing.getId().toString(), thing.getValue())
                .then();
    }

    public Mono<Thing> getThing(Integer id) {
        return this.redisStringReactiveCommands.get(id.toString())
                .map(response -> Thing.builder().id(id).value(response).build());
    }
}
