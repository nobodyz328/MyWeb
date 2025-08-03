package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 权限数据访问接口
 * 
 * 提供权限实体的数据库操作方法
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * 根据权限名称查找权限
     * 
     * @param name 权限名称
     * @return 权限对象
     */
    Optional<Permission> findByName(String name);
    
    /**
     * 根据权限名称查找启用的权限
     * 
     * @param name 权限名称
     * @return 权限对象
     */
    Optional<Permission> findByNameAndEnabledTrue(String name);
    
    /**
     * 根据资源类型和操作类型查找权限
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 权限对象
     */
    Optional<Permission> findByResourceTypeAndActionType(String resourceType, String actionType);
    
    /**
     * 根据资源类型和操作类型查找启用的权限
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 权限对象
     */
    Optional<Permission> findByResourceTypeAndActionTypeAndEnabledTrue(String resourceType, String actionType);
    
    /**
     * 根据资源类型查找权限
     * 
     * @param resourceType 资源类型
     * @return 权限列表
     */
    List<Permission> findByResourceTypeAndEnabledTrueOrderByPermissionLevelAsc(String resourceType);
    
    /**
     * 根据操作类型查找权限
     * 
     * @param actionType 操作类型
     * @return 权限列表
     */
    List<Permission> findByActionTypeAndEnabledTrueOrderByPermissionLevelAsc(String actionType);
    
    /**
     * 根据权限分组查找权限
     * 
     * @param permissionGroup 权限分组
     * @return 权限列表
     */
    List<Permission> findByPermissionGroupAndEnabledTrueOrderByPermissionLevelAsc(String permissionGroup);
    
    /**
     * 查找所有启用的权限
     * 
     * @return 权限列表
     */
    List<Permission> findByEnabledTrueOrderByPermissionGroupAscPermissionLevelAsc();
    
    /**
     * 查找所有系统权限
     * 
     * @return 系统权限列表
     */
    List<Permission> findBySystemPermissionTrueOrderByPermissionGroupAscPermissionLevelAsc();
    
    /**
     * 查找非系统权限
     * 
     * @return 非系统权限列表
     */
    List<Permission> findBySystemPermissionFalseOrderByPermissionGroupAscPermissionLevelAsc();
    
    /**
     * 根据权限级别范围查找权限
     * 
     * @param minLevel 最小权限级别
     * @param maxLevel 最大权限级别
     * @return 权限列表
     */
    List<Permission> findByPermissionLevelBetweenAndEnabledTrueOrderByPermissionLevelAsc(Integer minLevel, Integer maxLevel);
    
    /**
     * 根据权限名称集合查找权限
     * 
     * @param names 权限名称集合
     * @return 权限列表
     */
    List<Permission> findByNameInAndEnabledTrue(Set<String> names);
    
    /**
     * 检查权限名称是否存在
     * 
     * @param name 权限名称
     * @return 是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 检查资源类型和操作类型组合是否存在
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否存在
     */
    boolean existsByResourceTypeAndActionType(String resourceType, String actionType);
    
    /**
     * 检查除指定ID外是否存在相同名称的权限
     * 
     * @param name 权限名称
     * @param id 排除的权限ID
     * @return 是否存在
     */
    boolean existsByNameAndIdNot(String name, Long id);
    
    /**
     * 检查除指定ID外是否存在相同资源类型和操作类型的权限
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @param id 排除的权限ID
     * @return 是否存在
     */
    boolean existsByResourceTypeAndActionTypeAndIdNot(String resourceType, String actionType, Long id);
    
    /**
     * 查找角色拥有的权限
     * 
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId AND p.enabled = true ORDER BY p.permissionGroup ASC, p.permissionLevel ASC")
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查找角色拥有的权限名称
     * 
     * @param roleId 角色ID
     * @return 权限名称列表
     */
    @Query("SELECT p.name FROM Permission p JOIN p.roles r WHERE r.id = :roleId AND p.enabled = true")
    List<String> findPermissionNamesByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查找用户拥有的权限
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND p.enabled = true AND r.enabled = true ORDER BY p.permissionGroup ASC, p.permissionLevel ASC")
    List<Permission> findByUserId(@Param("userId") Long userId);
    
    /**
     * 查找用户拥有的权限名称
     * 
     * @param userId 用户ID
     * @return 权限名称列表
     */
    @Query("SELECT DISTINCT p.name FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND p.enabled = true AND r.enabled = true")
    List<String> findPermissionNamesByUserId(@Param("userId") Long userId);
    
    /**
     * 检查用户是否拥有指定权限
     * 
     * @param userId 用户ID
     * @param permissionName 权限名称
     * @return 是否拥有权限
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND p.name = :permissionName AND p.enabled = true AND r.enabled = true")
    boolean hasPermission(@Param("userId") Long userId, @Param("permissionName") String permissionName);
    
    /**
     * 检查用户是否拥有指定资源和操作的权限
     * 
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 是否拥有权限
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND p.resourceType = :resourceType AND p.actionType = :actionType AND p.enabled = true AND r.enabled = true")
    boolean hasPermission(@Param("userId") Long userId, @Param("resourceType") String resourceType, @Param("actionType") String actionType);
    
    /**
     * 统计拥有指定权限的角色数量
     * 
     * @param permissionId 权限ID
     * @return 角色数量
     */
    @Query("SELECT COUNT(r) FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    Long countRolesByPermissionId(@Param("permissionId") Long permissionId);
    
    /**
     * 查找所有权限分组
     * 
     * @return 权限分组列表
     */
    @Query("SELECT DISTINCT p.permissionGroup FROM Permission p WHERE p.permissionGroup IS NOT NULL AND p.enabled = true ORDER BY p.permissionGroup")
    List<String> findAllPermissionGroups();
    
    /**
     * 查找指定资源类型的所有操作类型
     * 
     * @param resourceType 资源类型
     * @return 操作类型列表
     */
    @Query("SELECT DISTINCT p.actionType FROM Permission p WHERE p.resourceType = :resourceType AND p.enabled = true ORDER BY p.actionType")
    List<String> findActionTypesByResourceType(@Param("resourceType") String resourceType);
    
    /**
     * 查找所有资源类型
     * 
     * @return 资源类型列表
     */
    @Query("SELECT DISTINCT p.resourceType FROM Permission p WHERE p.enabled = true ORDER BY p.resourceType")
    List<String> findAllResourceTypes();
}