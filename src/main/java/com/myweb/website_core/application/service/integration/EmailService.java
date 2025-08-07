package com.myweb.website_core.application.service.integration;

import com.myweb.website_core.domain.business.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("注册验证码");
        message.setText("您的验证码是：" + code + "，5分钟内有效。");
        mailSender.send(message);
    }
    
    /**
     * 发送通用邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    @Async
    public void sendEmail(String to, String subject, String content) {
        try {
            log.info("发送邮件: to={}, subject={}", to, subject);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
            
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送欢迎邮件
     * 
     * @param user 注册成功的用户
     */
    @Async
    public void sendWelcomeEmail(User user) {
        try {
            log.info("发送欢迎邮件: userId={}, email={}", user.getId(), user.getEmail());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("欢迎加入MyWeb博客系统！");
            message.setText(buildWelcomeEmailContent(user));
            
            mailSender.send(message);
            log.info("欢迎邮件发送成功: userId={}, email={}", user.getId(), user.getEmail());
            
        } catch (Exception e) {
            log.error("欢迎邮件发送失败: userId={}, email={}, error={}", 
                    user.getId(), user.getEmail(), e.getMessage(), e);
            // 欢迎邮件发送失败不应该影响注册流程，只记录日志
        }
    }
    
    /**
     * 构建欢迎邮件内容
     * 
     * @param user 用户信息
     * @return 邮件内容
     */
    private String buildWelcomeEmailContent(User user) {
        return String.format("""
            亲爱的 %s，
            
            欢迎加入MyWeb博客系统！
            
            您的账户已成功创建，现在可以开始您的博客之旅了：
            
            ✨ 发布您的第一篇博客文章
            ✨ 关注感兴趣的作者
            ✨ 参与评论和讨论
            ✨ 个性化您的个人资料
            
            账户信息：
            - 用户名：%s
            - 邮箱：%s
            - 注册时间：%s
            
            安全提醒：
            - 请妥善保管您的账户密码
            - 如发现异常登录，请及时联系我们
            - 建议定期更新密码以确保账户安全
            
            如果您有任何问题或建议，欢迎随时联系我们的客服团队。
            
            再次感谢您的加入，祝您使用愉快！
            
            MyWeb团队
            %s
            """, 
            user.getUsername(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt().toString(),
            LocalDateTime.now().toString()
        );
    }
} 