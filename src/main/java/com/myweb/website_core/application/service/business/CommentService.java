package com.myweb.website_core.application.service.business;

import com.myweb.website_core.application.service.security.integeration.dataManage.DataIntegrityService;
import com.myweb.website_core.common.exception.DataIntegrityException;
import com.myweb.website_core.common.util.DTOConverter;
import com.myweb.website_core.domain.business.dto.CommentDTO;
import com.myweb.website_core.domain.business.entity.Comment;
import com.myweb.website_core.domain.business.entity.Post;
import com.myweb.website_core.infrastructure.persistence.repository.comment.CommentRepository;
import com.myweb.website_core.infrastructure.persistence.repository.post.PostRepository;
import com.myweb.website_core.domain.business.entity.User;
import com.myweb.website_core.infrastructure.persistence.repository.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final DataIntegrityService dataIntegrityService;
    
    @Autowired
    public CommentService(CommentRepository commentRepository, 
                         PostRepository postRepository,
                         UserRepository userRepository,
                         DataIntegrityService dataIntegrityService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.dataIntegrityService = dataIntegrityService;
    }
    
    /**
     *  创建评论
     */
    @Transactional
    public Comment createComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Comment comment = new Comment(content, post, user);
        Comment savedComment = commentRepository.save(comment);
        
        // 更新帖子的评论数
        updatePostCommentCount(postId);
        
        return savedComment;
    }
    

    /**
     *  创建回复
     */
    @Transactional
    public Comment createReply(Long postId, Long parentCommentId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("父评论不存在"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Comment reply = new Comment(content, post, user, parentComment);
        Comment savedReply = commentRepository.save(reply);
        
        // 更新帖子的评论数
        updatePostCommentCount(postId);
        
        return savedReply;
    }
    
     /**
      *获取帖子的所有评论（包括回复）
      */
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        List<Comment> topLevelComments = commentRepository.findTopLevelCommentsByPostId(postId);
        
        return topLevelComments.stream()
                .map(DTOConverter::convertToDTO)
                .toList();
    }

    /**
     *获取单个评论
     */
    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
    }
    
    /**
     * 修改评论内容
     * 集成数据完整性验证，符合需求3.2, 3.4
     */
    @Transactional
    public Comment editComment(Long commentId, Long userId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 只允许作者修改自己的评论
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此评论");
        }
        
        // 验证原内容完整性
        if (comment.getContentHash() != null) {
            log.info("验证评论完整性: commentId={}", commentId);
            
            DataIntegrityService.IntegrityCheckResult integrityResult = 
                dataIntegrityService.checkCommentIntegrity(commentId, comment.getContent(), comment.getContentHash());
            
            if (!integrityResult.isValid()) {
                String errorMsg = String.format("评论内容完整性验证失败，可能已被篡改: commentId=%d, error=%s", 
                    commentId, integrityResult.getErrorMessage());
                log.error(errorMsg);
                throw new DataIntegrityException(errorMsg);
            }
            
            log.info("评论完整性验证通过: commentId={}", commentId);
        }
        
        // 更新内容
        String oldContent = comment.getContent();
        comment.setContent(newContent);
        comment.setUpdatedAt(LocalDateTime.now());
        
        // 重新计算哈希值（通过@PreUpdate自动触发）
        Comment savedComment = commentRepository.save(comment);
        
        log.info("评论修改成功: commentId={}, 原内容长度={}, 新内容长度={}", 
            commentId, oldContent.length(), newContent.length());
        
        return savedComment;
    }
    
    /**
     * 删除评论
     * 集成数据完整性验证，符合需求3.2, 3.4, 3.6
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 只允许作者删除自己的评论
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权限删除此评论");
        }
        
        // 验证数据完整性
        if (comment.getContentHash() != null) {
            log.info("删除前验证评论完整性: commentId={}", commentId);
            
            DataIntegrityService.IntegrityCheckResult integrityResult = 
                dataIntegrityService.checkCommentIntegrity(commentId, comment.getContent(), comment.getContentHash());
            
            if (!integrityResult.isValid()) {
                String errorMsg = String.format("评论删除失败，内容完整性验证失败: commentId=%d, error=%s", 
                    commentId, integrityResult.getErrorMessage());
                log.error(errorMsg);
                throw new DataIntegrityException(errorMsg);
            }
            
            log.info("删除前评论完整性验证通过: commentId={}", commentId);
        }
        
        Long postId = comment.getPost().getId();
        
        // 记录删除操作的审计信息
        log.info("删除评论: commentId={}, postId={}, userId={}, contentLength={}", 
            commentId, postId, userId, comment.getContent().length());
        
        commentRepository.delete(comment);
        
        // 更新帖子的评论数
        updatePostCommentCount(postId);
        
        log.info("评论删除成功: commentId={}", commentId);
    }
    
    /**
     * 验证单个评论的完整性
     * 符合需求3.6
     */
    public DataIntegrityService.IntegrityCheckResult verifyCommentIntegrity(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        log.info("开始验证评论完整性: commentId={}", commentId);
        
        DataIntegrityService.IntegrityCheckResult result = 
            dataIntegrityService.checkCommentIntegrity(commentId, comment.getContent(), comment.getContentHash());
        
        if (!result.isValid()) {
            log.warn("评论完整性验证失败: commentId={}, error={}", commentId, result.getErrorMessage());
        } else {
            log.info("评论完整性验证通过: commentId={}", commentId);
        }
        
        return result;
    }
    
    /**
     * 批量验证评论完整性
     * 符合需求3.6
     */
    public List<DataIntegrityService.IntegrityCheckResult> verifyCommentsIntegrity(List<Long> commentIds) {
        log.info("开始批量验证评论完整性: 评论数量={}", commentIds.size());
        
        List<DataIntegrityService.IntegrityCheckResult> results = commentIds.stream()
            .map(this::verifyCommentIntegrity)
            .toList();
        
        long failedCount = results.stream()
            .mapToLong(result -> result.isValid() ? 0 : 1)
            .sum();
        
        log.info("批量评论完整性验证完成: 总数={}, 失败数={}", results.size(), failedCount);
        
        return results;
    }
    
    /**
     * 验证帖子下所有评论的完整性
     * 符合需求3.6
     */
    public List<DataIntegrityService.IntegrityCheckResult> verifyPostCommentsIntegrity(Long postId) {
        log.info("开始验证帖子下所有评论的完整性: postId={}", postId);
        
        List<Comment> comments = commentRepository.findByPostId(postId);
        List<Long> commentIds = comments.stream()
            .map(Comment::getId)
            .toList();
        
        return verifyCommentsIntegrity(commentIds);
    }
    
    /**
     * 获取评论完整性统计信息
     * 符合需求3.6
     */
    public CommentIntegrityStats getCommentIntegrityStats() {
        log.info("获取评论完整性统计信息");
        
        List<Comment> allComments = commentRepository.findAll();
        
        int totalComments = allComments.size();
        int commentsWithHash = 0;
        int validComments = 0;
        int invalidComments = 0;
        
        for (Comment comment : allComments) {
            if (comment.getContentHash() != null) {
                commentsWithHash++;
                
                DataIntegrityService.IntegrityCheckResult result = 
                    dataIntegrityService.checkCommentIntegrity(comment.getId(), comment.getContent(), comment.getContentHash());
                
                if (result.isValid()) {
                    validComments++;
                } else {
                    invalidComments++;
                }
            }
        }
        
        CommentIntegrityStats stats = new CommentIntegrityStats(
            totalComments, commentsWithHash, validComments, invalidComments);
        
        log.info("评论完整性统计: {}", stats);
        
        return stats;
    }
    
    /**
     * 更新帖子的评论数
     */
    private void updatePostCommentCount(Long postId) {
        long commentCount = commentRepository.countByPostId(postId);
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentCount((int) commentCount);
            postRepository.save(post);
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 评论完整性统计信息
     */
    public static class CommentIntegrityStats {
        private final int totalComments;
        private final int commentsWithHash;
        private final int validComments;
        private final int invalidComments;
        
        public CommentIntegrityStats(int totalComments, int commentsWithHash, 
                                   int validComments, int invalidComments) {
            this.totalComments = totalComments;
            this.commentsWithHash = commentsWithHash;
            this.validComments = validComments;
            this.invalidComments = invalidComments;
        }
        
        public int getTotalComments() { return totalComments; }
        public int getCommentsWithHash() { return commentsWithHash; }
        public int getValidComments() { return validComments; }
        public int getInvalidComments() { return invalidComments; }
        
        public double getIntegrityRate() {
            return commentsWithHash > 0 ? (double) validComments / commentsWithHash : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CommentIntegrityStats{总评论数=%d, 有哈希评论数=%d, 完整评论数=%d, 损坏评论数=%d, 完整性率=%.2f%%}",
                totalComments, commentsWithHash, validComments, invalidComments, getIntegrityRate() * 100);
        }
    }

}