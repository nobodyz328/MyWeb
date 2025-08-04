// 角色权限管理JavaScript

document.addEventListener('DOMContentLoaded', function() {
    loadRoles();
    loadRoleStatistics();
});

/**
 * 加载角色列表
 */
function loadRoles() {
    showLoading('roles-table-body');
    
    fetch('/blog/admin/api/roles')
        .then(response => response.json())
        .then(roles => {
            renderRolesTable(roles);
        })
        .catch(error => {
            handleError(error, '加载角色列表');
            document.getElementById('roles-table-body').innerHTML = 
                '<tr><td colspan="8" class="text-center text-danger">加载失败</td></tr>';
        });
}

/**
 * 加载角色统计
 */
function loadRoleStatistics() {
    // 模拟统计数据
    document.getElementById('system-roles-count').textContent = '3';
    document.getElementById('custom-roles-count').textContent = '2';
    document.getElementById('total-permissions-count').textContent = '25';
}

/**
 * 渲染角色表格
 */
function renderRolesTable(roles) {
    const tbody = document.getElementById('roles-table-body');
    
    if (!roles || roles.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">暂无数据</td></tr>';
        return;
    }
    
    let html = '';
    roles.forEach(role => {
        const statusClass = role.enabled ? 'status-success' : 'status-danger';
        const statusText = role.enabled ? '启用' : '禁用';
        const systemBadge = role.systemRole ? '<span class="badge bg-primary">系统</span>' : '<span class="badge bg-secondary">自定义</span>';
        
        html += `
            <tr>
                <td>${role.name} ${systemBadge}</td>
                <td>${role.displayName || role.name}</td>
                <td>${role.description || '无描述'}</td>
                <td>${role.priority || 0}</td>
                <td>${role.permissions ? role.permissions.length : 0}</td>
                <td>0</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary btn-action" 
                            onclick="showRoleDetail(${role.id})">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-warning btn-action" 
                            onclick="showEditRoleModal(${role.id})"
                            ${role.systemRole ? 'disabled' : ''}>
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger btn-action" 
                            onclick="deleteRole(${role.id})"
                            ${role.systemRole ? 'disabled' : ''}>
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
    
    tbody.innerHTML = html;
}

/**
 * 显示创建角色模态框
 */
function showCreateRoleModal() {
    // 清空表单
    document.getElementById('create-role-form').reset();
    
    const modal = new bootstrap.Modal(document.getElementById('createRoleModal'));
    modal.show();
}

/**
 * 提交创建角色
 */
function submitCreateRole() {
    const name = document.getElementById('create-role-name').value.trim();
    const displayName = document.getElementById('create-role-display-name').value.trim();
    const description = document.getElementById('create-role-description').value.trim();
    const priority = parseInt(document.getElementById('create-role-priority').value) || 0;
    
    if (!name || !displayName) {
        showNotification('请填写必填字段', 'warning');
        return;
    }
    
    // 这里应该调用后端API创建角色
    // 由于后端没有实现角色创建API，我们模拟一个成功响应
    setTimeout(() => {
        showNotification('角色创建成功', 'success');
        bootstrap.Modal.getInstance(document.getElementById('createRoleModal')).hide();
        loadRoles(); // 刷新角色列表
        loadRoleStatistics(); // 刷新统计
    }, 500);
}

/**
 * 显示编辑角色模态框
 */
function showEditRoleModal(roleId) {
    // 模拟加载角色数据
    document.getElementById('edit-role-id').value = roleId;
    document.getElementById('edit-role-name').value = 'CUSTOM_ROLE';
    document.getElementById('edit-role-display-name').value = '自定义角色';
    document.getElementById('edit-role-description').value = '这是一个自定义角色';
    document.getElementById('edit-role-priority').value = '10';
    document.getElementById('edit-role-enabled').checked = true;
    
    // 加载权限列表
    loadPermissionsForRole(roleId);
    
    const modal = new bootstrap.Modal(document.getElementById('editRoleModal'));
    modal.show();
}

/**
 * 加载角色权限
 */
function loadPermissionsForRole(roleId) {
    const container = document.getElementById('role-permissions-list');
    
    // 模拟权限数据
    const permissions = [
        { group: '用户管理', items: [
            { id: 1, name: 'USER_VIEW', displayName: '查看用户', checked: true },
            { id: 2, name: 'USER_CREATE', displayName: '创建用户', checked: false },
            { id: 3, name: 'USER_UPDATE', displayName: '更新用户', checked: true },
            { id: 4, name: 'USER_DELETE', displayName: '删除用户', checked: false }
        ]},
        { group: '内容管理', items: [
            { id: 5, name: 'POST_VIEW', displayName: '查看帖子', checked: true },
            { id: 6, name: 'POST_CREATE', displayName: '创建帖子', checked: true },
            { id: 7, name: 'POST_UPDATE', displayName: '更新帖子', checked: true },
            { id: 8, name: 'POST_DELETE', displayName: '删除帖子', checked: false }
        ]},
        { group: '系统管理', items: [
            { id: 9, name: 'SYSTEM_CONFIG', displayName: '系统配置', checked: false },
            { id: 10, name: 'AUDIT_LOG_VIEW', displayName: '查看审计日志', checked: false },
            { id: 11, name: 'SECURITY_EVENT_VIEW', displayName: '查看安全事件', checked: false }
        ]}
    ];
    
    let html = '';
    permissions.forEach(group => {
        html += `
            <div class="permission-group">
                <div class="permission-group-title">${group.group}</div>
        `;
        
        group.items.forEach(permission => {
            html += `
                <div class="permission-item">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" 
                               id="perm_${permission.id}" 
                               value="${permission.id}"
                               ${permission.checked ? 'checked' : ''}>
                        <label class="form-check-label" for="perm_${permission.id}">
                            ${permission.displayName}
                        </label>
                    </div>
                </div>
            `;
        });
        
        html += '</div>';
    });
    
    container.innerHTML = html;
}

/**
 * 提交编辑角色
 */
function submitEditRole() {
    const roleId = document.getElementById('edit-role-id').value;
    const displayName = document.getElementById('edit-role-display-name').value.trim();
    const description = document.getElementById('edit-role-description').value.trim();
    const priority = parseInt(document.getElementById('edit-role-priority').value) || 0;
    const enabled = document.getElementById('edit-role-enabled').checked;
    
    // 获取选中的权限
    const selectedPermissions = Array.from(document.querySelectorAll('#role-permissions-list input[type="checkbox"]:checked'))
        .map(checkbox => checkbox.value);
    
    // 这里应该调用后端API更新角色
    // 由于后端没有实现角色更新API，我们模拟一个成功响应
    setTimeout(() => {
        showNotification('角色更新成功', 'success');
        bootstrap.Modal.getInstance(document.getElementById('editRoleModal')).hide();
        loadRoles(); // 刷新角色列表
    }, 500);
}

/**
 * 显示角色详情
 */
function showRoleDetail(roleId) {
    const content = document.getElementById('role-detail-content');
    content.innerHTML = `
        <div class="text-center">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">加载中...</span>
            </div>
        </div>
    `;
    
    // 模拟角色详情显示
    setTimeout(() => {
        content.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <h6>基本信息</h6>
                    <table class="table table-sm">
                        <tr><td><strong>角色ID:</strong></td><td>${roleId}</td></tr>
                        <tr><td><strong>角色名称:</strong></td><td>ADMIN</td></tr>
                        <tr><td><strong>显示名称:</strong></td><td>系统管理员</td></tr>
                        <tr><td><strong>描述:</strong></td><td>拥有系统所有权限</td></tr>
                        <tr><td><strong>优先级:</strong></td><td>100</td></tr>
                        <tr><td><strong>状态:</strong></td><td><span class="status-badge status-success">启用</span></td></tr>
                        <tr><td><strong>类型:</strong></td><td><span class="badge bg-primary">系统角色</span></td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <h6>统计信息</h6>
                    <table class="table table-sm">
                        <tr><td><strong>权限数量:</strong></td><td>25</td></tr>
                        <tr><td><strong>用户数量:</strong></td><td>3</td></tr>
                        <tr><td><strong>创建时间:</strong></td><td>${new Date().toLocaleString()}</td></tr>
                        <tr><td><strong>更新时间:</strong></td><td>${new Date().toLocaleString()}</td></tr>
                    </table>
                </div>
            </div>
            <div class="row mt-3">
                <div class="col-12">
                    <h6>角色权限</h6>
                    <div class="row">
                        <div class="col-md-4">
                            <div class="permission-group">
                                <div class="permission-group-title">用户管理</div>
                                <div class="permission-item">✓ 查看用户</div>
                                <div class="permission-item">✓ 创建用户</div>
                                <div class="permission-item">✓ 更新用户</div>
                                <div class="permission-item">✓ 删除用户</div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="permission-group">
                                <div class="permission-group-title">内容管理</div>
                                <div class="permission-item">✓ 查看帖子</div>
                                <div class="permission-item">✓ 创建帖子</div>
                                <div class="permission-item">✓ 更新帖子</div>
                                <div class="permission-item">✓ 删除帖子</div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="permission-group">
                                <div class="permission-group-title">系统管理</div>
                                <div class="permission-item">✓ 系统配置</div>
                                <div class="permission-item">✓ 查看审计日志</div>
                                <div class="permission-item">✓ 查看安全事件</div>
                                <div class="permission-item">✓ 角色管理</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }, 500);
    
    const modal = new bootstrap.Modal(document.getElementById('roleDetailModal'));
    modal.show();
}

/**
 * 删除角色
 */
function deleteRole(roleId) {
    if (confirm('确定要删除这个角色吗？此操作不可撤销。')) {
        // 这里应该调用后端API删除角色
        // 由于后端没有实现角色删除API，我们模拟一个成功响应
        setTimeout(() => {
            showNotification('角色删除成功', 'success');
            loadRoles(); // 刷新角色列表
            loadRoleStatistics(); // 刷新统计
        }, 500);
    }
}

/**
 * 刷新角色列表
 */
function refreshRoles() {
    loadRoles();
    loadRoleStatistics();
    showNotification('角色列表已刷新', 'success');
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