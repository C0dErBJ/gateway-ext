package com.zjl.spring.gatewayext.filter.factory;

import com.alibaba.fastjson.JSONObject;
import com.zjl.spring.gatewayext.filter.factory.encyption.KeyFetcher;
import com.zjl.spring.gatewayext.support.CryptoHelper;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * @author Zhu jialiang
 */
public class MACGatewayFilterFactory extends AbstractGatewayFilterFactory<MACGatewayFilterFactory.Config> {
    public static final String PREFIX_KEY = "mac";
    private static final String MODE_PARAM = "param";
    private static final String MODE_CONTENT = "content";
    private static final String KEYMODE_NONE = "none";
    private static final String SIGNMODE_REMOVE = "remove";
    private KeyFetcher keyFetcher;


    public MACGatewayFilterFactory(KeyFetcher keyFetcher) {
        super(Config.class);
        this.keyFetcher = keyFetcher;
    }

    @Override
    public GatewayFilter apply(MACGatewayFilterFactory.Config config) {
        String keyValue = this.keyFetcher.fetch(config.keyName);
        String keyContent = this.decryptKey(config, keyValue);
        return (exchange, chain) -> new ModifyRequestBodyGatewayFilterFactory().apply(new ModifyRequestBodyGatewayFilterFactory.Config()
                .setRewriteFunction(String.class, String.class, (exchange1, originalRequestBody) -> Mono.just(checkSignature(config, keyContent, originalRequestBody)))).filter(exchange, chain);
    }

    /**
     * 验签
     *
     * @param config
     * @param keyValue
     * @param requestContent
     * @return
     */
    private String checkSignature(Config config, String keyValue, String requestContent) {
        JSONObject jsonValue = JSONObject.parseObject(requestContent);
        String sign = jsonValue.getString(config.getSignature());
        if (SIGNMODE_REMOVE.equalsIgnoreCase(config.getSignMode())) {
            jsonValue.remove(config.getSignature());
        } else {
            jsonValue.put(config.getSignature(), config.getSignMode());
        }
        if (MODE_PARAM.equalsIgnoreCase(config.getMode())) {
            String localSign = CryptoHelper.getInstance().encrypt(keyValue, jsonValue.getString(config.getParam()), config.getAlgorithm());
            if (!sign.equals(localSign)) {
                jsonValue.put("_CODE", "0000");
                jsonValue.put("_MAC_MESSAGE", "验签失败");
            }
        }
        if (MODE_CONTENT.equalsIgnoreCase(config.getMode())) {
            jsonValue.put(config.getSignature(), "");
            String localSign = CryptoHelper.getInstance().encrypt(keyValue, jsonValue.toString(), config.getAlgorithm());
            if (!sign.equals(localSign)) {
                jsonValue.put("_CODE", "0000");
                jsonValue.put("_MAC_MESSAGE", "验签失败");
            }
        }
        return jsonValue.toString();
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
        return Arrays.asList("mode", "keyMode", "keyName", "algorithm", "param", "signature", "signMode");
    }

    public static class Config extends CryptoGatewayFilterFactory.Config {
        /**
         * 签名
         */
        String signature;
        /**
         * 签名模式
         * 1. replace:用填充字符替换，直接在当前值的位置写上填充字符串
         * 2. remove：直接去掉签名字段
         */
        String signMode;

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getSignMode() {
            return signMode;
        }

        public void setSignMode(String signMode) {
            this.signMode = signMode;
        }
    }
}
