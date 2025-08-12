package com.myweb.website_core.application.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 查询性能监控服务
 * <p>
 * 提供动态查询的性能监控功能：
 * 1. 查询执行时间统计
 * 2. 慢查询检测和告警
 * 3. 查询性能趋势分析
 * 4. 查询缓存命中率统计
 * <p>
 * 符合需求：5.7 - 查询性能监控
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryPerformanceMonitorService {
    
    // 慢查询阈值（毫秒）
    private static final long SLOW_QUERY_THRESHOLD = 1000L;
    
    // 性能统计数据
    private final Map<String, QueryStatistics> queryStats = new ConcurrentHashMap<>();
    
    // 全局统计
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong totalSlowQueries = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    
    /**
     * 开始监控查询执行
     * 
     * @param queryType 查询类型
     * @param queryDescription 查询描述
     * @return 性能监控器
     */
    public QueryPerformanceMonitor startMonitoring(String queryType, String queryDescription) {
        return new QueryPerformanceMonitor(queryType, queryDescription);
    }
    
    /**
     * 记录查询执行结果
     * 
     * @param queryType 查询类型
     * @param executionTime 执行时间（毫秒）
     * @param resultCount 结果数量
     * @param success 是否成功
     */
    public void recordQueryExecution(String queryType, long executionTime, int resultCount, boolean success) {
        // 更新全局统计
        totalQueries.incrementAndGet();
        totalExecutionTime.addAndGet(executionTime);
        
        // 检查是否为慢查询
        if (executionTime > SLOW_QUERY_THRESHOLD) {
            totalSlowQueries.incrementAndGet();
            log.warn("检测到慢查询: type={}, executionTime={}ms, resultCount={}", 
                    queryType, executionTime, resultCount);
        }
        
        // 更新查询类型统计
        queryStats.computeIfAbsent(queryType, k -> new QueryStatistics(k))
                 .recordExecution(executionTime, resultCount, success);
        
        // 记录性能日志
        if (log.isDebugEnabled()) {
            log.debug("查询执行完成: type={}, executionTime={}ms, resultCount={}, success={}", 
                     queryType, executionTime, resultCount, success);
        }
    }
    
    /**
     * 获取查询性能统计
     * 
     * @param queryType 查询类型
     * @return 性能统计信息
     */
    public QueryStatistics getQueryStatistics(String queryType) {
        return queryStats.get(queryType);
    }
    
    /**
     * 获取全局性能统计
     * 
     * @return 全局统计信息
     */
    public GlobalStatistics getGlobalStatistics() {
        long totalCount = totalQueries.get();
        long totalTime = totalExecutionTime.get();
        long slowCount = totalSlowQueries.get();
        
        double avgExecutionTime = totalCount > 0 ? (double) totalTime / totalCount : 0.0;
        double slowQueryRate = totalCount > 0 ? (double) slowCount / totalCount * 100 : 0.0;
        
        return new GlobalStatistics(totalCount, slowCount, totalTime, avgExecutionTime, slowQueryRate);
    }
    
    /**
     * 获取所有查询类型的统计信息
     * 
     * @return 所有查询统计
     */
    public Map<String, QueryStatistics> getAllQueryStatistics() {
        return new ConcurrentHashMap<>(queryStats);
    }
    
    /**
     * 重置统计数据
     */
    public void resetStatistics() {
        queryStats.clear();
        totalQueries.set(0);
        totalSlowQueries.set(0);
        totalExecutionTime.set(0);
        log.info("查询性能统计数据已重置");
    }
    
    /**
     * 检查是否存在性能问题
     * 
     * @return 性能检查结果
     */
    public PerformanceCheckResult checkPerformance() {
        GlobalStatistics global = getGlobalStatistics();
        
        boolean hasPerformanceIssue = false;
        StringBuilder issues = new StringBuilder();
        
        // 检查慢查询率
        if (global.getSlowQueryRate() > 10.0) {
            hasPerformanceIssue = true;
            issues.append("慢查询率过高: ").append(String.format("%.2f%%", global.getSlowQueryRate())).append("; ");
        }
        
        // 检查平均执行时间
        if (global.getAvgExecutionTime() > 500.0) {
            hasPerformanceIssue = true;
            issues.append("平均执行时间过长: ").append(String.format("%.2fms", global.getAvgExecutionTime())).append("; ");
        }
        
        // 检查各查询类型的性能
        for (Map.Entry<String, QueryStatistics> entry : queryStats.entrySet()) {
            QueryStatistics stats = entry.getValue();
            if (stats.getAvgExecutionTime() > 1000.0) {
                hasPerformanceIssue = true;
                issues.append("查询类型 ").append(entry.getKey()).append(" 平均执行时间过长: ")
                       .append(String.format("%.2fms", stats.getAvgExecutionTime())).append("; ");
            }
        }
        
        return new PerformanceCheckResult(hasPerformanceIssue, issues.toString());
    }
    
    /**
     * 查询性能监控器
     */
    public class QueryPerformanceMonitor {
        private final String queryType;
        private final String queryDescription;
        private final StopWatch stopWatch;
        private final LocalDateTime startTime;
        
        public QueryPerformanceMonitor(String queryType, String queryDescription) {
            this.queryType = queryType;
            this.queryDescription = queryDescription;
            this.stopWatch = new StopWatch("Query-" + queryType);
            this.startTime = LocalDateTime.now();
            this.stopWatch.start();
        }
        
        /**
         * 结束监控并记录结果
         * 
         * @param resultCount 结果数量
         * @param success 是否成功
         */
        public void finish(int resultCount, boolean success) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            
            recordQueryExecution(queryType, executionTime, resultCount, success);
            
            if (log.isDebugEnabled()) {
                log.debug("查询监控完成: type={}, description={}, executionTime={}ms, resultCount={}, success={}", 
                         queryType, queryDescription, executionTime, resultCount, success);
            }
        }
        
        /**
         * 结束监控（成功情况）
         * 
         * @param resultCount 结果数量
         */
        public void finish(int resultCount) {
            finish(resultCount, true);
        }
        
        /**
         * 结束监控（异常情况）
         * 
         * @param exception 异常信息
         */
        public void finishWithError(Exception exception) {
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            
            recordQueryExecution(queryType, executionTime, 0, false);
            
            log.error("查询执行失败: type={}, description={}, executionTime={}ms, error={}", 
                     queryType, queryDescription, executionTime, exception.getMessage());
        }
        
        public String getQueryType() { return queryType; }
        public String getQueryDescription() { return queryDescription; }
        public LocalDateTime getStartTime() { return startTime; }
    }
    
    /**
     * 查询统计信息
     */
    public static class QueryStatistics {
        private final String queryType;
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong totalResultCount = new AtomicLong(0);
        private final AtomicReference<Long> minExecutionTime = new AtomicReference<>(Long.MAX_VALUE);
        private final AtomicReference<Long> maxExecutionTime = new AtomicReference<>(0L);
        private final LocalDateTime createdAt;
        
        public QueryStatistics(String queryType) {
            this.queryType = queryType;
            this.createdAt = LocalDateTime.now();
        }
        
        public void recordExecution(long executionTime, int resultCount, boolean success) {
            totalExecutions.incrementAndGet();
            totalExecutionTime.addAndGet(executionTime);
            totalResultCount.addAndGet(resultCount);
            
            if (success) {
                successfulExecutions.incrementAndGet();
            }
            
            // 更新最小执行时间
            minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
            
            // 更新最大执行时间
            maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
        }
        
        public String getQueryType() { return queryType; }
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getTotalExecutionTime() { return totalExecutionTime.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getTotalResultCount() { return totalResultCount.get(); }
        public long getMinExecutionTime() { 
            long min = minExecutionTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        public double getAvgExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }
        
        public double getSuccessRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) successfulExecutions.get() / total * 100 : 0.0;
        }
        
        public double getAvgResultCount() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalResultCount.get() / total : 0.0;
        }
    }
    
    /**
     * 全局统计信息
     */
    public static class GlobalStatistics {
        private final long totalQueries;
        private final long totalSlowQueries;
        private final long totalExecutionTime;
        private final double avgExecutionTime;
        private final double slowQueryRate;
        
        public GlobalStatistics(long totalQueries, long totalSlowQueries, long totalExecutionTime, 
                              double avgExecutionTime, double slowQueryRate) {
            this.totalQueries = totalQueries;
            this.totalSlowQueries = totalSlowQueries;
            this.totalExecutionTime = totalExecutionTime;
            this.avgExecutionTime = avgExecutionTime;
            this.slowQueryRate = slowQueryRate;
        }
        
        public long getTotalQueries() { return totalQueries; }
        public long getTotalSlowQueries() { return totalSlowQueries; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public double getAvgExecutionTime() { return avgExecutionTime; }
        public double getSlowQueryRate() { return slowQueryRate; }
    }
    
    /**
     * 性能检查结果
     */
    public static class PerformanceCheckResult {
        private final boolean hasIssue;
        private final String issueDescription;
        
        public PerformanceCheckResult(boolean hasIssue, String issueDescription) {
            this.hasIssue = hasIssue;
            this.issueDescription = issueDescription;
        }
        
        public boolean hasIssue() { return hasIssue; }
        public String getIssueDescription() { return issueDescription; }
    }
}