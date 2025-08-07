-- 为Post和Comment表添加内容哈希字段
-- 符合GB/T 22239-2019数据完整性保护要求

-- 为posts表添加内容哈希字段
ALTER TABLE posts 
ADD COLUMN content_hash VARCHAR(255),
ADD COLUMN hash_calculated_at TIMESTAMP;

-- 为comments表添加内容哈希字段
ALTER TABLE comments 
ADD COLUMN content_hash VARCHAR(255),
ADD COLUMN hash_calculated_at TIMESTAMP;

-- 创建索引以提高查询性能
CREATE INDEX idx_posts_content_hash ON posts(content_hash);
CREATE INDEX idx_comments_content_hash ON comments(content_hash);
CREATE INDEX idx_posts_hash_calculated_at ON posts(hash_calculated_at);
CREATE INDEX idx_comments_hash_calculated_at ON comments(hash_calculated_at);

-- 添加注释说明
COMMENT ON COLUMN posts.content_hash IS '帖子内容的SHA-256哈希值，用于数据完整性验证';
COMMENT ON COLUMN posts.hash_calculated_at IS '内容哈希值的计算时间';
COMMENT ON COLUMN comments.content_hash IS '评论内容的SHA-256哈希值，用于数据完整性验证';
COMMENT ON COLUMN comments.hash_calculated_at IS '内容哈希值的计算时间';