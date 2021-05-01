package com.nickolasfisher.reactiveredis.controller;

import com.nickolasfisher.reactiveredis.model.Thing;
import com.nickolasfisher.reactiveredis.service.RedisDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SampleController {
    private final RedisDataService redisDataService;

    public SampleController(RedisDataService redisDataService) {
        this.redisDataService = redisDataService;
    }

    @GetMapping("/redis/{key}")
    public Mono<ResponseEntity<Thing>> getRedisValue(@PathVariable("key") Integer key) {
        return redisDataService.getThing(key)
                .flatMap(thing -> Mono.just(ResponseEntity.ok(thing)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/primary-redis/{key}")
    public Mono<ResponseEntity<Thing>> getPrimaryRedisValue(@PathVariable("key") Integer key) {
        return redisDataService.getThingPrimary(key)
                .flatMap(thing -> Mono.just(ResponseEntity.ok(thing)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
