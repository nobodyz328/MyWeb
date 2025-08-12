package com.myweb.website_core.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密解密工具类
 * <p>
 * 提供常用的加密解密功能，包括：
 * - AES加密解密
 * - 哈希计算（MD5、SHA-256、SHA-512）
 * - Base64编码解码
 * - 密码哈希和验证
 * - 随机盐生成
 * - HMAC计算
 * <p>
 * 符合需求：10.3 - 提供加密解密功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
public class EncryptionUtils {
    
    // ==================== 常量定义 ====================
    
    /**
     * AES加密算法
     */
    private static final String AES_ALGORITHM = "AES";
    
    /**
     * AES/GCM加密模式
     */
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    
    /**
     * GCM标签长度（位）
     */
    private static final int GCM_TAG_LENGTH = 128;
    
    /**
     * GCM初始化向量长度（字节）
     */
    private static final int GCM_IV_LENGTH = 12;
    
    /**
     * AES密钥长度（位）
     */
    private static final int AES_KEY_LENGTH = 256;
    
    /**
     * 盐长度（字节）
     */
    private static final int SALT_LENGTH = 32;
    
    /**
     * 默认密钥（仅用于演示，生产环境应使用配置文件或密钥管理系统）
     */
    private static final String DEFAULT_SECRET_KEY = "MyWebSecretKey2025!@#$%^&*()_+";
    
    /**
     * 安全随机数生成器
     */
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private EncryptionUtils() {
        // 工具类不允许实例化
    }
    
    // ==================== AES加密解密 ====================
    
    /**
     * AES加密（使用默认密钥）
     * 
     * @param plainText 明文
     * @return Base64编码的密文
     */
    public static String encrypt(String plainText) {
        return encrypt(plainText, DEFAULT_SECRET_KEY);
    }
    
    /**
     * AES加密（指定密钥）
     * 
     * @param plainText 明文
     * @param secretKey 密钥
     * @return Base64编码的密文
     */
    public static String encrypt(String plainText, String secretKey) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            
            // 生成密钥
            SecretKeySpec keySpec = generateKeySpec(secretKey);
            
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // 初始化加密器
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            
            // 执行加密
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // 将IV和加密数据合并
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
            
            // Base64编码
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * AES解密（使用默认密钥）
     * 
     * @param encryptedText Base64编码的密文
     * @return 明文
     */
    public static String decrypt(String encryptedText) {
        return decrypt(encryptedText, DEFAULT_SECRET_KEY);
    }
    
    /**
     * AES解密（指定密钥）
     * 
     * @param encryptedText Base64编码的密文
     * @param secretKey 密钥
     * @return 明文
     */
    public static String decrypt(String encryptedText, String secretKey) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) {
                return encryptedText;
            }
            
            // Base64解码
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
            
            if (encryptedWithIv.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("加密数据格式不正确");
            }
            
            // 提取IV和加密数据
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);
            
            // 生成密钥
            SecretKeySpec keySpec = generateKeySpec(secretKey);
            
            // 初始化解密器
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            
            // 执行解密
            byte[] decryptedData = cipher.doFinal(encryptedData);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成AES密钥规范
     * 
     * @param secretKey 密钥字符串
     * @return SecretKeySpec
     */
    private static SecretKeySpec generateKeySpec(String secretKey) {
        try {
            // 使用SHA-256哈希密钥字符串以确保密钥长度正确
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, AES_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成密钥规范失败", e);
        }
    }
    
    // ==================== 哈希计算 ====================
    
    /**
     * 计算MD5哈希值
     * 
     * @param input 输入字符串
     * @return MD5哈希值（十六进制字符串）
     */
    public static String md5(String input) {
        return hash(input, "MD5");
    }
    
    /**
     * 计算SHA-256哈希值
     * 
     * @param input 输入字符串
     * @return SHA-256哈希值（十六进制字符串）
     */
    public static String sha256(String input) {
        return hash(input, "SHA-256");
    }
    
    /**
     * 计算SHA-512哈希值
     * 
     * @param input 输入字符串
     * @return SHA-512哈希值（十六进制字符串）
     */
    public static String sha512(String input) {
        return hash(input, "SHA-512");
    }
    
    /**
     * 计算哈希值
     * 
     * @param input 输入字符串
     * @param algorithm 哈希算法
     * @return 哈希值（十六进制字符串）
     */
    public static String hash(String input, String algorithm) {
        try {
            if (input == null) {
                return null;
            }
            
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            return bytesToHex(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            log.error("不支持的哈希算法: {}", algorithm, e);
            throw new RuntimeException("哈希计算失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 计算带盐的哈希值
     * 
     * @param input 输入字符串
     * @param salt 盐值
     * @param algorithm 哈希算法
     * @return 哈希值（十六进制字符串）
     */
    public static String hashWithSalt(String input, String salt, String algorithm) {
        if (input == null || salt == null) {
            return null;
        }
        
        return hash(input + salt, algorithm);
    }
    
    // ==================== 密码哈希和验证 ====================
    
    /**
     * 生成密码哈希（带随机盐）
     * 
     * @param password 密码
     * @return 包含盐和哈希的字符串（格式：salt:hash）
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        
        // 生成随机盐
        String salt = generateSalt();
        
        // 计算哈希
        String hash = hashWithSalt(password, salt, "SHA-256");
        
        // 返回盐和哈希的组合
        return salt + ":" + hash;
    }
    
    /**
     * 验证密码
     * 
     * @param password 待验证的密码
     * @param storedHash 存储的哈希值（格式：salt:hash）
     * @return 如果密码正确返回true
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        
        try {
            // 分离盐和哈希
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2) {
                return false;
            }
            
            String salt = parts[0];
            String expectedHash = parts[1];
            
            // 计算输入密码的哈希
            String actualHash = hashWithSalt(password, salt, "SHA-256");
            
            // 使用安全的字符串比较
            return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8)
            );
            
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }
    
    // ==================== Base64编码解码 ====================
    
    /**
     * Base64编码
     * 
     * @param input 输入字符串
     * @return Base64编码字符串
     */
    public static String base64Encode(String input) {
        if (input == null) {
            return null;
        }
        
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Base64解码
     * 
     * @param encodedInput Base64编码字符串
     * @return 解码后的字符串
     */
    public static String base64Decode(String encodedInput) {
        if (encodedInput == null) {
            return null;
        }
        
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedInput);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("Base64解码失败", e);
            throw new RuntimeException("Base64解码失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * URL安全的Base64编码
     * 
     * @param input 输入字符串
     * @return URL安全的Base64编码字符串
     */
    public static String base64UrlEncode(String input) {
        if (input == null) {
            return null;
        }
        
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * URL安全的Base64解码
     * 
     * @param encodedInput URL安全的Base64编码字符串
     * @return 解码后的字符串
     */
    public static String base64UrlDecode(String encodedInput) {
        if (encodedInput == null) {
            return null;
        }
        
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedInput);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("URL安全Base64解码失败", e);
            throw new RuntimeException("URL安全Base64解码失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 随机数和盐生成 ====================
    
    /**
     * 生成随机盐
     * 
     * @return 随机盐（十六进制字符串）
     */
    public static String generateSalt() {
        return generateSalt(SALT_LENGTH);
    }
    
    /**
     * 生成指定长度的随机盐
     * 
     * @param length 盐长度（字节）
     * @return 随机盐（十六进制字符串）
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        secureRandom.nextBytes(salt);
        return bytesToHex(salt);
    }
    
    /**
     * 生成随机字节数组
     * 
     * @param length 字节数组长度
     * @return 随机字节数组
     */
    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
    
    /**
     * 生成AES密钥
     * 
     * @return Base64编码的AES密钥
     */
    public static String generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }
    
    // ==================== HMAC计算 ====================
    
    /**
     * 计算HMAC-SHA256
     * 
     * @param data 数据
     * @param key 密钥
     * @return HMAC值（十六进制字符串）
     */
    public static String hmacSha256(String data, String key) {
        return hmac(data, key, "HmacSHA256");
    }
    
    /**
     * 计算HMAC-SHA512
     * 
     * @param data 数据
     * @param key 密钥
     * @return HMAC值（十六进制字符串）
     */
    public static String hmacSha512(String data, String key) {
        return hmac(data, key, "HmacSHA512");
    }
    
    /**
     * 计算HMAC
     * 
     * @param data 数据
     * @param key 密钥
     * @param algorithm HMAC算法
     * @return HMAC值（十六进制字符串）
     */
    public static String hmac(String data, String key, String algorithm) {
        try {
            if (data == null || key == null) {
                return null;
            }
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
            
        } catch (Exception e) {
            log.error("HMAC计算失败: algorithm={}", algorithm, e);
            throw new RuntimeException("HMAC计算失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 字节数组转十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * 十六进制字符串转字节数组
     * 
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("无效的十六进制字符串");
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }
    
    /**
     * 安全清除字符串（将字符串内容置零）
     * 注意：由于Java字符串的不可变性，此方法仅用于演示，实际应用中应使用char[]
     * 
     * @param sensitiveData 敏感数据
     */
    public static void clearSensitiveData(char[] sensitiveData) {
        if (sensitiveData != null) {
            java.util.Arrays.fill(sensitiveData, '\0');
        }
    }
    
    /**
     * 检查加密算法是否可用
     * 
     * @param algorithm 算法名称
     * @return 如果算法可用返回true
     */
    public static boolean isAlgorithmAvailable(String algorithm) {
        try {
            MessageDigest.getInstance(algorithm);
            return true;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}