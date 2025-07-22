package com.myweb.website_core.service;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.interaction.PostBookmark;
import com.myweb.website_core.demos.web.interaction.PostBookmarkRepository;
import com.myweb.website_core.demos.web.interaction.PostLike;
import com.myweb.website_core.demos.web.interaction.PostLikeRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import com.myweb.website_core.dto.InteractionResponse;
import com.myweb.website_core.dto.PostInteractionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 帖子交互服务实现类
 * 提供异步的点赞、书签和交互状态查询功能
 */
@Service
public class PostInteractionServiceImpl implements PostInteractionService {

    private static final Logger logger = LoggerFactory.getLogger(PostInteractionServiceImpl.class);

    // Cache key constants
    private static final String CACHE_POST_LIKES_COUNT = "post:likes:count:";
    private static final String CACHE_POST_BOOKMARKS_COUNT = "post:bookmarks:count:";
    private static final String CACHE_POST_COMMENTS_COUNT = "post:comments:count:";
    private static final String CACHE_USER_INTERACTION_STATUS = "user:%d:post:%d:interactions";
    private static final String CACHE_USER_BOOKMARKS = "user:%d:bookmarks:page:%d";

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostBookmarkRepository postBookmarkRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InteractionMessageService interactionMessageService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Async
    @Transactional
    public CompletableFuture<InteractionResponse> toggleLike(Long postId, Long userId) {
        logger.info("开始处理点赞操作: postId={}, userId={}", postId, userId);

        try {
            // 验证用户和帖子是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Post> postOpt = postRepository.findById(postId);

            if (userOpt.isEmpty()) {
                logger.warn("用户不存在: userId={}", userId);
                return CompletableFuture.completedFuture(
                    InteractionResponse.failure("like", postId, userId, "用户不存在")
                );
            }

            if (postOpt.isEmpty()) {
                logger.warn("帖子不存在: postId={}", postId);
                return CompletableFuture.completedFuture(
                    InteractionResponse.failure("like", postId, userId, "帖子不存在")
                );
            }

            User user = userOpt.get();
            Post post = postOpt.get();

            // 检查是否已经点赞
            Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(userId, postId);
            boolean isLike;
            String operation;

            if (existingLike.isPresent()) {
                // 取消点赞
                postLikeRepository.delete(existingLike.get());
                isLike = false;
                operation = "unlike";
                logger.info("取消点赞成功: postId={}, userId={}", postId, userId);
            } else {
                // 添加点赞
                PostLike newLike = new PostLike(user, post);
                postLikeRepository.save(newLike);
                isLike = true;
                operation = "like";
                logger.info("点赞成功: postId={}, userId={}", postId, userId);
            }

            // 获取新的点赞数
            long newLikeCount = postLikeRepository.countByPostId(postId);

            // 清除相关缓存
            clearInteractionCache(postId, userId);

            // 发送异步消息
            interactionMessageService.sendLikeMessage(
                userId, user.getUsername(), postId, isLike, 
                post.getTitle(), post.getAuthor().getId()
            );

            // 发送统计更新消息
            interactionMessageService.sendStatsUpdateMessage(
                userId, user.getUsername(), postId, operation, 
                isLike ? 1 : -1, "LIKE"
            );

            return CompletableFuture.completedFuture(
                InteractionResponse.success(operation, postId, userId, newLikeCount, isLike)
            );

        } catch (Exception e) {
            logger.error("点赞操作失败: postId={}, userId={}", postId, userId, e);
            return CompletableFuture.completedFuture(
                InteractionResponse.failure("like", postId, userId, "操作失败: " + e.getMessage())
            );
        }
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<InteractionResponse> toggleBookmark(Long postId, Long userId) {
        logger.info("开始处理书签操作: postId={}, userId={}", postId, userId);

        try {
            // 验证用户和帖子是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Post> postOpt = postRepository.findById(postId);

            if (userOpt.isEmpty()) {
                logger.warn("用户不存在: userId={}", userId);
                return CompletableFuture.completedFuture(
                    InteractionResponse.failure("bookmark", postId, userId, "用户不存在")
                );
            }

            if (postOpt.isEmpty()) {
                logger.warn("帖子不存在: postId={}", postId);
                return CompletableFuture.completedFuture(
                    InteractionResponse.failure("bookmark", postId, userId, "帖子不存在")
                );
            }

            User user = userOpt.get();
            Post post = postOpt.get();

            // 检查是否已经收藏
            Optional<PostBookmark> existingBookmark = postBookmarkRepository.findByUserIdAndPostId(userId, postId);
            boolean isBookmark;
            String operation;

            if (existingBookmark.isPresent()) {
                // 取消收藏
                postBookmarkRepository.delete(existingBookmark.get());
                isBookmark = false;
                operation = "unbookmark";
                logger.info("取消收藏成功: postId={}, userId={}", postId, userId);
            } else {
                // 添加收藏
                PostBookmark newBookmark = new PostBookmark(user, post);
                postBookmarkRepository.save(newBookmark);
                isBookmark = true;
                operation = "bookmark";
                logger.info("收藏成功: postId={}, userId={}", postId, userId);
            }

            // 获取新的收藏数
            long newBookmarkCount = postBookmarkRepository.countByPostId(postId);

            // 清除相关缓存
            clearInteractionCache(postId, userId);

            // 发送异步消息
            interactionMessageService.sendBookmarkMessage(
                userId, user.getUsername(), postId, isBookmark,
                post.getTitle(), post.getAuthor().getId(), post.getAuthor().getUsername()
            );

            // 发送统计更新消息
            interactionMessageService.sendStatsUpdateMessage(
                userId, user.getUsername(), postId, operation,
                isBookmark ? 1 : -1, "BOOKMARK"
            );

            return CompletableFuture.completedFuture(
                InteractionResponse.success(operation, postId, userId, newBookmarkCount, isBookmark)
            );

        } catch (Exception e) {
            logger.error("书签操作失败: postId={}, userId={}", postId, userId, e);
            return CompletableFuture.completedFuture(
                InteractionResponse.failure("bookmark", postId, userId, "操作失败: " + e.getMessage())
            );
        }
    }

    @Override
    @Async
    @Cacheable(value = "postInteractionStatus", key = "#postId + ':' + (#userId != null ? #userId : 'anonymous')")
    public CompletableFuture<PostInteractionStatus> getInteractionStatus(Long postId, Long userId) {
        logger.info("获取交互状态: postId={}, userId={}", postId, userId);

        try {
            // 验证帖子是否存在
            if (!postRepository.existsById(postId)) {
                logger.warn("帖子不存在: postId={}", postId);
                return CompletableFuture.completedFuture(null);
            }

            // 从缓存获取计数信息
            long likeCount = getLikeCountFromCache(postId);
            long bookmarkCount = getBookmarkCountFromCache(postId);
            long commentCount = getCommentCountFromCache(postId);

            boolean isLiked = false;
            boolean isBookmarked = false;

            // 如果用户已登录，检查用户的交互状态
            if (userId != null) {
                isLiked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
                isBookmarked = postBookmarkRepository.existsByUserIdAndPostId(userId, postId);
            }

            PostInteractionStatus status = new PostInteractionStatus(
                postId, userId, isLiked, isBookmarked, 
                likeCount, bookmarkCount, commentCount
            );

            logger.info("交互状态获取成功: {}", status);
            return CompletableFuture.completedFuture(status);

        } catch (Exception e) {
            logger.error("获取交互状态失败: postId={}, userId={}", postId, userId, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    @Async
    @Cacheable(value = "userBookmarks", key = "#userId + ':' + #pageable.pageNumber")
    public CompletableFuture<Page<PostBookmark>> getUserBookmarks(Long userId, Pageable pageable) {
        logger.info("获取用户书签列表: userId={}, page={}", userId, pageable.getPageNumber());

        try {
            // 验证用户是否存在
            if (!userRepository.existsById(userId)) {
                logger.warn("用户不存在: userId={}", userId);
                return CompletableFuture.completedFuture(Page.empty());
            }

            Page<PostBookmark> bookmarks = postBookmarkRepository.findUserBookmarksWithPostDetails(userId, pageable);
            logger.info("用户书签列表获取成功: userId={}, totalElements={}", userId, bookmarks.getTotalElements());
            
            return CompletableFuture.completedFuture(bookmarks);

        } catch (Exception e) {
            logger.error("获取用户书签列表失败: userId={}", userId, e);
            return CompletableFuture.completedFuture(Page.empty());
        }
    }

    /**
     * 从缓存获取点赞数，如果缓存不存在则从数据库获取并缓存
     */
    private long getLikeCountFromCache(Long postId) {
        String cacheKey = CACHE_POST_LIKES_COUNT + postId;
        Object cachedCount = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedCount != null) {
            return Long.parseLong(cachedCount.toString());
        }
        
        // 从数据库获取并缓存
        long count = postLikeRepository.countByPostId(postId);
        redisTemplate.opsForValue().set(cacheKey, count, 1, TimeUnit.HOURS);
        return count;
    }

    /**
     * 从缓存获取书签数，如果缓存不存在则从数据库获取并缓存
     */
    private long getBookmarkCountFromCache(Long postId) {
        String cacheKey = CACHE_POST_BOOKMARKS_COUNT + postId;
        Object cachedCount = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedCount != null) {
            return Long.parseLong(cachedCount.toString());
        }
        
        // 从数据库获取并缓存
        long count = postBookmarkRepository.countByPostId(postId);
        redisTemplate.opsForValue().set(cacheKey, count, 1, TimeUnit.HOURS);
        return count;
    }

    /**
     * 从缓存获取评论数，如果缓存不存在则返回帖子的评论数
     */
    private long getCommentCountFromCache(Long postId) {
        String cacheKey = CACHE_POST_COMMENTS_COUNT + postId;
        Object cachedCount = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedCount != null) {
            return Long.parseLong(cachedCount.toString());
        }
        
        // 从帖子实体获取评论数并缓存
        Optional<Post> postOpt = postRepository.findById(postId);
        long count = postOpt.map(post -> post.getCommentCount().longValue()).orElse(0L);
        redisTemplate.opsForValue().set(cacheKey, count, 1, TimeUnit.HOURS);
        return count;
    }

    /**
     * 清除交互相关的缓存
     */
    @CacheEvict(value = {"postInteractionStatus", "userBookmarks"}, allEntries = true)
    private void clearInteractionCache(Long postId, Long userId) {
        // 清除计数缓存
        redisTemplate.delete(CACHE_POST_LIKES_COUNT + postId);
        redisTemplate.delete(CACHE_POST_BOOKMARKS_COUNT + postId);
        
        // 清除用户交互状态缓存
        String userInteractionKey = String.format(CACHE_USER_INTERACTION_STATUS, userId, postId);
        redisTemplate.delete(userInteractionKey);
        
        logger.debug("清除交互缓存: postId={}, userId={}", postId, userId);
    }
}