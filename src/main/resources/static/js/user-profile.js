// 获取URL中的用户ID和当前登录用户ID
const targetUserId = location.pathname.match(/\/blog\/(\d+)\/profile/)?.[1];
const currentUserId = AuthUtils.getUserId();

console.log('Target User ID:', targetUserId);
console.log('Current User ID:', currentUserId);

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', async function () {
  // 初始化认证状态（如果用户已登录）
  if (AuthUtils.isLoggedIn()) {
    await AuthUtils.initAuth();
  }
  
  if (targetUserId) {
    await loadUserProfile();
    initializeTabs();
    await loadUserPosts();
    
    // 如果当前用户已登录且不是查看自己的主页，显示关注按钮
    if (currentUserId && currentUserId !== targetUserId) {
      initializeFollowButton();
    }
  }
});

// 加载用户资料
async function loadUserProfile() {
  try {
    console.log('Fetching profile for user ID:', targetUserId);
    const response = await fetch(`/blog/users/${targetUserId}/profile`);
    
    if (!response.ok) {
      throw new Error('Network response was not ok: ' + response.status);
    }
    
    const data = await response.json();
    console.log('Profile data received:', data);
    
    // 更新个人资料页面
    document.getElementById('profileUsername').textContent = data?.username || '用户';
    document.getElementById('profileBio').textContent = data?.bio || '这个人很懒，什么都没有留下...';
    document.getElementById('profileFollowing').textContent = data?.followingCount || 0;
    document.getElementById('profileFollowers').textContent = data?.followersCount || 0;
    document.getElementById('profileLiked').textContent = data?.likedCount || 0;
    document.getElementById('profileAvatar').src = data?.avatarUrl || '/blog/static/images/noface.gif';
    
  } catch (error) {
    console.error('Failed to load user profile:', error);
    showNotification('加载用户资料失败', 'error');
  }
}

// 初始化标签页切换
function initializeTabs() {
  const tabs = document.querySelectorAll('.profile-tab');
  const contents = document.querySelectorAll('.tab-content');
  
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      const targetId = tab.dataset.tab;
      
      // 更新标签页状态
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      
      // 更新内容显示
      contents.forEach(content => {
        content.classList.remove('active');
        if (content.id === targetId) {
          content.classList.add('active');
        }
      });
      
      // 根据标签页加载对应内容
      switch (targetId) {
        case 'postsTabContent':
          loadUserPosts();
          break;
        case 'followsTabContent':
          loadFollowedUsers();
          break;
      }
    });
  });
}

// 加载用户的帖子
async function loadUserPosts() {
  const container = document.getElementById('userPostsList');
  container.innerHTML = '<div class="loading-spinner"></div> 正在加载...';
  
  try {
    const response = await fetch(`/blog/api/posts/mine?userId=${targetUserId}`);
    const posts = await response.json();
    
    if (!posts || posts.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">📝</div>
          <div class="empty-state-text">还没有发布任何帖子</div>
          <div class="empty-state-hint">TA还没有分享任何内容</div>
        </div>
      `;
      return;
    }
    
    container.innerHTML = posts.map(post => `
      <div class="post-card">
        <div class="post-title">
          <a href="/blog/post/${post.id}">${escapeHtml(post.title)}</a>
        </div>
        <div class="post-meta">
          <span>📅 ${formatTime(post.createdAt)}</span>
          <span>❤️ ${post.likeCount || 0} 赞</span>
          <span>⭐ ${post.collectCount || 0} 收藏</span>
          <span>💬 ${post.commentCount || 0} 评论</span>
        </div>
        <div class="post-summary">
          ${escapeHtml(post.content ? post.content.slice(0, 150) + (post.content.length > 150 ? '...' : '') : '')}
        </div>
      </div>
    `).join('');
    
  } catch (error) {
    console.error('Failed to load user posts:', error);
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">❌</div>
        <div class="empty-state-text">加载失败</div>
        <div class="empty-state-hint">请刷新页面重试</div>
      </div>
    `;
  }
}

// 加载关注的用户
async function loadFollowedUsers() {
  const container = document.getElementById('followedUsersList');
  container.innerHTML = '<div class="loading-spinner"></div> 正在加载...';
  
  try {
    const response = await fetch(`/blog/users/${targetUserId}/followed-users`);
    const users = await response.json();
    
    if (!users || users.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">👥</div>
          <div class="empty-state-text">还没有关注任何人</div>
          <div class="empty-state-hint">TA还没有关注其他用户</div>
        </div>
      `;
      return;
    }
    
    container.innerHTML = users.map(user => `
      <div class="user-card">
        <div class="user-info">
          <img src="${user.avatarUrl || '/blog/static/images/noface.gif'}" alt="用户头像" class="user-avatar">
          <div class="user-details">
            <div class="user-name">
              <a href="/blog/users/${user.id}/profile">${escapeHtml(user.username)}</a>
            </div>
            <div class="user-bio">${escapeHtml(user.bio || '这个人很懒，什么都没有留下...')}</div>
            <div class="user-stats">
              <span>粉丝 ${user.followersCount || 0}</span>
              <span>关注 ${user.followingCount || 0}</span>
            </div>
          </div>
        </div>
      </div>
    `).join('');
    
  } catch (error) {
    console.error('Failed to load followed users:', error);
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">❌</div>
        <div class="empty-state-text">加载失败</div>
        <div class="empty-state-hint">请刷新页面重试</div>
      </div>
    `;
  }
}

// 初始化关注按钮
function initializeFollowButton() {
  const followBtn = document.getElementById('followBtn');
  followBtn.style.display = 'inline-block';
  followBtn.addEventListener('click', handleFollow);
  
  // 加载关注状态
  loadFollowStatus();
}

// 加载关注状态
async function loadFollowStatus() {
  if (!currentUserId) return;
  
  try {
    const response = await fetch(`/blog/users/${targetUserId}/follow-status?userId=${currentUserId}`);
    if (response.ok) {
      const data = await response.json();
      if (data.success) {
        const followBtn = document.getElementById('followBtn');
        if (followBtn) {
          updateFollowButton(followBtn, data.data);
        }
      }
    }
  } catch (error) {
    console.warn('加载关注状态失败:', error);
  }
}

// 处理关注
async function handleFollow(e) {
  e.preventDefault();
  if (!currentUserId) {
    showNotification('请先登录', 'error');
    return;
  }

  const button = e.currentTarget;
  const wasFollowed = button.classList.contains('active');
  
  // 立即更新UI
  updateFollowButton(button, !wasFollowed);

  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/users/${targetUserId}/follow?userId=${currentUserId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (data.success) {
      updateFollowButton(button, data.data.followed);
      showNotification(data.data.followed ? '关注成功' : '取消关注成功', 'success');
      
      // 更新粉丝数
      const followersElement = document.getElementById('profileFollowers');
      if (followersElement) {
        followersElement.textContent = data.data.followersCount;
      }
    } else {
      updateFollowButton(button, wasFollowed);
      showNotification(data.message || '关注操作失败', 'error');
    }
  } catch (error) {
    updateFollowButton(button, wasFollowed);
    showNotification('网络错误，请重试', 'error');
  }
}

// 更新关注按钮
function updateFollowButton(button, isFollowed) {
  if (isFollowed) {
    button.classList.add('active');
    button.innerHTML = '✅ <span class="text">已关注</span>';
  } else {
    button.classList.remove('active');
    button.innerHTML = '👤 <span class="text">关注</span>';
  }
}

// 工具函数
function escapeHtml(text) {
  if (!text) return '';
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

function showNotification(message, type = 'info') {
  // 移除现有通知
  document.querySelectorAll('.notification').forEach(n => n.remove());

  const notification = document.createElement('div');
  notification.className = `notification notification-${type}`;
  notification.textContent = message;
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 12px 20px;
    border-radius: 6px;
    color: white;
    font-size: 14px;
    z-index: 1000;
    max-width: 300px;
    word-wrap: break-word;
    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
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

function showError(message) {
  showNotification(message, 'error');
}

function showSuccess(message) {
  showNotification(message, 'success');
}