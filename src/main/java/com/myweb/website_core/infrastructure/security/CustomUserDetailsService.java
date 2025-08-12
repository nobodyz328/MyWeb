package com.myweb.website_core.infrastructure.security;

import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.domain.security.entity.Permission;
import com.myweb.website_core.domain.security.entity.Role;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义用户详情服务
 * <p>
 * 实现Spring Security的UserDetailsService接口，
 * 提供基于数据库的用户认证和权限加载功能
 * <p>
 * 符合GB/T 22239-2019二级等保要求的身份鉴别和访问控制机制
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 根据用户名加载用户详情
     * 
     * @param username 用户名或邮箱
     * @return 用户详情对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 支持用户名或邮箱登录
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        return new CustomUserPrincipal(user);
    }
    
    /**
     * 自定义用户主体类
     * 实现UserDetails接口，封装用户信息和权限
     */
    public static class CustomUserPrincipal implements UserDetails {

        /**
         * -- GETTER --
         *  获取用户对象
         *
         */
        @Getter
        private final User user;
        private final Set<GrantedAuthority> authorities;
        
        public CustomUserPrincipal(User user) {
            this.user = user;
            this.authorities = loadAuthorities(user);
        }
        
        /**
         * 加载用户权限
         * 包括角色权限和直接权限
         * 
         * @param user 用户对象
         * @return 权限集合
         */
        private Set<GrantedAuthority> loadAuthorities(User user) {
            Set<GrantedAuthority> authorities = new HashSet<>();
            
            // 添加传统角色权限（向后兼容）
            if (user.getRole() != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            }
            
            // 添加RBAC角色权限
            if (user.getRoles() != null) {
                user.getRoles().stream()
                    .filter(Role::isEnabled)
                    .forEach(role -> {
                        // 添加角色权限
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                        
                        // 添加角色包含的具体权限
                        if (role.getPermissions() != null) {
                            role.getPermissions().stream()
                                .filter(Permission::isEnabled)
                                .forEach(permission -> {
                                    authorities.add(new SimpleGrantedAuthority(permission.getName()));
                                });
                        }
                    });
            }
            
            return authorities;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }
        
        @Override
        public String getPassword() {
            return user.getPasswordHash();
        }
        
        @Override
        public String getUsername() {
            return user.getUsername();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true; // 账户不过期
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return !user.isAccountLocked();
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true; // 凭证不过期
        }
        
        @Override
        public boolean isEnabled() {
            return user.getEmailVerified() != null ? user.getEmailVerified() : true;
        }

        /**
         * 获取用户ID
         * 
         * @return 用户ID
         */
        public Long getUserId() {
            return user.getId();
        }
        
        /**
         * 获取用户邮箱
         * 
         * @return 用户邮箱
         */
        public String getEmail() {
            return user.getEmail();
        }
        
        /**
         * 检查是否具有指定权限
         * 
         * @param permission 权限名称
         * @return 是否具有权限
         */
        public boolean hasPermission(String permission) {
            return authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals(permission));
        }
        
        /**
         * 检查是否具有指定角色
         * 
         * @param role 角色名称
         * @return 是否具有角色
         */
        public boolean hasRole(String role) {
            String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            return authorities.stream()
                    .anyMatch(auth -> auth.getAuthority().equals(roleAuthority));
        }
        
        /**
         * 检查是否需要二次验证
         * 
         * @return 是否需要二次验证
         */
        public boolean requiresTwoFactorAuth() {
            return user.requiresTwoFactorAuth();
        }
        
        /**
         * 检查是否启用了TOTP
         * 
         * @return 是否启用TOTP
         */
        public boolean isTotpEnabled() {
            return user.getTotpEnabled() != null && user.getTotpEnabled();
        }
    }
}