package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 角色数据访问接口
 * 
 * 提供角色实体的数据库操作方法
 * 符合GB/T 22239-2019二级等保要求的访问控制机制
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 根据角色名称查找角色
     * 
     * @param name 角色名称
     * @return 角色对象
     */
    Optional<Role> findByName(String name);
    
    /**
     * 根据角色名称查找启用的角色
     * 
     * @param name 角色名称
     * @return 角色对象
     */
    Optional<Role> findByNameAndEnabledTrue(String name);
    
    /**
     * 查找所有启用的角色
     * 
     * @return 启用的角色列表
     */
    List<Role> findByEnabledTrueOrderByPriorityDesc();
    
    /**
     * 查找所有系统角色
     * 
     * @return 系统角色列表
     */
    List<Role> findBySystemRoleTrueOrderByPriorityDesc();
    
    /**
     * 查找非系统角色
     * 
     * @return 非系统角色列表
     */
    List<Role> findBySystemRoleFalseOrderByPriorityDesc();
    
    /**
     * 根据优先级范围查找角色
     * 
     * @param minPriority 最小优先级
     * @param maxPriority 最大优先级
     * @return 角色列表
     */
    List<Role> findByPriorityBetweenAndEnabledTrueOrderByPriorityDesc(Integer minPriority, Integer maxPriority);
    
    /**
     * 根据角色名称集合查找角色
     * 
     * @param names 角色名称集合
     * @return 角色列表
     */
    List<Role> findByNameInAndEnabledTrue(Set<String> names);
    
    /**
     * 检查角色名称是否存在
     * 
     * @param name 角色名称
     * @return 是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 检查除指定ID外是否存在相同名称的角色
     * 
     * @param name 角色名称
     * @param id 排除的角色ID
     * @return 是否存在
     */
    boolean existsByNameAndIdNot(String name, Long id);
    
    /**
     * 查找拥有指定权限的角色
     * 
     * @param permissionName 权限名称
     * @return 角色列表
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.permissions p WHERE p.name = :permissionName AND r.enabled = true")
    List<Role> findByPermissionName(@Param("permissionName") String permissionName);
    
    /**
     * 查找拥有指定资源类型和操作类型权限的角色
     * 
     * @param resourceType 资源类型
     * @param actionType 操作类型
     * @return 角色列表
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.permissions p WHERE p.resourceType = :resourceType AND p.actionType = :actionType AND r.enabled = true")
    List<Role> findByPermissionResourceAndAction(@Param("resourceType") String resourceType, @Param("actionType") String actionType);
    
    /**
     * 查找用户拥有的角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId AND r.enabled = true ORDER BY r.priority DESC")
    List<Role> findByUserId(@Param("userId") Long userId);
    
    /**
     * 查找用户拥有的角色名称
     * 
     * @param userId 用户ID
     * @return 角色名称列表
     */
    @Query("SELECT r.name FROM Role r JOIN r.users u WHERE u.id = :userId AND r.enabled = true")
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);
    
    /**
     * 统计拥有指定角色的用户数量
     * 
     * @param roleId 角色ID
     * @return 用户数量
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId")
    Long countUsersByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查找优先级高于指定值的角色
     * 
     * @param priority 优先级阈值
     * @return 角色列表
     */
    List<Role> findByPriorityGreaterThanAndEnabledTrueOrderByPriorityDesc(Integer priority);
    
    /**
     * 查找最高优先级的角色
     * 
     * @return 最高优先级角色
     */
    @Query("SELECT r FROM Role r WHERE r.enabled = true ORDER BY r.priority DESC LIMIT 1")
    Optional<Role> findHighestPriorityRole();
}