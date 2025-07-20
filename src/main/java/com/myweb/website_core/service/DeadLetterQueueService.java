package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.dto.InteractionMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 死信队列处理服务
 */
@Service
public class DeadLetterQueueService {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueService.class);
    private static final int MAX_RETRY_COUNT = 3;
    private static final String FAILED_MESSAGE_KEY_PREFIX = "failed:interaction:";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理死信队列中的消息
     */
    @RabbitListener(queues = RabbitMQConfig.INTERACTION_DLQ)
    public void handleDeadLetterMessage(InteractionMessageDto message) {
        try {
            logger.warn("处理死信队列消息: {}", message);

            // 增加重试计数
            message.setRetryCount(message.getRetryCount() + 1);

            if (message.getRetryCount() <= MAX_RETRY_COUNT) {
                // 如果重试次数未超过限制，延迟重新发送到原队列
                retryMessage(message);
            } else {
                // 超过重试次数，记录到Redis并发送告警
                recordFailedMessage(message);
                sendFailureAlert(message);
            }

        } catch (Exception e) {
            logger.error("处理死信队列消息失败: {}", message, e);
            recordFailedMessage(message);
        }
    }

    /**
     * 重试发送消息
     */
    private void retryMessage(InteractionMessageDto message) {
        try {
            String routingKey = getRoutingKeyByMessageType(message.getMessageType());
            
            // 延迟发送（使用延迟队列或定时任务）
            Thread.sleep(5000 * message.getRetryCount()); // 简单的延迟策略
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INTERACTION_EXCHANGE,
                routingKey,
                message
            );

            logger.info("重试发送消息成功: messageId={}, retryCount={}", 
                       message.getMessageId(), message.getRetryCount());

        } catch (Exception e) {
            logger.error("重试发送消息失败: {}", message, e);
            recordFailedMessage(message);
        }
    }

    /**
     * 记录失败的消息到Redis
     */
    private void recordFailedMessage(InteractionMessageDto message) {
        try {
            String key = FAILED_MESSAGE_KEY_PREFIX + message.getMessageId();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // 存储失败消息详情
            redisTemplate.opsForHash().put(key, "message", message);
            redisTemplate.opsForHash().put(key, "failedAt", timestamp);
            redisTemplate.opsForHash().put(key, "retryCount", message.getRetryCount());
            
            // 设置过期时间（保留7天）
            redisTemplate.expire(key, 7, TimeUnit.DAYS);

            // 添加到失败消息列表
            String listKey = "failed:interaction:list:" + LocalDateTime.now().toLocalDate();
            redisTemplate.opsForList().rightPush(listKey, message.getMessageId());
            redisTemplate.expire(listKey, 30, TimeUnit.DAYS);

            logger.info("记录失败消息到Redis: messageId={}", message.getMessageId());

        } catch (Exception e) {
            logger.error("记录失败消息到Redis失败: {}", message, e);
        }
    }

    /**
     * 发送失败告警
     */
    private void sendFailureAlert(InteractionMessageDto message) {
        try {
            // 这里可以发送邮件、短信或其他告警通知
            logger.error("交互消息处理最终失败，需要人工介入: messageId={}, messageType={}, userId={}, postId={}", 
                        message.getMessageId(), message.getMessageType(), message.getUserId(), message.getPostId());

            // 可以发送到告警队列或直接调用告警服务
            // alertService.sendAlert("交互消息处理失败", message.toString());

        } catch (Exception e) {
            logger.error("发送失败告警失败: {}", message, e);
        }
    }

    /**
     * 根据消息类型获取路由键
     */
    private String getRoutingKeyByMessageType(String messageType) {
        switch (messageType) {
            case "LIKE":
                return RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY;
            case "BOOKMARK":
                return RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY;
            case "COMMENT":
                return RabbitMQConfig.INTERACTION_COMMENT_ROUTING_KEY;
            case "STATS_UPDATE":
                return RabbitMQConfig.INTERACTION_STATS_UPDATE_ROUTING_KEY;
            default:
                throw new IllegalArgumentException("未知的消息类型: " + messageType);
        }
    }

    /**
     * 获取失败消息统计
     */
    public long getFailedMessageCount(LocalDateTime date) {
        String listKey = "failed:interaction:list:" + date.toLocalDate();
        Long count = redisTemplate.opsForList().size(listKey);
        return count != null ? count : 0;
    }

    /**
     * 手动重试失败的消息
     */
    public boolean retryFailedMessage(String messageId) {
        try {
            String key = FAILED_MESSAGE_KEY_PREFIX + messageId;
            InteractionMessageDto message = (InteractionMessageDto) redisTemplate.opsForHash().get(key, "message");
            
            if (message != null) {
                message.setRetryCount(0); // 重置重试计数
                retryMessage(message);
                
                // 从失败列表中移除
                redisTemplate.delete(key);
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("手动重试失败消息失败: messageId={}", messageId, e);
            return false;
        }
    }
}