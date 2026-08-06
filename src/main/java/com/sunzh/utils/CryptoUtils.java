package com.sunzh.utils;

import java.util.Base64;

/**
 * 加密工具类
 * 使用 Base64 对密码进行编码/解码
 */
public class CryptoUtils {
    private CryptoUtils() {}

    /**
     * 加密（Base64 编码）
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        return Base64.getEncoder().encodeToString(plainText.getBytes());
    }

    /**
     * 解密（Base64 解码）
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        try {
            return new String(Base64.getDecoder().decode(encryptedText));
        } catch (Exception e) {
            // 解密失败（可能是旧数据明文），返回原值
            return encryptedText;
        }
    }
}