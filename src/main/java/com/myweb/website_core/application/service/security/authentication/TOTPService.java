package com.myweb.website_core.application.service.security.authentication;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.myweb.website_core.common.constant.SecurityConstants;
import com.myweb.website_core.common.security.exception.TOTPValidationException;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * TOTP动态口令服务
 * 
 * 提供TOTP（Time-based One-Time Password）相关功能，包括：
 * - TOTP密钥生成
 * - TOTP验证码验证
 * - 二维码生成
 * - 时间窗口容错机制
 * 
 * 符合GB/T 22239-2019 7.1.4.1 身份鉴别要求
 * 支持Google Authenticator兼容的TOTP算法
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class TOTPService {
    
    private final SecretGenerator secretGenerator;
    private final CodeGenerator codeGenerator;
    private final CodeVerifier codeVerifier;
    private final QrGenerator qrGenerator;
    private final TimeProvider timeProvider;
    
    @Value("${app.name:MyWeb}")
    private String applicationName;
    
    @Value("${app.issuer:MyWeb}")
    private String issuer;
    
    /**
     * 构造函数，初始化TOTP相关组件
     */
    public TOTPService() {
        this.secretGenerator = new DefaultSecretGenerator(SecurityConstants.TOTP_SECRET_LENGTH);
        this.codeGenerator = new DefaultCodeGenerator();
        this.timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        this.qrGenerator = new ZxingPngQrGenerator();
        
        log.info("TOTP服务初始化完成，时间窗口: {}秒，容错窗口: {}", 
                SecurityConstants.TOTP_TIME_WINDOW_SECONDS, 
                SecurityConstants.TOTP_TOLERANCE_WINDOWS);
    }
    
    /**
     * 生成TOTP密钥
     * 生成一个新的Base32编码的密钥，用于TOTP算法
     * 
     * @return Base32编码的TOTP密钥
     */
    public String generateSecret() {
        try {
            String secret = secretGenerator.generate();
            log.debug("生成新的TOTP密钥，长度: {}", secret.length());
            return secret;
        } catch (Exception e) {
            log.error("生成TOTP密钥失败", e);
            throw new RuntimeException("生成TOTP密钥失败", e);
        }
    }
    
    /**
     * 验证TOTP代码
     * 验证用户提供的6位数字代码是否正确
     * 支持时间窗口容错机制，允许前后30秒的时间偏差
     * 
     * @param secret TOTP密钥
     * @param code 用户提供的6位数字代码
     * @return 验证是否成功
     * @throws TOTPValidationException 如果验证失败
     */
    public boolean validateTOTP(String secret, String code) {
        return validateTOTP(secret, code, null);
    }
    
    /**
     * 验证TOTP代码（带用户名，用于日志记录）
     * 验证用户提供的6位数字代码是否正确
     * 支持时间窗口容错机制，允许前后30秒的时间偏差
     * 
     * @param secret TOTP密钥
     * @param code 用户提供的6位数字代码
     * @param username 用户名（用于日志记录）
     * @return 验证是否成功
     * @throws TOTPValidationException 如果验证失败
     */
    public boolean validateTOTP(String secret, String code, String username) {
        String logPrefix = username != null ? "用户 " + username : "TOTP";
        
        try {
            // 验证输入参数
            if (secret == null || secret.trim().isEmpty()) {
                log.warn("{} TOTP验证失败: 密钥为空", logPrefix);
                throw TOTPValidationException.invalidCode(username);
            }
            
            if (code == null || code.trim().isEmpty()) {
                log.warn("{} TOTP验证失败: 验证码为空", logPrefix);
                throw TOTPValidationException.invalidCode(username);
            }
            
            // 验证代码格式（6位数字）
            if (!code.matches("^\\d{6}$")) {
                log.warn("{} TOTP验证失败: 验证码格式不正确 - {}", logPrefix, code);
                throw TOTPValidationException.invalidCode(username);
            }
            
            // 使用容错窗口验证代码
            boolean isValid = codeVerifier.isValidCode(secret, code);
            
            if (isValid) {
                log.debug("{} TOTP验证成功", logPrefix);
                return true;
            } else {
                log.warn("{} TOTP验证失败: 代码不匹配 - {}", logPrefix, code);
                throw TOTPValidationException.codeNotMatch(username, code);
            }
            
        } catch (TOTPValidationException e) {
            // 重新抛出TOTP验证异常
            throw e;
        } catch (Exception e) {
            log.error("{} TOTP验证过程中发生异常", logPrefix, e);
            throw new TOTPValidationException("TOTP验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成当前时间的TOTP代码
     * 主要用于测试和调试目的
     * 
     * @param secret TOTP密钥
     * @return 当前时间的6位TOTP代码
     */
    public String generateCurrentCode(String secret) {
        try {
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalArgumentException("TOTP密钥不能为空");
            }
            
            long currentTime = timeProvider.getTime();
            String code = codeGenerator.generate(secret, currentTime);
            
            log.debug("生成当前TOTP代码成功，时间戳: {}", currentTime);
            return code;
            
        } catch (Exception e) {
            log.error("生成TOTP代码失败", e);
            throw new RuntimeException("生成TOTP代码失败", e);
        }
    }
    
    /**
     * 生成TOTP二维码
     * 生成Google Authenticator兼容的二维码图片数据
     * 
     * @param username 用户名
     * @param secret TOTP密钥
     * @return 二维码图片的字节数组
     * @throws RuntimeException 如果生成失败
     */
    public byte[] generateQRCode(String username, String secret) {
        try {
            // 验证输入参数
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("用户名不能为空");
            }
            
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalArgumentException("TOTP密钥不能为空");
            }
            
            // 创建QR码数据
            QrData qrData = new QrData.Builder()
                    .label(username)
                    .secret(secret)
                    .issuer(issuer)
                    .digits(6)  // 6位数字代码
                    .period(SecurityConstants.TOTP_TIME_WINDOW_SECONDS)  // 30秒时间窗口
                    .build();
            
            // 生成二维码
            byte[] qrCodeImage = qrGenerator.generate(qrData);
            
            log.debug("为用户 {} 生成TOTP二维码成功，图片大小: {} bytes", username, qrCodeImage.length);
            return qrCodeImage;
            
        } catch (QrGenerationException e) {
            log.error("生成TOTP二维码失败: username={}", username, e);
            throw new RuntimeException("生成TOTP二维码失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("生成TOTP二维码过程中发生异常: username={}", username, e);
            throw new RuntimeException("生成TOTP二维码失败", e);
        }
    }
    
    /**
     * 生成TOTP二维码（自定义尺寸）
     * 使用ZXing库生成指定尺寸的二维码
     * 
     * @param username 用户名
     * @param secret TOTP密钥
     * @param width 二维码宽度
     * @param height 二维码高度
     * @return 二维码图片的字节数组
     * @throws RuntimeException 如果生成失败
     */
    public byte[] generateQRCode(String username, String secret, int width, int height) {
        try {
            // 验证输入参数
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("用户名不能为空");
            }
            
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalArgumentException("TOTP密钥不能为空");
            }
            
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("二维码尺寸必须大于0");
            }
            
            // 构建TOTP URI
            String totpUri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=%d",
                issuer, username, secret, issuer, SecurityConstants.TOTP_TIME_WINDOW_SECONDS
            );
            
            // 使用ZXing生成二维码
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(totpUri, BarcodeFormat.QR_CODE, width, height);
            
            // 转换为PNG图片字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrCodeImage = outputStream.toByteArray();
            
            log.debug("为用户 {} 生成自定义尺寸TOTP二维码成功，尺寸: {}x{}, 图片大小: {} bytes", 
                    username, width, height, qrCodeImage.length);
            
            return qrCodeImage;
            
        } catch (WriterException | IOException e) {
            log.error("生成自定义尺寸TOTP二维码失败: username={}, size={}x{}", username, width, height, e);
            throw new RuntimeException("生成TOTP二维码失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("生成自定义尺寸TOTP二维码过程中发生异常: username={}", username, e);
            throw new RuntimeException("生成TOTP二维码失败", e);
        }
    }
    
    /**
     * 获取TOTP URI
     * 生成Google Authenticator兼容的TOTP URI字符串
     * 
     * @param username 用户名
     * @param secret TOTP密钥
     * @return TOTP URI字符串
     */
    public String getTOTPUri(String username, String secret) {
        try {
            // 验证输入参数
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("用户名不能为空");
            }
            
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalArgumentException("TOTP密钥不能为空");
            }
            
            // 构建TOTP URI
            String totpUri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=%d",
                issuer, username, secret, issuer, SecurityConstants.TOTP_TIME_WINDOW_SECONDS
            );
            
            log.debug("为用户 {} 生成TOTP URI成功", username);
            return totpUri;
            
        } catch (Exception e) {
            log.error("生成TOTP URI失败: username={}", username, e);
            throw new RuntimeException("生成TOTP URI失败", e);
        }
    }
    
    /**
     * 验证TOTP密钥格式
     * 检查密钥是否为有效的Base32格式
     * 
     * @param secret TOTP密钥
     * @return 密钥是否有效
     */
    public boolean isValidSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return false;
        }
        
        // 检查Base32格式（只包含A-Z和2-7）
        if (!secret.matches("^[A-Z2-7]+$")) {
            return false;
        }
        
        try {
            // 尝试使用密钥生成代码来验证格式
            codeGenerator.generate(secret, timeProvider.getTime());
            return true;
        } catch (Exception e) {
            log.debug("TOTP密钥格式验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前时间窗口
     * 返回当前时间对应的TOTP时间窗口
     * 
     * @return 当前时间窗口（30秒为一个窗口）
     */
    public long getCurrentTimeWindow() {
        return timeProvider.getTime() / SecurityConstants.TOTP_TIME_WINDOW_SECONDS;
    }
    
    /**
     * 获取指定时间的时间窗口
     * 
     * @param timestamp 时间戳（秒）
     * @return 时间窗口
     */
    public long getTimeWindow(long timestamp) {
        return timestamp / SecurityConstants.TOTP_TIME_WINDOW_SECONDS;
    }
    
    /**
     * 检查代码是否在指定时间窗口内有效
     * 用于更精确的时间窗口控制
     * 
     * @param secret TOTP密钥
     * @param code 验证码
     * @param timeWindow 目标时间窗口
     * @return 是否有效
     */
    public boolean isValidCodeForTimeWindow(String secret, String code, long timeWindow) {
        try {
            if (secret == null || code == null) {
                return false;
            }
            
            // 使用与验证器相同的时间计算方式
            long timestamp = timeWindow * SecurityConstants.TOTP_TIME_WINDOW_SECONDS;
            String expectedCode = codeGenerator.generate(secret, timestamp);
            
            return expectedCode.equals(code);
            
        } catch (Exception e) {
            log.debug("时间窗口代码验证失败", e);
            return false;
        }
    }
    
    /**
     * 获取剩余时间
     * 返回当前时间窗口剩余的秒数
     * 
     * @return 剩余秒数
     */
    public int getRemainingTimeInWindow() {
        long currentTime = timeProvider.getTime();
        long windowStart = (currentTime / SecurityConstants.TOTP_TIME_WINDOW_SECONDS) * SecurityConstants.TOTP_TIME_WINDOW_SECONDS;
        long windowEnd = windowStart + SecurityConstants.TOTP_TIME_WINDOW_SECONDS;
        
        return (int) (windowEnd - currentTime);
    }
    
    /**
     * 获取服务配置信息
     * 返回TOTP服务的配置信息，用于调试和监控
     * 
     * @return 配置信息字符串
     */
    public String getServiceInfo() {
        return String.format(
            "TOTP服务配置 - 应用名称: %s, 发行者: %s, 时间窗口: %d秒, 容错窗口: %d, 密钥长度: %d",
            applicationName, issuer, 
            SecurityConstants.TOTP_TIME_WINDOW_SECONDS,
            SecurityConstants.TOTP_TOLERANCE_WINDOWS,
            SecurityConstants.TOTP_SECRET_LENGTH
        );
    }
}