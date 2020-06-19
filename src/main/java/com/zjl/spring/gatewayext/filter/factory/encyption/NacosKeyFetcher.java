package com.zjl.spring.gatewayext.filter.factory.encyption;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * @author Zhu jialiang
 */
public class NacosKeyFetcher implements KeyFetcher {
    private ConfigService configService;

    public NacosKeyFetcher(Properties nacosProperties) throws NacosException {
        this.configService = NacosFactory.createConfigService(nacosProperties);
    }

    @Override
    public String fetchAndDecrypt(String keyName) {
        return this.fetch(keyName);
    }

    @Override
    public String fetch(String keyName) {
        String value = null;
        try {
            value = this.configService.getConfig(keyName, "MAC_GROUP", 500);
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return value;
    }


}
