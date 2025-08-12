package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.file.ImageService;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.application.service.security.SafeQueryService;
import com.myweb.website_core.application.service.security.QueryPerformanceMonitorService;
import com.myweb.website_core.common.util.SafeSqlBuilder;
import com.myweb.website_core.common.util.SecurityUtils;
import com.myweb.website_core.common.util.LoggingUtils;
import com.myweb.website_core.common.exception.DataIntegrityException;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.dto.PostDTO;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.dto.CollectResponse;
import com.myweb.website_core.domain.business.dto.LikeResponse;
import com.myweb.website_core.domain.business.entity.PostCollect;
import com.myweb.website_core.infrastructure.persistence.repository.interaction.PostCollectRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import com.myweb.website_core.infrastructure.persistence.mapper.PostMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MessageProducerService messageProducerService;
    private final PostMapper postMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostCollectRepository postCollectRepository;
    private final PostLikeService postLikeService;
    private final ImageService imageService;
    private final DataIntegrityService dataIntegrityService;
    private final AuditLogService auditLogService;
    private final SafeQueryService safeQueryService;
    private final QueryPerformanceMonitorService queryPerformanceMonitorService;


    //@Async
    //@CacheEvict(value = "posts", allEntries = true)
    public Post createPost(Post post) {
        long startTime = System.currentTimeMillis();
        String username = "unknown";
        
        try {
            // 只允许已登录用户发帖，author.id必须存在
            if (post.getAuthor() == null || post.getAuthor().getId() == null) {
                throw new RuntimeException("未指定作者");
            }
            
            User author = userRepository.findById(post.getAuthor().getId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            username = author.getUsername();
            
            // 使用安全工具类验证输入
            SecurityUtils.validatePostTitle(post.getTitle());
            SecurityUtils.validatePostContent(post.getContent());
            
            // 使用DataIntegrityService计算内容哈希值用于完整性检查
            String contentHash = dataIntegrityService.calculateHash(post.getContent());
            post.setContentHash(contentHash);
            post.setHashCalculatedAt(java.time.LocalDateTime.now());
            
            post.setAuthor(author);
            post.setCreatedAt(java.time.LocalDateTime.now());

            // postMapper.insertPost(post);
            Post savedPost = postRepository.save(post);

            // 清除Redis缓存
            // redisTemplate.delete("posts:all");

            // 发送消息到RabbitMQ
            // messageProducerService.sendPostCreatedMessage(post);
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "CREATE_POST", username, "POST:" + savedPost.getId(), "SUCCESS", 
                "标题: " + post.getTitle(), executionTime);
            log.info(logMessage);

            return savedPost;
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "POST_CREATION_ERROR", e.getMessage(), username, 
                null, "创建帖子", null);
            log.error(errorLog);
            throw e;
        }
    }

    // @Async
    public Post editPost(Long id, Post updatedPost) {
        long startTime = System.currentTimeMillis();
        String username = "unknown";
        
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
            username = post.getAuthor().getUsername();
            
            // 只允许作者本人编辑
            if (updatedPost.getAuthor() == null || !post.getAuthor().getId().equals(updatedPost.getAuthor().getId())) {
                throw new RuntimeException("无权限编辑");
            }
            
            // 使用DataIntegrityService验证原内容完整性
            if (post.getContentHash() != null) {
                DataIntegrityService.IntegrityCheckResult integrityResult = 
                    dataIntegrityService.checkPostIntegrity(id, post.getContent(), post.getContentHash());
                
                if (!integrityResult.isValid()) {
                    String errorMessage = "帖子内容完整性验证失败: " + integrityResult.getErrorMessage();
                    log.warn("帖子编辑时完整性验证失败: postId={}, error={}", id, integrityResult.getErrorMessage());
                    
                    // 记录完整性验证失败的审计日志
                    auditLogService.logSecurityEvent(
                        AuditOperation.INTEGRITY_CHECK,
                        username,
                        String.format("帖子编辑时完整性验证失败: postId=%d, 原因=%s", id, integrityResult.getErrorMessage())
                    );
                    
                    throw new DataIntegrityException(errorMessage);
                }
                
                // 记录完整性验证成功的审计日志
                auditLogService.logSecurityEvent(
                    AuditOperation.INTEGRITY_CHECK,
                    username,
                    String.format("帖子编辑前完整性验证通过: postId=%d", id)
                );
            }
            
            // 使用安全工具类验证新输入
            SecurityUtils.validatePostTitle(updatedPost.getTitle());
            SecurityUtils.validatePostContent(updatedPost.getContent());
            
            // 更新内容并使用DataIntegrityService计算新哈希
            post.setTitle(updatedPost.getTitle());
            post.setContent(updatedPost.getContent());
            post.setContentHash(dataIntegrityService.calculateHash(updatedPost.getContent()));
            post.setHashCalculatedAt(java.time.LocalDateTime.now());
            
            // post.setImageIds(updatedPost.getImageIds());
            Post savedPost = postRepository.save(post);
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "EDIT_POST", username, "POST:" + id, "SUCCESS", 
                "更新标题: " + updatedPost.getTitle(), executionTime);
            log.info(logMessage);
            
            // 记录帖子编辑成功的审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.POST_UPDATE,
                username,
                String.format("帖子编辑成功: postId=%d, 标题=%s", id, updatedPost.getTitle())
            );
            
            return savedPost;
            
        } catch (DataIntegrityException e) {
            // 数据完整性异常特殊处理
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "POST_INTEGRITY_ERROR", e.getMessage(), username, 
                null, "编辑帖子ID:" + id + " 完整性验证失败", null);
            log.error(errorLog);
            throw e;
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "POST_EDIT_ERROR", e.getMessage(), username, 
                null, "编辑帖子ID:" + id, null);
            log.error(errorLog);
            throw e;
        }
    }

    // @Async
    public LikeResponse likePost(Long postId, Long userId) {
        return postLikeService.toggleLike(postId, userId);
    }

    // @Async
    @Transactional
    public CollectResponse collectPost(Long postId, Long userId) {
       return postLikeService.toggleCollect(postId, userId);
    }

    @Async
    public CompletableFuture<List<Post>> getTopLikedPosts() {
        // 获赞前20逻辑
        return CompletableFuture.completedFuture(null);
    }

    // @Async
    @CachePut(value = "posts", key = "'posts:all'")
    public List<Post> getAllPosts() {
        try {
            return postRepository.findAll();
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error in getAllPosts: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    // @Async
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    public List<Post> findPostsByUserId(Long userId) {
        return postRepository.findByAuthorId(userId);
    }

    public List<Post> findCollectedPostsByUserId(Long userId) {
        List<PostCollect> collects = postCollectRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return collects.stream()
                .map(PostCollect::getPost)
                .toList();
    }

    public List<Post> findLikedPostsByUserId(Long userId) {
        return postLikeService.getUserLikedPosts(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
    }

    public boolean isPostLikedByUser(Long postId, Long userId) {
        return postLikeService.isLikedByUser(postId, userId);
    }
    
    public boolean isPostCollectedByUser(Long postId, Long userId) {
      return postLikeService.isPostCollectedByUser(postId, userId);
    }

    /**
     * 将Post转换为PostDTO，包含图片URL
     */
    public PostDTO convertToDTO(Post post) {
        List<String> imageUrls = new ArrayList<>();
        
        try {
            // 获取图片ID列表（现在从关联的Image实体获取）
            List<Long> imageIds = post.getImageIds();
            if (imageIds != null && !imageIds.isEmpty()) {
                imageUrls = imageIds.stream()
                        .filter(Objects::nonNull)
                        .map(imageId -> "/blog/api/images/" + imageId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("转换图片URL时出错: " + e.getMessage());
            // 如果出错，返回空的图片列表
            imageUrls = new ArrayList<>();
        }
        
        return new PostDTO(post, imageUrls);
    }

    /**
     * 将Post列表转换为PostDTO列表
     */
    public List<PostDTO> convertToDTOList(List<Post> posts) {
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 通过图片ID列表关联图片到帖子（推荐方式）
     */
    public void associateImagesByIds(Long postId, List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            System.out.println("没有图片ID需要关联到帖子 " + postId);
            return;
        }

        System.out.println("开始通过ID关联图片到帖子 " + postId + ", 图片IDs: " + imageIds);

        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));

            for (Long imageId : imageIds) {
                try {
                    imageService.associateImageToPost(imageId, post);
                    System.out.println("成功关联图片 " + imageId + " 到帖子 " + postId);
                } catch (Exception e) {
                    System.err.println("关联图片失败: imageId=" + imageId + ", error=" + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "通过ID关联图片到帖子失败: postId=" + postId + ", imageIds=" + imageIds + ", error=" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将未关联的图片关联到指定帖子（通过URL，向后兼容）
     */
    public void associateImagesToPost(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            System.out.println("没有图片需要关联到帖子 " + postId);
            return;
        }

        System.out.println("开始关联图片到帖子 " + postId + ", 图片URLs: " + imageUrls);

        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));

            for (String imageUrl : imageUrls) {
                System.out.println("处理图片URL: '" + imageUrl + "'");
                String imageIdStr;
                if (imageUrl.startsWith("/blog/api/images/")) {
                    imageIdStr = imageUrl.substring("/blog/api/images/".length());
                } else if (imageUrl.contains("/blog/api/images/")) {
                    int index = imageUrl.indexOf("/blog/api/images/");
                    imageIdStr = imageUrl.substring(index + "/blog/api/images/".length());
                } else {
                    System.err.println("图片URL格式不匹配: '" + imageUrl);
                    continue;
                }

                if (!imageIdStr.isEmpty()) {
                    try {
                        Long imageId = Long.parseLong(imageIdStr);
                        System.out.println("解析的图片ID: " + imageId);

                        imageService.associateImageToPost(imageId, post);
                        System.out.println("成功关联图片 " + imageId + " 到帖子 " + postId);

                    } catch (NumberFormatException e) {
                        System.err.println(
                                "无效的图片ID格式: '" + imageIdStr + "', 完整URL: '" + imageUrl + "', 错误: " + e.getMessage());
                    }
                } else {
                    System.err.println("无法提取图片ID: '" + imageUrl + "'");
                }
            }

            System.out.println("关联完成: 成功关联 " + imageUrls.size() + " 张图片到帖子 " + postId);

        } catch (Exception e) {
            System.err
                    .println("关联图片到帖子失败: postId=" + postId + ", imageUrls=" + imageUrls + ", error=" + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响帖子创建
        }
    }

    /**
     * 验证单个帖子的完整性
     * 
     * @param postId 帖子ID
     * @return 完整性检查结果
     */
    public DataIntegrityService.IntegrityCheckResult verifyPostIntegrity(Long postId) {
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
            if (post.getContentHash() == null) {
                return DataIntegrityService.IntegrityCheckResult.builder()
                    .entityType("POST")
                    .entityId(postId)
                    .isValid(false)
                    .errorMessage("帖子未设置完整性哈希值")
                    .checkTime(java.time.LocalDateTime.now())
                    .build();
            }
            
            return dataIntegrityService.checkPostIntegrity(postId, post.getContent(), post.getContentHash());
            
        } catch (Exception e) {
            log.error("验证帖子完整性失败: postId={}", postId, e);
            return DataIntegrityService.IntegrityCheckResult.builder()
                .entityType("POST")
                .entityId(postId)
                .isValid(false)
                .errorMessage("验证帖子完整性异常: " + e.getMessage())
                .checkTime(java.time.LocalDateTime.now())
                .build();
        }
    }
    
    /**
     * 批量验证帖子完整性
     * 
     * @param postIds 帖子ID列表
     * @return 完整性检查结果列表
     */
    public List<DataIntegrityService.IntegrityCheckResult> verifyPostsIntegrity(List<Long> postIds) {
        return postIds.stream()
                .map(this::verifyPostIntegrity)
                .collect(Collectors.toList());
    }
    
    /**
     * 验证用户所有帖子的完整性
     * 
     * @param userId 用户ID
     * @return 完整性检查结果列表
     */
    public List<DataIntegrityService.IntegrityCheckResult> verifyUserPostsIntegrity(Long userId) {
        List<Post> userPosts = postRepository.findByAuthorId(userId);
        return userPosts.stream()
                .map(post -> verifyPostIntegrity(post.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取帖子完整性统计信息
     * 
     * @return 帖子完整性统计
     */
    public PostIntegrityStats getPostIntegrityStats() {
        try {
            List<Post> allPosts = postRepository.findAll();
            
            long totalPosts = allPosts.size();
            long postsWithHash = allPosts.stream()
                    .mapToLong(post -> post.getContentHash() != null ? 1 : 0)
                    .sum();
            
            // 验证有哈希值的帖子
            List<DataIntegrityService.IntegrityCheckResult> results = allPosts.stream()
                    .filter(post -> post.getContentHash() != null)
                    .map(post -> dataIntegrityService.checkPostIntegrity(
                        post.getId(), post.getContent(), post.getContentHash()))
                    .collect(Collectors.toList());
            
            long validPosts = results.stream()
                    .mapToLong(result -> result.isValid() ? 1 : 0)
                    .sum();
            
            long invalidPosts = results.stream()
                    .mapToLong(result -> result.isValid() ? 0 : 1)
                    .sum();
            
            double integrityRate = postsWithHash > 0 ? (double) validPosts / postsWithHash : 0.0;
            
            return PostIntegrityStats.builder()
                    .totalPosts(totalPosts)
                    .postsWithHash(postsWithHash)
                    .validPosts(validPosts)
                    .invalidPosts(invalidPosts)
                    .integrityRate(integrityRate)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取帖子完整性统计失败", e);
            return PostIntegrityStats.builder()
                    .totalPosts(0)
                    .postsWithHash(0)
                    .validPosts(0)
                    .invalidPosts(0)
                    .integrityRate(0.0)
                    .build();
        }
    }
    
    /**
     * 帖子完整性统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class PostIntegrityStats {
        private long totalPosts;
        private long postsWithHash;
        private long validPosts;
        private long invalidPosts;
        private double integrityRate;
        
        @Override
        public String toString() {
            return String.format("PostIntegrityStats{总帖子数=%d, 有哈希帖子数=%d, 有效帖子数=%d, 无效帖子数=%d, 完整性率=%.2f%%}",
                    totalPosts, postsWithHash, validPosts, invalidPosts, integrityRate * 100);
        }
    }

    /**
     * 安全的帖子搜索功能
     * 使用SafeQueryService进行安全验证和SQL注入检测
     * 
     * @param keyword 搜索关键词
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @param page 页码
     * @param size 页大小
     * @return 搜索结果
     */
    public Page<PostDTO> searchPostsSafely(String keyword, String sortField, 
                                         String sortDirection, int page, int size) {
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.info("开始执行安全帖子搜索: keyword={}, sortField={}, sortDirection={}, page={}, size={}", 
                    keyword, sortField, sortDirection, page, size);
            
            // 使用SafeQueryService验证搜索关键词
            if (keyword != null && !keyword.trim().isEmpty()) {
                boolean isValidInput = safeQueryService.validateUserInputSafety(keyword, "SEARCH", "keyword");
                if (!isValidInput) {
                    throw new IllegalArgumentException("搜索关键词包含非法字符或潜在的安全威胁");
                }
                
                // 检测SQL注入攻击
                boolean isSqlInjection = safeQueryService.detectSqlInjectionAttempt(keyword, "POST_SEARCH");
                if (isSqlInjection) {
                    log.warn("检测到SQL注入攻击尝试: keyword={}", keyword);
                    auditLogService.logSecurityEvent(
                        AuditOperation.SECURITY_EVENT,
                        username,
                        String.format("帖子搜索中检测到SQL注入攻击: keyword=%s", keyword)
                    );
                    throw new IllegalArgumentException("检测到潜在的SQL注入攻击");
                }
            }
            
            // 验证排序字段是否在白名单中
            if (sortField != null && !sortField.trim().isEmpty()) {
                List<String> allowedSortFields = safeQueryService.getAllowedSortFields("posts");
                if (!allowedSortFields.contains(sortField)) {
                    log.warn("尝试使用不允许的排序字段: {}", sortField);
                    throw new IllegalArgumentException("不允许的排序字段: " + sortField);
                }
            }
            
            // 使用安全的Repository进行搜索
            Page<Post> posts = postRepository.findPostsWithSafeSearch(
                keyword, sortField, sortDirection, page, size);
            
            // 转换为DTO
            List<PostDTO> postDTOs = posts.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            // 创建分页结果
            Page<PostDTO> result = new org.springframework.data.domain.PageImpl<>(
                postDTOs, posts.getPageable(), posts.getTotalElements());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "SEARCH_POSTS", username, "SEARCH", "SUCCESS", 
                String.format("搜索关键词: %s, 结果数: %d", keyword, result.getTotalElements()), 
                executionTime);
            log.info(logMessage);
            
            return result;
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "POST_SEARCH_ERROR", e.getMessage(), username, 
                null, String.format("搜索帖子失败: keyword=%s", keyword), null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 安全的分页查询帖子
     * 
     * @param page 页码
     * @param size 页大小
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return 分页结果
     */
    public Page<PostDTO> getPostsWithSafePagination(int page, int size, 
                                                  String sortField, String sortDirection) {
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.debug("执行安全分页查询: page={}, size={}, sortField={}, sortDirection={}", 
                     page, size, sortField, sortDirection);
            
            // 验证分页参数
            if (page < 0) {
                throw new IllegalArgumentException("页码不能为负数");
            }
            if (size <= 0 || size > 100) {
                throw new IllegalArgumentException("页大小必须在1-100之间");
            }
            
            // 验证排序字段
            if (sortField != null && !sortField.trim().isEmpty()) {
                List<String> allowedSortFields = safeQueryService.getAllowedSortFields("posts");
                if (!allowedSortFields.contains(sortField)) {
                    throw new IllegalArgumentException("不允许的排序字段: " + sortField);
                }
            }
            
            // 使用安全的Repository进行分页查询
            Map<String, Object> conditions = new HashMap<>();
            Page<Post> posts = postRepository.findSafePaginated(
                conditions, sortField, sortDirection, page, size);
            
            // 转换为DTO
            List<PostDTO> postDTOs = posts.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Page<PostDTO> result = new org.springframework.data.domain.PageImpl<>(
                postDTOs, posts.getPageable(), posts.getTotalElements());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "GET_POSTS_PAGINATED", username, "QUERY", "SUCCESS", 
                String.format("分页查询: page=%d, size=%d, 结果数=%d", page, size, result.getTotalElements()), 
                executionTime);
            log.debug(logMessage);
            
            return result;
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "POST_PAGINATION_ERROR", e.getMessage(), username, 
                null, String.format("分页查询失败: page=%d, size=%d", page, size), null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 根据作者ID安全查询帖子
     * 
     * @param authorId 作者ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    public Page<PostDTO> getPostsByAuthorSafely(Long authorId, int page, int size) {
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.debug("执行安全作者帖子查询: authorId={}, page={}, size={}", authorId, page, size);
            
            // 验证作者ID
            if (authorId == null || authorId <= 0) {
                throw new IllegalArgumentException("无效的作者ID");
            }
            
            // 验证分页参数
            if (page < 0) {
                throw new IllegalArgumentException("页码不能为负数");
            }
            if (size <= 0 || size > 100) {
                throw new IllegalArgumentException("页大小必须在1-100之间");
            }
            
            // 使用安全的Repository进行查询
            Page<Post> posts = postRepository.findPostsByAuthorWithSafePagination(authorId, page, size);
            
            // 转换为DTO
            List<PostDTO> postDTOs = posts.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Page<PostDTO> result = new org.springframework.data.domain.PageImpl<>(
                postDTOs, posts.getPageable(), posts.getTotalElements());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "GET_POSTS_BY_AUTHOR", username, "QUERY", "SUCCESS", 
                String.format("作者帖子查询: authorId=%d, 结果数=%d", authorId, result.getTotalElements()), 
                executionTime);
            log.debug(logMessage);
            
            return result;
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "POST_BY_AUTHOR_ERROR", e.getMessage(), username, 
                null, String.format("作者帖子查询失败: authorId=%d", authorId), null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 安全的热门帖子查询
     * 
     * @param limit 限制数量
     * @return 热门帖子列表
     */
    public List<PostDTO> getTopLikedPostsSafely(int limit) {
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.debug("执行安全热门帖子查询: limit={}", limit);
            
            // 验证限制参数
            if (limit <= 0 || limit > 100) {
                throw new IllegalArgumentException("限制数量必须在1-100之间");
            }
            
            // 使用安全的Repository进行查询
            List<Post> posts = postRepository.findTopLikedPostsSafely(limit);
            
            // 转换为DTO
            List<PostDTO> postDTOs = posts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "GET_TOP_LIKED_POSTS", username, "QUERY", "SUCCESS", 
                String.format("热门帖子查询: limit=%d, 结果数=%d", limit, postDTOs.size()), 
                executionTime);
            log.debug(logMessage);
            
            return postDTOs;
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "TOP_LIKED_POSTS_ERROR", e.getMessage(), username, 
                null, String.format("热门帖子查询失败: limit=%d", limit), null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 安全的复合条件查询
     * 
     * @param conditions 查询条件
     * @param sortField 排序字段
     * @param sortDirection 排序方向
     * @return 查询结果
     */
    public List<PostDTO> findPostsByConditionsSafely(Map<String, Object> conditions, 
                                                    String sortField, String sortDirection) {
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.debug("执行安全复合条件查询: conditions={}, sortField={}, sortDirection={}", 
                     conditions, sortField, sortDirection);
            
            // 验证查询条件中的字符串值
            if (conditions != null) {
                for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        String value = (String) entry.getValue();
                        boolean isValidInput = safeQueryService.validateUserInputSafety(
                            value, "CONDITION", entry.getKey());
                        if (!isValidInput) {
                            throw new IllegalArgumentException(
                                String.format("查询条件包含非法字符: %s=%s", entry.getKey(), value));
                        }
                    }
                }
            }
            
            // 验证排序字段
            if (sortField != null && !sortField.trim().isEmpty()) {
                List<String> allowedSortFields = safeQueryService.getAllowedSortFields("posts");
                if (!allowedSortFields.contains(sortField)) {
                    throw new IllegalArgumentException("不允许的排序字段: " + sortField);
                }
            }
            
            // 使用安全的Repository进行查询
            List<Post> posts = postRepository.findPostsByConditionsSafely(
                conditions, sortField, sortDirection);
            
            // 转换为DTO
            List<PostDTO> postDTOs = posts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "FIND_POSTS_BY_CONDITIONS", username, "QUERY", "SUCCESS", 
                String.format("复合条件查询: 结果数=%d", postDTOs.size()), 
                executionTime);
            log.debug(logMessage);
            
            return postDTOs;
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "POSTS_BY_CONDITIONS_ERROR", e.getMessage(), username, 
                null, "复合条件查询失败", null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 获取允许的排序字段列表
     * 
     * @return 允许的排序字段列表
     */
    public List<String> getAllowedSortFields() {
        return safeQueryService.getAllowedSortFields("posts");
    }
    
    /**
     * 添加允许的排序字段
     * 
     * @param fields 字段列表
     */
    public void addAllowedSortFields(List<String> fields) {
        safeQueryService.addAllowedSortFields("posts", fields);
        log.info("为posts表添加允许的排序字段: {}", fields);
    }
    
    /**
     * 使用动态查询构建器进行复杂查询
     * 支持多表关联、聚合函数、子查询等复杂场景
     * 
     * @param queryBuilder 查询构建器
     * @return 查询结果
     */
    public List<PostDTO> executeComplexDynamicQuery(SafeSqlBuilder.DynamicQueryBuilder queryBuilder) {
        QueryPerformanceMonitorService.QueryPerformanceMonitor monitor = 
            queryPerformanceMonitorService.startMonitoring("COMPLEX_DYNAMIC_QUERY", 
                "复杂动态查询: " + queryBuilder.getMainTable());
        
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.debug("执行复杂动态查询: mainTable={}", queryBuilder.getMainTable());
            
            // 验证查询构建器的安全性
            boolean isValidBuilder = safeQueryService.validateQueryBuilder(queryBuilder);
            if (!isValidBuilder) {
                throw new IllegalArgumentException("查询构建器包含不安全的内容");
            }
            
            // 构建安全的动态查询
            String dynamicQuery = safeQueryService.buildComplexDynamicQuery(queryBuilder);
            
            // 执行查询（这里使用Repository的安全查询方法）
            List<Post> posts = postRepository.findPostsByComplexQuery(dynamicQuery, queryBuilder.getConditions());
            
            // 转换为DTO
            List<PostDTO> postDTOs = posts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            // 记录成功的性能监控
            monitor.finish(postDTOs.size());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "COMPLEX_DYNAMIC_QUERY", username, "QUERY", "SUCCESS", 
                String.format("复杂动态查询: 结果数=%d", postDTOs.size()), 
                executionTime);
            log.debug(logMessage);
            
            return postDTOs;
            
        } catch (Exception e) {
            // 记录失败的性能监控
            monitor.finishWithError(e);
            
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "COMPLEX_DYNAMIC_QUERY_ERROR", e.getMessage(), username, 
                null, "复杂动态查询失败", null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 构建并执行参数化查询
     * 确保所有参数都被正确参数化，防止SQL注入
     * 
     * @param queryTemplate 查询模板
     * @param parameters 查询参数
     * @return 查询结果
     */
    public List<PostDTO> executeParameterizedQuery(String queryTemplate, Map<String, Object> parameters) {
        QueryPerformanceMonitorService.QueryPerformanceMonitor monitor = 
            queryPerformanceMonitorService.startMonitoring("PARAMETERIZED_QUERY", 
                "参数化查询: " + queryTemplate.substring(0, Math.min(50, queryTemplate.length())));
        
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.debug("执行参数化查询: parameterCount={}", parameters != null ? parameters.size() : 0);
            
            // 构建参数化查询
            SafeSqlBuilder.ParameterizedQuery parameterizedQuery = 
                safeQueryService.buildParameterizedQuery(queryTemplate, parameters);
            
            // 执行查询
            List<Post> posts = postRepository.findPostsByParameterizedQuery(
                parameterizedQuery.getQuery(), parameterizedQuery.getParameters());
            
            // 转换为DTO
            List<PostDTO> postDTOs = posts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            // 记录成功的性能监控
            monitor.finish(postDTOs.size());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "PARAMETERIZED_QUERY", username, "QUERY", "SUCCESS", 
                String.format("参数化查询: 参数数=%d, 结果数=%d", 
                             parameters != null ? parameters.size() : 0, postDTOs.size()), 
                executionTime);
            log.debug(logMessage);
            
            return postDTOs;
            
        } catch (Exception e) {
            // 记录失败的性能监控
            monitor.finishWithError(e);
            
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "PARAMETERIZED_QUERY_ERROR", e.getMessage(), username, 
                null, "参数化查询失败", null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 构建复杂的统计查询
     * 支持聚合函数、分组、HAVING等复杂统计场景
     * 
     * @param aggregateFields 聚合字段列表
     * @param groupByFields 分组字段列表
     * @param conditions 查询条件
     * @param havingConditions HAVING条件
     * @return 统计结果
     */
    public List<Map<String, Object>> executeAggregateQuery(List<String> aggregateFields, 
                                                          List<String> groupByFields,
                                                          Map<String, Object> conditions,
                                                          Map<String, Object> havingConditions) {
        QueryPerformanceMonitorService.QueryPerformanceMonitor monitor = 
            queryPerformanceMonitorService.startMonitoring("AGGREGATE_QUERY", 
                "聚合统计查询: " + String.join(",", aggregateFields));
        
        long startTime = System.currentTimeMillis();
        String username = "anonymous";
        
        try {
            log.debug("执行聚合统计查询: aggregateFields={}, groupByFields={}", 
                     aggregateFields, groupByFields);
            
            // 创建动态查询构建器
            SafeSqlBuilder.DynamicQueryBuilder queryBuilder = safeQueryService.createQueryBuilder("posts");
            
            // 添加聚合字段
            if (aggregateFields != null && !aggregateFields.isEmpty()) {
                queryBuilder.aggregate(aggregateFields.toArray(new String[0]));
            }
            
            // 添加分组字段
            if (groupByFields != null && !groupByFields.isEmpty()) {
                queryBuilder.groupBy(groupByFields.toArray(new String[0]));
            }
            
            // 添加WHERE条件
            if (conditions != null && !conditions.isEmpty()) {
                queryBuilder.where(conditions);
            }
            
            // 添加HAVING条件
            if (havingConditions != null && !havingConditions.isEmpty()) {
                for (Map.Entry<String, Object> entry : havingConditions.entrySet()) {
                    queryBuilder.having(entry.getKey(), entry.getValue());
                }
            }
            
            // 验证查询构建器
            boolean isValidBuilder = safeQueryService.validateQueryBuilder(queryBuilder);
            if (!isValidBuilder) {
                throw new IllegalArgumentException("聚合查询构建器包含不安全的内容");
            }
            
            // 构建并执行查询
            String dynamicQuery = safeQueryService.buildComplexDynamicQuery(queryBuilder);
            List<Map<String, Object>> results = postRepository.executeAggregateQuery(dynamicQuery, conditions);
            
            // 记录成功的性能监控
            monitor.finish(results.size());
            
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            String logMessage = LoggingUtils.formatOperationLog(
                "AGGREGATE_QUERY", username, "QUERY", "SUCCESS", 
                String.format("聚合统计查询: 结果数=%d", results.size()), 
                executionTime);
            log.debug(logMessage);
            
            return results;
            
        } catch (Exception e) {
            // 记录失败的性能监控
            monitor.finishWithError(e);
            
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            String errorLog = LoggingUtils.formatErrorLog(
                "AGGREGATE_QUERY_ERROR", e.getMessage(), username, 
                null, "聚合统计查询失败", null);
            log.error(errorLog);
            throw e;
        }
    }
    
    /**
     * 获取查询性能统计信息
     * 
     * @return 性能统计
     */
    public Map<String, Object> getQueryPerformanceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取全局统计
        QueryPerformanceMonitorService.GlobalStatistics globalStats = 
            queryPerformanceMonitorService.getGlobalStatistics();
        stats.put("global", globalStats);
        
        // 获取各查询类型统计
        Map<String, QueryPerformanceMonitorService.QueryStatistics> queryStats = 
            queryPerformanceMonitorService.getAllQueryStatistics();
        stats.put("queryTypes", queryStats);
        
        // 检查性能问题
        QueryPerformanceMonitorService.PerformanceCheckResult checkResult = 
            queryPerformanceMonitorService.checkPerformance();
        stats.put("performanceCheck", checkResult);
        
        return stats;
    }
    
    /**
     * 重置查询性能统计
     */
    public void resetQueryPerformanceStatistics() {
        queryPerformanceMonitorService.resetStatistics();
        log.info("查询性能统计数据已重置");
    }

    @Async
    @CacheEvict(value = "posts", allEntries = true)
    public CompletableFuture<Void> deletePost(Long id, Long userId) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            String username = "unknown";
            
            try {
                Post post = postRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("帖子不存在"));

                username = post.getAuthor().getUsername();

                // 只允许作者本人删除
                if (!post.getAuthor().getId().equals(userId)) {
                    throw new RuntimeException("无权限删除");
                }

                // 删除前验证数据完整性
                if (post.getContentHash() != null) {
                    DataIntegrityService.IntegrityCheckResult integrityResult = 
                        dataIntegrityService.checkPostIntegrity(id, post.getContent(), post.getContentHash());
                    
                    if (!integrityResult.isValid()) {
                        String errorMessage = "帖子删除前完整性验证失败: " + integrityResult.getErrorMessage();
                        log.warn("帖子删除时完整性验证失败: postId={}, error={}", id, integrityResult.getErrorMessage());
                        
                        // 记录完整性验证失败的审计日志
                        auditLogService.logSecurityEvent(
                            AuditOperation.INTEGRITY_CHECK,
                            username,
                            String.format("帖子删除前完整性验证失败: postId=%d, 原因=%s", id, integrityResult.getErrorMessage())
                        );
                        
                        throw new DataIntegrityException(errorMessage);
                    }
                    
                    // 记录完整性验证成功的审计日志
                    auditLogService.logSecurityEvent(
                        AuditOperation.INTEGRITY_CHECK,
                        username,
                        String.format("帖子删除前完整性验证通过: postId=%d", id)
                    );
                }

                // 删除相关的点赞记录
                postLikeService.deleteAllLikesForPost(id);

                // 删除帖子
                postRepository.deleteById(id);

                // 记录删除操作的审计日志
                long executionTime = System.currentTimeMillis() - startTime;
                auditLogService.logSecurityEvent(
                    AuditOperation.POST_DELETE,
                    username,
                    String.format("帖子删除成功: postId=%d, 标题=%s, 执行时间=%dms", id, post.getTitle(), executionTime)
                );

                // 发送审计日志（保持原有逻辑）
                userRepository.findById(userId).ifPresent(user -> messageProducerService.sendAuditLogMessage(
                        userId.toString(),
                        user.getUsername(),
                        "DELETE_POST",
                        "删除帖子: " + post.getTitle()));
                        
                // 记录操作日志
                String logMessage = LoggingUtils.formatOperationLog(
                    "DELETE_POST", username, "POST:" + id, "SUCCESS", 
                    "删除帖子: " + post.getTitle(), executionTime);
                log.info(logMessage);
                
            } catch (DataIntegrityException e) {
                // 数据完整性异常特殊处理
                long executionTime = System.currentTimeMillis() - startTime;
                String errorLog = LoggingUtils.formatErrorLog(
                    "POST_DELETE_INTEGRITY_ERROR", e.getMessage(), username, 
                    null, "删除帖子ID:" + id + " 完整性验证失败", null);
                log.error(errorLog);
                throw new RuntimeException("删除帖子时数据完整性验证失败", e);
            } catch (Exception ex) {
                // 记录错误日志
                long executionTime = System.currentTimeMillis() - startTime;
                String errorLog = LoggingUtils.formatErrorLog(
                    "POST_DELETE_ERROR", ex.getMessage(), username, 
                    null, "删除帖子ID:" + id, null);
                log.error(errorLog);
                throw new RuntimeException("删除帖子过程中发生异常", ex);
            }
        }).exceptionally(ex -> {
            // 记录异常并返回null作为Void类型
            log.error("删除帖子异步操作失败: postId={}, userId={}, error={}", id, userId, ex.getMessage(), ex);
            return null;
        });
    }
}
