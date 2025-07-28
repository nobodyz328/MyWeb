console.log('Script loaded');

// 获取用户ID
const userId = localStorage.getItem('userId');
console.log('User ID:', userId);

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function () {
  console.log('DOMContentLoaded event fired');
  loadAllPosts();
  initializeUserInterface();
});

// 异步加载所有帖子列表
function loadAllPosts() {
  console.log('loadAllPosts function called');
  const url = '/blog/api/posts';
  console.log('Fetching URL:', url);
  
  fetch(url)
    .then(res => res.json())
    .then(posts => {
      const list = document.getElementById('allPostsList');
      if (!posts || posts.length === 0) {
        list.innerHTML = '<div class="empty-state">暂无帖子</div>';
        return;
      }
      
      list.innerHTML = posts.map(post => `
        <div class="post-card">
          <div class="post-title">
            <a href="/blog/post/${post.id}">${escapeHtml(post.title)}</a>
          </div>
          <div class="post-meta">
            <span class="post-author">${escapeHtml(post.author?.username || '未知作者')}</span> |
            <span class="post-time">${formatTime(post.createdAt)}</span>
            <div class="post-stats">
              <span class="post-stat"><i class="stat-icon">👁️</i> ${post.viewCount || 0}</span>
              <span class="post-stat"><i class="stat-icon">❤️</i> ${post.likeCount || 0}</span>
              <span class="post-stat"><i class="stat-icon">⭐</i> ${post.collectCount || 0}</span>
              <span class="post-stat"><i class="stat-icon">💬</i> ${post.commentCount || 0}</span>
            </div>
          </div>
          <div class="post-summary">${escapeHtml(post.content ? post.content.slice(0, 80) + (post.content.length > 80 ? '...' : '') : '')}</div>
          <div class="post-interaction-preview">
            <button class="interaction-preview-btn" data-action="like" data-post-id="${post.id}">
              ${post.userLiked ? '❤️ ' : '🤍 '}
            </button>
            <button class="interaction-preview-btn" data-action="bookmark" data-post-id="${post.id}">
              ${post.userBookmarked ? '⭐ ' : '☆ '}
            </button>
            <a href="/blog/post/${post.id}#comments" class="interaction-preview-link">💬 评论 (${post.commentCount || 0})</a>
          </div>
        </div>
      `).join('');

      // 设置交互按钮事件处理
      setupInteractionButtons();
    })
    .catch(error => {
      console.error('加载帖子失败:', error);
      const list = document.getElementById('allPostsList');
      list.innerHTML = '<div class="empty-state">加载失败，请刷新重试</div>';
    });
}

// 设置交互按钮事件处理
function setupInteractionButtons() {
  // 如果用户未登录，不需要设置事件处理
  if (!userId) return;

  // 点赞按钮
  document.querySelectorAll('.interaction-preview-btn[data-action="like"]').forEach(btn => {
    btn.addEventListener('click', async function () {
      const postId = this.dataset.postId;
      if (!postId) return;

      // 立即更新UI状态
      const wasLiked = this.classList.contains('active');
      updateLikeButtonState(this, !wasLiked);

      try {
        // 发送请求
        const response = await fetch(`/blog/api/posts/${postId}/like?userId=${userId}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        const data = await response.json();

        if (data.success) {
          // 更新按钮状态
          updateLikeButtonState(this, data.data.liked);

          // 更新统计数
          const statElement = this.closest('.post-card').querySelector('.post-stat:nth-child(2)');
          if (statElement) {
            const countElement = statElement.childNodes[1];
            if (countElement) {
              countElement.textContent = ` ${data.data.likeCount || 0}`;
            }

            // 更新图标状态
            const iconElement = statElement.querySelector('.stat-icon');
            if (iconElement) {
              if (data.data.liked) {
                iconElement.classList.add('active');
              } else {
                iconElement.classList.remove('active');
              }
            }
          }
        } else {
          // 恢复原始状态
          updateLikeButtonState(this, wasLiked);
          showNotification('点赞操作失败，请重试', 'error');
        }
      } catch (error) {
        console.error('点赞操作失败:', error);
        // 恢复原始状态
        updateLikeButtonState(this, wasLiked);
        showNotification('网络错误，请检查连接', 'error');
      }
    });
  });

  // 收藏按钮
  document.querySelectorAll('.interaction-preview-btn[data-action="bookmark"]').forEach(btn => {
    btn.addEventListener('click', async function () {
      const postId = this.dataset.postId;
      if (!postId) return;

      // 立即更新UI状态
      const wasBookmarked = this.classList.contains('active');
      updateBookmarkButtonState(this, !wasBookmarked);

      try {
        // 发送请求
        const response = await fetch(`/blog/api/posts/${postId}/collect?userId=${userId}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        const data = await response.json();

        if (data.success) {
          // 更新按钮状态
          updateBookmarkButtonState(this, data.data.collected);

          // 更新统计数
          const statElement = this.closest('.post-card').querySelector('.post-stat:nth-child(3)');
          if (statElement) {
            const countElement = statElement.childNodes[1];
            if (countElement) {
              countElement.textContent = ` ${data.data.collectCount || 0}`;
            }

            // 更新图标状态
            const iconElement = statElement.querySelector('.stat-icon');
            if (iconElement) {
              if (data.data.collected) {
                iconElement.classList.add('active');
              } else {
                iconElement.classList.remove('active');
              }
            }
          }
        } else {
          // 恢复原始状态
          updateBookmarkButtonState(this, wasBookmarked);
          showNotification('收藏操作失败，请重试', 'error');
        }
      } catch (error) {
        console.error('收藏操作失败:', error);
        // 恢复原始状态
        updateBookmarkButtonState(this, wasBookmarked);
        showNotification('网络错误，请检查连接', 'error');
      }
    });
  });
}

// 初始化用户界面
function initializeUserInterface() {
  if (userId) {
    loadUserProfile();
  }
}

// 加载用户资料
function loadUserProfile() {
  fetch('/blog/users/' + userId + '/profile')
    .then(res => res.json())
    .then(data => {
      const username = data?.username || '用户';
      const followingCount = data?.followingCount || 0;
      const followersCount = data?.followersCount || 0;
      const likedCount = data?.likedCount || 0;
      const avatarUrl = data?.avatarUrl || '/blog/static/images/noface.gif';

      // 更新导航栏
      updateNavigation(username);
      
      // 更新用户卡片数据
      updateUserCard(username, followingCount, followersCount, likedCount, avatarUrl);
      
      // 初始化用户卡片交互
      initializeUserCard();
    })
    .catch(error => {
      console.error('获取用户信息失败:', error);
      // 处理错误情况，显示默认值
      updateNavigationForGuest();
    });
}

// 更新导航栏
function updateNavigation(username) {
  const navUser = document.getElementById('navUser');
  const loginNav = document.getElementById('loginNav');
  
  if (loginNav) {
    navUser.removeChild(loginNav);
  }
  
  const userLi = document.createElement('li');
  userLi.innerHTML = `<a href="javascript:void(0);" id="navUsername">${escapeHtml(username)}</a>`;
  navUser.appendChild(userLi);
}

// 更新导航栏（访客状态）
function updateNavigationForGuest() {
  const navUser = document.getElementById('navUser');
  const loginNav = document.getElementById('loginNav');
  
  if (loginNav) {
    navUser.removeChild(loginNav);
  }
  
  const userLi = document.createElement('li');
  userLi.innerHTML = `<a href="/blog/login" id="navUsername">登录/注册</a>`;
  navUser.appendChild(userLi);
}

// 更新用户卡片
function updateUserCard(username, followingCount, followersCount, likedCount, avatarUrl) {
  document.getElementById('userCardName').textContent = username;
  document.getElementById('userCardFollow').children[0].textContent = followingCount;
  document.getElementById('userCardFans').children[0].textContent = followersCount;
  document.getElementById('userCardLike').children[0].textContent = likedCount;
  document.getElementById('userCardAvatar').src = avatarUrl;
}

// 初始化用户卡片交互
function initializeUserCard() {
  // 点击用户名弹出卡片
  const navUsername = document.getElementById('navUsername');
  if (navUsername) {
    navUsername.addEventListener('click', function (e) {
      e.preventDefault();
      const card = document.getElementById('userCard');
      card.style.display = card.style.display === 'none' ? 'block' : 'none';
    });
  }

  // 点击退出登录
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', function () {
      if (confirm('确定要退出登录吗？')) {
        localStorage.removeItem('userId');
        window.location.reload();
      }
    });
  }

  // 点击页面其他地方关闭卡片
  document.addEventListener('click', function (e) {
    const card = document.getElementById('userCard');
    const navUsername = document.getElementById('navUsername');
    
    if (card.style.display === 'block' && 
        !card.contains(e.target) && 
        e.target !== navUsername) {
      card.style.display = 'none';
    }
  });
}

// 更新点赞按钮状态
function updateLikeButtonState(button, isLiked) {
  if (isLiked) {
    button.classList.add('active');
    button.innerHTML = '❤️ 已点赞';
  } else {
    button.classList.remove('active');
    button.innerHTML = '🤍 点赞';
  }
}

// 更新收藏按钮状态
function updateBookmarkButtonState(button, isBookmarked) {
  if (isBookmarked) {
    button.classList.add('active');
    button.innerHTML = '⭐ 已收藏';
  } else {
    button.classList.remove('active');
    button.innerHTML = '☆ 收藏';
  }
}

// 显示通知
function showNotification(message, type = 'info') {
  // 移除现有通知
  document.querySelectorAll('.notification').forEach(n => n.remove());

  const notification = document.createElement('div');
  notification.className = `notification notification-${type}`;
  notification.textContent = message;

  document.body.appendChild(notification);

  // 3秒后自动移除
  setTimeout(() => {
    if (notification.parentNode) {
      notification.remove();
    }
  }, 3000);
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