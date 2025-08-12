package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.PostService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子完整性监控控制器
 * 
 * 提供帖子数据完整性监控和统计功能，包括：
 * - 单个帖子完整性验证
 * - 批量帖子完整性检查
 * - 用户帖子完整性统计
 * - 系统帖子完整性概览
 * 
 * 符合需求3.6 - 帖子完整性监控
 * 
 * @author MyWeb Security Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/monitoring/post-integrity")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PostIntegrityMonitoringController {

    private final PostService postService;

    /**
     * 验证单个帖子的完整性
     */
    @GetMapping("/verify/{postId}")
    public ResponseEntity<ApiResponse<DataIntegrityService.IntegrityCheckResult>> verifyPostIntegrity(
            @PathVariable Long postId) {
        
        log.info("管理员请求验证帖子完整性: postId={}", postId);
        
        try {
            DataIntegrityService.IntegrityCheckResult result = postService.verifyPostIntegrity(postId);
            
            if (result.isValid()) {
                log.info("帖子完整性验证通过: postId={}", postId);
                return ResponseEntity.ok(ApiResponse.success( "帖子完整性验证通过",result));
            } else {
                log.warn("帖子完整性验证失败: postId={}, error={}", postId, result.getErrorMessage());
                return ResponseEntity.ok(ApiResponse.success( "帖子完整性验证失败",result));
            }
            
        } catch (Exception e) {
            log.error("验证帖子完整性异常: postId={}", postId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("验证帖子完整性失败: " + e.getMessage()));
        }
    }

    /**
     * 批量验证帖子完整性
     */
    @PostMapping("/verify/batch")
    public ResponseEntity<ApiResponse<List<DataIntegrityService.IntegrityCheckResult>>> verifyPostsIntegrity(
          @RequestBody List<Long> postIds) {
        
        log.info("管理员请求批量验证帖子完整性: 帖子数量={}", postIds.size());
        
        try {
            List<DataIntegrityService.IntegrityCheckResult> results = 
                postService.verifyPostsIntegrity(postIds);
            
            long failedCount = results.stream()
                .mapToLong(result -> result.isValid() ? 0 : 1)
                .sum();
            
            String message = String.format("批量验证完成: 总数=%d, 失败数=%d", results.size(), failedCount);
            log.info("批量帖子完整性验证完成: {}", message);
            
            return ResponseEntity.ok(ApiResponse.success( message,results));

        } catch (Exception e) {
            log.error("批量验证帖子完整性异常: postIds={}", postIds, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("批量验证帖子完整性失败: " + e.getMessage()));
        }
    }

    /**
     * 验证用户所有帖子的完整性
     */
    @GetMapping("/verify/user/{userId}")
    public ResponseEntity<ApiResponse<List<DataIntegrityService.IntegrityCheckResult>>> verifyUserPostsIntegrity(
             @PathVariable Long userId) {
        
        log.info("管理员请求验证用户帖子完整性: userId={}", userId);
        
        try {
            List<DataIntegrityService.IntegrityCheckResult> results = 
                postService.verifyUserPostsIntegrity(userId);
            
            long failedCount = results.stream()
                .mapToLong(result -> result.isValid() ? 0 : 1)
                .sum();
            
            String message = String.format("用户帖子验证完成: 总数=%d, 失败数=%d", results.size(), failedCount);
            log.info("用户帖子完整性验证完成: userId={}, {}", userId, message);
            
            return ResponseEntity.ok(ApiResponse.success( message,results));
            
        } catch (Exception e) {
            log.error("验证用户帖子完整性异常: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("验证用户帖子完整性失败: " + e.getMessage()));
        }
    }

    /**
     * 获取帖子完整性统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PostService.PostIntegrityStats>> getPostIntegrityStats() {
        
        log.info("管理员请求获取帖子完整性统计信息");
        
        try {
            PostService.PostIntegrityStats stats = postService.getPostIntegrityStats();
            
            log.info("帖子完整性统计获取成功: {}", stats);
            
            return ResponseEntity.ok(ApiResponse.success("帖子完整性统计获取成功",stats));
            
        } catch (Exception e) {
            log.error("获取帖子完整性统计异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取帖子完整性统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取帖子完整性监控概览
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostIntegrityOverview() {
        
        log.info("管理员请求获取帖子完整性监控概览");
        
        try {
            PostService.PostIntegrityStats stats = postService.getPostIntegrityStats();
            
            Map<String, Object> overview = Map.of(
                "totalPosts", stats.getTotalPosts(),
                "postsWithHash", stats.getPostsWithHash(),
                "validPosts", stats.getValidPosts(),
                "invalidPosts", stats.getInvalidPosts(),
                "integrityRate", stats.getIntegrityRate()
                 );
            
            log.info("帖子完整性监控概览获取成功: 完整性率={}%, 问题数={}, 哈希覆盖率={}%",
                stats.getIntegrityRate() * 100, stats.getInvalidPosts(),
                stats.getTotalPosts() > 0 ? (double) stats.getPostsWithHash() / stats.getTotalPosts() * 100 : 0.0);
            
            return ResponseEntity.ok(ApiResponse.success( "帖子完整性监控概览获取成功",overview));
            
        } catch (Exception e) {
            log.error("获取帖子完整性监控概览异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取帖子完整性监控概览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取有完整性问题的帖子列表
     */
    @GetMapping("/issues")
    public ResponseEntity<ApiResponse<List<DataIntegrityService.IntegrityCheckResult>>> getPostIntegrityIssues() {
        
        log.info("管理员请求获取有完整性问题的帖子列表");
        
        try {
            PostService.PostIntegrityStats stats = postService.getPostIntegrityStats();
            
            if (stats.getInvalidPosts() == 0) {
                log.info("未发现帖子完整性问题");
                return ResponseEntity.ok(ApiResponse.success( "未发现帖子完整性问题",List.of()));
            }
            
            // 获取所有帖子并检查完整性，返回有问题的帖子
            List<DataIntegrityService.IntegrityCheckResult> allResults = 
                postService.verifyPostsIntegrity(
                    postService.getAllPosts().stream()
                        .map(post -> post.getId())
                        .toList()
                );
            
            List<DataIntegrityService.IntegrityCheckResult> issues = allResults.stream()
                .filter(result -> !result.isValid())
                .toList();
            
            String message = String.format("发现%d个帖子存在完整性问题", issues.size());
            log.warn("发现帖子完整性问题: 问题数={}", issues.size());
            
            return ResponseEntity.ok(ApiResponse.success( message,issues));
            
        } catch (Exception e) {
            log.error("获取帖子完整性问题异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取帖子完整性问题失败: " + e.getMessage()));
        }
    }

    /**
     * 获取没有哈希值的帖子列表
     */
    @GetMapping("/no-hash")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPostsWithoutHash() {
        
        log.info("管理员请求获取没有哈希值的帖子列表");
        
        try {
            List<Map<String, Object>> postsWithoutHash = postService.getAllPosts().stream()
                .filter(post -> post.getContentHash() == null)
                .map(post -> {
                    Map<String, Object> postInfo = new java.util.HashMap<>();
                    postInfo.put("id", post.getId());
                    postInfo.put("title", post.getTitle());
                    postInfo.put("authorId", post.getAuthor().getId());
                    postInfo.put("authorName", post.getAuthor().getUsername());
                    postInfo.put("createdAt", post.getCreatedAt() != null ? post.getCreatedAt().toString() : "未知");
                    return postInfo;
                })
                .collect(Collectors.toList());
            
            String message = String.format("发现%d个帖子没有设置完整性哈希值", postsWithoutHash.size());
            log.info("没有哈希值的帖子统计: 数量={}", postsWithoutHash.size());
            
            return ResponseEntity.ok(ApiResponse.success(message,postsWithoutHash));
            
        } catch (Exception e) {
            log.error("获取没有哈希值的帖子异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取没有哈希值的帖子失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发帖子完整性检查
     */
    @PostMapping("/check/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerIntegrityCheck() {
        
        log.info("管理员手动触发帖子完整性检查");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 获取统计信息作为检查结果
            PostService.PostIntegrityStats stats = postService.getPostIntegrityStats();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = Map.of(
                "checkTime", java.time.LocalDateTime.now().toString(),
                "executionTimeMs", executionTime,
                "stats", stats,
                "summary", String.format("检查完成: 总帖子数=%d, 有效帖子数=%d, 无效帖子数=%d, 完整性率=%.2f%%",
                    stats.getTotalPosts(), stats.getValidPosts(), stats.getInvalidPosts(), 
                    stats.getIntegrityRate() * 100)
            );
            
            log.info("手动完整性检查完成: 执行时间={}ms, 完整性率={:.2f}%", 
                executionTime, stats.getIntegrityRate() * 100);
            
            return ResponseEntity.ok(ApiResponse.success( "帖子完整性检查完成",result));
            
        } catch (Exception e) {
            log.error("手动触发帖子完整性检查异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("手动触发帖子完整性检查失败: " + e.getMessage()));
        }
    }

    /**
     * 获取帖子完整性趋势数据
     */
    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIntegrityTrend() {
        
        log.info("管理员请求获取帖子完整性趋势数据");
        
        try {
            // 简化实现，返回当前统计信息
            // 实际应用中可以从历史数据表获取趋势数据
            PostService.PostIntegrityStats currentStats = postService.getPostIntegrityStats();
            
            Map<String, Object> trend = Map.of(
                "currentTime", java.time.LocalDateTime.now().toString(),
                "currentStats", currentStats,
                "trend", "stable", // 简化处理，实际应该计算趋势
                "recommendation", currentStats.getIntegrityRate() < 0.95 ? 
                    "建议立即检查完整性问题并修复" : "完整性状况良好",
                "nextCheckTime", java.time.LocalDateTime.now().plusHours(24).toString() // 建议下次检查时间
            );
            
            log.info("帖子完整性趋势数据获取成功");
            
            return ResponseEntity.ok(ApiResponse.success("帖子完整性趋势数据获取成功",trend ));
            
        } catch (Exception e) {
            log.error("获取帖子完整性趋势数据异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取帖子完整性趋势数据失败: " + e.getMessage()));
        }
    }
}