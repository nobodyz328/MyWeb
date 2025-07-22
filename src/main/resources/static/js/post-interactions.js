/**
 * Post Interaction System - Frontend JavaScript
 * Handles likes, bookmarks, comments, and replies with real-time feedback
 */

class PostInteractionManager {
    constructor(postId, userId) {
        this.postId = postId;
        this.userId = userId;
        this.apiBase = '/api/posts';
        this.loadingStates = new Set();
        this.init();
    }

    init() {
        this.initializeLikeButton();
        this.initializeBookmarkButton();
        this.initializeCommentForm();
        this.initializeReplyButtons();
        this.loadInteractionStatus();
    }

    // Like functionality with immediate UI feedback
    initializeLikeButton() {
        const likeBtn = document.querySelector('[data-action="like"]');
        if (!likeBtn) return;

        likeBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            if (this.isLoading('like')) return;

            this.setLoading('like', true);
            const wasLiked = likeBtn.classList.contains('liked');
            
            // Immediate UI feedback
            this.updateLikeButton(!wasLiked);
            
            try {
                const response = await this.makeRequest(`${this.apiBase}/${this.postId}/interactions/like`, {
                    method: 'POST'
                });

                if (response.success) {
                    this.updateLikeButton(response.data.liked, response.data.likeCount);
                } else {
                    // Revert on failure
                    this.updateLikeButton(wasLiked);
                    this.showError('点赞操作失败，请重试');
                }
            } catch (error) {
                // Revert on error
                this.updateLikeButton(wasLiked);
                this.showError('网络错误，请检查连接');
            } finally {
                this.setLoading('like', false);
            }
        });
    }

    updateLikeButton(isLiked, count = null) {
        const likeBtn = document.querySelector('[data-action="like"]');
        const countSpan = likeBtn.querySelector('.count');
        
        if (isLiked) {
            likeBtn.classList.add('liked');
            likeBtn.innerHTML = '❤️ <span class="count">' + (count || (parseInt(countSpan?.textContent || '0') + 1)) + '</span>';
        } else {
            likeBtn.classList.remove('liked');
            likeBtn.innerHTML = '🤍 <span class="count">' + (count || Math.max(0, parseInt(countSpan?.textContent || '0') - 1)) + '</span>';
        }
    }

    // Bookmark functionality with toggle state management
    initializeBookmarkButton() {
        const bookmarkBtn = document.querySelector('[data-action="bookmark"]');
        if (!bookmarkBtn) return;

        bookmarkBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            if (this.isLoading('bookmark')) return;

            this.setLoading('bookmark', true);
            const wasBookmarked = bookmarkBtn.classList.contains('bookmarked');
            
            // Immediate UI feedback
            this.updateBookmarkButton(!wasBookmarked);
            
            try {
                const response = await this.makeRequest(`${this.apiBase}/${this.postId}/interactions/bookmark`, {
                    method: 'POST'
                });

                if (response.success) {
                    this.updateBookmarkButton(response.data.bookmarked);
                } else {
                    // Revert on failure
                    this.updateBookmarkButton(wasBookmarked);
                    this.showError('收藏操作失败，请重试');
                }
            } catch (error) {
                // Revert on error
                this.updateBookmarkButton(wasBookmarked);
                this.showError('网络错误，请检查连接');
            } finally {
                this.setLoading('bookmark', false);
            }
        });
    }

    updateBookmarkButton(isBookmarked) {
        const bookmarkBtn = document.querySelector('[data-action="bookmark"]');
        
        if (isBookmarked) {
            bookmarkBtn.classList.add('bookmarked');
            bookmarkBtn.innerHTML = '⭐ 已收藏';
        } else {
            bookmarkBtn.classList.remove('bookmarked');
            bookmarkBtn.innerHTML = '☆ 收藏';
        }
    }

    // Comment submission with real-time posting
    initializeCommentForm() {
        const commentForm = document.querySelector('#comment-form');
        if (!commentForm) return;

        commentForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (this.isLoading('comment')) return;

            const textarea = commentForm.querySelector('textarea');
            const content = textarea.value.trim();
            
            if (!content) {
                this.showError('评论内容不能为空');
                return;
            }

            this.setLoading('comment', true);
            const submitBtn = commentForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.textContent = '发表中...';
            submitBtn.disabled = true;

            try {
                const response = await this.makeRequest(`${this.apiBase}/${this.postId}/comments`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        content: content,
                        postId: this.postId
                    })
                });

                if (response.success) {
                    // Add comment to UI immediately
                    this.addCommentToUI(response.data);
                    textarea.value = '';
                    this.showSuccess('评论发表成功');
                } else {
                    this.showError('评论发表失败，请重试');
                }
            } catch (error) {
                this.showError('网络错误，请检查连接');
            } finally {
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
                this.setLoading('comment', false);
            }
        });
    }

    // Reply functionality with nested comment display
    initializeReplyButtons() {
        document.addEventListener('click', async (e) => {
            if (e.target.matches('[data-action="reply"]')) {
                e.preventDefault();
                const commentId = e.target.dataset.commentId;
                this.showReplyForm(commentId);
            }
            
            if (e.target.matches('[data-action="submit-reply"]')) {
                e.preventDefault();
                const commentId = e.target.dataset.commentId;
                await this.submitReply(commentId, e.target);
            }
            
            if (e.target.matches('[data-action="cancel-reply"]')) {
                e.preventDefault();
                const commentId = e.target.dataset.commentId;
                this.hideReplyForm(commentId);
            }
        });
    }

    showReplyForm(commentId) {
        // Hide any existing reply forms
        document.querySelectorAll('.reply-form').forEach(form => form.remove());
        
        const commentElement = document.querySelector(`[data-comment-id="${commentId}"]`);
        if (!commentElement) return;

        const replyForm = document.createElement('div');
        replyForm.className = 'reply-form';
        replyForm.innerHTML = `
            <div class="reply-form-container">
                <textarea placeholder="写下你的回复..." class="reply-textarea"></textarea>
                <div class="reply-actions">
                    <button type="button" data-action="submit-reply" data-comment-id="${commentId}" class="steam-btn steam-btn-primary">回复</button>
                    <button type="button" data-action="cancel-reply" data-comment-id="${commentId}" class="steam-btn steam-btn-secondary">取消</button>
                </div>
            </div>
        `;
        
        commentElement.appendChild(replyForm);
        replyForm.querySelector('textarea').focus();
    }

    async submitReply(commentId, button) {
        if (this.isLoading(`reply-${commentId}`)) return;

        const replyForm = button.closest('.reply-form');
        const textarea = replyForm.querySelector('textarea');
        const content = textarea.value.trim();
        
        if (!content) {
            this.showError('回复内容不能为空');
            return;
        }

        this.setLoading(`reply-${commentId}`, true);
        button.textContent = '回复中...';
        button.disabled = true;

        try {
            const response = await this.makeRequest(`${this.apiBase}/${this.postId}/comments/${commentId}/replies`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    content: content,
                    parentCommentId: commentId
                })
            });

            if (response.success) {
                this.addReplyToUI(commentId, response.data);
                this.hideReplyForm(commentId);
                this.showSuccess('回复发表成功');
            } else {
                this.showError('回复发表失败，请重试');
            }
        } catch (error) {
            this.showError('网络错误，请检查连接');
        } finally {
            button.textContent = '回复';
            button.disabled = false;
            this.setLoading(`reply-${commentId}`, false);
        }
    }

    hideReplyForm(commentId) {
        const commentElement = document.querySelector(`[data-comment-id="${commentId}"]`);
        const replyForm = commentElement?.querySelector('.reply-form');
        if (replyForm) {
            replyForm.remove();
        }
    }

    // Load initial interaction status
    async loadInteractionStatus() {
        if (!this.userId) return;

        try {
            const response = await this.makeRequest(`${this.apiBase}/${this.postId}/interactions/status`);
            if (response.success) {
                const { liked, bookmarked, likeCount } = response.data;
                this.updateLikeButton(liked, likeCount);
                this.updateBookmarkButton(bookmarked);
            }
        } catch (error) {
            console.warn('Failed to load interaction status:', error);
        }
    }

    // Add comment to UI
    addCommentToUI(comment) {
        const commentsContainer = document.querySelector('.comments-list');
        if (!commentsContainer) return;

        const commentElement = this.createCommentElement(comment);
        commentsContainer.insertBefore(commentElement, commentsContainer.firstChild);
    }

    // Add reply to UI
    addReplyToUI(parentCommentId, reply) {
        const parentComment = document.querySelector(`[data-comment-id="${parentCommentId}"]`);
        if (!parentComment) return;

        let repliesContainer = parentComment.querySelector('.replies-container');
        if (!repliesContainer) {
            repliesContainer = document.createElement('div');
            repliesContainer.className = 'replies-container';
            parentComment.appendChild(repliesContainer);
        }

        const replyElement = this.createCommentElement(reply, true);
        repliesContainer.appendChild(replyElement);
    }

    // Create comment/reply HTML element
    createCommentElement(comment, isReply = false) {
        const div = document.createElement('div');
        div.className = `comment-card ${isReply ? 'reply-card' : ''}`;
        div.setAttribute('data-comment-id', comment.id);
        
        div.innerHTML = `
            <div class="comment-header">
                <span class="comment-author">${this.escapeHtml(comment.author.username || comment.author.name)}</span>
                <span class="comment-time">${this.formatTime(comment.createdAt)}</span>
            </div>
            <div class="comment-content">${this.escapeHtml(comment.content)}</div>
            ${!isReply ? `<div class="comment-actions">
                <button data-action="reply" data-comment-id="${comment.id}" class="reply-btn">回复</button>
            </div>` : ''}
        `;
        
        return div;
    }

    // Utility methods
    async makeRequest(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                ...options.headers
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return await response.json();
    }

    setLoading(operation, isLoading) {
        if (isLoading) {
            this.loadingStates.add(operation);
        } else {
            this.loadingStates.delete(operation);
        }
    }

    isLoading(operation) {
        return this.loadingStates.has(operation);
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showNotification(message, type = 'info') {
        // Remove existing notifications
        document.querySelectorAll('.notification').forEach(n => n.remove());
        
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        document.body.appendChild(notification);
        
        // Auto remove after 3 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 3000);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    formatTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;
        
        if (diff < 60000) return '刚刚';
        if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
        if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
        return date.toLocaleDateString('zh-CN');
    }
}

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    const postId = window.postId || document.querySelector('[data-post-id]')?.dataset.postId;
    const userId = window.userId || localStorage.getItem('userId');
    
    if (postId) {
        window.postInteractionManager = new PostInteractionManager(postId, userId);
    }
});