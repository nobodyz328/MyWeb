-- ========================================
-- 用户安全字段增强迁移脚本
-- 符合GB/T 22239-2019二级等保要求
-- ========================================

-- 添加安全相关字段到users表
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS login_attempts INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_login_time TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(45),
ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(255),
ADD COLUMN IF NOT EXISTS totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER',
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- 为现有用户设置默认值
UPDATE users 
SET 
    password_hash = password,  -- 临时使用原密码，后续需要重新加密
    email_verified = FALSE,
    login_attempts = 0,
    totp_enabled = FALSE,
    role = 'USER',
    created_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE password_hash = '';

-- 添加索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_account_locked_until ON users(account_locked_until);
CREATE INDEX IF NOT EXISTS idx_users_last_login_time ON users(last_login_time);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 添加约束
ALTER TABLE users 
ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
ADD CONSTRAINT chk_users_login_attempts CHECK (login_attempts >= 0);

-- 添加注释
COMMENT ON COLUMN users.password_hash IS '密码哈希值，使用BCrypt算法加密';
COMMENT ON COLUMN users.email_verified IS '邮箱验证状态';
COMMENT ON COLUMN users.login_attempts IS '登录失败次数';
COMMENT ON COLUMN users.account_locked_until IS '账户锁定截止时间';
COMMENT ON COLUMN users.last_login_time IS '最后登录时间';
COMMENT ON COLUMN users.last_login_ip IS '最后登录IP地址';
COMMENT ON COLUMN users.totp_secret IS 'TOTP动态口令密钥';
COMMENT ON COLUMN users.totp_enabled IS 'TOTP动态口令启用状态';
COMMENT ON COLUMN users.role IS '用户角色：USER/MODERATOR/ADMIN';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';