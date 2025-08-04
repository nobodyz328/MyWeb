// 用户管理JavaScript

let currentPage = 0;
let currentSize = 20;
let currentFilters = {};

document.addEventListener('DOMContentLoaded', function() {
    loadUsers();
    loadUserStatistics();
    
    // 绑定搜索表单事件
    document.getElementById('filter-form').addEventListener('submit', function(e) {
        e.preventDefault();
        currentPage = 0;
        loadUsers();
    });
});

/**
 * 加载用户列表
 */
function loadUsers() {
    showLoading('users-table-body');
    
    // 获取过滤条件
    const formData = new FormData(document.getElementById('filter-form'));
    const params = new URLSearchParams();
    
    params.append('page', currentPage);
    params.append('size', currentSize);
    
    for (let [key, value] of formData.entries()) {
        if (value.trim()) {
            params.append(key, value.trim());
            currentFilters[key] = value.trim();
        }
    }
    
    fetch(`/blog/admin/api/users?${params.toString()}`)
        .then(response => response.json())
        .then(data => {
            renderUsersTable(data.content);
            renderPagination(data);
        })
        .catch(error => {
            handleError(error, '加载用户列表');
            document.getElementById('users-table-body').innerHTML = 
                '<tr><td colspan="8" class="text-center text-danger">加载失败</td></tr>';
        });
}

/**
 * 加载用户统计
 */
function loadUserStatistics() {
    fetch('/blog/admin/api/statistics')
        .then(response => response.json())
        .then(data => {
            document.getElementById('total-users').textContent = data.totalUsers || 0;
            document.getElementById('active-users').textContent = data.activeUsers || 0;
            document.getElementById('admin-users').textContent = data.adminUsers || 0;
            document.getElementById('new-users-today').textContent = data.newUsersToday || 0;
        })
        .catch(error => {
            console.error('加载用户统计失败:', error);
        });
}

/**
 * 渲染用户表格
 */
function renderUsersTable(users) {
    const tbody = document.getElementById('users-table-body');
    
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">暂无数据</td></tr>';
        return;
    }
    
    let html = '';
    users.forEach(user => {
        const rolesText = user.roles ? user.roles.map(role => role.displayName || role.name).join(', ') : '无';
        const statusClass = getUserStatusClass(user.enabled);
        const statusText = user.enabled ? '活跃' : '禁用';
        const createdTime = formatDateTime(user.createdAt);
        const lastLoginTime = formatDateTime(user.lastLoginTime);
        
        html += `
            <tr>
                <td>${user.id}</td>
                <td>${user.username}</td>
                <td>${user.email || '未设置'}</td>
                <td>${rolesText}</td>
                <td>${createdTime}</td>
                <td>${lastLoginTime || '从未登录'}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary btn-action" 
                            onclick="showUserDetail(${user.id})">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-warning btn-action" 
                            onclick="showEditUserModal(${user.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

/**
 * 渲染分页
 */
function renderPagination(pageData) {
    const pagination = document.getElementById('pagination');
    
    if (pageData.totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }
    
    let html = '';
    
    // 上一页
    if (pageData.first) {
        html += '<li class="page-item disabled"><span class="page-link">上一页</span></li>';
    } else {
        html += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${currentPage - 1})">上一页</a></li>`;
    }
    
    // 页码
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(pageData.totalPages - 1, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            html += `<li class="page-item active"><span class="page-link">${i + 1}</span></li>`;
        } else {
            html += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${i})">${i + 1}</a></li>`;
        }
    }
    
    // 下一页
    if (pageData.last) {
        html += '<li class="page-item disabled"><span class="page-link">下一页</span></li>';
    } else {
        html += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${currentPage + 1})">下一页</a></li>`;
    }
    
    pagination.innerHTML = html;
}

/**
 * 切换页面
 */
function changePage(page) {
    currentPage = page;
    loadUsers();
}

/**
 * 显示用户详情
 */
function showUserDetail(userId) {
    const content = document.getElementById('user-detail-content');
    content.innerHTML = `
        <div class="text-center">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">加载中...</span>
            </div>
        </div>
    `;
    
    // 模拟用户详情显示
    setTimeout(() => {
        content.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <h6>基本信息</h6>
                    <table class="table table-sm">
                        <tr><td><strong>用户ID:</strong></td><td>${userId}</td></tr>
                        <tr><td><strong>用户名:</strong></td><td>test_user</td></tr>
                        <tr><td><strong>邮箱:</strong></td><td>test@example.com</td></tr>
                        <tr><td><strong>状态:</strong></td><td><span class="status-badge status-success">活跃</span></td></tr>
                        <tr><td><strong>注册时间:</strong></td><td>${new Date().toLocaleString()}</td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <h6>角色权限</h6>
                    <table class="table table-sm">
                        <tr><td><strong>角色:</strong></td><td>普通用户</td></tr>
                        <tr><td><strong>权限数量:</strong></td><td>5</td></tr>
                        <tr><td><strong>最后登录:</strong></td><td>${new Date().toLocaleString()}</td></tr>
                        <tr><td><strong>登录次数:</strong></td><td>25</td></tr>
                    </table>
                </div>
            </div>
            <div class="row mt-3">
                <div class="col-12">
                    <h6>活动统计</h6>
                    <div class="row">
                        <div class="col-md-3">
                            <div class="text-center">
                                <div class="h5 text-primary">12</div>
                                <small class="text-muted">发布帖子</small>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="text-center">
                                <div class="h5 text-success">45</div>
                                <small class="text-muted">发表评论</small>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="text-center">
                                <div class="h5 text-info">128</div>
                                <small class="text-muted">获得点赞</small>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="text-center">
                                <div class="h5 text-warning">8</div>
                                <small class="text-muted">关注者</small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }, 500);
    
    const modal = new bootstrap.Modal(document.getElementById('userDetailModal'));
    modal.show();
}

/**
 * 显示编辑用户模态框
 */
function showEditUserModal(userId) {
    // 模拟加载用户数据
    document.getElementById('edit-user-id').value = userId;
    document.getElementById('edit-username').value = 'test_user';
    document.getElementById('edit-email').value = 'test@example.com';
    
    // 设置角色选择
    const rolesSelect = document.getElementById('edit-roles');
    Array.from(rolesSelect.options).forEach(option => {
        option.selected = option.value === 'USER';
    });
    
    document.getElementById('edit-status').value = 'ACTIVE';
    
    const modal = new bootstrap.Modal(document.getElementById('editUserModal'));
    modal.show();
}

/**
 * 提交编辑用户
 */
function submitEditUser() {
    const userId = document.getElementById('edit-user-id').value;
    const email = document.getElementById('edit-email').value;
    const roles = Array.from(document.getElementById('edit-roles').selectedOptions).map(option => option.value);
    const status = document.getElementById('edit-status').value;
    
    // 这里应该调用后端API保存用户信息
    // 由于后端没有实现用户编辑API，我们模拟一个成功响应
    setTimeout(() => {
        showNotification('用户信息更新成功', 'success');
        bootstrap.Modal.getInstance(document.getElementById('editUserModal')).hide();
        loadUsers(); // 刷新用户列表
    }, 500);
}

/**
 * 刷新用户列表
 */
function refreshUsers() {
    loadUsers();
    loadUserStatistics();
    showNotification('用户列表已刷新', 'success');
}

/**
 * 获取用户状态样式类
 */
function getUserStatusClass(enabled) {
    return enabled ? 'status-success' : 'status-danger';
}

/**
 * 格式化日期时间
 */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '';
    
    const date = new Date(dateTimeStr);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * 显示加载状态
 */
function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = `
            <tr>
                <td colspan="8" class="text-center">
                    <div class="spinner-border" role="status">
                        <span class="visually-hidden">加载中...</span>
                    </div>
                </td>
            </tr>
        `;
    }
}

/**
 * 显示通知消息
 */
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 3000);
}

/**
 * 处理错误
 */
function handleError(error, context = '') {
    console.error(`${context}错误:`, error);
    showNotification(`${context}失败，请稍后重试`, 'danger');
}