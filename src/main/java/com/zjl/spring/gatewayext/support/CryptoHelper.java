package com.zjl.spring.gatewayext.support;

import java.awt.image.VolatileImage;

/**
 * @author Zhu jialiang
 */
public class CryptoHelper {
    private static volatile CryptoHelper instance;

    private CryptoHelper() {
    }

    public static CryptoHelper getInstance() {
        if (instance == null) {
            synchronized (CryptoHelper.class) {
                if (instance == null) {
                    return new CryptoHelper();
                }
            }
        }
        return instance;
    }

    public String encrypt(String encryptionKey, String content, String algorithm) {
        return "";
    }

    public String encrypt(String content, String algorithm) {
        return this.encrypt(null, content, algorithm);
    }

    public String decrypt(String content, String algorithm) {
        return this.decrypt(null, content, algorithm);
    }

    public String decrypt(String decryptionKey, String content, String algorithm) {
        return "";
    }
}
