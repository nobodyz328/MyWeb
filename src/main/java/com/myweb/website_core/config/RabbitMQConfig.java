package com.myweb.website_core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 队列名称常量
    public static final String POST_CREATED_QUEUE = "post.created.queue";
    public static final String POST_LIKED_QUEUE = "post.liked.queue";
    public static final String EMAIL_NOTIFICATION_QUEUE = "email.notification.queue";
    public static final String AUDIT_LOG_QUEUE = "audit.log.queue";
    
    // 交换机名称常量
    public static final String POST_EXCHANGE = "post.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String AUDIT_EXCHANGE = "audit.exchange";

    // 路由键常量
    public static final String POST_CREATED_ROUTING_KEY = "post.created";
    public static final String POST_LIKED_ROUTING_KEY = "post.liked";
    public static final String EMAIL_NOTIFICATION_ROUTING_KEY = "email.notification";
    public static final String AUDIT_LOG_ROUTING_KEY = "audit.log";

    // 帖子相关队列
    @Bean
    public Queue postCreatedQueue() {
        return new Queue(POST_CREATED_QUEUE, true);
    }

    @Bean
    public Queue postLikedQueue() {
        return new Queue(POST_LIKED_QUEUE, true);
    }

    // 通知相关队列
    @Bean
    public Queue emailNotificationQueue() {
        return new Queue(EMAIL_NOTIFICATION_QUEUE, true);
    }

    // 审计日志队列
    @Bean
    public Queue auditLogQueue() {
        return new Queue(AUDIT_LOG_QUEUE, true);
    }

    // 交换机
    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(POST_EXCHANGE);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE);
    }

    // 绑定关系
    @Bean
    public Binding postCreatedBinding() {
        return BindingBuilder.bind(postCreatedQueue())
                .to(postExchange())
                .with(POST_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding postLikedBinding() {
        return BindingBuilder.bind(postLikedQueue())
                .to(postExchange())
                .with(POST_LIKED_ROUTING_KEY);
    }

    @Bean
    public Binding emailNotificationBinding() {
        return BindingBuilder.bind(emailNotificationQueue())
                .to(notificationExchange())
                .with(EMAIL_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding auditLogBinding() {
        return BindingBuilder.bind(auditLogQueue())
                .to(auditExchange())
                .with(AUDIT_LOG_ROUTING_KEY);
    }

    // 消息转换器
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate配置
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
} 