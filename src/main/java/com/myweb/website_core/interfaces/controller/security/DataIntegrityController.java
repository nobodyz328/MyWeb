package com.myweb.website_core.interfaces.controller.security;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.infrastructure.security.audit.Auditable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 数据完整性管理控制器
 * 
 * 提供数据完整性检查和管理的REST API接口
 * 符合GB/T 22239-2019数据完整性保护要求
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/data-integrity")
@PreAuthorize("hasRole('ADMIN')")
public class DataIntegrityController {
    
    private final DataIntegrityService dataIntegrityService;
    
    @Autowired
    public DataIntegrityController(DataIntegrityService dataIntegrityService) {
        this.dataIntegrityService = dataIntegrityService;
    }
    
    /**
     * 手动触发完整性检查
     * 
     * @return 检查结果
     */
    @PostMapping("/check")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "DATA_INTEGRITY")
    public ResponseEntity<String> triggerIntegrityCheck() {
        try {
            log.info("管理员手动触发数据完整性检查");
            
            // 异步执行完整性检查
            CompletableFuture<Void> checkFuture = dataIntegrityService.triggerManualIntegrityCheck();
            
            return ResponseEntity.ok("数据完整性检查已启动，请查看日志获取详细结果");
            
        } catch (Exception e) {
            log.error("触发数据完整性检查失败", e);
            return ResponseEntity.internalServerError()
                    .body("触发完整性检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查特定帖子的完整性
     * 
     * @param postId 帖子ID
     * @return 检查结果
     */
    @GetMapping("/check/post/{postId}")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "POST")
    public ResponseEntity<Map<String, Object>> checkPostIntegrity(@PathVariable Long postId) {
        try {
            log.info("检查帖子完整性: postId={}", postId);
            
            // TODO: 实际实现时需要从数据库查询帖子信息
            // 这里先返回模拟结果
            Map<String, Object> result = new HashMap<>();
            result.put("postId", postId);
            result.put("isValid", true);
            result.put("message", "帖子内容完整性验证通过");
            result.put("checkTime", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("检查帖子完整性失败: postId={}", postId, e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("postId", postId);
            errorResult.put("isValid", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("checkTime", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 检查特定评论的完整性
     * 
     * @param commentId 评论ID
     * @return 检查结果
     */
    @GetMapping("/check/comment/{commentId}")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "COMMENT")
    public ResponseEntity<Map<String, Object>> checkCommentIntegrity(@PathVariable Long commentId) {
        try {
            log.info("检查评论完整性: commentId={}", commentId);
            
            // TODO: 实际实现时需要从数据库查询评论信息
            // 这里先返回模拟结果
            Map<String, Object> result = new HashMap<>();
            result.put("commentId", commentId);
            result.put("isValid", true);
            result.put("message", "评论内容完整性验证通过");
            result.put("checkTime", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("检查评论完整性失败: commentId={}", commentId, e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("commentId", commentId);
            errorResult.put("isValid", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("checkTime", java.time.LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 计算数据哈希值
     * 
     * @param request 包含要计算哈希的数据
     * @return 哈希值
     */
    @PostMapping("/hash/calculate")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "HASH_CALCULATION")
    public ResponseEntity<Map<String, Object>> calculateHash(@RequestBody Map<String, String> request) {
        try {
            String data = request.get("data");
            String algorithm = request.getOrDefault("algorithm", "SHA-256");
            
            if (data == null || data.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "数据不能为空"));
            }
            
            log.info("计算数据哈希值: algorithm={}, dataLength={}", algorithm, data.length());
            
            String hash = dataIntegrityService.calculateHash(data, algorithm);
            
            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("algorithm", algorithm);
            result.put("hash", hash);
            result.put("calculatedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("计算数据哈希值失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "计算哈希值失败: " + e.getMessage()));
        }
    }
    
    /**
     * 验证数据完整性
     * 
     * @param request 包含数据和期望哈希值的请求
     * @return 验证结果
     */
    @PostMapping("/verify")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "INTEGRITY_VERIFICATION")
    public ResponseEntity<Map<String, Object>> verifyIntegrity(@RequestBody Map<String, String> request) {
        try {
            String data = request.get("data");
            String expectedHash = request.get("expectedHash");
            
            if (data == null || expectedHash == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "数据和期望哈希值不能为空"));
            }
            
            log.info("验证数据完整性: dataLength={}, expectedHash={}", data.length(), expectedHash);
            
            boolean isValid = dataIntegrityService.verifyIntegrity(data, expectedHash);
            String actualHash = dataIntegrityService.calculateHash(data);
            
            Map<String, Object> result = new HashMap<>();
            result.put("isValid", isValid);
            result.put("actualHash", actualHash);
            result.put("expectedHash", expectedHash);
            result.put("verifiedAt", java.time.LocalDateTime.now());
            
            if (!isValid) {
                result.put("message", "数据完整性验证失败，哈希值不匹配");
            } else {
                result.put("message", "数据完整性验证通过");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("验证数据完整性失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "验证完整性失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取支持的哈希算法列表
     * 
     * @return 支持的算法列表
     */
    @GetMapping("/algorithms")
    public ResponseEntity<Map<String, Object>> getSupportedAlgorithms() {
        try {
            String[] algorithms = dataIntegrityService.getSupportedHashAlgorithms();
            
            Map<String, Object> result = new HashMap<>();
            result.put("algorithms", algorithms);
            result.put("default", "SHA-256");
            result.put("recommended", "SHA-256");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取支持的哈希算法失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取算法列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取数据完整性统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @Auditable(operation = AuditOperation.INTEGRITY_CHECK, resourceType = "STATISTICS")
    public ResponseEntity<Map<String, Object>> getIntegrityStatistics() {
        try {
            log.info("获取数据完整性统计信息");
            
            // TODO: 实际实现时需要从数据库查询统计信息
            // 这里先返回模拟数据
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPosts", 1000);
            statistics.put("totalComments", 5000);
            statistics.put("postsWithHash", 950);
            statistics.put("commentsWithHash", 4800);
            statistics.put("integrityIssues", 3);
            statistics.put("lastCheckTime", java.time.LocalDateTime.now().minusHours(2));
            statistics.put("nextCheckTime", java.time.LocalDateTime.now().plusDays(1).withHour(4).withMinute(0));
            
            // 计算完整性率
            double postIntegrityRate = (double) (950 - 1) / 950 * 100;
            double commentIntegrityRate = (double) (4800 - 2) / 4800 * 100;
            double overallIntegrityRate = (postIntegrityRate + commentIntegrityRate) / 2;
            
            statistics.put("postIntegrityRate", String.format("%.2f%%", postIntegrityRate));
            statistics.put("commentIntegrityRate", String.format("%.2f%%", commentIntegrityRate));
            statistics.put("overallIntegrityRate", String.format("%.2f%%", overallIntegrityRate));
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("获取数据完整性统计信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取统计信息失败: " + e.getMessage()));
        }
    }
}