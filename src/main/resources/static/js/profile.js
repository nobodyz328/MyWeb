// 获取当前登录用户ID
const userId = localStorage.getItem('userId');

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function () {
  // 检查用户登录状态
  if (!userId) {
    console.error('No user ID found in localStorage');
    window.location.href = '/blog/login';
    return;
  }

  // 加载用户资料
  loadUserProfile();
  
  // 初始化标签页切换
  initializeTabs();
  
  // 初始化设置功能
  initializeSettings();
  
  // 默认加载我的帖子
  loadMyPosts();
});

// 加载用户资料
async function loadUserProfile() {
  try {
    console.log('Fetching profile for user ID:', userId);
    const response = await fetch(`/blog/users/${userId}/profile`);
    
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
    
    const avatarUrl = data?.avatarUrl || '/blog/static/images/noface.gif';
    document.getElementById('profileAvatar').src = avatarUrl;
    
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
          loadMyPosts();
          break;
        case 'collectsTabContent':
          loadCollectedPosts();
          break;
        case 'likedTabContent':
          loadLikedPosts();
          break;
        case 'followsTabContent':
          loadFollowedUsers();
          break;
        case 'settingsTabContent':
          // 设置页面不需要额外加载
          break;
      }
    });
  });
}

// 加载我的帖子
async function loadMyPosts() {
  const container = document.getElementById('myPostsList');
  container.innerHTML = '<div class="loading-spinner"></div> 正在加载...';
  
  try {
    const response = await fetch(`/blog/api/posts/mine?userId=${userId}`);
    const posts = await response.json();
    
    if (!posts || posts.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">📝</div>
          <div class="empty-state-text">还没有发布任何帖子</div>
          <div class="empty-state-hint">点击上方按钮开始创作你的第一篇帖子吧！</div>
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
        <div class="post-actions">
          <button class="post-action-btn" onclick="editPost(${post.id})">✏️ 编辑</button>
          <button class="post-action-btn danger" onclick="deletePost(${post.id})">🗑️ 删除</button>
        </div>
      </div>
    `).join('');
    
  } catch (error) {
    console.error('Failed to load my posts:', error);
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">❌</div>
        <div class="empty-state-text">加载失败</div>
        <div class="empty-state-hint">请刷新页面重试</div>
      </div>
    `;
  }
}

// 加载收藏的帖子
async function loadCollectedPosts() {
  const container = document.getElementById('collectedPostsList');
  container.innerHTML = '<div class="loading-spinner"></div> 正在加载...';
  
  try {
    const response = await fetch(`/blog/api/posts/collected?userId=${userId}`);
    const posts = await response.json();
    
    if (!posts || posts.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">⭐</div>
          <div class="empty-state-text">还没有收藏任何帖子</div>
          <div class="empty-state-hint">去发现一些有趣的内容收藏起来吧！</div>
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
          <span>👤 ${escapeHtml(post.author?.username || '未知作者')}</span>
          <span>📅 ${formatTime(post.createdAt)}</span>
          <span>❤️ ${post.likeCount || 0} 赞</span>
          <span>💬 ${post.commentCount || 0} 评论</span>
        </div>
        <div class="post-summary">
          ${escapeHtml(post.content ? post.content.slice(0, 150) + (post.content.length > 150 ? '...' : '') : '')}
        </div>
        <div class="post-actions">
          <button class="post-action-btn danger" onclick="uncollectPost(${post.id})">💔 取消收藏</button>
        </div>
      </div>
    `).join('');
    
  } catch (error) {
    console.error('Failed to load collected posts:', error);
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">❌</div>
        <div class="empty-state-text">加载失败</div>
        <div class="empty-state-hint">请刷新页面重试</div>
      </div>
    `;
  }
}

// 加载点赞的帖子
async function loadLikedPosts() {
  const container = document.getElementById('likedPostsList');
  container.innerHTML = '<div class="loading-spinner"></div> 正在加载...';
  
  try {
    const response = await fetch(`/blog/api/posts/liked?userId=${userId}`);
    const posts = await response.json();
    
    if (!posts || posts.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">❤️</div>
          <div class="empty-state-text">还没有点赞任何帖子</div>
          <div class="empty-state-hint">去给喜欢的内容点个赞吧！</div>
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
          <span>👤 ${escapeHtml(post.author?.username || '未知作者')}</span>
          <span>📅 ${formatTime(post.createdAt)}</span>
          <span>❤️ ${post.likeCount || 0} 赞</span>
          <span>💬 ${post.commentCount || 0} 评论</span>
        </div>
        <div class="post-summary">
          ${escapeHtml(post.content ? post.content.slice(0, 150) + (post.content.length > 150 ? '...' : '') : '')}
        </div>
      </div>
    `).join('');
    
  } catch (error) {
    console.error('Failed to load liked posts:', error);
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">❌</div>
        <div class="empty-state-text">加载失败</div>
        <div class="empty-state-hint">请刷新页面重试</div>
      </div>
    `;
  }
}

// 加载关注的用户（占位功能）
function loadFollowedUsers() {
  const container = document.getElementById('followedUsersList');
  container.innerHTML = `
    <div class="empty-state">
      <div class="empty-state-icon">👥</div>
      <div class="empty-state-text">关注功能</div>
      <div class="empty-state-hint">此功能正在开发中，敬请期待！</div>
    </div>
  `;
}

// 初始化设置功能
function initializeSettings() {
  // 邮箱绑定功能
  const getCodeBtn = document.getElementById('getBindCodeBtn');
  const bindForm = document.getElementById('emailBindForm');
  
  if (getCodeBtn) {
    getCodeBtn.addEventListener('click', handleGetEmailCode);
  }
  
  if (bindForm) {
    bindForm.addEventListener('submit', handleEmailBind);
  }
}

// 处理获取邮箱验证码
async function handleGetEmailCode() {
  const emailInput = document.getElementById('bindEmailInput');
  const statusDiv = document.getElementById('emailBindStatus');
  const button = document.getElementById('getBindCodeBtn');
  
  const email = emailInput.value.trim();
  if (!email) {
    showStatus(statusDiv, '请输入邮箱地址', 'error');
    return;
  }
  
  if (!isValidEmail(email)) {
    showStatus(statusDiv, '请输入有效的邮箱地址', 'error');
    return;
  }
  
  button.disabled = true;
  button.textContent = '发送中...';
  
  try {
    const response = await fetch(`/blog/users/${userId}/bind-email/code?email=${encodeURIComponent(email)}`, {
      method: 'POST'
    });
    
    if (response.ok) {
      showStatus(statusDiv, '验证码已发送到邮箱，请查收', 'success');
      startCountdown(button);
    } else {
      showStatus(statusDiv, '发送失败，请重试', 'error');
      button.disabled = false;
      button.textContent = '获取验证码';
    }
  } catch (error) {
    console.error('Failed to send email code:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
    button.disabled = false;
    button.textContent = '获取验证码';
  }
}

// 处理邮箱绑定
async function handleEmailBind(e) {
  e.preventDefault();
  
  const emailInput = document.getElementById('bindEmailInput');
  const codeInput = document.getElementById('bindEmailCode');
  const statusDiv = document.getElementById('emailBindStatus');
  const submitBtn = e.target.querySelector('button[type="submit"]');
  
  const email = emailInput.value.trim();
  const code = codeInput.value.trim();
  
  if (!email || !code) {
    showStatus(statusDiv, '请填写完整信息', 'error');
    return;
  }
  
  submitBtn.disabled = true;
  submitBtn.textContent = '绑定中...';
  
  try {
    const response = await fetch(`/blog/users/${userId}/bind-email`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, code })
    });
    
    if (response.ok) {
      showStatus(statusDiv, '邮箱绑定成功！', 'success');
      emailInput.value = '';
      codeInput.value = '';
    } else {
      showStatus(statusDiv, '绑定失败，请检查验证码', 'error');
    }
  } catch (error) {
    console.error('Failed to bind email:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = '绑定邮箱';
  }
}

// 编辑帖子
function editPost(postId) {
  window.location.href = `/blog/posts/edit/${postId}`;
}

// 删除帖子
async function deletePost(postId) {
  if (!confirm('确定要删除这个帖子吗？此操作不可撤销。')) {
    return;
  }
  
  try {
    const response = await fetch(`/blog/api/posts/${postId}?userId=${userId}`, {
      method: 'DELETE'
    });
    
    if (response.ok) {
      showNotification('帖子删除成功', 'success');
      loadMyPosts(); // 重新加载帖子列表
    } else {
      showNotification('删除失败，请重试', 'error');
    }
  } catch (error) {
    console.error('Failed to delete post:', error);
    showNotification('网络错误，请重试', 'error');
  }
}

// 取消收藏帖子
async function uncollectPost(postId) {
  if (!confirm('确定要取消收藏这个帖子吗？')) {
    return;
  }
  
  try {
    const response = await fetch(`/blog/api/posts/${postId}/collect?userId=${userId}`, {
      method: 'POST'
    });
    
    const data = await response.json();
    if (data.success) {
      showNotification('取消收藏成功', 'success');
      loadCollectedPosts(); // 重新加载收藏列表
    } else {
      showNotification('操作失败，请重试', 'error');
    }
  } catch (error) {
    console.error('Failed to uncollect post:', error);
    showNotification('网络错误，请重试', 'error');
  }
}

// 退出登录
function logout() {
  if (confirm('确定要退出登录吗？')) {
    localStorage.removeItem('userId');
    window.location.href = '/blog/view';
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

function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

function showStatus(element, message, type) {
  element.className = `status-message status-${type}`;
  element.textContent = message;
  element.style.display = 'block';
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

function startCountdown(button) {
  let count = 60;
  const timer = setInterval(() => {
    button.textContent = `${count}秒后重试`;
    count--;
    
    if (count < 0) {
      clearInterval(timer);
      button.disabled = false;
      button.textContent = '获取验证码';
    }
  }, 1000);
}