package com.nickolasfisher.reactiveredis.service;

import com.nickolasfisher.reactiveredis.model.Thing;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Log4j2
public class RedisDataService {

    private final RedisStringReactiveCommands<String, String> redisPrimaryCommands;
    private RedisStringReactiveCommands<String, String> redisReplicaCommands;

    public RedisDataService(
            @Qualifier("redis-primary-commands") RedisStringReactiveCommands<String, String> redisPrimaryCommands,
            @Qualifier("redis-replica-commands") RedisStringReactiveCommands<String, String> redisReplicaCommands
    ) {
        this.redisPrimaryCommands = redisPrimaryCommands;
        this.redisReplicaCommands = redisReplicaCommands;
    }

    public Mono<Void> writeThing(Thing thing) {
        return this.redisPrimaryCommands
                .set(thing.getId().toString(), thing.getValue())
                .then();
    }

    public Mono<Thing> getThing(Integer id) {
        log.info("getting {} from replica", id);
        return this.redisReplicaCommands.get(id.toString())
                .map(response -> Thing.builder().id(id).value(response).build());
    }

    public Mono<Thing> getThingPrimary(Integer id) {
        log.info("getting {} from primary", id);
        return this.redisPrimaryCommands.get(id.toString())
                .map(response -> Thing.builder().id(id).value(response).build());
    }
}
