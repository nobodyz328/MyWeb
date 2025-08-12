package com.myweb.website_core.infrastructure.persistence.repository;

import com.myweb.website_core.domain.business.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment>, CommentRepositoryCustom {
    
    // 查找帖子的所有顶级评论（非回复）
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelCommentsByPostId(@Param("postId") Long postId);
    
    // 查找评论的所有回复
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);
    
    // 统计帖子的评论总数（包括回复）
    long countByPostId(Long postId);
    
    // 查找用户的所有评论
    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    
    // 删除帖子的所有评论
    void deleteByPostId(Long postId);
    
    // 查找帖子的所有评论（包括回复）
    List<Comment> findByPostId(Long postId);
}