package com.myweb.website_core.application.service.security.IPS.virusprotect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 依赖安全监控服务
 * 负责监控依赖包的安全状态，生成和分发安全报告
 * 
 * @author MyWeb Security Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DependencySecurityService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Value("${app.security.dependency-check.report-path:target/dependency-check-report}")
    private String reportPath;

    @Value("${app.security.dependency-check.notification.enabled:true}")
    private boolean notificationEnabled;

    @Value("${app.security.dependency-check.notification.recipients:}")
    private List<String> notificationRecipients;

    @Value("${app.security.dependency-check.high-severity-threshold:7.0}")
    private double highSeverityThreshold;

    /**
     * 解析依赖检查报告
     */
    public SecurityScanResult parseSecurityReport() {
        try {
            Path reportFile = Paths.get(reportPath, "dependency-check-report.json");
            if (!Files.exists(reportFile)) {
                log.warn("依赖检查报告文件不存在: {}", reportFile);
                return SecurityScanResult.empty();
            }

            JsonNode reportJson = objectMapper.readTree(reportFile.toFile());
            return parseReportJson(reportJson);

        } catch (IOException e) {
            log.error("解析依赖检查报告失败", e);
            return SecurityScanResult.empty();
        }
    }

    /**
     * 解析报告JSON数据
     */
    private SecurityScanResult parseReportJson(JsonNode reportJson) {
        SecurityScanResult.Builder resultBuilder = SecurityScanResult.builder();

        // 解析项目信息
        JsonNode projectInfo = reportJson.get("projectInfo");
        if (projectInfo != null) {
            resultBuilder.projectName(projectInfo.get("name").asText())
                        .scanTime(LocalDateTime.now());
        }

        // 解析依赖信息
        JsonNode dependencies = reportJson.get("dependencies");
        if (dependencies != null && dependencies.isArray()) {
            int totalDependencies = dependencies.size();
            int vulnerableDependencies = 0;
            List<VulnerabilityInfo> vulnerabilities = new ArrayList<>();

            for (JsonNode dependency : dependencies) {
                JsonNode vulns = dependency.get("vulnerabilities");
                if (vulns != null && vulns.isArray() && vulns.size() > 0) {
                    vulnerableDependencies++;
                    
                    for (JsonNode vuln : vulns) {
                        VulnerabilityInfo vulnInfo = VulnerabilityInfo.builder()
                            .cve(vuln.get("name").asText())
                            .severity(vuln.get("severity").asText())
                            .score(vuln.get("cvssv3") != null ? 
                                   vuln.get("cvssv3").get("baseScore").asDouble() : 0.0)
                            .description(vuln.get("description").asText())
                            .dependencyName(dependency.get("fileName").asText())
                            .build();
                        vulnerabilities.add(vulnInfo);
                    }
                }
            }

            resultBuilder.totalDependencies(totalDependencies)
                        .vulnerableDependencies(vulnerableDependencies)
                        .vulnerabilities(vulnerabilities);
        }

        return resultBuilder.build();
    }

    /**
     * 生成安全报告摘要
     */
    public String generateSecuritySummary(SecurityScanResult result) {
        StringBuilder summary = new StringBuilder();
        summary.append("=== MyWeb 依赖安全扫描报告 ===\n");
        summary.append("扫描时间: ").append(result.getScanTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        summary.append("项目名称: ").append(result.getProjectName()).append("\n\n");

        summary.append("📊 扫描统计:\n");
        summary.append("- 总依赖数量: ").append(result.getTotalDependencies()).append("\n");
        summary.append("- 存在漏洞的依赖: ").append(result.getVulnerableDependencies()).append("\n");

        // 按严重程度统计
        Map<String, Long> severityCount = result.getVulnerabilities().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                VulnerabilityInfo::getSeverity,
                java.util.stream.Collectors.counting()
            ));

        summary.append("- 高危漏洞: ").append(severityCount.getOrDefault("HIGH", 0L)).append("\n");
        summary.append("- 中危漏洞: ").append(severityCount.getOrDefault("MEDIUM", 0L)).append("\n");
        summary.append("- 低危漏洞: ").append(severityCount.getOrDefault("LOW", 0L)).append("\n\n");

        // 高危漏洞详情
        List<VulnerabilityInfo> highSeverityVulns = result.getVulnerabilities().stream()
            .filter(v -> "HIGH".equals(v.getSeverity()) || v.getScore() >= highSeverityThreshold)
            .toList();

        if (!highSeverityVulns.isEmpty()) {
            summary.append("⚠️ 高危漏洞详情:\n");
            for (VulnerabilityInfo vuln : highSeverityVulns) {
                summary.append("- ").append(vuln.getCve())
                       .append(" (").append(vuln.getSeverity()).append("/").append(vuln.getScore()).append(")")
                       .append(" - ").append(vuln.getDependencyName()).append("\n");
                summary.append("  描述: ").append(vuln.getDescription()).append("\n\n");
            }
        } else {
            summary.append("✅ 未发现高危漏洞\n");
        }

        return summary.toString();
    }

    /**
     * 发送安全报告通知
     */
    @Async
    public CompletableFuture<Void> sendSecurityNotification(SecurityScanResult result) {
        if (!notificationEnabled || notificationRecipients.isEmpty()) {
            log.info("安全通知已禁用或未配置收件人");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String summary = generateSecuritySummary(result);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notificationRecipients.toArray(new String[0]));
            message.setSubject("MyWeb 依赖安全扫描报告 - " + 
                             result.getScanTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            message.setText(summary);

            mailSender.send(message);
            log.info("安全报告通知已发送给: {}", notificationRecipients);

        } catch (Exception e) {
            log.error("发送安全报告通知失败", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 检查是否存在高危漏洞
     */
    public boolean hasHighSeverityVulnerabilities(SecurityScanResult result) {
        return result.getVulnerabilities().stream()
            .anyMatch(v -> "HIGH".equals(v.getSeverity()) || v.getScore() >= highSeverityThreshold);
    }

    /**
     * 定期执行安全扫描检查
     */
    @Scheduled(cron = "0 0 2 * * MON") // 每周一凌晨2点
    public void scheduledSecurityCheck() {
        log.info("开始定期安全扫描检查");
        
        SecurityScanResult result = parseSecurityReport();
        if (result.getTotalDependencies() > 0) {
            sendSecurityNotification(result);
            
            if (hasHighSeverityVulnerabilities(result)) {
                log.warn("发现高危漏洞，需要立即处理！");
                // 可以在这里触发更严格的告警机制
            }
        }
    }

    /**
     * 安全扫描结果数据类
     */
    public static class SecurityScanResult {
        private String projectName;
        private LocalDateTime scanTime;
        private int totalDependencies;
        private int vulnerableDependencies;
        private List<VulnerabilityInfo> vulnerabilities;

        public static SecurityScanResult empty() {
            return builder()
                .projectName("Unknown")
                .scanTime(LocalDateTime.now())
                .totalDependencies(0)
                .vulnerableDependencies(0)
                .vulnerabilities(Collections.emptyList())
                .build();
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getProjectName() { return projectName; }
        public LocalDateTime getScanTime() { return scanTime; }
        public int getTotalDependencies() { return totalDependencies; }
        public int getVulnerableDependencies() { return vulnerableDependencies; }
        public List<VulnerabilityInfo> getVulnerabilities() { return vulnerabilities; }

        public static class Builder {
            private SecurityScanResult result = new SecurityScanResult();

            public Builder projectName(String projectName) {
                result.projectName = projectName;
                return this;
            }

            public Builder scanTime(LocalDateTime scanTime) {
                result.scanTime = scanTime;
                return this;
            }

            public Builder totalDependencies(int totalDependencies) {
                result.totalDependencies = totalDependencies;
                return this;
            }

            public Builder vulnerableDependencies(int vulnerableDependencies) {
                result.vulnerableDependencies = vulnerableDependencies;
                return this;
            }

            public Builder vulnerabilities(List<VulnerabilityInfo> vulnerabilities) {
                result.vulnerabilities = vulnerabilities;
                return this;
            }

            public SecurityScanResult build() {
                return result;
            }
        }
    }

    /**
     * 漏洞信息数据类
     */
    public static class VulnerabilityInfo {
        private String cve;
        private String severity;
        private double score;
        private String description;
        private String dependencyName;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getCve() { return cve; }
        public String getSeverity() { return severity; }
        public double getScore() { return score; }
        public String getDescription() { return description; }
        public String getDependencyName() { return dependencyName; }

        public static class Builder {
            private VulnerabilityInfo info = new VulnerabilityInfo();

            public Builder cve(String cve) {
                info.cve = cve;
                return this;
            }

            public Builder severity(String severity) {
                info.severity = severity;
                return this;
            }

            public Builder score(double score) {
                info.score = score;
                return this;
            }

            public Builder description(String description) {
                info.description = description;
                return this;
            }

            public Builder dependencyName(String dependencyName) {
                info.dependencyName = dependencyName;
                return this;
            }

            public VulnerabilityInfo build() {
                return info;
            }
        }
    }
}