package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 交互消息服务，负责发送各种交互相关的消息
 */
@Service
public class InteractionMessageService {

    private static final Logger logger = LoggerFactory.getLogger(InteractionMessageService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送点赞消息
     */
    public void sendLikeMessage(Long userId, String username, Long postId, boolean isLike, 
                               String postTitle, Long postAuthorId) {
        try {
            LikeMessageDto message = new LikeMessageDto(
                generateMessageId(),
                userId,
                username,
                postId,
                isLike,
                postTitle,
                postAuthorId
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INTERACTION_EXCHANGE,
                RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY,
                message
            );

            logger.info("发送点赞消息成功: {}", message);
        } catch (Exception e) {
            logger.error("发送点赞消息失败: userId={}, postId={}, isLike={}", userId, postId, isLike, e);
        }
    }

    /**
     * 发送书签消息
     */
    public void sendBookmarkMessage(Long userId, String username, Long postId, boolean isBookmark,
                                   String postTitle, Long postAuthorId, String postAuthorName) {
        try {
            BookmarkMessageDto message = new BookmarkMessageDto(
                generateMessageId(),
                userId,
                username,
                postId,
                isBookmark,
                postTitle,
                postAuthorId,
                postAuthorName
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INTERACTION_EXCHANGE,
                RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY,
                message
            );

            logger.info("发送书签消息成功: {}", message);
        } catch (Exception e) {
            logger.error("发送书签消息失败: userId={}, postId={}, isBookmark={}", userId, postId, isBookmark, e);
        }
    }

    /**
     * 发送评论消息
     */
    public void sendCommentMessage(Long userId, String username, Long postId, String content,
                                  Long parentCommentId, String postTitle, Long postAuthorId, String postAuthorName) {
        try {
            CommentMessageDto message = new CommentMessageDto(
                generateMessageId(),
                userId,
                username,
                postId,
                content,
                parentCommentId,
                postTitle,
                postAuthorId,
                postAuthorName
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INTERACTION_EXCHANGE,
                RabbitMQConfig.INTERACTION_COMMENT_ROUTING_KEY,
                message
            );

            logger.info("发送评论消息成功: {}", message);
        } catch (Exception e) {
            logger.error("发送评论消息失败: userId={}, postId={}, content={}", userId, postId, content, e);
        }
    }

    /**
     * 发送统计更新消息
     */
    public void sendStatsUpdateMessage(Long userId, String username, Long postId, 
                                      String operationType, int countChange, String statsType) {
        try {
            StatsUpdateMessageDto message = new StatsUpdateMessageDto(
                generateMessageId(),
                userId,
                username,
                postId,
                operationType,
                countChange,
                statsType
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.INTERACTION_EXCHANGE,
                RabbitMQConfig.INTERACTION_STATS_UPDATE_ROUTING_KEY,
                message
            );

            logger.info("发送统计更新消息成功: {}", message);
        } catch (Exception e) {
            logger.error("发送统计更新消息失败: userId={}, postId={}, operationType={}", 
                        userId, postId, operationType, e);
        }
    }

    /**
     * 生成唯一消息ID
     */
    private String generateMessageId() {
        return "MSG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}