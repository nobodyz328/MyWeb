package com.myweb.website_core.application.task;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.application.service.security.audit.AuditLogService;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.infrastructure.persistence.repository.PostRepository;
import com.myweb.website_core.infrastructure.persistence.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据完整性检查定时任务
 * <p>
 * 定期执行数据完整性检查，确保重要数据未被篡改
 * 符合GB/T 22239-2019数据完整性保护要求
 * 
 * 功能包括：
 * - 定时检查所有帖子完整性
 * - 实现评论数据的定时完整性检查
 * - 添加完整性问题的告警机制
 * - 创建完整性检查报告
 * - 实现完整性修复建议功能
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Slf4j
@Component
public class DataIntegrityTask {
    
    private final DataIntegrityService dataIntegrityService;
    private final AuditLogService auditLogService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    
    @Value("${app.security.data-integrity.alert-threshold:10}")
    private int alertThreshold;
    
    @Value("${app.security.data-integrity.batch-size:100}")
    private int batchSize;
    
    @Autowired
    public DataIntegrityTask(DataIntegrityService dataIntegrityService,
                           AuditLogService auditLogService,
                           PostRepository postRepository,
                           CommentRepository commentRepository) {
        this.dataIntegrityService = dataIntegrityService;
        this.auditLogService = auditLogService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }
    
    // ==================== 定时任务方法 ====================
    
    /**
     * 每日数据完整性检查
     * 每天凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void performDailyIntegrityCheck() {
        try {
            log.info("开始执行每日数据完整性检查任务");
            
            // 记录任务开始
            auditLogService.logSecurityEvent(
                AuditOperation.SCHEDULED_TASK,
                "SYSTEM",
                "开始执行每日数据完整性检查任务"
            );
            
            // 执行完整性检查
            IntegrityCheckReport report = performCompleteIntegrityCheck().get();
            
            // 记录检查结果
            String resultMessage = String.format(
                "每日完整性检查完成: 检查帖子%d个(问题%d个), 检查评论%d个(问题%d个), 总问题数%d个",
                report.getPostsChecked(), report.getPostIssues(),
                report.getCommentsChecked(), report.getCommentIssues(),
                report.getTotalIssues()
            );
            
            log.info(resultMessage);
            auditLogService.logSecurityEvent(AuditOperation.INTEGRITY_CHECK, "SYSTEM", resultMessage);
            
            // 如果发现问题超过阈值，发送告警
            if (report.getTotalIssues() > alertThreshold) {
                sendIntegrityAlert(report);
            }
            
        } catch (Exception e) {
            log.error("执行每日数据完整性检查任务失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.SCHEDULED_TASK,
                "SYSTEM",
                "每日数据完整性检查任务失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 每周深度完整性检查
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void performWeeklyDeepIntegrityCheck() {
        try {
            log.info("开始执行每周深度数据完整性检查任务");
            
            auditLogService.logSecurityEvent(
                AuditOperation.SCHEDULED_TASK,
                "SYSTEM",
                "开始执行每周深度数据完整性检查任务"
            );
            
            // 执行深度检查（包括历史数据和修复建议）
            IntegrityCheckReport report = performDeepIntegrityCheck().get();
            
            // 生成修复建议
            List<String> repairSuggestions = generateRepairSuggestions(report);
            
            String resultMessage = String.format(
                "每周深度完整性检查完成: 总问题数%d个, 生成修复建议%d条",
                report.getTotalIssues(), repairSuggestions.size()
            );
            
            log.info(resultMessage);
            auditLogService.logSecurityEvent(AuditOperation.INTEGRITY_CHECK, "SYSTEM", resultMessage);
            
            // 记录修复建议
            for (String suggestion : repairSuggestions) {
                auditLogService.logSecurityEvent(AuditOperation.INTEGRITY_CHECK, "SYSTEM", 
                    "修复建议: " + suggestion);
            }
            
            // 发送告警（深度检查的阈值更低）
            if (report.getTotalIssues() > 0) {
                sendIntegrityAlert(report);
            }
            
        } catch (Exception e) {
            log.error("执行每周深度数据完整性检查任务失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.SCHEDULED_TASK,
                "SYSTEM",
                "每周深度数据完整性检查任务失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 每月完整性统计报告
     * 每月1号凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyIntegrityReport() {
        try {
            log.info("开始生成每月数据完整性统计报告");
            
            auditLogService.logSecurityEvent(
                AuditOperation.SCHEDULED_TASK,
                "SYSTEM",
                "开始生成每月数据完整性统计报告"
            );
            
            // 生成统计报告
            IntegrityStatisticsReport statisticsReport = generateIntegrityStatistics().get();
            
            String reportMessage = String.format(
                "每月完整性统计报告: 总检查数据%d条, 发现问题%d个, 完整性率%.2f%%, 趋势: %s",
                statisticsReport.getTotalChecked(),
                statisticsReport.getIssuesFound(),
                statisticsReport.getIntegrityRate(),
                statisticsReport.getTrend()
            );
            
            log.info(reportMessage);
            auditLogService.logSecurityEvent(AuditOperation.INTEGRITY_CHECK, "SYSTEM", reportMessage);
            
        } catch (Exception e) {
            log.error("生成每月数据完整性统计报告失败", e);
            auditLogService.logSecurityEvent(
                AuditOperation.SCHEDULED_TASK,
                "SYSTEM",
                "每月数据完整性统计报告生成失败: " + e.getMessage()
            );
        }
    }
    
    // ==================== 核心检查方法 ====================
    
    /**
     * 执行完整的数据完整性检查
     * 
     * @return 检查报告
     */
    @Async
    public CompletableFuture<IntegrityCheckReport> performCompleteIntegrityCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始执行完整数据完整性检查");
                
                IntegrityCheckReport report = new IntegrityCheckReport();
                report.setCheckTime(LocalDateTime.now());
                
                // 检查所有帖子
                CompletableFuture<Integer> postCheckFuture = checkAllPostsIntegrityActual();
                
                // 检查所有评论
                CompletableFuture<Integer> commentCheckFuture = checkAllCommentsIntegrityActual();
                
                // 等待检查完成
                CompletableFuture.allOf(postCheckFuture, commentCheckFuture).join();
                
                report.setPostIssues(postCheckFuture.join());
                report.setCommentIssues(commentCheckFuture.join());
                report.setPostsChecked(postRepository.findAll().size());
                report.setCommentsChecked(commentRepository.findAll().size());
                report.setTotalIssues(report.getPostIssues() + report.getCommentIssues());
                
                log.info("完整数据完整性检查完成: {}", report);
                return report;
                
            } catch (Exception e) {
                log.error("执行完整数据完整性检查失败", e);
                throw new RuntimeException("完整性检查失败", e);
            }
        });
    }
    
    /**
     * 执行深度完整性检查
     * 包括历史数据和详细分析
     * 
     * @return 检查报告
     */
    @Async
    public CompletableFuture<IntegrityCheckReport> performDeepIntegrityCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始执行深度数据完整性检查");
                
                IntegrityCheckReport report = new IntegrityCheckReport();
                report.setCheckTime(LocalDateTime.now());
                report.setDeepCheck(true);
                
                // 深度检查所有帖子（包括历史数据）
                CompletableFuture<Integer> postCheckFuture = checkAllPostsIntegrityDeep();
                
                // 深度检查所有评论（包括历史数据）
                CompletableFuture<Integer> commentCheckFuture = checkAllCommentsIntegrityDeep();
                
                // 等待检查完成
                CompletableFuture.allOf(postCheckFuture, commentCheckFuture).join();
                
                report.setPostIssues(postCheckFuture.join());
                report.setCommentIssues(commentCheckFuture.join());
                report.setPostsChecked(postRepository.findAll().size());
                report.setCommentsChecked(commentRepository.findAll().size());
                report.setTotalIssues(report.getPostIssues() + report.getCommentIssues());
                
                log.info("深度数据完整性检查完成: {}", report);
                return report;
                
            } catch (Exception e) {
                log.error("执行深度数据完整性检查失败", e);
                throw new RuntimeException("深度完整性检查失败", e);
            }
        });
    }
    
    /**
     * 实际检查所有帖子的完整性
     * 
     * @return 发现问题的帖子数量
     */
    @Async
    public CompletableFuture<Integer> checkAllPostsIntegrityActual() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始检查所有帖子的完整性");
            AtomicInteger issueCount = new AtomicInteger(0);
            
            try {
                List<Post> allPosts = postRepository.findAll();
                log.info("找到{}个帖子需要检查", allPosts.size());
                
                // 分批处理以避免内存问题
                for (int i = 0; i < allPosts.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, allPosts.size());
                    List<Post> batch = allPosts.subList(i, endIndex);
                    
                    for (Post post : batch) {
                        try {
                            DataIntegrityService.IntegrityCheckResult result = 
                                dataIntegrityService.checkPostIntegrity(
                                    post.getId(), post.getContent(), post.getContentHash());
                            
                            if (!result.isValid()) {
                                issueCount.incrementAndGet();
                                log.warn("发现帖子完整性问题: postId={}, error={}", 
                                    post.getId(), result.getErrorMessage());
                                
                                // 记录具体的完整性问题
                                auditLogService.logSecurityEvent(
                                    AuditOperation.INTEGRITY_CHECK,
                                    "SYSTEM",
                                    String.format("帖子完整性问题: ID=%d, 错误=%s", 
                                        post.getId(), result.getErrorMessage())
                                );
                            }
                        } catch (Exception e) {
                            log.error("检查帖子完整性时发生异常: postId={}", post.getId(), e);
                            issueCount.incrementAndGet();
                        }
                    }
                    
                    // 记录批次进度
                    log.debug("已检查帖子批次: {}/{}", endIndex, allPosts.size());
                }
                
                log.info("帖子完整性检查完成，发现{}个问题", issueCount.get());
                return issueCount.get();
                
            } catch (Exception e) {
                log.error("检查所有帖子完整性失败", e);
                throw new RuntimeException("帖子完整性检查失败", e);
            }
        });
    }
    
    /**
     * 实际检查所有评论的完整性
     * 
     * @return 发现问题的评论数量
     */
    @Async
    public CompletableFuture<Integer> checkAllCommentsIntegrityActual() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始检查所有评论的完整性");
            AtomicInteger issueCount = new AtomicInteger(0);
            
            try {
                List<Comment> allComments = commentRepository.findAll();
                log.info("找到{}个评论需要检查", allComments.size());
                
                // 分批处理以避免内存问题
                for (int i = 0; i < allComments.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, allComments.size());
                    List<Comment> batch = allComments.subList(i, endIndex);
                    
                    for (Comment comment : batch) {
                        try {
                            DataIntegrityService.IntegrityCheckResult result = 
                                dataIntegrityService.checkCommentIntegrity(
                                    comment.getId(), comment.getContent(), comment.getContentHash());
                            
                            if (!result.isValid()) {
                                issueCount.incrementAndGet();
                                log.warn("发现评论完整性问题: commentId={}, error={}", 
                                    comment.getId(), result.getErrorMessage());
                                
                                // 记录具体的完整性问题
                                auditLogService.logSecurityEvent(
                                    AuditOperation.INTEGRITY_CHECK,
                                    "SYSTEM",
                                    String.format("评论完整性问题: ID=%d, 错误=%s", 
                                        comment.getId(), result.getErrorMessage())
                                );
                            }
                        } catch (Exception e) {
                            log.error("检查评论完整性时发生异常: commentId={}", comment.getId(), e);
                            issueCount.incrementAndGet();
                        }
                    }
                    
                    // 记录批次进度
                    log.debug("已检查评论批次: {}/{}", endIndex, allComments.size());
                }
                
                log.info("评论完整性检查完成，发现{}个问题", issueCount.get());
                return issueCount.get();
                
            } catch (Exception e) {
                log.error("检查所有评论完整性失败", e);
                throw new RuntimeException("评论完整性检查失败", e);
            }
        });
    }
    
    /**
     * 深度检查所有帖子的完整性
     * 包括检查哈希计算时间、内容变更历史等
     * 
     * @return 发现问题的帖子数量
     */
    @Async
    public CompletableFuture<Integer> checkAllPostsIntegrityDeep() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始深度检查所有帖子的完整性");
            AtomicInteger issueCount = new AtomicInteger(0);
            
            try {
                List<Post> allPosts = postRepository.findAll();
                log.info("找到{}个帖子需要深度检查", allPosts.size());
                
                for (Post post : allPosts) {
                    try {
                        // 基础完整性检查
                        DataIntegrityService.IntegrityCheckResult result = 
                            dataIntegrityService.checkPostIntegrity(
                                post.getId(), post.getContent(), post.getContentHash());
                        
                        if (!result.isValid()) {
                            issueCount.incrementAndGet();
                        }
                        
                        // 深度检查：检查哈希是否需要重新计算
                        if (post.needsHashRecalculation()) {
                            log.warn("帖子需要重新计算哈希: postId={}", post.getId());
                            auditLogService.logSecurityEvent(
                                AuditOperation.INTEGRITY_CHECK,
                                "SYSTEM",
                                String.format("帖子需要重新计算哈希: ID=%d", post.getId())
                            );
                        }
                        
                        // 深度检查：验证内容完整性
                        if (!post.verifyContentIntegrity()) {
                            issueCount.incrementAndGet();
                            log.warn("帖子内容完整性验证失败: postId={}", post.getId());
                        }
                        
                    } catch (Exception e) {
                        log.error("深度检查帖子完整性时发生异常: postId={}", post.getId(), e);
                        issueCount.incrementAndGet();
                    }
                }
                
                log.info("帖子深度完整性检查完成，发现{}个问题", issueCount.get());
                return issueCount.get();
                
            } catch (Exception e) {
                log.error("深度检查所有帖子完整性失败", e);
                throw new RuntimeException("帖子深度完整性检查失败", e);
            }
        });
    }
    
    /**
     * 深度检查所有评论的完整性
     * 包括检查哈希计算时间、内容变更历史等
     * 
     * @return 发现问题的评论数量
     */
    @Async
    public CompletableFuture<Integer> checkAllCommentsIntegrityDeep() {
        return CompletableFuture.supplyAsync(() -> {
            log.info("开始深度检查所有评论的完整性");
            AtomicInteger issueCount = new AtomicInteger(0);
            
            try {
                List<Comment> allComments = commentRepository.findAll();
                log.info("找到{}个评论需要深度检查", allComments.size());
                
                for (Comment comment : allComments) {
                    try {
                        // 基础完整性检查
                        DataIntegrityService.IntegrityCheckResult result = 
                            dataIntegrityService.checkCommentIntegrity(
                                comment.getId(), comment.getContent(), comment.getContentHash());
                        
                        if (!result.isValid()) {
                            issueCount.incrementAndGet();
                        }
                        
                        // 深度检查：检查哈希是否需要重新计算
                        if (comment.needsHashRecalculation()) {
                            log.warn("评论需要重新计算哈希: commentId={}", comment.getId());
                            auditLogService.logSecurityEvent(
                                AuditOperation.INTEGRITY_CHECK,
                                "SYSTEM",
                                String.format("评论需要重新计算哈希: ID=%d", comment.getId())
                            );
                        }
                        
                        // 深度检查：验证内容完整性
                        if (!comment.verifyContentIntegrity()) {
                            issueCount.incrementAndGet();
                            log.warn("评论内容完整性验证失败: commentId={}", comment.getId());
                        }
                        
                    } catch (Exception e) {
                        log.error("深度检查评论完整性时发生异常: commentId={}", comment.getId(), e);
                        issueCount.incrementAndGet();
                    }
                }
                
                log.info("评论深度完整性检查完成，发现{}个问题", issueCount.get());
                return issueCount.get();
                
            } catch (Exception e) {
                log.error("深度检查所有评论完整性失败", e);
                throw new RuntimeException("评论深度完整性检查失败", e);
            }
        });
    }
    
    // ==================== 告警和报告方法 ====================
    
    /**
     * 发送完整性告警
     * 
     * @param report 检查报告
     */
    @Async
    public void sendIntegrityAlert(IntegrityCheckReport report) {
        try {
            String alertMessage = String.format(
                "数据完整性检查发现%d个问题：帖子%d个，评论%d个。检查时间: %s。请立即检查数据安全性。",
                report.getTotalIssues(),
                report.getPostIssues(),
                report.getCommentIssues(),
                report.getCheckTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            log.warn("发送数据完整性告警: {}", alertMessage);
            
            // 记录告警到审计日志
            auditLogService.logSecurityEvent(
                AuditOperation.SECURITY_ALERT,
                "SYSTEM",
                "数据完整性告警: " + alertMessage
            );
            
            // TODO: 实际实现时可以集成邮件或短信告警服务
            // 例如：emailService.sendAlert(alertMessage);
            
        } catch (Exception e) {
            log.error("发送数据完整性告警失败", e);
        }
    }
    
    /**
     * 生成修复建议
     * 
     * @param report 检查报告
     * @return 修复建议列表
     */
    public List<String> generateRepairSuggestions(IntegrityCheckReport report) {
        List<String> suggestions = new java.util.ArrayList<>();
        
        try {
            if (report.getPostIssues() > 0) {
                suggestions.add(String.format(
                    "发现%d个帖子完整性问题，建议：1) 检查数据库是否被非法修改；2) 重新计算受影响帖子的哈希值；3) 检查应用程序是否存在数据篡改漏洞",
                    report.getPostIssues()
                ));
            }
            
            if (report.getCommentIssues() > 0) {
                suggestions.add(String.format(
                    "发现%d个评论完整性问题，建议：1) 检查评论数据的访问权限；2) 验证评论修改流程的安全性；3) 考虑启用评论内容的版本控制",
                    report.getCommentIssues()
                ));
            }
            
            if (report.getTotalIssues() > alertThreshold) {
                suggestions.add("完整性问题数量超过告警阈值，建议：1) 立即进行安全审计；2) 检查系统是否遭受攻击；3) 考虑临时禁用数据修改功能");
            }
            
            if (report.isDeepCheck() && report.getTotalIssues() == 0) {
                suggestions.add("深度检查未发现问题，建议：1) 继续保持当前的安全策略；2) 定期更新完整性检查算法；3) 考虑增加检查频率");
            }
            
            // 通用建议
            suggestions.add("通用建议：1) 定期备份重要数据；2) 启用数据库审计日志；3) 加强访问控制和权限管理；4) 定期更新安全补丁");
            
        } catch (Exception e) {
            log.error("生成修复建议失败", e);
            suggestions.add("生成修复建议时发生错误，请联系系统管理员进行人工分析");
        }
        
        return suggestions;
    }
    
    /**
     * 生成完整性统计信息
     * 
     * @return 统计报告
     */
    @Async
    public CompletableFuture<IntegrityStatisticsReport> generateIntegrityStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始生成完整性统计信息");
                
                IntegrityStatisticsReport report = new IntegrityStatisticsReport();
                report.setReportTime(LocalDateTime.now());
                
                // 统计当前数据量
                int totalPosts = postRepository.findAll().size();
                int totalComments = commentRepository.findAll().size();
                int totalChecked = totalPosts + totalComments;
                
                // 执行一次完整性检查来获取当前问题数量
                IntegrityCheckReport currentCheck = performCompleteIntegrityCheck().get();
                int issuesFound = currentCheck.getTotalIssues();
                
                // 计算完整性率
                double integrityRate = totalChecked > 0 ? 
                    ((double)(totalChecked - issuesFound) / totalChecked) * 100 : 100.0;
                
                report.setTotalChecked(totalChecked);
                report.setIssuesFound(issuesFound);
                report.setIntegrityRate(integrityRate);
                
                // 简单的趋势分析（实际实现中可以基于历史数据）
                String trend = integrityRate >= 99.0 ? "优秀" : 
                              integrityRate >= 95.0 ? "良好" : 
                              integrityRate >= 90.0 ? "一般" : "需要关注";
                report.setTrend(trend);
                
                // 设置详细统计
                report.setPostsChecked(totalPosts);
                report.setCommentsChecked(totalComments);
                report.setPostIssues(currentCheck.getPostIssues());
                report.setCommentIssues(currentCheck.getCommentIssues());
                
                log.info("完整性统计信息生成完成: {}", report);
                return report;
                
            } catch (Exception e) {
                log.error("生成完整性统计信息失败", e);
                throw new RuntimeException("统计信息生成失败", e);
            }
        });
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 手动触发完整性检查
     * 
     * @return 检查报告
     */
    public CompletableFuture<IntegrityCheckReport> triggerManualIntegrityCheck() {
        log.info("手动触发数据完整性检查");
        return performCompleteIntegrityCheck();
    }
    
    /**
     * 手动触发深度完整性检查
     * 
     * @return 检查报告
     */
    public CompletableFuture<IntegrityCheckReport> triggerManualDeepIntegrityCheck() {
        log.info("手动触发深度数据完整性检查");
        return performDeepIntegrityCheck();
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 完整性检查报告
     */
    public static class IntegrityCheckReport {
        private LocalDateTime checkTime;
        private int postsChecked;
        private int commentsChecked;
        private int postIssues;
        private int commentIssues;
        private int totalIssues;
        private boolean deepCheck = false;
        
        // Getters and Setters
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        
        public int getPostsChecked() { return postsChecked; }
        public void setPostsChecked(int postsChecked) { this.postsChecked = postsChecked; }
        
        public int getCommentsChecked() { return commentsChecked; }
        public void setCommentsChecked(int commentsChecked) { this.commentsChecked = commentsChecked; }
        
        public int getPostIssues() { return postIssues; }
        public void setPostIssues(int postIssues) { this.postIssues = postIssues; }
        
        public int getCommentIssues() { return commentIssues; }
        public void setCommentIssues(int commentIssues) { this.commentIssues = commentIssues; }
        
        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }
        
        public boolean isDeepCheck() { return deepCheck; }
        public void setDeepCheck(boolean deepCheck) { this.deepCheck = deepCheck; }
        
        @Override
        public String toString() {
            return String.format("IntegrityCheckReport{checkTime=%s, postsChecked=%d, commentsChecked=%d, " +
                    "postIssues=%d, commentIssues=%d, totalIssues=%d, deepCheck=%s}",
                    checkTime, postsChecked, commentsChecked, postIssues, commentIssues, totalIssues, deepCheck);
        }
    }
    
    /**
     * 完整性统计报告
     */
    public static class IntegrityStatisticsReport {
        private LocalDateTime reportTime;
        private int totalChecked;
        private int issuesFound;
        private double integrityRate;
        private String trend;
        private int postsChecked;
        private int commentsChecked;
        private int postIssues;
        private int commentIssues;
        
        // Getters and Setters
        public LocalDateTime getReportTime() { return reportTime; }
        public void setReportTime(LocalDateTime reportTime) { this.reportTime = reportTime; }
        
        public int getTotalChecked() { return totalChecked; }
        public void setTotalChecked(int totalChecked) { this.totalChecked = totalChecked; }
        
        public int getIssuesFound() { return issuesFound; }
        public void setIssuesFound(int issuesFound) { this.issuesFound = issuesFound; }
        
        public double getIntegrityRate() { return integrityRate; }
        public void setIntegrityRate(double integrityRate) { this.integrityRate = integrityRate; }
        
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
        
        public int getPostsChecked() { return postsChecked; }
        public void setPostsChecked(int postsChecked) { this.postsChecked = postsChecked; }
        
        public int getCommentsChecked() { return commentsChecked; }
        public void setCommentsChecked(int commentsChecked) { this.commentsChecked = commentsChecked; }
        
        public int getPostIssues() { return postIssues; }
        public void setPostIssues(int postIssues) { this.postIssues = postIssues; }
        
        public int getCommentIssues() { return commentIssues; }
        public void setCommentIssues(int commentIssues) { this.commentIssues = commentIssues; }
        
        @Override
        public String toString() {
            return String.format("IntegrityStatisticsReport{reportTime=%s, totalChecked=%d, issuesFound=%d, " +
                    "integrityRate=%.2f%%, trend='%s', postsChecked=%d, commentsChecked=%d, " +
                    "postIssues=%d, commentIssues=%d}",
                    reportTime, totalChecked, issuesFound, integrityRate, trend, 
                    postsChecked, commentsChecked, postIssues, commentIssues);
        }
    }
}