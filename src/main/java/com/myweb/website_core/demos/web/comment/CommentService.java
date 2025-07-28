package com.myweb.website_core.demos.web.comment;

import com.myweb.website_core.demos.web.blog.Post;
import com.myweb.website_core.demos.web.blog.PostRepository;
import com.myweb.website_core.demos.web.user.User;
import com.myweb.website_core.demos.web.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public CommentService(CommentRepository commentRepository, 
                         PostRepository postRepository,
                         UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }
    
    // 创建评论
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
    
    // 创建回复
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
    
    // 获取帖子的所有评论（包括回复）
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        List<Comment> topLevelComments = commentRepository.findTopLevelCommentsByPostId(postId);
        
        return topLevelComments.stream()
                .map(this::convertToDTO)
                .toList();
    }
    
    // 删除评论
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 只允许作者删除自己的评论
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权限删除此评论");
        }
        
        Long postId = comment.getPost().getId();
        commentRepository.delete(comment);
        
        // 更新帖子的评论数
        updatePostCommentCount(postId);
    }
    
    // 更新帖子的评论数
    private void updatePostCommentCount(Long postId) {
        long commentCount = commentRepository.countByPostId(postId);
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentCount((int) commentCount);
            postRepository.save(post);
        }
    }
    
    // 转换为DTO
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        
        // 设置作者信息
        CommentDTO.AuthorInfo authorInfo = new CommentDTO.AuthorInfo();
        authorInfo.setId(comment.getAuthor().getId());
        authorInfo.setUsername(comment.getAuthor().getUsername());
        authorInfo.setAvatarUrl(comment.getAuthor().getAvatarUrl());
        dto.setAuthor(authorInfo);
        
        // 设置回复
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDTO> replies = comment.getReplies().stream()
                    .map(this::convertToDTO)
                    .toList();
            dto.setReplies(replies);
        }
        
        return dto;
    }
}