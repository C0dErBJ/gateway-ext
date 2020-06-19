package com.zjl.spring.gatewayext.filter.factory.encyption;

/**
 * @author Zhu jialiang
 */
public interface KeyFetcher {
    /**
     * @return 返回密钥
     */
     String fetch(String keyName);

    /**
     * 有些密钥可能还会加密存放，所以获取的时候需要解密后使用
     * @return 返回解密后的密钥
     */
     String fetchAndDecrypt(String keyName);
}
