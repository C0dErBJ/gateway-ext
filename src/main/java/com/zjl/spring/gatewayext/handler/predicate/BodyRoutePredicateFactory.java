package com.zjl.spring.gatewayext.handler.predicate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
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
 * 根据body中的json字段转发请求
 * 参考配置：
 * spring.cloud.gateway.routes[0].predicates[0]=Body=jsonParam,jsonValue
 * 其中jsonParam是对应的请求体中的json字段,jsonValue就是要对比的值
 */
public class BodyRoutePredicateFactory extends AbstractRoutePredicateFactory<BodyRoutePredicateFactory.Config> {
    protected static final Log log = LogFactory.getLog(BodyRoutePredicateFactory.class);
    private final List<HttpMessageReader<?>> messageReaders;
    private static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";

    public BodyRoutePredicateFactory() {
        super(BodyRoutePredicateFactory.Config.class);
        this.messageReaders = HandlerStrategies.withDefaults().messageReaders();
    }

    public BodyRoutePredicateFactory(List<HttpMessageReader<?>> messageReaders) {
        super(BodyRoutePredicateFactory.Config.class);
        this.messageReaders = messageReaders;
    }


    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        throw new UnsupportedOperationException("BodyRoutePredicateFactory is only async.");
    }

    @Override
    public AsyncPredicate<ServerWebExchange> applyAsync(BodyRoutePredicateFactory.Config config) {
        return exchange -> {
            String cachedBody = exchange.getAttribute(CACHE_REQUEST_BODY_OBJECT_KEY);
            if (cachedBody != null) {
                return Mono.just(doCompare(cachedBody, config.getParam(), config.getValue()));
            } else {
                return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange,
                        (serverHttpRequest) ->
                                ServerRequest.create(exchange.mutate()
                                                .request(serverHttpRequest).build(),
                                        BodyRoutePredicateFactory.this.messageReaders).bodyToMono(String.class)
                                        .doOnNext((objectValue) -> {
                                            exchange.getAttributes().put(CACHE_REQUEST_BODY_OBJECT_KEY, objectValue);
                                        }).map((objectValue) -> doCompare(objectValue, config.getParam(), config.getValue())));
            }
        };
    }

    public boolean doCompare(String requestValue, String param, String comparedValue) {
        JSONObject jo = JSON.parseObject(requestValue);
        String value = jo.getString(param);
        if (StringUtils.isEmpty(comparedValue)) {
            BodyRoutePredicateFactory.log.info("匹配字段未配置");
            return false;
        }
        if (StringUtils.isEmpty(value)) {
            BodyRoutePredicateFactory.log.info("未在字段【" + param + "】中找到任何值");
            return false;
        }
        return comparedValue.equals(value);
    }

    public static class Config {
        public String param;
        public String value;

        public Config() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }
    }

}
