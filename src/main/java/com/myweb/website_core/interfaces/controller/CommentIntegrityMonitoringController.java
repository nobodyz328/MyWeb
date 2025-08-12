package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.CommentService;
import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.domain.business.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评论完整性监控控制器
 * 
 * 提供评论数据完整性监控和统计功能，包括：
 * - 单个评论完整性验证
 * - 批量评论完整性检查
 * - 帖子评论完整性统计
 * - 系统评论完整性概览
 * 
 * 符合需求3.6 - 评论完整性监控
 * 
 * @author MyWeb Security Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/monitoring/comment-integrity")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CommentIntegrityMonitoringController {

    private final CommentService commentService;

    /**
     * 验证单个评论的完整性
     */
    @GetMapping("/verify/{commentId}")
    public ResponseEntity<ApiResponse<DataIntegrityService.IntegrityCheckResult>> verifyCommentIntegrity(
             @PathVariable Long commentId) {
        
        log.info("管理员请求验证评论完整性: commentId={}", commentId);
        
        try {
            DataIntegrityService.IntegrityCheckResult result = commentService.verifyCommentIntegrity(commentId);
            
            if (result.isValid()) {
                log.info("评论完整性验证通过: commentId={}", commentId);
                return ResponseEntity.ok(ApiResponse.success("评论完整性验证通过",result ));
            } else {
                log.warn("评论完整性验证失败: commentId={}, error={}", commentId, result.getErrorMessage());
                return ResponseEntity.ok(ApiResponse.success("评论完整性验证失败",result ));
            }
            
        } catch (Exception e) {
            log.error("验证评论完整性异常: commentId={}", commentId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("验证评论完整性失败: " + e.getMessage()));
        }
    }

    /**
     * 批量验证评论完整性
     */
    @PostMapping("/verify/batch")
    public ResponseEntity<ApiResponse<List<DataIntegrityService.IntegrityCheckResult>>> verifyCommentsIntegrity(
            @RequestBody List<Long> commentIds) {
        
        log.info("管理员请求批量验证评论完整性: 评论数量={}", commentIds.size());
        
        try {
            List<DataIntegrityService.IntegrityCheckResult> results = 
                commentService.verifyCommentsIntegrity(commentIds);
            
            long failedCount = results.stream()
                .mapToLong(result -> result.isValid() ? 0 : 1)
                .sum();
            
            String message = String.format("批量验证完成: 总数=%d, 失败数=%d", results.size(), failedCount);
            log.info("批量评论完整性验证完成: {}", message);
            
            return ResponseEntity.ok(ApiResponse.success(message,results ));
            
        } catch (Exception e) {
            log.error("批量验证评论完整性异常: commentIds={}", commentIds, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("批量验证评论完整性失败: " + e.getMessage()));
        }
    }

    /**
     * 验证帖子下所有评论的完整性
     */
    @GetMapping("/verify/post/{postId}")
    public ResponseEntity<ApiResponse<List<DataIntegrityService.IntegrityCheckResult>>> verifyPostCommentsIntegrity(
            @PathVariable Long postId) {
        
        log.info("管理员请求验证帖子评论完整性: postId={}", postId);
        
        try {
            List<DataIntegrityService.IntegrityCheckResult> results = 
                commentService.verifyPostCommentsIntegrity(postId);
            
            long failedCount = results.stream()
                .mapToLong(result -> result.isValid() ? 0 : 1)
                .sum();
            
            String message = String.format("帖子评论验证完成: 总数=%d, 失败数=%d", results.size(), failedCount);
            log.info("帖子评论完整性验证完成: postId={}, {}", postId, message);
            
            return ResponseEntity.ok(ApiResponse.success( message,results ));
            
        } catch (Exception e) {
            log.error("验证帖子评论完整性异常: postId={}", postId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("验证帖子评论完整性失败: " + e.getMessage()));
        }
    }

    /**
     * 获取评论完整性统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CommentService.CommentIntegrityStats>> getCommentIntegrityStats() {
        
        log.info("管理员请求获取评论完整性统计信息");
        
        try {
            CommentService.CommentIntegrityStats stats = commentService.getCommentIntegrityStats();
            
            log.info("评论完整性统计获取成功: {}", stats);
            
            return ResponseEntity.ok(ApiResponse.success("评论完整性统计获取成功", stats));
            
        } catch (Exception e) {
            log.error("获取评论完整性统计异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取评论完整性统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取评论完整性监控概览
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommentIntegrityOverview() {
        
        log.info("管理员请求获取评论完整性监控概览");
        
        try {
            CommentService.CommentIntegrityStats stats = commentService.getCommentIntegrityStats();
            
            Map<String, Object> overview = Map.of(
                "totalComments", stats.getTotalComments(),
                "commentsWithHash", stats.getCommentsWithHash(),
                "validComments", stats.getValidComments(),
                "invalidComments", stats.getInvalidComments(),
                "integrityRate", stats.getIntegrityRate(),
                "integrityRatePercentage", String.format("%.2f%%", stats.getIntegrityRate() * 100),
                "hasIntegrityIssues", stats.getInvalidComments() > 0,
                "needsAttention", stats.getIntegrityRate() < 0.95 // 完整性率低于95%需要关注
            );
            
            log.info("评论完整性监控概览获取成功: 完整性率={:.2f}%, 问题数={}", 
                stats.getIntegrityRate() * 100, stats.getInvalidComments());
            
            return ResponseEntity.ok(ApiResponse.success("评论完整性监控概览获取成功", overview));
            
        } catch (Exception e) {
            log.error("获取评论完整性监控概览异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取评论完整性监控概览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取有完整性问题的评论列表
     */
    @GetMapping("/issues")
    public ResponseEntity<ApiResponse<List<DataIntegrityService.IntegrityCheckResult>>> getCommentIntegrityIssues() {
        
        log.info("管理员请求获取有完整性问题的评论列表");
        
        try {
            CommentService.CommentIntegrityStats stats = commentService.getCommentIntegrityStats();
            
            if (stats.getInvalidComments() == 0) {
                log.info("未发现评论完整性问题");
                return ResponseEntity.ok(ApiResponse.success("未发现评论完整性问题", List.of()));
            }
            
            // 这里简化处理，实际应该返回具体的问题评论
            // 由于当前架构限制，返回统计信息提示
            String message = String.format("发现%d个评论存在完整性问题，请使用批量验证功能获取详细信息", 
                stats.getInvalidComments());
            
            log.warn("发现评论完整性问题: 问题数={}", stats.getInvalidComments());
            
            return ResponseEntity.ok(ApiResponse.success(message, List.of()));
            
        } catch (Exception e) {
            log.error("获取评论完整性问题异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取评论完整性问题失败: " + e.getMessage()));
        }
    }
}