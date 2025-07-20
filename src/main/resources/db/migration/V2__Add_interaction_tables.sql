-- Migration for post interaction system
-- Creates post_likes and post_bookmarks tables with proper constraints and indexes

-- Post Likes Table
CREATE TABLE post_likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_likes_user_post UNIQUE(user_id, post_id)
);

-- Post Bookmarks Table
CREATE TABLE post_bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_post_bookmarks_user_post UNIQUE(user_id, post_id)
);

-- Performance indexes for post_likes
CREATE INDEX idx_post_likes_user_id ON post_likes(user_id);
CREATE INDEX idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX idx_post_likes_created_at ON post_likes(created_at);

-- Performance indexes for post_bookmarks
CREATE INDEX idx_post_bookmarks_user_id ON post_bookmarks(user_id);
CREATE INDEX idx_post_bookmarks_post_id ON post_bookmarks(post_id);
CREATE INDEX idx_post_bookmarks_created_at ON post_bookmarks(created_at);

-- Additional performance indexes for comments (enhanced for interaction system)
CREATE INDEX IF NOT EXISTS idx_comments_is_deleted ON comments(is_deleted);
CREATE INDEX IF NOT EXISTS idx_comments_updated_at ON comments(updated_at);

-- Composite indexes for common query patterns
CREATE INDEX idx_post_likes_post_created ON post_likes(post_id, created_at);
CREATE INDEX idx_post_bookmarks_user_created ON post_bookmarks(user_id, created_at);
CREATE INDEX idx_comments_post_not_deleted ON comments(post_id, is_deleted);
CREATE INDEX idx_comments_parent_not_deleted ON comments(parent_comment_id, is_deleted) WHERE parent_comment_id IS NOT NULL;