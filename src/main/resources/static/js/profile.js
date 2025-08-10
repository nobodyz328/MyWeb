// 获取当前登录用户ID
const userId = AuthUtils.getUserId();

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', async function () {
  // 检查用户登录状态
  if (!AuthUtils.requireAuth()) {
    return;
  }
  
  // 初始化认证状态
  await AuthUtils.initAuth();

  // 加载用户资料
  loadUserProfile();
  
  // 初始化标签页切换
  initializeTabs();
  
  // 初始化设置功能
  initializeSettings();
  
  // 初始化TOTP功能
  initializeTOTP();
  
  // 默认加载我的帖子
  loadMyPosts();
});

// 加载用户资料
async function loadUserProfile() {
  try {
    console.log('Fetching profile for user ID:', userId);
    const response = await AuthUtils.authenticatedFetch(`/blog/users/${userId}/profile`);
    
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
    const response = await AuthUtils.authenticatedFetch(`/blog/api/posts/mine?userId=${userId}`);
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
    const response = await AuthUtils.authenticatedFetch(`/blog/api/posts/collected?userId=${userId}`);
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
    const response = await AuthUtils.authenticatedFetch(`/blog/api/posts/liked?userId=${userId}`);
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
  // 加载用户设置信息
  loadUserSettings();
  
  // 个人信息表单
  const basicInfoForm = document.getElementById('basicInfoForm');
  if (basicInfoForm) {
    basicInfoForm.addEventListener('submit', handleUpdateBasicInfo);
  }
  
  // 邮箱绑定功能
  const getCodeBtn = document.getElementById('getBindCodeBtn');
  const bindForm = document.getElementById('emailBindForm');
  
  if (getCodeBtn) {
    getCodeBtn.addEventListener('click', handleGetEmailCode);
  }
  
  if (bindForm) {
    bindForm.addEventListener('submit', handleEmailBind);
  }
  
  // 密码修改功能
  const passwordForm = document.getElementById('changePasswordForm');
  if (passwordForm) {
    passwordForm.addEventListener('submit', handleChangePassword);
  }
  
  // TOTP相关功能
  initializeTOTPSettings();
  
  // 管理员访问功能
  const adminForm = document.getElementById('adminAccessForm');
  if (adminForm) {
    adminForm.addEventListener('submit', handleAdminAccess);
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
    const response = await AuthUtils.authenticatedFetch(`/blog/api/posts/${postId}?userId=${userId}`, {
      method: 'DELETE'
    });
    
    if (response.ok) {
      showNotification('帖子删除成功', 'success');
      await loadMyPosts(); // 重新加载帖子列表
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
    const response = await AuthUtils.authenticatedFetch(`/blog/api/posts/${postId}/collect?userId=${userId}`, {
      method: 'POST'
    });
    
    const data = await response.json();
    if (data.success) {
      showNotification('取消收藏成功', 'success');
      await loadCollectedPosts(); // 重新加载收藏列表
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
    AuthUtils.logout();
  }
}

// ==================== TOTP 二次验证功能 ====================

// 加载TOTP状态
async function loadTOTPStatus() {
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/status`);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const data = await response.json();
    console.log('TOTP status response:', data);
    
    // 检查响应格式
      if (data.success) {
        updateTOTPUI(data.data);
      } else {
        showTOTPError('加载TOTP状态失败: ' + (data.message || '未知错误'));
      }
  } catch (error) {
    console.error('Failed to load TOTP status:', error);
    showTOTPError('网络错误，请重试: ' + error.message);
  }
}

// 更新TOTP界面
function updateTOTPUI(status) {
  const totpStatus = document.getElementById('totpStatus');
  const disabledSection = document.getElementById('totpDisabledSection');
  const setupSection = document.getElementById('totpSetupSection');
  const enabledSection = document.getElementById('totpEnabledSection');
  const requiredNotice = document.getElementById('totpRequiredNotice');
  const disableBtn = document.getElementById('disableTotpBtn');
  
  // 隐藏加载状态
  totpStatus.style.display = 'none';
  
  if (status.enabled) {
    // TOTP已启用
    disabledSection.style.display = 'none';
    setupSection.style.display = 'none';
    enabledSection.style.display = 'block';
    
    // 更新剩余时间
    updateTOTPRemainingTime(status.remainingTime);
    
    // 管理员不能禁用TOTP
    if (!status.required) {
      disableBtn.style.display = 'inline-block';
    }
  } else if (status.configured) {
    // TOTP已配置但未启用（设置过程中）
    disabledSection.style.display = 'none';
    setupSection.style.display = 'block';
    enabledSection.style.display = 'none';
    
    // 加载二维码
    loadTOTPQRCode();
  } else {
    // TOTP未配置
    disabledSection.style.display = 'block';
    setupSection.style.display = 'none';
    enabledSection.style.display = 'none';
    
    // 显示管理员必须启用的提示
    if (status.required) {
      requiredNotice.style.display = 'block';
    }
  }
}

// 设置TOTP
async function setupTOTP() {
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/setup`);
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    const data = await response.json();
    console.log('TOTP setup response:', data);
    
    // 检查响应格式
    let setupData = data;
    if (data.success !== undefined) {
      if (data.success) {
        setupData = data.data;
      } else {
        showTOTPError('设置TOTP失败: ' + (data.message || '未知错误'));
        return;
      }
    }
    
    // 验证必要的数据字段
    if (!setupData.secret) {
      showTOTPError('设置TOTP失败: 服务器未返回密钥');
      return;
    }
    
    // 显示设置界面
    document.getElementById('totpDisabledSection').style.display = 'none';
    document.getElementById('totpSetupSection').style.display = 'block';
    
    // 设置密钥
    document.getElementById('totpSecret').textContent = setupData.secret;
    
    // 加载二维码
    await loadTOTPQRCode();
    
  } catch (error) {
    console.error('Failed to setup TOTP:', error);
    showTOTPError('网络错误，请重试: ' + error.message);
  }
}


// 启用TOTP
async function enableTOTP(event) {
  event.preventDefault();
  
  const verificationCode = document.getElementById('totpVerificationCode').value;
  const secret = document.getElementById('totpSecret').textContent;
  
  if (!verificationCode || verificationCode.length !== 6) {
    showTOTPSetupStatus('请输入6位验证码', 'error');
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/enable`, {
      method: 'POST',
      body: JSON.stringify({
        secret: secret,
        verificationCode: verificationCode
      })
    });
    if (response.ok) {
      const data = await response.json();
      showTOTPSetupStatus(data.message, 'success');
      setTimeout(() => {
        loadTOTPStatus(); // 重新加载状态
      }, 1000);
    } else {
      showTOTPSetupStatus('启用失败: ', 'error');
    }
    

  } catch (error) {
    console.error('Failed to enable TOTP:', error);
    showTOTPSetupStatus('网络错误，请重试', 'error');
  }
}


// 测试TOTP
async function testTOTP(event) {
  event.preventDefault();
  
  const testCode = document.getElementById('testTotpCode').value;
  
  if (!testCode || testCode.length !== 6) {
    showNotification('请输入6位验证码', 'error');
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/verify`, {
      method: 'POST',
      body: JSON.stringify({
        verificationCode: testCode
      })
    });
    
    const data = await response.json();
    
    if (data.success) {
      showNotification('验证码正确！', 'success');
      document.getElementById('testTotpCode').value = '';
      document.getElementById('testTotpSection').style.display = 'none';
    } else {
      showNotification('验证码错误: ' + data.message, 'error');
    }
  } catch (error) {
    console.error('Failed to test TOTP:', error);
    showNotification('网络错误，请重试', 'error');
  }
}

// 禁用TOTP
async function disableTOTP() {
  const verificationCode = prompt('请输入当前的6位验证码以确认禁用TOTP:');
  
  if (!verificationCode || verificationCode.length !== 6) {
    showNotification('请输入有效的6位验证码', 'error');
    return;
  }
  
  if (!confirm('确定要禁用二次验证吗？这会降低您账户的安全性。')) {
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/disable`, {
      method: 'POST',
      body: JSON.stringify({
        verificationCode: verificationCode
      })
    });
    
    const data = await response.json();
    
    if (data.success) {
      showNotification('TOTP已禁用', 'success');
      await loadTOTPStatus(); // 重新加载状态
    } else {
      showNotification('禁用失败: ' + data.message, 'error');
    }
  } catch (error) {
    console.error('Failed to disable TOTP:', error);
    showNotification('网络错误，请重试', 'error');
  }
}

// 重置TOTP密钥
async function resetTOTP() {
  const verificationCode = prompt('请输入当前的6位验证码以确认重置密钥:');
  
  if (!verificationCode || verificationCode.length !== 6) {
    showNotification('请输入有效的6位验证码', 'error');
    return;
  }
  
  if (!confirm('确定要重置TOTP密钥吗？您需要重新设置验证器应用。')) {
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/reset`, {
      method: 'POST',
      body: JSON.stringify({
        verificationCode: verificationCode
      })
    });
    
    const data = await response.json();
    
    if (data.success) {
      showNotification('TOTP密钥已重置', 'success');
      await loadTOTPStatus(); // 重新加载状态
    } else {
      showNotification('重置失败: ' + data.message, 'error');
    }
  } catch (error) {
    console.error('Failed to reset TOTP:', error);
    showNotification('网络错误，请重试', 'error');
  }
}

// 更新TOTP剩余时间
function updateTOTPRemainingTime(remainingTime) {
  const element = document.getElementById('totpRemainingTime');
  if (element) {
    element.textContent = remainingTime || '--';
  }
}

// 显示TOTP错误
function showTOTPError(message) {
  const totpStatus = document.getElementById('totpStatus');
  totpStatus.innerHTML = `<div class="error">${message}</div>`;
  totpStatus.style.display = 'block';
}

// 显示TOTP设置状态
function showTOTPSetupStatus(message, type) {
  const statusElement = document.getElementById('totpSetupStatus');
  statusElement.className = `status-message status-${type}`;
  statusElement.textContent = message;
  statusElement.style.display = 'block';
}

// 初始化TOTP功能
function initializeTOTP() {
  // 加载TOTP状态
  loadTOTPStatus();
  
  // 绑定TOTP相关事件监听器
  const setupTotpBtn = document.getElementById('setupTotpBtn');
  if (setupTotpBtn) {
    setupTotpBtn.addEventListener('click', setupTOTP);
  }
  
  const enableTotpForm = document.getElementById('enableTotpForm');
  if (enableTotpForm) {
    enableTotpForm.addEventListener('submit', enableTOTP);
  }
  
  const cancelTotpSetup = document.getElementById('cancelTotpSetup');
  if (cancelTotpSetup) {
    cancelTotpSetup.addEventListener('click', cancelTOTPSetup);
  }
  
  const testTotpBtn = document.getElementById('testTotpBtn');
  if (testTotpBtn) {
    testTotpBtn.addEventListener('click', function() {
      document.getElementById('testTotpSection').style.display = 'block';
    });
  }
  
  const testTotpForm = document.getElementById('testTotpForm');
  if (testTotpForm) {
    testTotpForm.addEventListener('submit', testTOTP);
  }
  
  const cancelTestTotp = document.getElementById('cancelTestTotp');
  if (cancelTestTotp) {
    cancelTestTotp.addEventListener('click', function() {
      document.getElementById('testTotpSection').style.display = 'none';
      document.getElementById('testTotpCode').value = '';
    });
  }
  
  const resetTotpBtn = document.getElementById('resetTotpBtn');
  if (resetTotpBtn) {
    resetTotpBtn.addEventListener('click', resetTOTP);
  }
  
  const disableTotpBtn = document.getElementById('disableTotpBtn');
  if (disableTotpBtn) {
    disableTotpBtn.addEventListener('click', disableTOTP);
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

// ==================== 用户设置相关功能 ====================

// 加载用户设置信息
async function loadUserSettings() {
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings`);
    if (!response.ok) {
      throw new Error('Failed to load user settings');
    }
    
    const settings = await response.json();
    
    // 填充个人信息
    document.getElementById('userBio').value = settings.bio || '';
    document.getElementById('userAvatar').value = settings.avatarUrl || '';
    
    // 显示当前邮箱信息
    if (settings.email) {
      document.getElementById('currentEmail').textContent = settings.email;
      document.getElementById('currentEmailInfo').style.display = 'block';
      if (settings.emailVerified) {
        document.getElementById('emailVerifiedBadge').style.display = 'inline';
      }
    }
    
    // 显示安全信息
    document.getElementById('accountCreatedAt').textContent = formatTime(settings.createdAt);
    document.getElementById('lastLoginTime').textContent = settings.lastLoginTime ? formatTime(settings.lastLoginTime) : '从未登录';
    document.getElementById('lastLoginIp').textContent = settings.lastLoginIp || '--';
    
    // 根据用户角色显示相应功能
    if (settings.role === 'ADMIN' || settings.role === 'MODERATOR') {
      document.getElementById('adminAccessSection').style.display = 'block';
      if (settings.role === 'ADMIN') {
        document.getElementById('adminTotpNotice').style.display = 'block';
        document.getElementById('adminTotpCode').style.display = 'block';
      }
    }
    
    // 显示TOTP相关信息
    if (settings.totpEnabled) {
      document.getElementById('passwordTotpRow').style.display = 'block';
    }
    
    // 加载TOTP状态
    await loadTOTPStatus();
    
  } catch (error) {
    console.error('Failed to load user settings:', error);
    showNotification('加载用户设置失败', 'error');
  }
}

// 更新基本信息
async function handleUpdateBasicInfo(e) {
  e.preventDefault();
  
  const bio = document.getElementById('userBio').value.trim();
  const avatarUrl = document.getElementById('userAvatar').value.trim();
  const statusDiv = document.getElementById('basicInfoStatus');
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/basic`, {
      method: 'PUT',
      body: JSON.stringify({ bio, avatarUrl })
    });
    
    if (response.ok) {
      showStatus(statusDiv, '个人信息更新成功', 'success');
      // 刷新个人资料显示
      await loadUserProfile();
    } else {
      const error = await response.text();
      showStatus(statusDiv, error, 'error');
    }
  } catch (error) {
    console.error('Failed to update basic info:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  }
}

// 修改密码
async function handleChangePassword(e) {
  e.preventDefault();
  
  const currentPassword = document.getElementById('currentPassword').value;
  const newPassword = document.getElementById('newPassword').value;
  const confirmPassword = document.getElementById('confirmPassword').value;
  const totpCode = document.getElementById('passwordTotpCode').value;
  const statusDiv = document.getElementById('passwordChangeStatus');
  
  // 验证新密码
  if (newPassword !== confirmPassword) {
    showStatus(statusDiv, '两次输入的密码不一致', 'error');
    return;
  }
  
  if (newPassword.length < 8) {
    showStatus(statusDiv, '密码长度至少8位', 'error');
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/password`, {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword, totpCode })
    });
    
    if (response.ok) {
      showStatus(statusDiv, '密码修改成功', 'success');
      // 清空表单
      document.getElementById('changePasswordForm').reset();
    } else {
      const error = await response.text();
      showStatus(statusDiv, error, 'error');
    }
  } catch (error) {
    console.error('Failed to change password:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  }
}

// 处理邮箱绑定验证码发送
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
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/email/send-code?email=${encodeURIComponent(email)}`, {
      method: 'POST'
    });
    
    if (response.ok) {
      showStatus(statusDiv, '验证码已发送到邮箱，请查收', 'success');
      startCountdown(button);
    } else {
      const error = await response.text();
      showStatus(statusDiv, error, 'error');
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
  
  const email = document.getElementById('bindEmailInput').value.trim();
  const verificationCode = document.getElementById('bindEmailCode').value.trim();
  const totpCode = document.getElementById('bindEmailTotpCode').value.trim();
  const statusDiv = document.getElementById('emailBindStatus');
  
  if (!email || !verificationCode) {
    showStatus(statusDiv, '请填写完整信息', 'error');
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/email/bind`, {
      method: 'POST',
      body: JSON.stringify({ email, verificationCode, totpCode })
    });
    
    if (response.ok) {
      showStatus(statusDiv, '邮箱绑定成功！', 'success');
      // 清空表单并刷新设置
      document.getElementById('emailBindForm').reset();
      await loadUserSettings();
    } else {
      const error = await response.text();
      showStatus(statusDiv, error, 'error');
    }
  } catch (error) {
    console.error('Failed to bind email:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  }
}

// ==================== TOTP相关功能 ====================

// 初始化TOTP设置
function initializeTOTPSettings() {
  // 设置TOTP按钮
  const setupBtn = document.getElementById('setupTotpBtn');
  if (setupBtn) {
    setupBtn.addEventListener('click', startTOTPSetup);
  }
  
  // 启用TOTP表单
  const enableForm = document.getElementById('enableTotpForm');
  if (enableForm) {
    enableForm.addEventListener('submit', handleEnableTOTP);
  }
  
  // 取消设置按钮
  const cancelSetupBtn = document.getElementById('cancelTotpSetup');
  if (cancelSetupBtn) {
    cancelSetupBtn.addEventListener('click', cancelTOTPSetup);
  }
  
  // 测试TOTP按钮
  const testBtn = document.getElementById('testTotpBtn');
  if (testBtn) {
    testBtn.addEventListener('click', showTestTOTP);
  }
  
  // 测试TOTP表单
  const testForm = document.getElementById('testTotpForm');
  if (testForm) {
    testForm.addEventListener('submit', handleTestTOTP);
  }
  
  // 取消测试按钮
  const cancelTestBtn = document.getElementById('cancelTestTotp');
  if (cancelTestBtn) {
    cancelTestBtn.addEventListener('click', hideTestTOTP);
  }
  
  // 重置TOTP按钮
  const resetBtn = document.getElementById('resetTotpBtn');
  if (resetBtn) {
    resetBtn.addEventListener('click', handleResetTOTP);
  }
  
  // 禁用TOTP按钮
  const disableBtn = document.getElementById('disableTotpBtn');
  if (disableBtn) {
    disableBtn.addEventListener('click', showDisableTOTP);
  }
  
  // 禁用TOTP表单
  const disableForm = document.getElementById('disableTotpForm');
  if (disableForm) {
    disableForm.addEventListener('submit', handleDisableTOTP);
  }
  
  // 取消禁用按钮
  const cancelDisableBtn = document.getElementById('cancelDisableTotp');
  if (cancelDisableBtn) {
    cancelDisableBtn.addEventListener('click', hideDisableTOTP);
  }
}

// 开始TOTP设置
async function startTOTPSetup() {
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp`);
    if (!response.ok) {
      throw new Error('Failed to get TOTP setup info');
    }
    
    const setupInfo = await response.json();
    
    // 显示设置界面
    document.getElementById('totpDisabledSection').style.display = 'none';
    document.getElementById('totpSetupSection').style.display = 'block';
    
    // 显示密钥
    document.getElementById('totpSecret').textContent = setupInfo.secret;
    
    // 加载二维码
    await loadTOTPQRCode();
    
  } catch (error) {
    console.error('Failed to start TOTP setup:', error);
    showNotification('获取TOTP设置信息失败', 'error');
  }
}

// 加载TOTP二维码
async function loadTOTPQRCode() {
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/qrcode?width=200&height=200`);
    if (!response.ok) {
      throw new Error('Failed to load QR code');
    }
    
    const blob = await response.blob();
    const imageUrl = URL.createObjectURL(blob);
    
    document.getElementById('totpQrCode').innerHTML = `<img src="${imageUrl}" alt="TOTP QR Code" style="max-width: 200px;">`;
    
  } catch (error) {
    console.error('Failed to load TOTP QR code:', error);
    document.getElementById('totpQrCode').innerHTML = '<div class="error">二维码加载失败</div>';
  }
}

// 启用TOTP
async function handleEnableTOTP(e) {
  e.preventDefault();
  
  const verificationCode = document.getElementById('totpVerificationCode').value.trim();
  const secret = document.getElementById('totpSecret').textContent;
  const statusDiv = document.getElementById('totpSetupStatus');
  
  if (!verificationCode || verificationCode.length !== 6) {
    showStatus(statusDiv, '请输入6位验证码', 'error');
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/enable`, {
      method: 'POST',
      body: JSON.stringify({ secret, verificationCode })
    });
    
    if (response.ok) {
      showStatus(statusDiv, 'TOTP启用成功！', 'success');
      setTimeout(() => {
        cancelTOTPSetup();
        loadTOTPStatus();
        loadUserSettings(); // 刷新用户设置
      }, 1500);
    } else {
      const error = await response.text();
      showStatus(statusDiv, error, 'error');
    }
  } catch (error) {
    console.error('Failed to enable TOTP:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  }
}

// 取消TOTP设置
function cancelTOTPSetup() {
  document.getElementById('totpSetupSection').style.display = 'none';
  document.getElementById('totpDisabledSection').style.display = 'block';
  document.getElementById('enableTotpForm').reset();
  document.getElementById('totpVerificationCode').value = '';
  document.getElementById('totpSetupStatus').style.display = 'none';
}

// 显示测试TOTP
function showTestTOTP() {
  document.getElementById('testTotpSection').style.display = 'block';
}

// 隐藏测试TOTP
function hideTestTOTP() {
  document.getElementById('testTotpSection').style.display = 'none';
  document.getElementById('testTotpForm').reset();
}

// 测试TOTP
async function handleTestTOTP(e) {
  e.preventDefault();
  
  const verificationCode = document.getElementById('testTotpCode').value.trim();
  const statusDiv = document.getElementById('testTotpStatus');
  
  if (!verificationCode || verificationCode.length !== 6) {
    showStatus(statusDiv, '请输入6位验证码', 'error');
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/verify`, {
      method: 'POST',
      body: JSON.stringify({ verificationCode })
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.valid) {
        showStatus(statusDiv, '✅ 验证码正确！', 'success');
      } else {
        showStatus(statusDiv, '❌ 验证码错误', 'error');
      }
    } else {
      const error = await response.text();
      showStatus(statusDiv, error, 'error');
    }
  } catch (error) {
    console.error('Failed to test TOTP:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  }
}

// 重置TOTP密钥
async function handleResetTOTP() {
  if (!confirm('重置TOTP密钥后，您需要重新设置验证器应用。确定要继续吗？')) {
    return;
  }
  
  const currentCode = prompt('请输入当前TOTP验证码以确认重置：');
  if (!currentCode) {
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/reset`, {
      method: 'POST',
      body: JSON.stringify({ currentVerificationCode: currentCode })
    });
    
    if (response.ok) {
      showNotification('TOTP密钥重置成功，请重新设置', 'success');
      await loadTOTPStatus();
    } else {
      const error = await response.text();
      showNotification(error, 'error');
    }
  } catch (error) {
    console.error('Failed to reset TOTP:', error);
    showNotification('网络错误，请重试', 'error');
  }
}

// 显示禁用TOTP
function showDisableTOTP() {
  document.getElementById('disableTotpSection').style.display = 'block';
}

// 隐藏禁用TOTP
function hideDisableTOTP() {
  document.getElementById('disableTotpSection').style.display = 'none';
  document.getElementById('disableTotpForm').reset();
}

// 禁用TOTP
async function handleDisableTOTP(e) {
  e.preventDefault();
  
  const verificationCode = document.getElementById('disableTotpCode').value.trim();
  const statusDiv = document.getElementById('disableTotpStatus');
  
  if (!verificationCode || verificationCode.length !== 6) {
    showStatus(statusDiv, '请输入6位验证码', 'error');
    return;
  }
  
  try {
    const response = await AuthUtils.authenticatedFetch(`/blog/api/users/${userId}/settings/totp/disable`, {
      method: 'POST',
      body: JSON.stringify({ verificationCode })
    });
    
    if (response.ok) {
      showStatus(statusDiv, 'TOTP禁用成功', 'success');
      setTimeout(() => {
        hideDisableTOTP();
        loadTOTPStatus();
        loadUserSettings(); // 刷新用户设置
      }, 1500);
    } else {
      const error = await response.text();
      showStatus(statusDiv, error, 'error');
    }
  } catch (error) {
    console.error('Failed to disable TOTP:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  }
}

// ==================== 管理员访问功能 ====================

// 处理管理员访问
async function handleAdminAccess(e) {
  e.preventDefault();
  
  const statusDiv = document.getElementById('adminAccessStatus');
  
  try {
    // 直接跳转到管理界面，不需要TOTP验证
    showStatus(statusDiv, '正在跳转到管理界面...', 'success');
    setTimeout(() => {
      window.location.href = '/blog/admin';
    }, 500);
  } catch (error) {
    console.error('Failed to access admin:', error);
    showStatus(statusDiv, '网络错误，请重试', 'error');
  }
}