-- 为图片表添加文件哈希字段以支持完整性验证
-- 任务21：增强FileUploadService安全检查

-- 添加文件哈希字段
ALTER TABLE post_images 
ADD file_hash VARCHAR(255);

-- 添加哈希计算时间字段
ALTER TABLE post_images 
ADD hash_calculated_at TIMESTAMP;

-- 为文件哈希字段添加索引以提高查询性能
CREATE INDEX idx_post_images_file_hash ON post_images(file_hash);

-- 添加注释
COMMENT ON COLUMN post_images.file_hash IS '文件哈希值（用于完整性验证）';
COMMENT ON COLUMN post_images.hash_calculated_at IS '哈希计算时间';
COMMENT ON TABLE post_images IS '帖子图片表（包含文件完整性验证功能）';