package com.zjl.spring.gatewayext.handler.predicate;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.ReadBodyPredicateFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Zhu jialiang
 */
public class BodyRoutePredicateFactory extends ReadBodyPredicateFactory {

    private final List<HttpMessageReader<?>> messageReaders;

    public BodyRoutePredicateFactory() {
        this.messageReaders = HandlerStrategies.withDefaults().messageReaders();
    }


    @Override
    public AsyncPredicate<ServerWebExchange> applyAsync(ReadBodyPredicateFactory.Config config) {
        return new AsyncPredicate<ServerWebExchange>() {
            @Override
            public Publisher<Boolean> apply(ServerWebExchange exchange) {
                Class inClass = config.getInClass();
                Object cachedBody = exchange.getAttribute("cachedRequestBodyObject");
                if (cachedBody != null) {
                    try {
                        boolean test = config.getPredicate().test(cachedBody);
                        exchange.getAttributes().put("read_body_predicate_test_attribute", test);
                        return Mono.just(test);
                    } catch (ClassCastException var6) {
                        if (ReadBodyPredicateFactory.log.isDebugEnabled()) {
                            ReadBodyPredicateFactory.log.debug("Predicate test failed because class in predicate does not match the cached body object", var6);
                        }

                        return Mono.just(false);
                    }
                } else {
                    return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, (serverHttpRequest) -> {
                        return ServerRequest.create(exchange.mutate().request(serverHttpRequest).build(), BodyRoutePredicateFactory.this.messageReaders).bodyToMono(inClass).doOnNext((objectValue) -> {
                            exchange.getAttributes().put("cachedRequestBodyObject", objectValue);
                        }).map((objectValue) -> config.getPredicate().test(objectValue));
                    });
                }
            }

            @Override
            public String toString() {
                return String.format("ReadBody: %s", config.getInClass());
            }
        };
    }


}
