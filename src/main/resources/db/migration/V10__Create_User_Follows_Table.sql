CREATE TABLE user_follows (
                              id BIGSERIAL PRIMARY KEY,
                              follower_id BIGINT NOT NULL, -- 关注者用户ID
                              following_id BIGINT NOT NULL, -- 被关注者用户ID
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 关注时间

    -- 外键约束
                              FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
                              FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,

    -- 唯一约束，防止重复关注
                              UNIQUE (follower_id, following_id)
);

-- 创建索引优化查询性能
CREATE INDEX idx_follower_id ON user_follows (follower_id);
CREATE INDEX idx_following_id ON user_follows (following_id);
CREATE INDEX idx_created_at ON user_follows (created_at);

-- 删除原有的user_followers表（如果存在）
DROP TABLE IF EXISTS user_followers;