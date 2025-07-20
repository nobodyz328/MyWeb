package com.myweb.website_core.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 交互消息DTO测试
 */
class InteractionMessageDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // 注册时间模块
    }

    @Test
    void testLikeMessageDtoSerialization() throws Exception {
        // 创建点赞消息DTO
        LikeMessageDto likeMessage = new LikeMessageDto(
            "MSG-123456",
            1L,
            "testuser",
            100L,
            true,
            "Test Post Title",
            2L
        );

        // 序列化
        String json = objectMapper.writeValueAsString(likeMessage);
        assertNotNull(json);
        assertTrue(json.contains("\"messageType\":\"LIKE\""));
        assertTrue(json.contains("\"like\":true"));
        assertTrue(json.contains("\"postTitle\":\"Test Post Title\""));

        // 反序列化
        LikeMessageDto deserializedMessage = objectMapper.readValue(json, LikeMessageDto.class);
        assertEquals(likeMessage.getMessageId(), deserializedMessage.getMessageId());
        assertEquals(likeMessage.getUserId(), deserializedMessage.getUserId());
        assertEquals(likeMessage.getPostId(), deserializedMessage.getPostId());
        assertEquals(likeMessage.isLike(), deserializedMessage.isLike());
        assertEquals(likeMessage.getPostTitle(), deserializedMessage.getPostTitle());
        assertEquals(likeMessage.getPostAuthorId(), deserializedMessage.getPostAuthorId());
    }

    @Test
    void testBookmarkMessageDtoSerialization() throws Exception {
        // 创建书签消息DTO
        BookmarkMessageDto bookmarkMessage = new BookmarkMessageDto(
            "MSG-789012",
            1L,
            "testuser",
            100L,
            true,
            "Test Post Title",
            2L,
            "author"
        );

        // 序列化
        String json = objectMapper.writeValueAsString(bookmarkMessage);
        assertNotNull(json);
        assertTrue(json.contains("\"messageType\":\"BOOKMARK\""));
        assertTrue(json.contains("\"bookmark\":true"));
        assertTrue(json.contains("\"postAuthorName\":\"author\""));

        // 反序列化
        BookmarkMessageDto deserializedMessage = objectMapper.readValue(json, BookmarkMessageDto.class);
        assertEquals(bookmarkMessage.getMessageId(), deserializedMessage.getMessageId());
        assertEquals(bookmarkMessage.isBookmark(), deserializedMessage.isBookmark());
        assertEquals(bookmarkMessage.getPostAuthorName(), deserializedMessage.getPostAuthorName());
    }

    @Test
    void testCommentMessageDtoSerialization() throws Exception {
        // 创建评论消息DTO
        CommentMessageDto commentMessage = new CommentMessageDto(
            "MSG-345678",
            1L,
            "testuser",
            100L,
            "This is a test comment",
            null,
            "Test Post Title",
            2L,
            "author"
        );

        // 序列化
        String json = objectMapper.writeValueAsString(commentMessage);
        assertNotNull(json);
        assertTrue(json.contains("\"messageType\":\"COMMENT\""));
        assertTrue(json.contains("\"content\":\"This is a test comment\""));

        // 反序列化
        CommentMessageDto deserializedMessage = objectMapper.readValue(json, CommentMessageDto.class);
        assertEquals(commentMessage.getContent(), deserializedMessage.getContent());
        assertEquals(commentMessage.getParentCommentId(), deserializedMessage.getParentCommentId());
        assertFalse(deserializedMessage.isReply());
    }

    @Test
    void testCommentReplyMessageDto() throws Exception {
        // 创建回复消息DTO
        CommentMessageDto replyMessage = new CommentMessageDto(
            "MSG-901234",
            1L,
            "testuser",
            100L,
            "This is a reply",
            50L, // 父评论ID
            "Test Post Title",
            2L,
            "author"
        );

        assertTrue(replyMessage.isReply());
        assertEquals(50L, replyMessage.getParentCommentId());

        // 序列化和反序列化测试
        String json = objectMapper.writeValueAsString(replyMessage);
        CommentMessageDto deserializedMessage = objectMapper.readValue(json, CommentMessageDto.class);
        assertTrue(deserializedMessage.isReply());
        assertEquals(50L, deserializedMessage.getParentCommentId());
    }

    @Test
    void testStatsUpdateMessageDtoSerialization() throws Exception {
        // 创建统计更新消息DTO
        StatsUpdateMessageDto statsMessage = new StatsUpdateMessageDto(
            "MSG-567890",
            1L,
            "testuser",
            100L,
            "LIKE",
            1,
            "LIKE_COUNT"
        );

        // 序列化
        String json = objectMapper.writeValueAsString(statsMessage);
        assertNotNull(json);
        assertTrue(json.contains("\"messageType\":\"STATS_UPDATE\""));
        assertTrue(json.contains("\"operationType\":\"LIKE\""));
        assertTrue(json.contains("\"countChange\":1"));
        assertTrue(json.contains("\"statsType\":\"LIKE_COUNT\""));

        // 反序列化
        StatsUpdateMessageDto deserializedMessage = objectMapper.readValue(json, StatsUpdateMessageDto.class);
        assertEquals(statsMessage.getOperationType(), deserializedMessage.getOperationType());
        assertEquals(statsMessage.getCountChange(), deserializedMessage.getCountChange());
        assertEquals(statsMessage.getStatsType(), deserializedMessage.getStatsType());
    }

    @Test
    void testPolymorphicDeserialization() throws Exception {
        // 测试多态反序列化
        LikeMessageDto likeMessage = new LikeMessageDto(
            "MSG-123456",
            1L,
            "testuser",
            100L,
            true,
            "Test Post Title",
            2L
        );

        // 序列化为基类
        String json = objectMapper.writeValueAsString((InteractionMessageDto) likeMessage);
        
        // 反序列化为基类，应该能正确识别具体类型
        InteractionMessageDto deserializedMessage = objectMapper.readValue(json, InteractionMessageDto.class);
        
        assertTrue(deserializedMessage instanceof LikeMessageDto);
        LikeMessageDto deserializedLikeMessage = (LikeMessageDto) deserializedMessage;
        assertEquals(likeMessage.isLike(), deserializedLikeMessage.isLike());
    }

    @Test
    void testMessageDtoDefaultValues() {
        // 测试默认值
        LikeMessageDto likeMessage = new LikeMessageDto();
        assertEquals("LIKE", likeMessage.getMessageType());
        assertEquals(0, likeMessage.getRetryCount());
        assertNotNull(likeMessage.getTimestamp());

        BookmarkMessageDto bookmarkMessage = new BookmarkMessageDto();
        assertEquals("BOOKMARK", bookmarkMessage.getMessageType());

        CommentMessageDto commentMessage = new CommentMessageDto();
        assertEquals("COMMENT", commentMessage.getMessageType());
        assertFalse(commentMessage.isReply());

        StatsUpdateMessageDto statsMessage = new StatsUpdateMessageDto();
        assertEquals("STATS_UPDATE", statsMessage.getMessageType());
    }

    @Test
    void testMessageDtoToString() {
        // 测试toString方法
        LikeMessageDto likeMessage = new LikeMessageDto(
            "MSG-123456",
            1L,
            "testuser",
            100L,
            true,
            "Test Post Title",
            2L
        );

        String toString = likeMessage.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("LikeMessageDto"));
        assertTrue(toString.contains("MSG-123456"));
        assertTrue(toString.contains("testuser"));
        assertTrue(toString.contains("isLike=true"));
    }

    @Test
    void testRetryCountIncrement() {
        // 测试重试计数
        InteractionMessageDto message = new LikeMessageDto();
        assertEquals(0, message.getRetryCount());

        message.setRetryCount(1);
        assertEquals(1, message.getRetryCount());

        message.setRetryCount(message.getRetryCount() + 1);
        assertEquals(2, message.getRetryCount());
    }
}