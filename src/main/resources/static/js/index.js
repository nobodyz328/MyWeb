console.log('Script loaded');

// 获取用户ID
const userId = AuthUtils.getUserId();
console.log('User ID:', userId);

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', async function () {
  console.log('DOMContentLoaded event fired');
  
  // 初始化认证状态
  await AuthUtils.initAuth();
  
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
    .then(async posts => {
      const list = document.getElementById('allPostsList');
      if (!posts || posts.length === 0) {
        list.innerHTML = '<div class="empty-state">暂无帖子</div>';
        return;
      }
      
      // 如果用户已登录，获取用户的交互状态
      let userInteractions = {};
      if (AuthUtils.isLoggedIn()) {
        userInteractions = await loadUserInteractionsForPosts(posts.map(p => p.id));
      }
      
      list.innerHTML = posts.map(post => {
        const userLiked = userInteractions[post.id]?.liked || false;
        const userBookmarked = userInteractions[post.id]?.collected || false;
        
        return `
          <div class="post-card">
            <div class="post-title">
              <a href="/blog/post/${post.id}">${escapeHtml(post.title)}</a>
            </div>
            <div class="post-meta">
              <span class="post-author">${escapeHtml(post.author?.username || '未知作者')}</span> |
              <span class="post-time">${formatTime(post.createdAt)}</span>
              <div class="post-stats">
                <span class="post-stat"><i class="stat-icon ${userLiked ? 'active' : ''}">👁️</i> ${post.viewCount || 0}</span>
                <span class="post-stat"><i class="stat-icon ${userLiked ? 'active' : ''}">❤️</i> ${post.likeCount || 0}</span>
                <span class="post-stat"><i class="stat-icon ${userBookmarked ? 'active' : ''}">⭐</i> ${post.collectCount || 0}</span>
                <span class="post-stat"><i class="stat-icon">💬</i> ${post.commentCount || 0}</span>
              </div>
            </div>
            <div class="post-summary">${escapeHtml(post.content ? post.content.slice(0, 80) + (post.content.length > 80 ? '...' : '') : '')}</div>
            <div class="post-interaction-preview">
              <button class="interaction-preview-btn ${userLiked ? 'active' : ''}" data-action="like" data-post-id="${post.id}">
                ${userLiked ? '❤️ 已点赞' : '🤍 点赞'}
              </button>
              <button class="interaction-preview-btn ${userBookmarked ? 'active' : ''}" data-action="bookmark" data-post-id="${post.id}">
                ${userBookmarked ? '⭐ 已收藏' : '☆ 收藏'}
              </button>
              <a href="/blog/post/${post.id}#comments" class="interaction-preview-link">💬 评论 (${post.commentCount || 0})</a>
            </div>
          </div>
        `;
      }).join('');

      // 设置交互按钮事件处理
      setupInteractionButtons();
    })
    .catch(error => {
      console.error('加载帖子失败:', error);
      const list = document.getElementById('allPostsList');
      list.innerHTML = '<div class="empty-state">加载失败，请刷新重试</div>';
    });
}

// 加载用户对帖子的交互状态
async function loadUserInteractionsForPosts(postIds) {
  if (!AuthUtils.isLoggedIn() || !postIds || postIds.length === 0) {
    return {};
  }

  const interactions = {};
  
  try {
    // 并发获取所有帖子的交互状态
    const promises = postIds.map(async postId => {
      try {
        // 获取点赞状态
        const likeResponse = await fetch(`/blog/api/posts/${postId}/like-status?userId=${userId}`);
        let liked = false;
        if (likeResponse.ok) {
          const likeData = await likeResponse.json();
          liked = likeData.success ? likeData.data : false;
        }

        // 获取收藏状态
        const collectResponse = await fetch(`/blog/api/posts/${postId}/collect-status?userId=${userId}`);
        let collected = false;
        if (collectResponse.ok) {
          const collectData = await collectResponse.json();
          collected = collectData.success ? collectData.data : false;
        }

        interactions[postId] = { liked, collected };
      } catch (error) {
        console.warn(`获取帖子 ${postId} 交互状态失败:`, error);
        interactions[postId] = { liked: false, collected: false };
      }
    });

    await Promise.all(promises);
  } catch (error) {
    console.error('批量获取交互状态失败:', error);
  }

  return interactions;
}

// 设置交互按钮事件处理
function setupInteractionButtons() {
  // 如果用户未登录，不需要设置事件处理
  if (!AuthUtils.isLoggedIn()) return;

  // 点赞按钮
  document.querySelectorAll('.interaction-preview-btn[data-action="like"]').forEach(btn => {
    btn.addEventListener('click', async function (e) {
      e.preventDefault();
      const postId = this.dataset.postId;
      if (!postId) return;

      // 立即更新UI状态
      const wasLiked = this.classList.contains('active');
      updateLikeButtonState(this, !wasLiked);

      try {
        // 发送请求
        const response = await AuthUtils.authenticatedFetch(`/blog/api/posts/${postId}/like?userId=${userId}`, {
          method: 'POST'
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
          showNotification(data.message || '点赞操作失败，请重试', 'error');
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
    btn.addEventListener('click', async function (e) {
      e.preventDefault();
      const postId = this.dataset.postId;
      if (!postId) return;

      // 立即更新UI状态
      const wasBookmarked = this.classList.contains('active');
      updateBookmarkButtonState(this, !wasBookmarked);

      try {
        // 发送请求
        const response = await AuthUtils.authenticatedFetch(`/blog/api/posts/${postId}/collect?userId=${userId}`, {
          method: 'POST'
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
          showNotification(data.message || '收藏操作失败，请重试', 'error');
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
  if (AuthUtils.isLoggedIn()) {
    loadUserProfile();
  }
}

// 加载用户资料
async function loadUserProfile() {
  try {
    const response = await AuthUtils.authenticatedFetch('/blog/users/' + userId + '/profile');
    const data = await response.json();
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
  } catch (error) {
    console.error('获取用户信息失败:', error);
    // 处理错误情况，显示默认值
    updateNavigationForGuest();
  }
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
        AuthUtils.logout();
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
    button.innerHTML = '❤️';
  } else {
    button.classList.remove('active');
    button.innerHTML = '🤍';
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