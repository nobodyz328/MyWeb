package com.myweb.website_core.application.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.website_core.application.service.integration.MessageProducerService;
import com.myweb.website_core.application.service.security.authentication.AuthenticationService;
import com.myweb.website_core.common.enums.AuditOperation;
import com.myweb.website_core.domain.business.dto.UserDataExportDTO;
import com.myweb.website_core.domain.business.dto.UserDataUpdateDTO;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.security.dto.UnifiedSecurityMessage;
import com.myweb.website_core.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 用户数据管理服务
 * <p>
 * 提供用户个人数据的查看、导出、修改和删除功能
 * 符合GB/T 22239-2019二级等保要求的个人信息保护机制
 * <p>
 * 功能包括：
 * - 用户数据查看（带权限控制）
 * - 用户数据导出（JSON/CSV格式）
 * - 用户数据修改（带验证和审计）
 * - 用户数据删除（物理删除）
 * - 数据脱敏显示
 * - 操作审计记录
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataManagementService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final MessageProducerService messageProducerService;
    private final ObjectMapper objectMapper;

    // ==================== 数据查看功能 ====================

    /**
     * 查看用户个人数据
     * 只有用户本人或管理员可以查看完整数据
     * 
     * @param userId 用户ID
     * @param requestIp 请求IP地址
     * @return 用户数据（脱敏处理）
     */
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    public CompletableFuture<UserDataExportDTO> viewUserData(Long userId, String requestIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始查看用户数据: userId={}, requestIp={}", userId, requestIp);
                
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
                
                User currentUser = authenticationService.getCurrentUser();
                boolean isAdmin = currentUser != null && currentUser.hasRole(com.myweb.website_core.common.enums.UserRole.ADMIN);
                boolean isOwner = currentUser != null && currentUser.getId().equals(userId);
                
                // 构建用户数据DTO
                UserDataExportDTO dataDTO = buildUserDataDTO(user, isAdmin || isOwner);
                
                // 记录审计日志
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.PROFILE_UPDATE,
                    "USER",
                    userId,
                    requestIp,
                    "SUCCESS",
                    "查看用户数据: " + user.getUsername()
                );
                
                log.info("用户数据查看成功: userId={}, username={}", userId, user.getUsername());
                return dataDTO;
                
            } catch (Exception e) {
                log.error("查看用户数据失败: userId={}", userId, e);
                
                User currentUser = authenticationService.getCurrentUser();
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.PROFILE_UPDATE,
                    "USER",
                    userId,
                    requestIp,
                    "FAILURE",
                    "查看用户数据失败: " + e.getMessage()
                );
                
                throw new RuntimeException("查看用户数据失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 导出用户数据（JSON格式）
     * 
     * @param userId 用户ID
     * @param requestIp 请求IP地址
     * @return JSON格式的用户数据
     */
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    public CompletableFuture<byte[]> exportUserDataAsJson(Long userId, String requestIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始导出用户数据(JSON): userId={}, requestIp={}", userId, requestIp);
                
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
                
                User currentUser = authenticationService.getCurrentUser();
                boolean isAdmin = currentUser != null && currentUser.hasRole(com.myweb.website_core.common.enums.UserRole.ADMIN);
                boolean isOwner = currentUser != null && currentUser.getId().equals(userId);
                
                // 构建完整的用户数据
                UserDataExportDTO dataDTO = buildUserDataDTO(user, isAdmin || isOwner);
                
                // 转换为JSON
                String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dataDTO);
                
                // 记录审计日志
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.DATA_EXPORT,
                    "USER",
                    userId,
                    requestIp,
                    "SUCCESS",
                    "导出用户数据(JSON): " + user.getUsername()
                );
                
                log.info("用户数据导出成功(JSON): userId={}, username={}, size={} bytes", 
                        userId, user.getUsername(), jsonData.length());
                
                return jsonData.getBytes(StandardCharsets.UTF_8);
                
            } catch (Exception e) {
                log.error("导出用户数据失败(JSON): userId={}", userId, e);
                
                User currentUser = authenticationService.getCurrentUser();
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.DATA_EXPORT,
                    "USER",
                    userId,
                    requestIp,
                    "FAILURE",
                    "导出用户数据失败(JSON): " + e.getMessage()
                );
                
                throw new RuntimeException("导出用户数据失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 导出用户数据（CSV格式）
     * 
     * @param userId 用户ID
     * @param requestIp 请求IP地址
     * @return CSV格式的用户数据
     */
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    public CompletableFuture<byte[]> exportUserDataAsCsv(Long userId, String requestIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始导出用户数据(CSV): userId={}, requestIp={}", userId, requestIp);
                
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
                
                User currentUser = authenticationService.getCurrentUser();
                boolean isAdmin = currentUser != null && currentUser.hasRole(com.myweb.website_core.common.enums.UserRole.ADMIN);
                boolean isOwner = currentUser != null && currentUser.getId().equals(userId);
                
                // 构建CSV数据
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("字段,值\n");
                
                // 基本信息
                csvBuilder.append("用户ID,").append(user.getId()).append("\n");
                csvBuilder.append("用户名,").append(user.getUsername()).append("\n");
                
                if (isAdmin || isOwner) {
                    csvBuilder.append("邮箱,").append(user.getEmail() != null ? user.getEmail() : "").append("\n");
                } else {
                    csvBuilder.append("邮箱,").append(maskEmail(user.getEmail())).append("\n");
                }
                
                csvBuilder.append("头像URL,").append(user.getAvatarUrl() != null ? user.getAvatarUrl() : "").append("\n");
                csvBuilder.append("个人简介,").append(user.getBio() != null ? user.getBio() : "").append("\n");
                csvBuilder.append("获赞数,").append(user.getLikedCount()).append("\n");
                csvBuilder.append("关注数,").append(user.getFollowing() != null ? user.getFollowing().size() : 0).append("\n");
                csvBuilder.append("粉丝数,").append(user.getFollowers() != null ? user.getFollowers().size() : 0).append("\n");
                
                // 安全信息（仅管理员或本人可见）
                if (isAdmin || isOwner) {
                    csvBuilder.append("邮箱验证状态,").append(user.getEmailVerified() ? "已验证" : "未验证").append("\n");
                    csvBuilder.append("用户角色,").append(user.getRole()).append("\n");
                    csvBuilder.append("TOTP启用状态,").append(user.getTotpEnabled() ? "已启用" : "未启用").append("\n");
                    csvBuilder.append("创建时间,").append(user.getCreatedAt() != null ? 
                        user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append("\n");
                    csvBuilder.append("更新时间,").append(user.getUpdatedAt() != null ? 
                        user.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append("\n");
                    
                    if (isAdmin) {
                        csvBuilder.append("登录失败次数,").append(user.getLoginAttempts()).append("\n");
                        csvBuilder.append("最后登录时间,").append(user.getLastLoginTime() != null ? 
                            user.getLastLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append("\n");
                        csvBuilder.append("最后登录IP,").append(user.getLastLoginIp() != null ? user.getLastLoginIp() : "").append("\n");
                        csvBuilder.append("账户锁定状态,").append(user.isAccountLocked() ? "已锁定" : "正常").append("\n");
                    }
                }
                
                String csvData = csvBuilder.toString();
                
                // 记录审计日志
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.DATA_EXPORT,
                    "USER",
                    userId,
                    requestIp,
                    "SUCCESS",
                    "导出用户数据(CSV): " + user.getUsername()
                );
                
                log.info("用户数据导出成功(CSV): userId={}, username={}, size={} bytes", 
                        userId, user.getUsername(), csvData.length());
                
                return csvData.getBytes(StandardCharsets.UTF_8);
                
            } catch (Exception e) {
                log.error("导出用户数据失败(CSV): userId={}", userId, e);
                
                User currentUser = authenticationService.getCurrentUser();
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.DATA_EXPORT,
                    "USER",
                    userId,
                    requestIp,
                    "FAILURE",
                    "导出用户数据失败(CSV): " + e.getMessage()
                );
                
                throw new RuntimeException("导出用户数据失败: " + e.getMessage(), e);
            }
        });
    }

    // ==================== 数据修改功能 ====================

    /**
     * 修改用户数据
     * 只有用户本人或管理员可以修改
     * 
     * @param userId 用户ID
     * @param updateDTO 更新数据
     * @param requestIp 请求IP地址
     * @return 更新后的用户数据
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    public CompletableFuture<UserDataExportDTO> updateUserData(Long userId, UserDataUpdateDTO updateDTO, String requestIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始修改用户数据: userId={}, requestIp={}", userId, requestIp);
                
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
                
                User currentUser = authenticationService.getCurrentUser();
                boolean isAdmin = currentUser != null && currentUser.hasRole(com.myweb.website_core.common.enums.UserRole.ADMIN);
                boolean isOwner = currentUser != null && currentUser.getId().equals(userId);
                
                // 记录修改前的数据
                String beforeData = buildChangeLogData(user);
                
                // 执行数据更新
                updateUserFields(user, updateDTO, isAdmin);
                
                // 保存更新
                User updatedUser = userRepository.save(user);
                
                // 记录修改后的数据
                String afterData = buildChangeLogData(updatedUser);
                
                // 构建返回数据
                UserDataExportDTO resultDTO = buildUserDataDTO(updatedUser, isAdmin || isOwner);
                
                // 记录审计日志
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.PROFILE_UPDATE,
                    "USER",
                    userId,
                    requestIp,
                    "SUCCESS",
                    String.format("修改用户数据: %s, 修改前: %s, 修改后: %s", 
                                user.getUsername(), beforeData, afterData)
                );
                
                log.info("用户数据修改成功: userId={}, username={}", userId, user.getUsername());
                return resultDTO;
                
            } catch (Exception e) {
                log.error("修改用户数据失败: userId={}", userId, e);
                
                User currentUser = authenticationService.getCurrentUser();
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.PROFILE_UPDATE,
                    "USER",
                    userId,
                    requestIp,
                    "FAILURE",
                    "修改用户数据失败: " + e.getMessage()
                );
                
                throw new RuntimeException("修改用户数据失败: " + e.getMessage(), e);
            }
        });
    }

    // ==================== 数据删除功能 ====================

    /**
     * 删除用户数据（物理删除）
     * 只有用户本人或管理员可以删除
     * 
     * @param userId 用户ID
     * @param requestIp 请求IP地址
     * @return 删除结果
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @userDataManagementService.isCurrentUser(#userId)")
    public CompletableFuture<Boolean> deleteUserData(Long userId, String requestIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始删除用户数据: userId={}, requestIp={}", userId, requestIp);
                
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
                
                User currentUser = authenticationService.getCurrentUser();
                
                // 记录删除前的用户信息
                String userData = String.format("用户ID: %d, 用户名: %s, 邮箱: %s, 创建时间: %s", 
                    user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
                
                // 执行物理删除
                userRepository.delete(user);
                
                // 记录审计日志
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.USER_DELETE,
                    "USER",
                    userId,
                    requestIp,
                    "SUCCESS",
                    "删除用户数据: " + userData
                );
                
                log.info("用户数据删除成功: userId={}, username={}", userId, user.getUsername());
                return true;
                
            } catch (Exception e) {
                log.error("删除用户数据失败: userId={}", userId, e);
                
                User currentUser = authenticationService.getCurrentUser();
                sendAuditMessage(
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getUsername() : "anonymous",
                    AuditOperation.USER_DELETE,
                    "USER",
                    userId,
                    requestIp,
                    "FAILURE",
                    "删除用户数据失败: " + e.getMessage()
                );
                
                throw new RuntimeException("删除用户数据失败: " + e.getMessage(), e);
            }
        });
    }

    // ==================== 权限检查方法 ====================

    /**
     * 检查是否为当前用户
     * 用于Spring Security的@PreAuthorize注解
     * 
     * @param userId 用户ID
     * @return 是否为当前用户
     */
    public boolean isCurrentUser(Long userId) {
        try {
            User currentUser = authenticationService.getCurrentUser();
            return currentUser != null && currentUser.getId().equals(userId);
        } catch (Exception e) {
            log.warn("检查当前用户失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建用户数据DTO
     */
    private UserDataExportDTO buildUserDataDTO(User user, boolean showSensitiveData) {
        UserDataExportDTO dto = new UserDataExportDTO();
        
        // 基本信息
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setLikedCount(user.getLikedCount());
        dto.setFollowersCount(user.getFollowers() != null ? user.getFollowers().size() : 0);
        dto.setFollowingCount(user.getFollowing() != null ? user.getFollowing().size() : 0);
        
        // 敏感信息处理
        if (showSensitiveData) {
            dto.setEmail(user.getEmail());
            dto.setEmailVerified(user.getEmailVerified());
            dto.setRole(user.getRole().name());
            dto.setTotpEnabled(user.getTotpEnabled());
            dto.setCreatedAt(user.getCreatedAt());
            dto.setUpdatedAt(user.getUpdatedAt());
            dto.setLoginAttempts(user.getLoginAttempts());
            dto.setLastLoginTime(user.getLastLoginTime());
            dto.setLastLoginIp(user.getLastLoginIp());
            dto.setAccountLocked(user.isAccountLocked());
        } else {
            dto.setEmail(maskEmail(user.getEmail()));
            dto.setEmailVerified(null);
            dto.setRole("USER");
            dto.setTotpEnabled(null);
            dto.setCreatedAt(null);
            dto.setUpdatedAt(null);
            dto.setLoginAttempts(null);
            dto.setLastLoginTime(null);
            dto.setLastLoginIp(null);
            dto.setAccountLocked(null);
        }
        
        return dto;
    }

    /**
     * 更新用户字段
     */
    private void updateUserFields(User user, UserDataUpdateDTO updateDTO, boolean isAdmin) {
        if (updateDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(updateDTO.getAvatarUrl());
        }
        
        if (updateDTO.getBio() != null) {
            user.setBio(updateDTO.getBio());
        }
        
        // 只有管理员可以修改敏感字段
        if (isAdmin) {
            if (updateDTO.getEmail() != null) {
                user.setEmail(updateDTO.getEmail());
                user.setEmailVerified(false); // 邮箱变更后需要重新验证
            }
            
            if (updateDTO.getRole() != null) {
                user.setRole(com.myweb.website_core.common.enums.UserRole.valueOf(updateDTO.getRole()));
            }
        }
    }

    /**
     * 构建变更日志数据
     */
    private String buildChangeLogData(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("email", maskEmail(user.getEmail()));
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("bio", user.getBio());
        data.put("role", user.getRole());
        data.put("updatedAt", user.getUpdatedAt());
        
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return data.toString();
        }
    }

    /**
     * 邮箱脱敏处理
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domainPart;
        } else {
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
        }
    }

    /**
     * 发送审计消息
     */
    private void sendAuditMessage(Long userId, String username, AuditOperation operation, 
                                String resourceType, Long resourceId, String ipAddress, 
                                String result, String description) {
        try {
            UnifiedSecurityMessage message = UnifiedSecurityMessage.auditLog(
                operation, userId, username, result, ipAddress
            );
            message.setResourceType(resourceType);
            message.setResourceId(resourceId);
            message.setDescription(description);
            
            messageProducerService.sendUnifiedSecurityMessage(message);
        } catch (Exception e) {
            log.error("发送审计消息失败: {}", e.getMessage(), e);
        }
    }
}