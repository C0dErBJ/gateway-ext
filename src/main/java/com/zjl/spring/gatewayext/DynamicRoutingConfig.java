package com.zjl.spring.gatewayext;


import com.alibaba.cloud.nacos.parser.NacosDataPropertiesParser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

@Component
public class DynamicRoutingConfig implements ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(DynamicRoutingConfig.class);

    private static final String DATA_ID = "example.json";
    private static final String Group = "DEFAULT_GROUP";

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    private ApplicationEventPublisher applicationEventPublisher;

    @Bean
    public void refreshRouting() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, "127.0.0.1:8848");
        properties.put(PropertyKeyConst.NAMESPACE, "5420ad8b-6bb6-47c7-bd41-ee3d340f74c6");
        properties.put(PropertyKeyConst.ENABLE_REMOTE_SYNC_CONFIG, "true");
        ConfigService configService = NacosFactory.createConfigService(properties);
        configService.addListener(DATA_ID, Group, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                logger.info(configInfo);
                boolean refreshGatewayRoute = JSONObject.parseObject(configInfo).getBoolean("refreshGatewayRoute");

                if (refreshGatewayRoute) {
                    List<RouteDefinition> list = JSON.parseArray(JSONObject.parseObject(configInfo).getString("routeList")).toJavaList(RouteDefinition.class);

                    for (RouteDefinition route : list) {
                        update(assembleRouteDefinition(route));
                    }
                } else {
                    logger.info("路由未发生变更");
                }


            }
        });
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 路由更新
     *
     * @param routeDefinition
     * @return
     */
    public void update(RouteDefinition routeDefinition) {

        try {
            this.routeDefinitionWriter.delete(Mono.just(routeDefinition.getId()));
            logger.info("路由更新成功");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        try {
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            this.applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this));
            logger.info("路由更新成功");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public RouteDefinition assembleRouteDefinition(RouteDefinition routeEntity) {

        RouteDefinition definition = new RouteDefinition();

        // ID
        definition.setId(routeEntity.getId());

        // Predicates
        List<PredicateDefinition> pdList = new ArrayList<>();
        for (PredicateDefinition predicateEntity : routeEntity.getPredicates()) {
            PredicateDefinition predicateDefinition = new PredicateDefinition();
            predicateDefinition.setArgs(predicateEntity.getArgs());
            predicateDefinition.setName(predicateEntity.getName());
            pdList.add(predicateDefinition);
        }
        definition.setPredicates(pdList);

        // Filters
        List<FilterDefinition> fdList = new ArrayList<>();
        for (FilterDefinition filterEntity : routeEntity.getFilters()) {
            FilterDefinition filterDefinition = new FilterDefinition();
            filterDefinition.setArgs(filterEntity.getArgs());
            filterDefinition.setName(filterEntity.getName());
            fdList.add(filterDefinition);
        }
        definition.setFilters(fdList);

        // URI
        URI uri = UriComponentsBuilder.fromUriString(routeEntity.getUri().getPath()).build().toUri();
        definition.setUri(uri);

        return definition;
    }
}