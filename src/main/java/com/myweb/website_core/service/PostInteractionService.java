package com.myweb.website_core.service;

import com.myweb.website_core.demos.web.interaction.PostBookmark;
import com.myweb.website_core.dto.InteractionResponse;
import com.myweb.website_core.dto.PostInteractionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.concurrent.CompletableFuture;

/**
 * 帖子交互服务接口
 * 提供异步的点赞、书签和交互状态查询功能
 */
public interface PostInteractionService {

    /**
     * 切换点赞状态（点赞/取消点赞）
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 异步操作结果
     */
    CompletableFuture<InteractionResponse> toggleLike(Long postId, Long userId);

    /**
     * 切换书签状态（添加书签/取消书签）
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 异步操作结果
     */
    CompletableFuture<InteractionResponse> toggleBookmark(Long postId, Long userId);

    /**
     * 获取帖子的交互状态
     * @param postId 帖子ID
     * @param userId 用户ID（可为null，表示未登录用户）
     * @return 异步的交互状态信息
     */
    CompletableFuture<PostInteractionStatus> getInteractionStatus(Long postId, Long userId);

    /**
     * 获取用户的书签列表（分页）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 异步的书签列表
     */
    CompletableFuture<Page<PostBookmark>> getUserBookmarks(Long userId, Pageable pageable);
}