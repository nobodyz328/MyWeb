package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.dto.InteractionMessageDto;
import com.myweb.website_core.dto.LikeMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 死信队列服务测试
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @InjectMocks
    private DeadLetterQueueService deadLetterQueueService;

    private LikeMessageDto testMessage;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        testMessage = new LikeMessageDto(
            "MSG-TEST123",
            1L,
            "testuser",
            100L,
            true,
            "Test Post",
            2L
        );
    }

    @Test
    void testHandleDeadLetterMessageWithRetry() throws Exception {
        // 设置重试次数小于最大值
        testMessage.setRetryCount(1);

        // 执行测试
        deadLetterQueueService.handleDeadLetterMessage(testMessage);

        // 验证重试计数增加
        assertEquals(2, testMessage.getRetryCount());

        // 验证消息被重新发送
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY),
            eq(testMessage)
        );

        // 验证没有记录到失败消息
        verify(hashOperations, never()).put(anyString(), eq("message"), any());
    }

    @Test
    void testHandleDeadLetterMessageExceedsMaxRetry() {
        // 设置重试次数超过最大值
        testMessage.setRetryCount(3);

        // 执行测试
        deadLetterQueueService.handleDeadLetterMessage(testMessage);

        // 验证重试计数增加
        assertEquals(4, testMessage.getRetryCount());

        // 验证消息没有被重新发送
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));

        // 验证消息被记录为失败
        String expectedKey = "failed:interaction:MSG-TEST123";
        verify(hashOperations).put(eq(expectedKey), eq("message"), eq(testMessage));
        verify(hashOperations).put(eq(expectedKey), eq("failedAt"), anyString());
        verify(hashOperations).put(eq(expectedKey), eq("retryCount"), eq(4));
        verify(redisTemplate).expire(eq(expectedKey), eq(7L), eq(TimeUnit.DAYS));

        // 验证添加到失败列表
        verify(listOperations).rightPush(anyString(), eq("MSG-TEST123"));
    }

    @Test
    void testHandleDeadLetterMessageWithException() {
        // 模拟重试时抛出异常
        doThrow(new RuntimeException("Retry failed")).when(rabbitTemplate)
            .convertAndSend(anyString(), anyString(), any(Object.class));

        testMessage.setRetryCount(1);

        // 执行测试
        deadLetterQueueService.handleDeadLetterMessage(testMessage);

        // 验证异常被捕获，消息被记录为失败
        String expectedKey = "failed:interaction:MSG-TEST123";
        verify(hashOperations).put(eq(expectedKey), eq("message"), eq(testMessage));
    }

    @Test
    void testGetFailedMessageCount() {
        // 模拟Redis返回值
        when(listOperations.size(anyString())).thenReturn(5L);

        LocalDateTime testDate = LocalDateTime.of(2023, 12, 1, 10, 0);
        long count = deadLetterQueueService.getFailedMessageCount(testDate);

        assertEquals(5L, count);
        verify(listOperations).size("failed:interaction:list:2023-12-01");
    }

    @Test
    void testGetFailedMessageCountWithNullResult() {
        // 模拟Redis返回null
        when(listOperations.size(anyString())).thenReturn(null);

        LocalDateTime testDate = LocalDateTime.of(2023, 12, 1, 10, 0);
        long count = deadLetterQueueService.getFailedMessageCount(testDate);

        assertEquals(0L, count);
    }

    @Test
    void testRetryFailedMessageSuccess() throws Exception {
        // 模拟Redis中存在失败消息
        when(hashOperations.get(anyString(), eq("message"))).thenReturn(testMessage);

        boolean result = deadLetterQueueService.retryFailedMessage("MSG-TEST123");

        assertTrue(result);

        // 验证重试计数被重置
        assertEquals(0, testMessage.getRetryCount());

        // 验证消息被重新发送
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY),
            eq(testMessage)
        );

        // 验证失败记录被删除
        verify(redisTemplate).delete("failed:interaction:MSG-TEST123");
    }

    @Test
    void testRetryFailedMessageNotFound() {
        // 模拟Redis中不存在失败消息
        when(hashOperations.get(anyString(), eq("message"))).thenReturn(null);

        boolean result = deadLetterQueueService.retryFailedMessage("MSG-NOTFOUND");

        assertFalse(result);

        // 验证没有尝试重新发送
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void testRetryFailedMessageWithException() {
        // 模拟Redis操作抛出异常
        when(hashOperations.get(anyString(), eq("message"))).thenThrow(new RuntimeException("Redis error"));

        boolean result = deadLetterQueueService.retryFailedMessage("MSG-ERROR");

        assertFalse(result);
    }

    @Test
    void testGetRoutingKeyByMessageType() {
        // 通过反射测试私有方法的逻辑，这里通过测试重试功能间接验证
        testMessage.setRetryCount(1);
        testMessage.setMessageType("LIKE");

        deadLetterQueueService.handleDeadLetterMessage(testMessage);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.INTERACTION_EXCHANGE),
            eq(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY),
            eq(testMessage)
        );
    }

    @Test
    void testRecordFailedMessageRedisOperations() {
        testMessage.setRetryCount(4);

        deadLetterQueueService.handleDeadLetterMessage(testMessage);

        // 验证Redis操作（预期值改为4+1=5）
        String expectedKey = "failed:interaction:MSG-TEST123";
        verify(hashOperations).put(eq(expectedKey), eq("message"), eq(testMessage));
        verify(hashOperations).put(eq(expectedKey), eq("failedAt"), anyString());
        verify(hashOperations).put(eq(expectedKey), eq("retryCount"), eq(5)); // 改为5
        verify(redisTemplate).expire(eq(expectedKey), eq(7L), eq(TimeUnit.DAYS));

        // 验证添加到失败列表
        ArgumentCaptor<String> listKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(listKeyCaptor.capture(), eq("MSG-TEST123"));
        
        String listKey = listKeyCaptor.getValue();
        assertTrue(listKey.startsWith("failed:interaction:list:"));
        verify(redisTemplate).expire(eq(listKey), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    void testRecordFailedMessageWithRedisException() {
        // 模拟Redis操作抛出异常
        doThrow(new RuntimeException("Redis error")).when(hashOperations)
            .put(anyString(), anyString(), any());

        testMessage.setRetryCount(4);

        // 执行测试，应该不抛出异常
        assertDoesNotThrow(() -> {
            deadLetterQueueService.handleDeadLetterMessage(testMessage);
        });
    }

    @Test
    void testRetryDelayCalculation() throws Exception {
        // 测试重试延迟计算
        testMessage.setRetryCount(2);

        long startTime = System.currentTimeMillis();
        deadLetterQueueService.handleDeadLetterMessage(testMessage);
        long endTime = System.currentTimeMillis();

        // 验证延迟时间（应该大于等于预期的延迟时间）
        // 注意：这个测试可能不稳定，因为依赖于实际的Thread.sleep
        long expectedDelay = 5000 * 3; // 5秒 * (retryCount + 1)
        long actualDelay = endTime - startTime;
        
        // 允许一定的误差范围
        assertTrue(actualDelay >= expectedDelay - 1000, 
                  "实际延迟时间应该接近预期延迟时间");
    }
}