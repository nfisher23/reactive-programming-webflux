package com.nickolasfisher.webfluxcontext;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.nickolasfisher.webfluxcontext.WebContextFilter.X_CUSTOM_HEADER;

@RestController
public class HelloController {

    private final WebClient webClient;

    public HelloController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping("/hello")
    public Mono<ResponseEntity<String>> hello() {
        return Mono.subscriberContext()
                .flatMap(context -> webClient.get()
                        .uri("/test")
                        .exchange()
                        .map(clientResponse -> {
                            String[] strings = context.get(X_CUSTOM_HEADER);
                            return ResponseEntity.status(200)
                                    .header(X_CUSTOM_HEADER, strings)
                                    .build();
                        }));
    }
}
