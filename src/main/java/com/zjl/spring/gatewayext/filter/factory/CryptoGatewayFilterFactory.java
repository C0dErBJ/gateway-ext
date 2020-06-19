package com.zjl.spring.gatewayext.filter.factory;

import com.alibaba.fastjson.JSONObject;
import com.zjl.spring.gatewayext.filter.factory.encyption.KeyFetcher;
import com.zjl.spring.gatewayext.support.CryptoHelper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * @author Zhu jialiang
 */
public class CryptoGatewayFilterFactory extends AbstractGatewayFilterFactory<CryptoGatewayFilterFactory.Config> {
    public static final String PREFIX_KEY = "crypto";
    private static final String MODE_PARAM = "param";
    private static final String MODE_CONTENT = "content";
    private static final String KEYMODE_NONE = "none";
    private KeyFetcher keyFetcher;



    public CryptoGatewayFilterFactory(KeyFetcher keyFetcher) {
        super(Config.class);
        this.keyFetcher = keyFetcher;
    }

    @Override
    public GatewayFilter apply(CryptoGatewayFilterFactory.Config config) {
        String keyValue = this.keyFetcher.fetch(config.keyName);
        String keyContent = this.decryptKey(config, keyValue);
        return (exchange, chain) -> new ModifyRequestBodyGatewayFilterFactory().apply(new ModifyRequestBodyGatewayFilterFactory.Config()
                .setRewriteFunction(String.class, String.class, (exchange1, originalRequestBody) -> Mono.just(decryptContent(config, keyContent, originalRequestBody)))).filter(exchange, chain);
    }

    /**
     * 解密
     * @param config
     * @param keyValue
     * @param requestContent
     * @return
     */
    private String decryptContent(Config config, String keyValue, String requestContent) {
        if (MODE_CONTENT.equalsIgnoreCase(config.mode)) {
            return CryptoHelper.getInstance().decrypt(requestContent, keyValue, config.algorithm);
        }
        if (MODE_PARAM.equalsIgnoreCase(config.keyMode)) {
            JSONObject jsonValue = JSONObject.parseObject(requestContent);
            String cryptoParam = jsonValue.getString(config.getParam());
            jsonValue.put(config.getParam(), CryptoHelper.getInstance().decrypt(cryptoParam, keyValue, config.algorithm));
            return jsonValue.toString();

        }
        return requestContent;
    }


    /**
     * 对密钥的解密，目前只支持对称算法
     *
     * @param config
     * @param keyValue
     * @return
     */
    private String decryptKey(Config config, String keyValue) {
        if (KEYMODE_NONE.equalsIgnoreCase(config.keyMode)) {
            return keyValue;
        }
        return CryptoHelper.getInstance().decrypt(keyValue, config.keyMode);
    }


    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("mode", "keyMode", "keyName", "algorithm", "param");
    }

    public static class Config {
        /**
         * 报文解密模式
         * content：全文加密。param和signature可以不填
         * param：字段模式。param指定被加密文本字段，signature指定签名字段
         */
        String mode;
        /**
         * 密钥名称加密模式
         * none:不加密，明文
         * 或者直接设置算法名称
         */
        String keyMode;
        String keyName;
        String algorithm;
        String param;


        public String getKeyMode() {
            return keyMode;
        }

        public void setKeyMode(String keyMode) {
            this.keyMode = keyMode;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }

    }
}
