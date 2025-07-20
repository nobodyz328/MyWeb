package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 交互消息服务测试
 */
@ExtendWith(MockitoExtension.class)
class InteractionMessageServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InteractionMessageService interactionMessageService;

    @Test
    void testSendLikeMessage() {
        // 准备测试数据
        Long userId = 1L;
        String username = "testuser";
        Long postId = 100L;
        boolean isLike = true;
        String postTitle = "Test Post";
        Long postAuthorId = 2L;

        // 执行测试
        interactionMessageService.sendLikeMessage(userId, username, postId, isLike, postTitle, postAuthorId);

        // 验证消息发送
        ArgumentCaptor<LikeMessageDto> messageCaptor = ArgumentCaptor.forClass(LikeMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY),
            messageCaptor.capture()
        );

        // 验证消息内容
        LikeMessageDto capturedMessage = messageCaptor.getValue();
        assertEquals(userId, capturedMessage.getUserId());
        assertEquals(username, capturedMessage.getUsername());
        assertEquals(postId, capturedMessage.getPostId());
        assertEquals(isLike, capturedMessage.isLike());
        assertEquals(postTitle, capturedMessage.getPostTitle());
        assertEquals(postAuthorId, capturedMessage.getPostAuthorId());
        assertEquals("LIKE", capturedMessage.getMessageType());
        assertNotNull(capturedMessage.getMessageId());
        assertNotNull(capturedMessage.getTimestamp());
    }

    @Test
    void testSendBookmarkMessage() {
        // 准备测试数据
        Long userId = 1L;
        String username = "testuser";
        Long postId = 100L;
        boolean isBookmark = true;
        String postTitle = "Test Post";
        Long postAuthorId = 2L;
        String postAuthorName = "author";

        // 执行测试
        interactionMessageService.sendBookmarkMessage(userId, username, postId, isBookmark, 
                                                     postTitle, postAuthorId, postAuthorName);

        // 验证消息发送
        ArgumentCaptor<BookmarkMessageDto> messageCaptor = ArgumentCaptor.forClass(BookmarkMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY),
            messageCaptor.capture()
        );

        // 验证消息内容
        BookmarkMessageDto capturedMessage = messageCaptor.getValue();
        assertEquals(userId, capturedMessage.getUserId());
        assertEquals(username, capturedMessage.getUsername());
        assertEquals(postId, capturedMessage.getPostId());
        assertEquals(isBookmark, capturedMessage.isBookmark());
        assertEquals(postTitle, capturedMessage.getPostTitle());
        assertEquals(postAuthorId, capturedMessage.getPostAuthorId());
        assertEquals(postAuthorName, capturedMessage.getPostAuthorName());
        assertEquals("BOOKMARK", capturedMessage.getMessageType());
    }

    @Test
    void testSendCommentMessage() {
        // 准备测试数据
        Long userId = 1L;
        String username = "testuser";
        Long postId = 100L;
        String content = "This is a test comment";
        Long parentCommentId = null;
        String postTitle = "Test Post";
        Long postAuthorId = 2L;
        String postAuthorName = "author";

        // 执行测试
        interactionMessageService.sendCommentMessage(userId, username, postId, content, 
                                                    parentCommentId, postTitle, postAuthorId, postAuthorName);

        // 验证消息发送
        ArgumentCaptor<CommentMessageDto> messageCaptor = ArgumentCaptor.forClass(CommentMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_COMMENT_ROUTING_KEY),
            messageCaptor.capture()
        );

        // 验证消息内容
        CommentMessageDto capturedMessage = messageCaptor.getValue();
        assertEquals(userId, capturedMessage.getUserId());
        assertEquals(username, capturedMessage.getUsername());
        assertEquals(postId, capturedMessage.getPostId());
        assertEquals(content, capturedMessage.getContent());
        assertEquals(parentCommentId, capturedMessage.getParentCommentId());
        assertEquals(postTitle, capturedMessage.getPostTitle());
        assertEquals(postAuthorId, capturedMessage.getPostAuthorId());
        assertEquals(postAuthorName, capturedMessage.getPostAuthorName());
        assertEquals("COMMENT", capturedMessage.getMessageType());
        assertFalse(capturedMessage.isReply());
    }

    @Test
    void testSendCommentReplyMessage() {
        // 准备测试数据（回复消息）
        Long userId = 1L;
        String username = "testuser";
        Long postId = 100L;
        String content = "This is a reply";
        Long parentCommentId = 50L; // 有父评论ID
        String postTitle = "Test Post";
        Long postAuthorId = 2L;
        String postAuthorName = "author";

        // 执行测试
        interactionMessageService.sendCommentMessage(userId, username, postId, content, 
                                                    parentCommentId, postTitle, postAuthorId, postAuthorName);

        // 验证消息发送
        ArgumentCaptor<CommentMessageDto> messageCaptor = ArgumentCaptor.forClass(CommentMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_COMMENT_ROUTING_KEY),
            messageCaptor.capture()
        );

        // 验证消息内容
        CommentMessageDto capturedMessage = messageCaptor.getValue();
        assertEquals(parentCommentId, capturedMessage.getParentCommentId());
        assertTrue(capturedMessage.isReply());
    }

    @Test
    void testSendStatsUpdateMessage() {
        // 准备测试数据
        Long userId = 1L;
        String username = "testuser";
        Long postId = 100L;
        String operationType = "LIKE";
        int countChange = 1;
        String statsType = "LIKE_COUNT";

        // 执行测试
        interactionMessageService.sendStatsUpdateMessage(userId, username, postId, 
                                                        operationType, countChange, statsType);

        // 验证消息发送
        ArgumentCaptor<StatsUpdateMessageDto> messageCaptor = ArgumentCaptor.forClass(StatsUpdateMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_STATS_UPDATE_ROUTING_KEY),
            messageCaptor.capture()
        );

        // 验证消息内容
        StatsUpdateMessageDto capturedMessage = messageCaptor.getValue();
        assertEquals(userId, capturedMessage.getUserId());
        assertEquals(username, capturedMessage.getUsername());
        assertEquals(postId, capturedMessage.getPostId());
        assertEquals(operationType, capturedMessage.getOperationType());
        assertEquals(countChange, capturedMessage.getCountChange());
        assertEquals(statsType, capturedMessage.getStatsType());
        assertEquals("STATS_UPDATE", capturedMessage.getMessageType());
    }

    @Test
    void testSendMessageWithException() {
        // 模拟RabbitTemplate抛出异常
        doThrow(new RuntimeException("Connection failed")).when(rabbitTemplate)
            .convertAndSend(any(String.class), any(String.class), any(Object.class));

        // 执行测试（应该不抛出异常，而是记录日志）
        assertDoesNotThrow(() -> {
            interactionMessageService.sendLikeMessage(1L, "testuser", 100L, true, "Test Post", 2L);
        });

        // 验证仍然尝试发送消息
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void testMessageIdGeneration() {
        // 发送多个消息，验证消息ID的唯一性
        interactionMessageService.sendLikeMessage(1L, "user1", 100L, true, "Post1", 2L);
        interactionMessageService.sendLikeMessage(1L, "user1", 101L, true, "Post2", 2L);

        // 捕获发送的消息
        ArgumentCaptor<LikeMessageDto> messageCaptor = ArgumentCaptor.forClass(LikeMessageDto.class);
        verify(rabbitTemplate, times(2)).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY),
            messageCaptor.capture()
        );

        // 验证消息ID不同
        var messages = messageCaptor.getAllValues();
        assertNotEquals(messages.get(0).getMessageId(), messages.get(1).getMessageId());
        
        // 验证消息ID格式
        assertTrue(messages.get(0).getMessageId().startsWith("MSG-"));
        assertTrue(messages.get(1).getMessageId().startsWith("MSG-"));
        assertEquals(20, messages.get(0).getMessageId().length()); // MSG- + 16位字符
        assertEquals(20, messages.get(1).getMessageId().length());
    }

    @Test
    void testUnlikeMessage() {
        // 测试取消点赞消息
        interactionMessageService.sendLikeMessage(1L, "testuser", 100L, false, "Test Post", 2L);

        ArgumentCaptor<LikeMessageDto> messageCaptor = ArgumentCaptor.forClass(LikeMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY),
            messageCaptor.capture()
        );

        LikeMessageDto capturedMessage = messageCaptor.getValue();
        assertFalse(capturedMessage.isLike());
    }

    @Test
    void testUnbookmarkMessage() {
        // 测试取消书签消息
        interactionMessageService.sendBookmarkMessage(1L, "testuser", 100L, false, 
                                                     "Test Post", 2L, "author");

        ArgumentCaptor<BookmarkMessageDto> messageCaptor = ArgumentCaptor.forClass(BookmarkMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY),
            messageCaptor.capture()
        );

        BookmarkMessageDto capturedMessage = messageCaptor.getValue();
        assertFalse(capturedMessage.isBookmark());
    }

    @Test
    void testNegativeCountChange() {
        // 测试负数计数变化（如取消点赞）
        interactionMessageService.sendStatsUpdateMessage(1L, "testuser", 100L, 
                                                        "UNLIKE", -1, "LIKE_COUNT");

        ArgumentCaptor<StatsUpdateMessageDto> messageCaptor = ArgumentCaptor.forClass(StatsUpdateMessageDto.class);
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_STATS_UPDATE_ROUTING_KEY),
            messageCaptor.capture()
        );

        StatsUpdateMessageDto capturedMessage = messageCaptor.getValue();
        assertEquals(-1, capturedMessage.getCountChange());
        assertEquals("UNLIKE", capturedMessage.getOperationType());
    }
}