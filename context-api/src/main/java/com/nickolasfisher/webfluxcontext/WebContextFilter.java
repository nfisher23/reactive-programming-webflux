package com.nickolasfisher.webfluxcontext;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class WebContextFilter implements WebFilter {

    public static final String X_CUSTOM_HEADER = "X-Custom-Header";

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        List<String> customHeaderValues = serverWebExchange.getRequest().getHeaders().get(X_CUSTOM_HEADER);
        String singleCustomHeader = customHeaderValues != null && customHeaderValues.size() == 1 ? customHeaderValues.get(0) : null;
        serverWebExchange.getResponse();
        return webFilterChain.filter(serverWebExchange).subscriberContext(new Function<Context, Context>() {
            @Override
            public Context apply(Context context) {
                return singleCustomHeader != null ? context.put(X_CUSTOM_HEADER, new String[] {singleCustomHeader}) : context;
            }
        });
    }
}
