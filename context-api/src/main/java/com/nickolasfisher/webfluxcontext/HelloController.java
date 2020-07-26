package com.nickolasfisher.webfluxcontext;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.Function;

import static com.nickolasfisher.webfluxcontext.WebContextFilter.X_CUSTOM_HEADER;

@RestController
public class HelloController {

    @GetMapping("/hello/{name}")
    public Mono<ResponseEntity<String>> hello(@PathVariable("name") String name) {
        return Mono.subscriberContext()
                .map(context -> {
                    String[] strings = context.get(X_CUSTOM_HEADER);
                    return ResponseEntity.status(200).header(X_CUSTOM_HEADER, strings).build();
                });
    }
}
