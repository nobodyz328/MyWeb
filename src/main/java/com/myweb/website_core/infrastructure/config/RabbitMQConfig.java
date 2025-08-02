package com.myweb.website_core.infrastructure.config;

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
    
    // 新增交互相关队列常量
    public static final String INTERACTION_LIKE_QUEUE = "interaction.like.queue";
    public static final String INTERACTION_BOOKMARK_QUEUE = "interaction.bookmark.queue";
    public static final String INTERACTION_COMMENT_QUEUE = "interaction.comment.queue";
    public static final String INTERACTION_STATS_UPDATE_QUEUE = "interaction.stats.update.queue";
    
    // 死信队列常量
    public static final String INTERACTION_DLQ = "interaction.dlq";
    public static final String INTERACTION_DLX = "interaction.dlx";
    
    // 交换机名称常量
    public static final String POST_EXCHANGE = "post.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String AUDIT_EXCHANGE = "audit.exchange";
    public static final String INTERACTION_EXCHANGE = "interaction.exchange";

    // 路由键常量
    public static final String POST_CREATED_ROUTING_KEY = "post.created";
    public static final String POST_LIKED_ROUTING_KEY = "post.liked";
    public static final String EMAIL_NOTIFICATION_ROUTING_KEY = "email.notification";
    public static final String AUDIT_LOG_ROUTING_KEY = "audit.log";
    
    // 新增交互相关路由键常量
    public static final String INTERACTION_LIKE_ROUTING_KEY = "interaction.like";
    public static final String INTERACTION_BOOKMARK_ROUTING_KEY = "interaction.bookmark";
    public static final String INTERACTION_COMMENT_ROUTING_KEY = "interaction.comment";
    public static final String INTERACTION_STATS_UPDATE_ROUTING_KEY = "interaction.stats.update";

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

    // 交互相关队列（带死信队列配置）
    @Bean
    public Queue interactionLikeQueue() {
        return QueueBuilder.durable(INTERACTION_LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", "like.failed")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .build();
    }

    @Bean
    public Queue interactionBookmarkQueue() {
        return QueueBuilder.durable(INTERACTION_BOOKMARK_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", "bookmark.failed")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .build();
    }

    @Bean
    public Queue interactionCommentQueue() {
        return QueueBuilder.durable(INTERACTION_COMMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", "comment.failed")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .build();
    }

    @Bean
    public Queue interactionStatsUpdateQueue() {
        return QueueBuilder.durable(INTERACTION_STATS_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", "stats.failed")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .build();
    }

    // 死信队列
    @Bean
    public Queue interactionDeadLetterQueue() {
        return QueueBuilder.durable(INTERACTION_DLQ).build();
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

    @Bean
    public TopicExchange interactionExchange() {
        return new TopicExchange(INTERACTION_EXCHANGE);
    }

    // 死信交换机
    @Bean
    public DirectExchange interactionDeadLetterExchange() {
        return new DirectExchange(INTERACTION_DLX);
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

    // 交互相关绑定
    @Bean
    public Binding interactionLikeBinding() {
        return BindingBuilder.bind(interactionLikeQueue())
                .to(interactionExchange())
                .with(INTERACTION_LIKE_ROUTING_KEY);
    }

    @Bean
    public Binding interactionBookmarkBinding() {
        return BindingBuilder.bind(interactionBookmarkQueue())
                .to(interactionExchange())
                .with(INTERACTION_BOOKMARK_ROUTING_KEY);
    }

    @Bean
    public Binding interactionCommentBinding() {
        return BindingBuilder.bind(interactionCommentQueue())
                .to(interactionExchange())
                .with(INTERACTION_COMMENT_ROUTING_KEY);
    }

    @Bean
    public Binding interactionStatsUpdateBinding() {
        return BindingBuilder.bind(interactionStatsUpdateQueue())
                .to(interactionExchange())
                .with(INTERACTION_STATS_UPDATE_ROUTING_KEY);
    }

    // 死信队列绑定
    @Bean
    public Binding interactionDeadLetterBinding() {
        return BindingBuilder.bind(interactionDeadLetterQueue())
                .to(interactionDeadLetterExchange())
                .with("*.failed");
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