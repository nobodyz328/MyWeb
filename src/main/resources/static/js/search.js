console.log('Search script loaded');

// 全局变量
let currentSearchType = 'ALL';
let currentSortBy = 'RELEVANCE';
let currentKeyword = '';
let lastPostId = 0;
let lastUserId = 0;
let isLoading = false;
let hasMoreResults = true;

// 获取用户ID
const userId = AuthUtils.getUserId();
console.log('User ID:', userId);

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', async function () {
  console.log('DOMContentLoaded event fired');
  
  // 初始化认证状态
  await AuthUtils.initAuth();
  
  initializeSearchPage();
  initializeUserInterface();
  loadHotKeywords();
  setupInfiniteScroll();
});

// 初始化搜索页面
function initializeSearchPage() {
  const searchInput = document.getElementById('searchInput');
  const searchBtn = document.getElementById('searchBtn');
  const filterBtns = document.querySelectorAll('.filter-btn');
  const sortBtns = document.querySelectorAll('.sort-btn');

  // 搜索按钮点击事件
  searchBtn.addEventListener('click', performSearch);

  // 搜索输入框回车事件
  searchInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
      performSearch();
    }
  });

  // 搜索类型过滤器事件
  filterBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      filterBtns.forEach(b => b.classList.remove('active'));
      this.classList.add('active');
      currentSearchType = this.dataset.type;
      
      if (currentKeyword) {
        resetSearchState();
        performSearch();
      }
    });
  });

  // 排序方式过滤器事件
  sortBtns.forEach(btn => {
    btn.addEventListener('click', function() {
      sortBtns.forEach(b => b.classList.remove('active'));
      this.classList.add('active');
      currentSortBy = this.dataset.sort;
      
      if (currentKeyword) {
        resetSearchState();
        performSearch();
      }
    });
  });

  // 从URL参数获取搜索关键词
  const urlParams = new URLSearchParams(window.location.search);
  const keyword = urlParams.get('q');
  if (keyword) {
    searchInput.value = keyword;
    performSearch();
  }
}

// 初始化用户界面
function initializeUserInterface() {
  if (userId) {
    loadUserProfile();
  }
}

// 加载用户资料
function loadUserProfile() {
  AuthUtils.authenticatedFetch('/blog/users/' + userId + '/profile')
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


// 加载热门搜索关键词
async function loadHotKeywords() {
  try {
    const response = await fetch('/blog/api/search/hot-keywords?limit=10'); // Public endpoint
    if (response.ok) {
      const result = await response.json();
      if (result.success && result.data && result.data.length > 0) {
        displayHotKeywords(result.data);
      } else {
        // 如果没有热门关键词，显示默认关键词
        displayHotKeywords(['游戏', '攻略', '评测', '新手', '推荐']);
      }
    }
  } catch (error) {
    console.error('加载热门关键词失败:', error);
    // 显示默认关键词
    displayHotKeywords(['游戏', '攻略', '评测', '新手', '推荐']);
  }
}

// 显示热门关键词
function displayHotKeywords(keywords) {
  const hotKeywordsContainer = document.getElementById('hotKeywords');
  hotKeywordsContainer.innerHTML = keywords.map(keyword => 
    `<span class="hot-keyword" onclick="searchByKeyword('${escapeHtml(keyword)}')">${escapeHtml(keyword)}</span>`
  ).join('');
}

// 通过关键词搜索
function searchByKeyword(keyword) {
  document.getElementById('searchInput').value = keyword;
  performSearch();
}

// 执行搜索
async function performSearch() {
  const searchInput = document.getElementById('searchInput');
  const keyword = searchInput.value.trim();
  currentKeyword = keyword;
  if (!keyword) {
    currentKeyword="all";
  }
  resetSearchState();
  
  // 更新URL
  const newUrl = new URL(window.location);
  newUrl.searchParams.set('q', keyword);
  window.history.pushState({}, '', newUrl);

  // 显示搜索结果区域
  document.getElementById('searchResultsSection').style.display = 'block';
  document.getElementById('emptyStateSection').style.display = 'none';
  document.getElementById('currentKeyword').textContent = keyword;

  await loadSearchResults();
}

// 重置搜索状态
function resetSearchState() {
  lastPostId = 0;
  lastUserId = 0;
  hasMoreResults = true;
  document.getElementById('searchResults').innerHTML = '';
  document.getElementById('resultsCount').textContent = '0';
}

// 加载搜索结果
async function loadSearchResults() {
  if (isLoading || !hasMoreResults) return;

  isLoading = true;
  showLoadingIndicator();

  try {
    let url;
    let params = new URLSearchParams({
      keyword: currentKeyword,
      size: '20',
      sortBy: currentSortBy
    });

    // 根据搜索类型构建URL
    switch (currentSearchType) {
      case 'POST':
        params.append('lastId', lastPostId.toString());
        url = `/blog/api/search/posts?${params}`;
        break;
      case 'USER':
        params.append('lastId', lastUserId.toString());
        url = `/blog/api/search/users?${params}`;
        break;
      case 'ALL':
      default:
        params.append('lastPostId', lastPostId.toString());
        params.append('lastUserId', lastUserId.toString());
        url = `/blog/api/search/all?${params}`;
        break;
    }

    const response = await fetch(url); // Public search endpoint
    const result = await response.json();

    if (result.success && result.data) {
      const searchData = result.data;
      
      if (searchData.items && searchData.items.length > 0) {
        displaySearchResults(searchData.items, searchData.type || currentSearchType);
        updateResultsCount(searchData.items.length);
        updateCursors(searchData);
        hasMoreResults = searchData.hasMore !== false;
      } else {
        if (lastPostId === 0 && lastUserId === 0) {
          // 首次搜索无结果
          showEmptyState();
        } else {
          // 没有更多结果
          showNoMoreResults();
        }
        hasMoreResults = false;
      }
    } else {
      console.error('搜索失败:', result.message);
      if (lastPostId === 0 && lastUserId === 0) {
        showEmptyState();
      }
    }
  } catch (error) {
    console.error('搜索请求失败:', error);
    if (lastPostId === 0 && lastUserId === 0) {
      showEmptyState();
    }
  } finally {
    isLoading = false;
    hideLoadingIndicator();
  }
}

// 显示搜索结果
function displaySearchResults(items, type) {
  const resultsContainer = document.getElementById('searchResults');
  
  items.forEach(item => {
    let resultHtml;
    
    if (type === 'ALL') {
      // 综合搜索，需要判断item类型
      if (item.title) {
        // 帖子
        resultHtml = createPostResultCard(item);
      } else if (item.username) {
        // 用户
        resultHtml = createUserResultCard(item);
      }
    } else if (type === 'POST' || currentSearchType === 'POST') {
      // 帖子搜索
      resultHtml = createPostResultCard(item);
    } else if (type === 'USER' || currentSearchType === 'USER') {
      // 用户搜索
      resultHtml = createUserResultCard(item);
    }
    
    if (resultHtml) {
      resultsContainer.insertAdjacentHTML('beforeend', resultHtml);
    }
  });
}

// 创建帖子结果卡片
function createPostResultCard(post) {
  return `
    <div class="post-result-card">
      <div class="post-result-header">
        <span class="result-type-badge">帖子</span>
      </div>
      <div class="post-result-title">
        <a href="/blog/post/${post.id}">${escapeHtml(post.title)}</a>
      </div>
      <div class="post-result-meta">
        <div class="post-result-author">
          <img src="${post.authorAvatarUrl || '/blog/static/images/noface.gif'}" alt="作者头像" class="author-avatar">
          <span>${escapeHtml(post.authorUsername || '未知作者')}</span>
          <span>•</span>
          <span>${formatTime(post.createdAt)}</span>
        </div>
        <div class="post-result-stats">
          <span class="post-result-stat">👁️ ${post.viewCount || 0}</span>
          <span class="post-result-stat">❤️ ${post.likeCount || 0}</span>
          <span class="post-result-stat">⭐ ${post.collectCount || 0}</span>
          <span class="post-result-stat">💬 ${post.commentCount || 0}</span>
        </div>
      </div>
      <div class="post-result-summary">
        ${escapeHtml(post.contentSummary || '暂无摘要')}
      </div>
    </div>
  `;
}

// 创建用户结果卡片
function createUserResultCard(user) {
  return `
    <div class="user-result-card">
      <div class="post-result-header">
        <span class="result-type-badge">用户</span>
      </div>
      <div class="user-result-header">
        <img src="${user.avatarUrl || '/blog/static/images/noface.gif'}" alt="用户头像" class="user-result-avatar">
        <div class="user-result-info">
          <div class="user-result-name">${escapeHtml(user.username)}</div>
          <div class="user-result-email">${escapeHtml(user.email || '')}</div>
        </div>
      </div>
      <div class="user-result-bio">
        ${escapeHtml(user.bio || '这个用户很神秘，什么都没有留下...')}
      </div>
      <div class="user-result-stats">
        <div class="user-result-stat">
          <div class="user-result-stat-number">${user.postsCount || 0}</div>
          <div class="user-result-stat-label">帖子</div>
        </div>
        <div class="user-result-stat">
          <div class="user-result-stat-number">${user.followersCount || 0}</div>
          <div class="user-result-stat-label">粉丝</div>
        </div>
        <div class="user-result-stat">
          <div class="user-result-stat-number">${user.followingCount || 0}</div>
          <div class="user-result-stat-label">关注</div>
        </div>
        <div class="user-result-stat">
          <div class="user-result-stat-number">${user.likedCount || 0}</div>
          <div class="user-result-stat-label">获赞</div>
        </div>
      </div>
    </div>
  `;
}

// 更新结果计数
function updateResultsCount(newCount) {
  const resultsCountElement = document.getElementById('resultsCount');
  const currentCount = parseInt(resultsCountElement.textContent) || 0;
  resultsCountElement.textContent = currentCount + newCount;
}

// 更新游标
function updateCursors(searchData) {
  if (searchData.nextCursor) {
    if (currentSearchType === 'ALL') {
      // 综合搜索的游标是JSON格式
      try {
        const cursors = JSON.parse(searchData.nextCursor);
        lastPostId = parseInt(cursors.postCursor) || lastPostId;
        lastUserId = parseInt(cursors.userCursor) || lastUserId;
      } catch (e) {
        console.error('解析游标失败:', e);
      }
    } else {
      // 单类型搜索
      const cursorId = parseInt(searchData.nextCursor);
      if (currentSearchType === 'POST') {
        lastPostId = cursorId;
      } else if (currentSearchType === 'USER') {
        lastUserId = cursorId;
      }
    }
  }
}

// 显示加载指示器
function showLoadingIndicator() {
  document.getElementById('loadingIndicator').style.display = 'flex';
  document.getElementById('noMoreResults').style.display = 'none';
}

// 隐藏加载指示器
function hideLoadingIndicator() {
  document.getElementById('loadingIndicator').style.display = 'none';
}

// 显示没有更多结果
function showNoMoreResults() {
  document.getElementById('noMoreResults').style.display = 'block';
}

// 显示空状态
function showEmptyState() {
  document.getElementById('searchResultsSection').style.display = 'none';
  document.getElementById('emptyStateSection').style.display = 'block';
}

// 设置无限滚动
function setupInfiniteScroll() {
  let ticking = false;
  
  window.addEventListener('scroll', function() {
    if (!ticking) {
      requestAnimationFrame(function() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const windowHeight = window.innerHeight;
        const documentHeight = document.documentElement.scrollHeight;
        
        // 当滚动到距离底部200px时加载更多
        if (scrollTop + windowHeight >= documentHeight - 200) {
          if (currentKeyword && hasMoreResults && !isLoading) {
            loadSearchResults();
          }
        }
        
        ticking = false;
      });
      
      ticking = true;
    }
  });
}

// 工具函数：HTML转义
function escapeHtml(text) {
  if (!text) return '';
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  };
  return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

// 工具函数：格式化时间
function formatTime(dateString) {
  if (!dateString) return '未知时间';
  
  const date = new Date(dateString);
  const now = new Date();
  const diff = now - date;
  
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  const week = 7 * day;
  const month = 30 * day;
  const year = 365 * day;
  
  if (diff < minute) {
    return '刚刚';
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前';
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前';
  } else if (diff < week) {
    return Math.floor(diff / day) + '天前';
  } else if (diff < month) {
    return Math.floor(diff / week) + '周前';
  } else if (diff < year) {
    return Math.floor(diff / month) + '个月前';
  } else {
    return Math.floor(diff / year) + '年前';
  }
}

// 清除搜索缓存（管理员功能）
// export async function clearSearchCache(keyword = null) {
//   try {
//     const url = keyword ?
//       `/blog/api/search/cache?keyword=${encodeURIComponent(keyword)}` :
//       '/blog/api/search/cache';
//
//     const response = await fetch(url, { method: 'DELETE' });
//     const result = await response.json();
//
//     if (result.success) {
//       console.log('搜索缓存清除成功');
//     } else {
//       console.error('清除搜索缓存失败:', result.message);
//     }
//   } catch (error) {
//     console.error('清除搜索缓存请求失败:', error);
//   }
// }

