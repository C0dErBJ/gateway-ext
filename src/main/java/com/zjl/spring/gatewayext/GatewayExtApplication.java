package com.zjl.spring.gatewayext;

import com.alibaba.nacos.api.annotation.NacosProperties;
import com.alibaba.nacos.spring.context.annotation.EnableNacos;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableNacos(
        globalProperties =
        @NacosProperties(serverAddr = "${spring.cloud.nacos.config.server-addr}",
                namespace = "${spring.cloud.nacos.config.namespace}")
)
public class GatewayExtApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayExtApplication.class, args);
    }

}
