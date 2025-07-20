package com.myweb.website_core.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RabbitMQ配置测试
 */
class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    void testInteractionQueuesCreation() {
        // 测试交互队列创建
        Queue likeQueue = rabbitMQConfig.interactionLikeQueue();
        Queue bookmarkQueue = rabbitMQConfig.interactionBookmarkQueue();
        Queue commentQueue = rabbitMQConfig.interactionCommentQueue();
        Queue statsQueue = rabbitMQConfig.interactionStatsUpdateQueue();

        assertNotNull(likeQueue);
        assertNotNull(bookmarkQueue);
        assertNotNull(commentQueue);
        assertNotNull(statsQueue);

        assertEquals(RabbitMQConfig.INTERACTION_LIKE_QUEUE, likeQueue.getName());
        assertEquals(RabbitMQConfig.INTERACTION_BOOKMARK_QUEUE, bookmarkQueue.getName());
        assertEquals(RabbitMQConfig.INTERACTION_COMMENT_QUEUE, commentQueue.getName());
        assertEquals(RabbitMQConfig.INTERACTION_STATS_UPDATE_QUEUE, statsQueue.getName());

        // 验证队列是持久化的
        assertTrue(likeQueue.isDurable());
        assertTrue(bookmarkQueue.isDurable());
        assertTrue(commentQueue.isDurable());
        assertTrue(statsQueue.isDurable());
    }

    @Test
    void testDeadLetterQueueConfiguration() {
        // 测试死信队列配置
        Queue dlq = rabbitMQConfig.interactionDeadLetterQueue();
        DirectExchange dlx = rabbitMQConfig.interactionDeadLetterExchange();

        assertNotNull(dlq);
        assertNotNull(dlx);

        assertEquals(RabbitMQConfig.INTERACTION_DLQ, dlq.getName());
        assertEquals(RabbitMQConfig.INTERACTION_DLX, dlx.getName());

        assertTrue(dlq.isDurable());
        assertTrue(dlx.isDurable());
    }

    @Test
    void testQueueDeadLetterConfiguration() {
        // 测试队列的死信配置
        Queue likeQueue = rabbitMQConfig.interactionLikeQueue();

        assertNotNull(likeQueue.getArguments());
        assertEquals(RabbitMQConfig.INTERACTION_DLX,
                likeQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("like.failed",
                likeQueue.getArguments().get("x-dead-letter-routing-key"));
        assertEquals(300000,
                likeQueue.getArguments().get("x-message-ttl"));
    }

    @Test
    void testInteractionExchangeCreation() {
        // 测试交互交换机创建
        TopicExchange interactionExchange = rabbitMQConfig.interactionExchange();

        assertNotNull(interactionExchange);
        assertEquals(RabbitMQConfig.INTERACTION_EXCHANGE, interactionExchange.getName());
        assertTrue(interactionExchange.isDurable());
    }

    @Test
    void testInteractionBindings() {
        // 测试交互绑定
        Binding likeBinding = rabbitMQConfig.interactionLikeBinding();
        Binding bookmarkBinding = rabbitMQConfig.interactionBookmarkBinding();
        Binding commentBinding = rabbitMQConfig.interactionCommentBinding();
        Binding statsBinding = rabbitMQConfig.interactionStatsUpdateBinding();

        assertNotNull(likeBinding);
        assertNotNull(bookmarkBinding);
        assertNotNull(commentBinding);
        assertNotNull(statsBinding);

        assertEquals(RabbitMQConfig.INTERACTION_LIKE_QUEUE, likeBinding.getDestination());
        assertEquals(RabbitMQConfig.INTERACTION_BOOKMARK_QUEUE, bookmarkBinding.getDestination());
        assertEquals(RabbitMQConfig.INTERACTION_COMMENT_QUEUE, commentBinding.getDestination());
        assertEquals(RabbitMQConfig.INTERACTION_STATS_UPDATE_QUEUE, statsBinding.getDestination());

        assertEquals(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY, likeBinding.getRoutingKey());
        assertEquals(RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY, bookmarkBinding.getRoutingKey());
        assertEquals(RabbitMQConfig.INTERACTION_COMMENT_ROUTING_KEY, commentBinding.getRoutingKey());
        assertEquals(RabbitMQConfig.INTERACTION_STATS_UPDATE_ROUTING_KEY, statsBinding.getRoutingKey());
    }

    @Test
    void testDeadLetterBinding() {
        // 测试死信绑定
        Binding dlqBinding = rabbitMQConfig.interactionDeadLetterBinding();

        assertNotNull(dlqBinding);
        assertEquals(RabbitMQConfig.INTERACTION_DLQ, dlqBinding.getDestination());
        assertEquals(RabbitMQConfig.INTERACTION_DLX, dlqBinding.getExchange());
        assertEquals("*.failed", dlqBinding.getRoutingKey());
    }

    @Test
    void testQueueConstants() {
        // 测试队列常量定义
        assertNotNull(RabbitMQConfig.INTERACTION_LIKE_QUEUE);
        assertNotNull(RabbitMQConfig.INTERACTION_BOOKMARK_QUEUE);
        assertNotNull(RabbitMQConfig.INTERACTION_COMMENT_QUEUE);
        assertNotNull(RabbitMQConfig.INTERACTION_STATS_UPDATE_QUEUE);
        assertNotNull(RabbitMQConfig.INTERACTION_DLQ);
        assertNotNull(RabbitMQConfig.INTERACTION_DLX);
        assertNotNull(RabbitMQConfig.INTERACTION_EXCHANGE);

        // 验证常量值
        assertEquals("interaction.like.queue", RabbitMQConfig.INTERACTION_LIKE_QUEUE);
        assertEquals("interaction.bookmark.queue", RabbitMQConfig.INTERACTION_BOOKMARK_QUEUE);
        assertEquals("interaction.comment.queue", RabbitMQConfig.INTERACTION_COMMENT_QUEUE);
        assertEquals("interaction.stats.update.queue", RabbitMQConfig.INTERACTION_STATS_UPDATE_QUEUE);
        assertEquals("interaction.dlq", RabbitMQConfig.INTERACTION_DLQ);
        assertEquals("interaction.dlx", RabbitMQConfig.INTERACTION_DLX);
        assertEquals("interaction.exchange", RabbitMQConfig.INTERACTION_EXCHANGE);
    }

    @Test
    void testRoutingKeyConstants() {
        // 测试路由键常量
        assertNotNull(RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY);
        assertNotNull(RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY);
        assertNotNull(RabbitMQConfig.INTERACTION_COMMENT_ROUTING_KEY);
        assertNotNull(RabbitMQConfig.INTERACTION_STATS_UPDATE_ROUTING_KEY);

        assertEquals("interaction.like", RabbitMQConfig.INTERACTION_LIKE_ROUTING_KEY);
        assertEquals("interaction.bookmark", RabbitMQConfig.INTERACTION_BOOKMARK_ROUTING_KEY);
        assertEquals("interaction.comment", RabbitMQConfig.INTERACTION_COMMENT_ROUTING_KEY);
        assertEquals("interaction.stats.update", RabbitMQConfig.INTERACTION_STATS_UPDATE_ROUTING_KEY);
    }
}