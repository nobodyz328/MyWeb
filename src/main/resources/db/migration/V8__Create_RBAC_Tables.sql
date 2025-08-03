-- =====================================================
-- MyWeb博客系统 RBAC权限模型数据库迁移脚本
-- 版本: V8
-- 描述: 创建基于角色的访问控制(RBAC)相关表结构
-- 符合: GB/T 22239-2019 二级等保要求
-- =====================================================

-- 创建角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

-- 创建角色表索引
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);
CREATE INDEX IF NOT EXISTS idx_roles_enabled ON roles(enabled);
CREATE INDEX IF NOT EXISTS idx_roles_system_role ON roles(system_role);
CREATE INDEX IF NOT EXISTS idx_roles_priority ON roles(priority);
CREATE INDEX IF NOT EXISTS idx_roles_created_at ON roles(created_at);

-- 创建权限表
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    resource_type VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    system_permission BOOLEAN NOT NULL DEFAULT FALSE,
    permission_level INTEGER NOT NULL DEFAULT 1,
    permission_group VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT uk_permissions_resource_action UNIQUE (resource_type, action_type)
);

-- 创建权限表索引
CREATE INDEX IF NOT EXISTS idx_permissions_name ON permissions(name);
CREATE INDEX IF NOT EXISTS idx_permissions_resource_type ON permissions(resource_type);
CREATE INDEX IF NOT EXISTS idx_permissions_action_type ON permissions(action_type);
CREATE INDEX IF NOT EXISTS idx_permissions_enabled ON permissions(enabled);
CREATE INDEX IF NOT EXISTS idx_permissions_system_permission ON permissions(system_permission);
CREATE INDEX IF NOT EXISTS idx_permissions_permission_group ON permissions(permission_group);
CREATE INDEX IF NOT EXISTS idx_permissions_created_at ON permissions(created_at);

-- 创建用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_roles UNIQUE (user_id, role_id)
);

-- 创建用户角色关联表索引
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_created_at ON user_roles(created_at);

-- 创建角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permissions UNIQUE (role_id, permission_id)
);

-- 创建角色权限关联表索引
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_created_at ON role_permissions(created_at);

-- =====================================================
-- 插入系统默认权限数据
-- =====================================================

-- 用户管理权限
INSERT INTO permissions (name, display_name, description, resource_type, action_type, system_permission, permission_level, permission_group) VALUES
('USER_CREATE', '创建用户', '创建新用户账户的权限', 'USER', 'CREATE', TRUE, 3, 'USER_MANAGEMENT'),
('USER_READ', '查看用户', '查看用户信息的权限', 'USER', 'READ', TRUE, 1, 'USER_MANAGEMENT'),
('USER_UPDATE', '更新用户', '更新用户信息的权限', 'USER', 'UPDATE', TRUE, 2, 'USER_MANAGEMENT'),
('USER_DELETE', '删除用户', '删除用户账户的权限', 'USER', 'DELETE', TRUE, 4, 'USER_MANAGEMENT'),
('USER_MANAGE', '管理用户', '完整的用户管理权限', 'USER', 'MANAGE', TRUE, 5, 'USER_MANAGEMENT')
ON CONFLICT (name) DO NOTHING;

-- 帖子管理权限
INSERT INTO permissions (name, display_name, description, resource_type, action_type, system_permission, permission_level, permission_group) VALUES
('POST_CREATE', '创建帖子', '发布新帖子的权限', 'POST', 'CREATE', TRUE, 1, 'POST_MANAGEMENT'),
('POST_READ', '查看帖子', '查看帖子内容的权限', 'POST', 'READ', TRUE, 1, 'POST_MANAGEMENT'),
('POST_UPDATE', '更新帖子', '编辑自己帖子的权限', 'POST', 'UPDATE', TRUE, 2, 'POST_MANAGEMENT'),
('POST_DELETE', '删除帖子', '删除自己帖子的权限', 'POST', 'DELETE', TRUE, 3, 'POST_MANAGEMENT'),
('POST_MANAGE', '管理帖子', '管理所有帖子的权限', 'POST', 'MANAGE', TRUE, 4, 'POST_MANAGEMENT')
ON CONFLICT (name) DO NOTHING;

-- 评论管理权限
INSERT INTO permissions (name, display_name, description, resource_type, action_type, system_permission, permission_level, permission_group) VALUES
('COMMENT_CREATE', '创建评论', '发表评论的权限', 'COMMENT', 'CREATE', TRUE, 1, 'COMMENT_MANAGEMENT'),
('COMMENT_READ', '查看评论', '查看评论内容的权限', 'COMMENT', 'READ', TRUE, 1, 'COMMENT_MANAGEMENT'),
('COMMENT_UPDATE', '更新评论', '编辑自己评论的权限', 'COMMENT', 'UPDATE', TRUE, 2, 'COMMENT_MANAGEMENT'),
('COMMENT_DELETE', '删除评论', '删除自己评论的权限', 'COMMENT', 'DELETE', TRUE, 3, 'COMMENT_MANAGEMENT'),
('COMMENT_MANAGE', '管理评论', '管理所有评论的权限', 'COMMENT', 'MANAGE', TRUE, 4, 'COMMENT_MANAGEMENT')
ON CONFLICT (name) DO NOTHING;

-- 系统管理权限
INSERT INTO permissions (name, display_name, description, resource_type, action_type, system_permission, permission_level, permission_group) VALUES
('SYSTEM_CONFIG', '系统配置', '修改系统配置的权限', 'SYSTEM', 'CONFIG', TRUE, 5, 'SYSTEM_MANAGEMENT'),
('SYSTEM_AUDIT', '审计查看', '查看系统审计日志的权限', 'SYSTEM', 'AUDIT', TRUE, 4, 'SYSTEM_MANAGEMENT'),
('SYSTEM_MONITOR', '系统监控', '查看系统监控信息的权限', 'SYSTEM', 'MONITOR', TRUE, 3, 'SYSTEM_MANAGEMENT'),
('SYSTEM_MANAGE', '系统管理', '完整的系统管理权限', 'SYSTEM', 'MANAGE', TRUE, 6, 'SYSTEM_MANAGEMENT')
ON CONFLICT (name) DO NOTHING;

-- 角色管理权限
INSERT INTO permissions (name, display_name, description, resource_type, action_type, system_permission, permission_level, permission_group) VALUES
('ROLE_CREATE', '创建角色', '创建新角色的权限', 'ROLE', 'CREATE', TRUE, 4, 'ROLE_MANAGEMENT'),
('ROLE_READ', '查看角色', '查看角色信息的权限', 'ROLE', 'READ', TRUE, 2, 'ROLE_MANAGEMENT'),
('ROLE_UPDATE', '更新角色', '修改角色信息的权限', 'ROLE', 'UPDATE', TRUE, 4, 'ROLE_MANAGEMENT'),
('ROLE_DELETE', '删除角色', '删除角色的权限', 'ROLE', 'DELETE', TRUE, 5, 'ROLE_MANAGEMENT'),
('ROLE_MANAGE', '管理角色', '完整的角色管理权限', 'ROLE', 'MANAGE', TRUE, 6, 'ROLE_MANAGEMENT')
ON CONFLICT (name) DO NOTHING;

-- 权限管理权限
INSERT INTO permissions (name, display_name, description, resource_type, action_type, system_permission, permission_level, permission_group) VALUES
('PERMISSION_READ', '查看权限', '查看权限信息的权限', 'PERMISSION', 'READ', TRUE, 2, 'PERMISSION_MANAGEMENT'),
('PERMISSION_ASSIGN', '分配权限', '分配权限给角色的权限', 'PERMISSION', 'ASSIGN', TRUE, 5, 'PERMISSION_MANAGEMENT'),
('PERMISSION_MANAGE', '管理权限', '完整的权限管理权限', 'PERMISSION', 'MANAGE', TRUE, 6, 'PERMISSION_MANAGEMENT')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- 插入系统默认角色数据
-- =====================================================

-- 普通用户角色
INSERT INTO roles (name, display_name, description, system_role, priority) VALUES
('USER', '普通用户', '系统的基础用户角色，具有基本的内容创建和互动权限', TRUE, 1)
ON CONFLICT (name) DO NOTHING;

-- 版主角色
INSERT INTO roles (name, display_name, description, system_role, priority) VALUES
('MODERATOR', '版主', '社区内容管理员，负责维护社区秩序和内容质量', TRUE, 2)
ON CONFLICT (name) DO NOTHING;

-- 系统管理员角色
INSERT INTO roles (name, display_name, description, system_role, priority) VALUES
('ADMIN', '系统管理员', '系统最高权限角色，负责系统管理和安全配置', TRUE, 3)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- 为角色分配权限
-- =====================================================

-- 普通用户权限分配
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN (
    'POST_CREATE', 'POST_READ', 'POST_UPDATE', 'POST_DELETE',
    'COMMENT_CREATE', 'COMMENT_READ', 'COMMENT_UPDATE', 'COMMENT_DELETE',
    'USER_READ'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 版主权限分配（包含普通用户权限 + 管理权限）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'MODERATOR' AND p.name IN (
    'POST_CREATE', 'POST_READ', 'POST_UPDATE', 'POST_DELETE', 'POST_MANAGE',
    'COMMENT_CREATE', 'COMMENT_READ', 'COMMENT_UPDATE', 'COMMENT_DELETE', 'COMMENT_MANAGE',
    'USER_READ', 'USER_UPDATE',
    'SYSTEM_MONITOR'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 系统管理员权限分配（所有权限）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- =====================================================
-- 为现有用户分配默认角色
-- =====================================================

-- 为所有现有用户根据其role字段分配对应的角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.role = r.name
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =====================================================
-- 添加表注释
-- =====================================================

COMMENT ON TABLE roles IS '角色表 - 存储系统中的角色信息，用于RBAC权限控制';
COMMENT ON TABLE permissions IS '权限表 - 存储系统中的权限信息，定义对资源的操作权限';
COMMENT ON TABLE user_roles IS '用户角色关联表 - 建立用户和角色的多对多关系';
COMMENT ON TABLE role_permissions IS '角色权限关联表 - 建立角色和权限的多对多关系';

-- 添加列注释
COMMENT ON COLUMN roles.name IS '角色名称，唯一标识';
COMMENT ON COLUMN roles.display_name IS '角色显示名称';
COMMENT ON COLUMN roles.system_role IS '是否为系统内置角色';
COMMENT ON COLUMN roles.priority IS '角色优先级，数值越大优先级越高';

COMMENT ON COLUMN permissions.name IS '权限名称，唯一标识';
COMMENT ON COLUMN permissions.resource_type IS '资源类型';
COMMENT ON COLUMN permissions.action_type IS '操作类型';
COMMENT ON COLUMN permissions.system_permission IS '是否为系统内置权限';
COMMENT ON COLUMN permissions.permission_level IS '权限级别';

-- =====================================================
-- 创建触发器更新updated_at字段
-- =====================================================

-- 创建更新时间戳函数（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为roles表创建触发器
DROP TRIGGER IF EXISTS update_roles_updated_at ON roles;
CREATE TRIGGER update_roles_updated_at 
    BEFORE UPDATE ON roles 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- 为permissions表创建触发器
DROP TRIGGER IF EXISTS update_permissions_updated_at ON permissions;
CREATE TRIGGER update_permissions_updated_at 
    BEFORE UPDATE ON permissions 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 脚本执行完成
-- =====================================================