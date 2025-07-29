// 获取帖子ID和用户ID
const postId = location.pathname.match(/\/post\/(\d+)/)?.[1];
const userId = localStorage.getItem('userId');

console.log('Post ID:', postId);
console.log('User ID:', userId);

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function () {
  if (postId) {
    loadPostDetail();
  } else {
    showError('帖子ID无效');
  }
});

// 加载帖子详情
async function loadPostDetail() {
  try {
    const response = await fetch(`/blog/api/posts/${postId}`);
    if (!response.ok) {
      throw new Error('帖子不存在');
    }
    
    const post = await response.json();
    renderPostDetail(post);
    
    // 加载评论
    loadComments();
    
    // 初始化交互功能
    initializeInteractions();
    
  } catch (error) {
    console.error('加载帖子详情失败:', error);
    showError('加载失败，请刷新重试');
  }
}

// 渲染帖子详情
function renderPostDetail(post) {
  // 隐藏加载状态，显示内容
  document.getElementById('loadingState').style.display = 'none';
  document.getElementById('postContentWrapper').classList.add('loaded');
  
  // 填充帖子信息
  document.getElementById('postTitle').textContent = post.title || '';
  document.getElementById('postAuthorName').textContent = post.author?.username || '未知作者';
  document.getElementById('postTime').textContent = formatTime(post.createdAt);
  document.getElementById('postContent').textContent = post.content || '';
  
  // 设置作者头像
  document.getElementById('postAuthorAvatar').src = post.author?.avatarUrl || '/blog/static/images/noface.gif';
  
  // 更新统计数据
  document.getElementById('postViewCount').textContent = post.viewCount || 0;
  document.getElementById('postLikeCount').textContent = post.likeCount || 0;
  document.getElementById('postCollectCount').textContent = post.collectCount || 0;
  document.getElementById('postCommentCount').textContent = post.commentCount || 0;
  document.getElementById('commentCountBtn').textContent = post.commentCount || 0;
  
  // 更新交互按钮
  const likeBtn = document.getElementById('likeBtn');
  const bookmarkBtn = document.getElementById('bookmarkBtn');
  likeBtn.dataset.postId = post.id;
  bookmarkBtn.dataset.postId = post.id;
  
  // 初始化点赞按钮状态
  updateLikeButton(likeBtn, false, post.likeCount || 0);
  
  // 处理图片
  if (post.images && post.images.length > 0) {
    const imagesContainer = document.getElementById('postImages');
    imagesContainer.style.display = 'grid';
    imagesContainer.innerHTML = post.images.map(img => 
      `<img src="${img}" alt="帖子图片" onclick="openImageModal('${img}')">`
    ).join('');
  }
  
  // 检查是否为作者，显示编辑删除按钮
  const isAuthor = userId && post.author && post.author.id == userId;
  if (isAuthor) {
    const authorActions = document.getElementById('authorActions');
    authorActions.style.display = 'flex';
    authorActions.style.gap = '12px';
    
    document.getElementById('editBtn').onclick = () => editPost(post.id);
    document.getElementById('deleteBtn').onclick = () => deletePost(post.id);
  }
  
  // 设置评论表单显示状态
  if (userId) {
    document.getElementById('commentForm').style.display = 'block';
    document.getElementById('loginPrompt').style.display = 'none';
  } else {
    document.getElementById('commentForm').style.display = 'none';
    document.getElementById('loginPrompt').style.display = 'block';
  }
}

// 初始化交互功能
function initializeInteractions() {
  // 点赞功能
  const likeBtn = document.getElementById('likeBtn');
  if (likeBtn && userId) {
    likeBtn.addEventListener('click', handleLike);
  }

  // 收藏功能
  const bookmarkBtn = document.getElementById('bookmarkBtn');
  if (bookmarkBtn && userId) {
    bookmarkBtn.addEventListener('click', handleBookmark);
  }

  // 评论表单
  const commentForm = document.getElementById('commentForm');
  if (commentForm && userId) {
    commentForm.addEventListener('submit', handleCommentSubmit);
  }

  // 加载用户交互状态
  if (userId) {
    loadUserInteractionStatus();
  }
}

// 处理点赞
async function handleLike(e) {
  e.preventDefault();
  if (!userId) {
    showError('请先登录');
    return;
  }

  const button = e.currentTarget;
  const wasLiked = button.classList.contains('active');
  
  // 立即更新UI
  updateLikeButton(button, !wasLiked, null);

  try {
    const response = await fetch(`/blog/api/posts/${postId}/like?userId=${userId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (data.success) {
      updateLikeButton(button, data.data.liked, data.data.likeCount);
      updatePostStat('❤️', data.data.likeCount);
    } else {
      updateLikeButton(button, wasLiked);
      showError(data.message || '点赞操作失败');
    }
  } catch (error) {
    updateLikeButton(button, wasLiked);
    showError('网络错误，请重试');
  }
}

// 处理收藏
async function handleBookmark(e) {
  e.preventDefault();
  if (!userId) {
    showError('请先登录');
    return;
  }

  const button = e.currentTarget;
  const wasBookmarked = button.classList.contains('active');
  
  // 立即更新UI
  updateBookmarkButton(button, !wasBookmarked);

  try {
    const response = await fetch(`/blog/api/posts/${postId}/collect?userId=${userId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (data.success) {
      updateBookmarkButton(button, data.data.collected, data.data.collectCount);
      updatePostStat('⭐', data.data.collectCount);
    } else {
      updateBookmarkButton(button, wasBookmarked);
      showError(data.message || '收藏操作失败');
    }
  } catch (error) {
    updateBookmarkButton(button, wasBookmarked);
    showError('网络错误，请重试');
  }
}

// 处理评论提交
async function handleCommentSubmit(e) {
  e.preventDefault();
  if (!userId) {
    showError('请先登录');
    return;
  }

  const form = e.target;
  const textarea = form.querySelector('textarea');
  const content = textarea.value.trim();
  
  if (!content) {
    showError('评论内容不能为空');
    return;
  }

  const submitBtn = form.querySelector('button[type="submit"]');
  const originalText = submitBtn.textContent;
  submitBtn.textContent = '发表中...';
  submitBtn.disabled = true;

  try {
    const response = await fetch(`/blog/api/posts/${postId}/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        content: content,
        postId: postId,
        userId: userId
      })
    });

    const data = await response.json();

    if (data.success) {
      textarea.value = '';
      showSuccess('评论发表成功');
      loadComments(); // 重新加载评论列表
      
      // 更新评论计数
      const currentCount = parseInt(document.getElementById('postCommentCount').textContent) || 0;
      updatePostStat('💬', currentCount + 1);
    } else {
      showError(data.message || '评论发表失败');
    }
  } catch (error) {
    showError('网络错误，请重试');
  } finally {
    submitBtn.textContent = originalText;
    submitBtn.disabled = false;
  }
}

// 加载评论列表
async function loadComments() {
  try {
    const response = await fetch(`/blog/api/posts/${postId}/comments`);
    const data = await response.json();

    const commentsList = document.getElementById('commentsList');
    if (!data || data.length === 0) {
      commentsList.innerHTML = '<div class="empty-comments">暂无评论，来发表第一条评论吧！</div>';
      return;
    }

    commentsList.innerHTML = data.map(comment => renderComment(comment)).join('');
    
    // 绑定回复按钮事件
    bindReplyEvents();
    
  } catch (error) {
    console.error('加载评论失败:', error);
    document.getElementById('commentsList').innerHTML = 
      '<div class="empty-comments">加载评论失败，请刷新重试</div>';
  }
}

// 渲染评论
function renderComment(comment, isReply = false) {
  return `
    <div class="comment-card ${isReply ? 'reply-card' : ''}" data-comment-id="${comment.id}">
      <div class="comment-header">
        <div class="comment-author-info">
          <img src="${comment.author?.avatarUrl || '/blog/static/images/noface.gif'}" 
               alt="用户头像" class="comment-author-avatar">
          <div>
            <span class="comment-author">${escapeHtml(comment.author?.username || '匿名用户')}</span>
          </div>
        </div>
        <span class="comment-time">${formatTime(comment.createdAt)}</span>
      </div>
      <div class="comment-content">${escapeHtml(comment.content)}</div>
      ${!isReply && userId ? `
        <div class="comment-actions">
          <button data-action="reply" data-comment-id="${comment.id}" class="reply-btn">回复</button>
        </div>
      ` : ''}
      ${comment.replies && comment.replies.length > 0 ? `
        <div class="replies-container">
          ${comment.replies.map(reply => renderComment(reply, true)).join('')}
        </div>
      ` : ''}
    </div>
  `;
}

// 绑定回复事件
function bindReplyEvents() {
  document.querySelectorAll('[data-action="reply"]').forEach(btn => {
    btn.addEventListener('click', handleReply);
  });
}

// 处理回复
function handleReply(e) {
  const commentId = e.target.dataset.commentId;
  const commentCard = document.querySelector(`[data-comment-id="${commentId}"]`);
  
  // 移除其他回复表单
  document.querySelectorAll('.reply-form').forEach(form => form.remove());
  
  const replyForm = document.createElement('div');
  replyForm.className = 'reply-form';
  replyForm.innerHTML = `
    <div class="reply-form-container">
      <textarea placeholder="写下你的回复..." class="reply-textarea"></textarea>
      <div class="reply-actions">
        <button type="button" onclick="submitReply('${commentId}', this)" class="steam-btn steam-btn-primary">回复</button>
        <button type="button" onclick="cancelReply(this)" class="steam-btn steam-btn-secondary">取消</button>
      </div>
    </div>
  `;
  
  commentCard.appendChild(replyForm);
  replyForm.querySelector('textarea').focus();
}

// 提交回复
async function submitReply(commentId, button) {
  if (!userId) {
    showError('请先登录');
    return;
  }

  const replyForm = button.closest('.reply-form');
  const textarea = replyForm.querySelector('textarea');
  const content = textarea.value.trim();
  
  if (!content) {
    showError('回复内容不能为空');
    return;
  }

  button.textContent = '回复中...';
  button.disabled = true;

  try {
    const response = await fetch(`/blog/api/posts/${postId}/comments/${commentId}/replies`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        content: content,
        parentCommentId: commentId,
        postId: postId,
        userId: userId
      })
    });

    const data = await response.json();

    if (data.success) {
      showSuccess('回复发表成功');
      replyForm.remove();
      loadComments(); // 重新加载评论列表
      
      // 更新评论计数
      const currentCount = parseInt(document.getElementById('postCommentCount').textContent) || 0;
      updatePostStat('💬', currentCount + 1);
    } else {
      showError(data.message || '回复发表失败');
    }
  } catch (error) {
    showError('网络错误，请重试');
  } finally {
    button.textContent = '回复';
    button.disabled = false;
  }
}

// 取消回复
function cancelReply(button) {
  button.closest('.reply-form').remove();
}

// 加载用户交互状态
async function loadUserInteractionStatus() {
  if (!userId) return;

  try {
    // 获取点赞状态
    const likeResponse = await fetch(`/blog/api/posts/${postId}/like-status?userId=${userId}`);
    if (likeResponse.ok) {
      const likeData = await likeResponse.json();
      if (likeData.success) {
        const likeBtn = document.getElementById('likeBtn');
        if (likeBtn) {
          const currentCount = parseInt(document.getElementById('postLikeCount').textContent) || 0;
          updateLikeButton(likeBtn, likeData.data, currentCount);
        }
      }
    }

    // 获取收藏状态
    const collectResponse = await fetch(`/blog/api/posts/${postId}/collect-status?userId=${userId}`);
    if (collectResponse.ok) {
      const collectData = await collectResponse.json();
      if (collectData.success) {
        const bookmarkBtn = document.getElementById('bookmarkBtn');
        if (bookmarkBtn) {
          updateBookmarkButton(bookmarkBtn, collectData.data);
        }
      }
    }
    
  } catch (error) {
    console.warn('加载用户交互状态失败:', error);
  }
}

// 更新点赞按钮
function updateLikeButton(button, isLiked, Count) {
  if (isLiked) {
    button.classList.add('active');
    button.innerHTML = '❤️<span class="count">' + (Count !== null ? Count : (parseInt(button.querySelector('.count')?.textContent || '0'))) + '</span>';
  } else {
    button.classList.remove('active');
    button.innerHTML = '🤍 <span class="count">' + (Count !== null ? Count :(parseInt(button.querySelector('.count')?.textContent || '0'))) + '</span>';
  }
}

// 更新收藏按钮
function updateBookmarkButton(button, isBookmarked, count = null) {
  if (isBookmarked) {
    button.classList.add('active');
    button.innerHTML = '⭐ <span class="text">已收藏</span>';
  } else {
    button.classList.remove('active');
    button.innerHTML = '☆ <span class="text">收藏</span>';
  }
}

// 更新帖子统计数据
function updatePostStat(icon, count) {
  if (icon === '❤️') {
    document.getElementById('postLikeCount').textContent = count;
  } else if (icon === '⭐') {
    document.getElementById('postCollectCount').textContent = count;
  } else if (icon === '💬') {
    document.getElementById('postCommentCount').textContent = count;
    document.getElementById('commentCountBtn').textContent = count;
  }
}

// 滚动到评论区
function scrollToComments() {
  const commentSection = document.getElementById('commentSection');
  if (commentSection) {
    commentSection.scrollIntoView({ behavior: 'smooth' });
  }
}

// 编辑帖子
function editPost(id) {
  window.location.href = `/blog/posts/edit/${id}`;
}

// 删除帖子
async function deletePost(id) {
  if (!confirm('确定要删除这个帖子吗？此操作不可撤销。')) {
    return;
  }

  try {
    const response = await fetch(`/blog/api/posts/${id}?userId=${userId}`, {
      method: 'DELETE'
    });

    if (response.ok) {
      showSuccess('帖子删除成功');
      setTimeout(() => {
        window.location.href = '/blog/view';
      }, 1500);
    } else {
      showError('删除失败，请重试');
    }
  } catch (error) {
    showError('网络错误，请重试');
  }
}

// 打开图片模态框
function openImageModal(src) {
  // 简单的图片查看功能
  const modal = document.createElement('div');
  modal.style.cssText = `
    position: fixed; top: 0; left: 0; width: 100%; height: 100%; 
    background: rgba(0,0,0,0.8); z-index: 10000; display: flex; 
    align-items: center; justify-content: center; cursor: pointer;
  `;
  
  const img = document.createElement('img');
  img.src = src;
  img.style.cssText = 'max-width: 90%; max-height: 90%; border-radius: 8px;';
  
  modal.appendChild(img);
  document.body.appendChild(modal);
  
  modal.addEventListener('click', () => modal.remove());
}

// 工具函数
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function formatTime(timestamp) {
  if (!timestamp) return '';
  
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;
  
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)}天前`;
  
  return date.toLocaleDateString('zh-CN');
}

function showError(message) {
  showNotification(message, 'error');
}

function showSuccess(message) {
  showNotification(message, 'success');
}

function showNotification(message, type = 'info') {
  // 移除现有通知
  document.querySelectorAll('.notification').forEach(n => n.remove());

  const notification = document.createElement('div');
  notification.className = `notification notification-${type}`;
  notification.textContent = message;
  notification.style.cssText = `
    position: fixed; top: 20px; right: 20px; padding: 12px 20px;
    border-radius: 6px; color: white; font-size: 14px; z-index: 1000;
    max-width: 300px; word-wrap: break-word; box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  `;

  if (type === 'success') {
    notification.style.background = '#4caf50';
    notification.style.border = '1px solid #45a049';
  } else if (type === 'error') {
    notification.style.background = '#f44336';
    notification.style.border = '1px solid #da190b';
  } else {
    notification.style.background = '#2196f3';
    notification.style.border = '1px solid #1976d2';
  }

  document.body.appendChild(notification);

  setTimeout(() => {
    if (notification.parentNode) {
      notification.remove();
    }
  }, 3000);
}