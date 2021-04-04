package com.nickolasfisher.reactiveredis.service;

import com.nickolasfisher.reactiveredis.Thing;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
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
