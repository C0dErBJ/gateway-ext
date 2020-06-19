package com.zjl.spring.gatewayext.config;


import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.zjl.spring.gatewayext.filter.factory.CryptoGatewayFilterFactory;
import com.zjl.spring.gatewayext.filter.factory.encyption.KeyFetcher;
import com.zjl.spring.gatewayext.filter.factory.encyption.NacosKeyFetcher;
import com.zjl.spring.gatewayext.handler.predicate.BodyRoutePredicateFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

import java.util.Properties;

/**
 * @author Zhu jialiang
 */
@Configuration(
        proxyBeanMethods = false
)
@ConditionalOnProperty(
        name = {"spring.cloud.gateway.enabled"},
        matchIfMissing = true
)
@EnableConfigurationProperties
@AutoConfigureBefore({HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class})
@AutoConfigureAfter({GatewayLoadBalancerClientAutoConfiguration.class, GatewayClassPathWarningAutoConfiguration.class})
@ConditionalOnClass({DispatcherHandler.class})
public class GatewayExtAutoConfiguration {

    @Value("${spring.cloud.nacos.config.server-addr:localhost:8848}")
    private String nacosAddress;

    @Value("${spring.cloud.nacos.config.namespace:your_namespace}")
    private String nacosNamespace;

    @Bean
    public CryptoGatewayFilterFactory CryptoGatewayFilterFactory(KeyFetcher keyFetcher) {
        return new CryptoGatewayFilterFactory(keyFetcher);
    }

    @Bean
    public BodyRoutePredicateFactory bodyRoutePredicateFactory() {
        return new BodyRoutePredicateFactory();
    }

    @Bean
    public KeyFetcher keyFetcher() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosAddress);
        properties.put(PropertyKeyConst.NAMESPACE, nacosNamespace);
        properties.put(PropertyKeyConst.ENABLE_REMOTE_SYNC_CONFIG, "true");
        return new NacosKeyFetcher(properties);
    }


}
