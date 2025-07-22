package com.myweb.website_core.service;

import com.myweb.website_core.config.RabbitMQConfig;
import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.interaction.PostBookmark;
import com.myweb.website_core.demos.web.interaction.PostBookmarkRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.BookmarkMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 书签消息消费者
 * 处理异步书签添加/移除操作
 */
@Service
public class BookmarkMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookmarkMessageConsumer.class);
    
    // Cache key constants
    private static final String CACHE_POST_BOOKMARKS_COUNT = "post:bookmarks:count:";
    private static final String CACHE_USER_INTERACTION_STATUS = "user:%d:post:%d:interactions";
    private static final String CACHE_USER_BOOKMARKS = "user:%d:bookmarks:page:%d";

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 处理书签消息
     */
    @RabbitListener(queues = RabbitMQConfig.INTERACTION_BOOKMARK_QUEUE)
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public void handleBookmarkMessage(BookmarkMessageDto message) {
        logger.info("开始处理书签消息: {}", message);

        try {
            // 验证消息数据
            if (!validateMessage(message)) {
                logger.error("书签消息验证失败: {}", message);
                return;
            }

            // 获取用户和帖子实体
            Optional<User> userOpt = userRepository.findById(message.getUserId());
            Optional<Post> postOpt = postRepository.findById(message.getPostId());

            if (userOpt.isEmpty()) {
                logger.error("用户不存在: userId={}", message.getUserId());
                return;
            }

            if (postOpt.isEmpty()) {
                logger.error("帖子不存在: postId={}", message.getPostId());
                return;
            }

            User user = userOpt.get();
            Post post = postOpt.get();

            // 处理书签添加/移除逻辑
            if (message.isBookmark()) {
                handleBookmarkOperation(user, post, message);
            } else {
                handleUnbookmarkOperation(user, post, message);
            }

            // 更新缓存
            updateBookmarkCache(message.getPostId(), message.getUserId());

            logger.info("书签消息处理成功: messageId={}, operation={}", 
                       message.getMessageId(), message.isBookmark() ? "bookmark" : "unbookmark");

        } catch (Exception e) {
            logger.error("处理书签消息失败: {}", message, e);
            throw e; // 重新抛出异常以触发重试机制
        }
    }

    /**
     * 处理添加书签操作
     */
    private void handleBookmarkOperation(User user, Post post, BookmarkMessageDto message) {
        // 检查是否已经收藏（防止重复收藏）
        Optional<PostBookmark> existingBookmark = postBookmarkRepository.findByUserIdAndPostId(
            message.getUserId(), message.getPostId());

        if (existingBookmark.isEmpty()) {
            // 创建新的书签记录
            PostBookmark newBookmark = new PostBookmark(user, post);
            postBookmarkRepository.save(newBookmark);
            
            logger.info("创建书签记录成功: userId={}, postId={}, postTitle={}", 
                       message.getUserId(), message.getPostId(), message.getPostTitle());
        } else {
            logger.warn("用户已经收藏过该帖子: userId={}, postId={}", 
                       message.getUserId(), message.getPostId());
        }
    }

    /**
     * 处理移除书签操作
     */
    private void handleUnbookmarkOperation(User user, Post post, BookmarkMessageDto message) {
        // 查找并删除书签记录
        Optional<PostBookmark> existingBookmark = postBookmarkRepository.findByUserIdAndPostId(
            message.getUserId(), message.getPostId());

        if (existingBookmark.isPresent()) {
            postBookmarkRepository.delete(existingBookmark.get());
            
            logger.info("删除书签记录成功: userId={}, postId={}, postTitle={}", 
                       message.getUserId(), message.getPostId(), message.getPostTitle());
        } else {
            logger.warn("未找到要删除的书签记录: userId={}, postId={}", 
                       message.getUserId(), message.getPostId());
        }
    }

    /**
     * 更新书签相关缓存
     */
    private void updateBookmarkCache(Long postId, Long userId) {
        try {
            // 更新书签计数缓存
            long newBookmarkCount = postBookmarkRepository.countByPostId(postId);
            String countCacheKey = CACHE_POST_BOOKMARKS_COUNT + postId;
            redisTemplate.opsForValue().set(countCacheKey, newBookmarkCount, 1, TimeUnit.HOURS);

            // 清除用户交互状态缓存
            String userInteractionKey = String.format(CACHE_USER_INTERACTION_STATUS, userId, postId);
            redisTemplate.delete(userInteractionKey);

            // 清除用户书签列表缓存（所有分页）
            clearUserBookmarksCache(userId);

            logger.debug("更新书签缓存成功: postId={}, newCount={}", postId, newBookmarkCount);

        } catch (Exception e) {
            logger.error("更新书签缓存失败: postId={}, userId={}", postId, userId, e);
            // 缓存更新失败不应该影响主要业务逻辑，所以不重新抛出异常
        }
    }

    /**
     * 清除用户书签列表缓存
     */
    private void clearUserBookmarksCache(Long userId) {
        try {
            // 清除用户书签列表的所有分页缓存
            // 这里使用模式匹配删除所有相关的缓存键
            String pattern = String.format("user:%d:bookmarks:page:*", userId);
            
            // 获取所有匹配的键并删除
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            logger.debug("清除用户书签缓存成功: userId={}", userId);

        } catch (Exception e) {
            logger.error("清除用户书签缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 验证消息数据的完整性
     */
    private boolean validateMessage(BookmarkMessageDto message) {
        if (message == null) {
            logger.error("书签消息为空");
            return false;
        }

        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            logger.error("消息ID为空: {}", message);
            return false;
        }

        if (message.getUserId() == null || message.getUserId() <= 0) {
            logger.error("用户ID无效: {}", message);
            return false;
        }

        if (message.getPostId() == null || message.getPostId() <= 0) {
            logger.error("帖子ID无效: {}", message);
            return false;
        }

        if (message.getUsername() == null || message.getUsername().trim().isEmpty()) {
            logger.error("用户名为空: {}", message);
            return false;
        }

        if (message.getPostTitle() == null || message.getPostTitle().trim().isEmpty()) {
            logger.error("帖子标题为空: {}", message);
            return false;
        }

        return true;
    }

    /**
     * 获取当前书签数（用于统计和监控）
     */
    public long getCurrentBookmarkCount(Long postId) {
        return postBookmarkRepository.countByPostId(postId);
    }

    /**
     * 检查用户是否已收藏（用于状态查询）
     */
    public boolean isUserBookmarked(Long userId, Long postId) {
        return postBookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * 获取用户书签总数
     */
    public long getUserBookmarkCount(Long userId) {
        return postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
    }
}