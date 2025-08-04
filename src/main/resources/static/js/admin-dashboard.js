// 管理员仪表板JavaScript

document.addEventListener('DOMContentLoaded', function() {
    loadRecentAuditLogs();
    loadRecentSecurityEvents();
    
    // 每30秒刷新一次数据
    setInterval(refreshData, 30000);
});

/**
 * 刷新所有数据
 */
function refreshData() {
    loadRecentAuditLogs();
    loadRecentSecurityEvents();
    updateSystemStatistics();
}

/**
 * 加载最近的审计日志
 */
function loadRecentAuditLogs() {
    fetch('/blog/admin/api/audit-logs?size=5&hours=24')
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('recent-audit-logs');
            if (data.content && data.content.length > 0) {
                let html = '<div class="list-group list-group-flush">';
                data.content.forEach(log => {
                    const resultClass = log.result === 'SUCCESS' ? 'text-success' : 'text-danger';
                    const operationText = formatOperation(log.operation);
                    const timeText = formatDateTime(log.timestamp);
                    
                    html += `
                        <div class="list-group-item d-flex justify-content-between align-items-start">
                            <div class="ms-2 me-auto">
                                <div class="fw-bold">${operationText}</div>
                                <small class="text-muted">用户: ${log.username || '未知'} | IP: ${log.ipAddress || '未知'}</small>
                            </div>
                            <div class="text-end">
                                <span class="badge ${resultClass === 'text-success' ? 'bg-success' : 'bg-danger'} rounded-pill">
                                    ${log.result}
                                </span>
                                <br>
                                <small class="text-muted">${timeText}</small>
                            </div>
                        </div>
                    `;
                });
                html += '</div>';
                container.innerHTML = html;
            } else {
                container.innerHTML = '<div class="text-center text-muted">暂无审计日志</div>';
            }
        })
        .catch(error => {
            console.error('加载审计日志失败:', error);
            document.getElementById('recent-audit-logs').innerHTML = 
                '<div class="text-center text-danger">加载失败</div>';
        });
}

/**
 * 加载最近的安全事件
 */
function loadRecentSecurityEvents() {
    fetch('/blog/admin/api/security-events?size=5&hours=24')
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById('recent-security-events');
            if (data.content && data.content.length > 0) {
                let html = '<div class="list-group list-group-flush">';
                data.content.forEach(event => {
                    const severityClass = getSeverityClass(event.severity);
                    const severityText = getSeverityText(event.severity);
                    const timeText = formatDateTime(event.eventTime);
                    
                    html += `
                        <div class="list-group-item d-flex justify-content-between align-items-start">
                            <div class="ms-2 me-auto">
                                <div class="fw-bold">${event.title || event.eventType}</div>
                                <small class="text-muted">用户: ${event.username || '未知'} | IP: ${event.sourceIp || '未知'}</small>
                            </div>
                            <div class="text-end">
                                <span class="badge ${severityClass} rounded-pill">
                                    ${severityText}
                                </span>
                                <br>
                                <small class="text-muted">${timeText}</small>
                            </div>
                        </div>
                    `;
                });
                html += '</div>';
                container.innerHTML = html;
            } else {
                container.innerHTML = '<div class="text-center text-muted">暂无安全事件</div>';
            }
        })
        .catch(error => {
            console.error('加载安全事件失败:', error);
            document.getElementById('recent-security-events').innerHTML = 
                '<div class="text-center text-danger">加载失败</div>';
        });
}

/**
 * 更新系统统计数据
 */
function updateSystemStatistics() {
    fetch('/blog/admin/api/statistics')
        .then(response => response.json())
        .then(data => {
            // 更新统计卡片
            updateStatCard('todayAuditLogs', data.todayAuditLogs || 0);
            updateStatCard('todaySecurityEvents', data.todaySecurityEvents || 0);
            updateStatCard('activeUsers', data.activeUsers || 0);
            updateStatCard('totalUsers', data.totalUsers || 0);
        })
        .catch(error => {
            console.error('更新统计数据失败:', error);
        });
}

/**
 * 更新统计卡片
 */
function updateStatCard(elementId, value) {
    const element = document.querySelector(`[th\\:text*="${elementId}"]`);
    if (element) {
        element.textContent = value;
    }
}

/**
 * 格式化操作类型
 */
function formatOperation(operation) {
    const operationMap = {
        'USER_LOGIN_SUCCESS': '用户登录成功',
        'USER_LOGIN_FAILURE': '用户登录失败',
        'USER_LOGOUT': '用户登出',
        'USER_REGISTER': '用户注册',
        'POST_CREATE': '创建帖子',
        'POST_UPDATE': '更新帖子',
        'POST_DELETE': '删除帖子',
        'COMMENT_CREATE': '创建评论',
        'COMMENT_DELETE': '删除评论',
        'ADMIN_ACCESS': '管理员访问',
        'AUDIT_LOG_QUERY': '查询审计日志',
        'SECURITY_EVENT_VIEW': '查看安全事件'
    };
    return operationMap[operation] || operation;
}

/**
 * 格式化日期时间
 */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '未知';
    
    const date = new Date(dateTimeStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffMins < 1) {
        return '刚刚';
    } else if (diffMins < 60) {
        return `${diffMins}分钟前`;
    } else if (diffHours < 24) {
        return `${diffHours}小时前`;
    } else if (diffDays < 7) {
        return `${diffDays}天前`;
    } else {
        return date.toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}

/**
 * 获取严重级别样式类
 */
function getSeverityClass(severity) {
    switch (severity) {
        case 1: return 'bg-success';
        case 2: return 'bg-warning';
        case 3: return 'bg-danger';
        case 4: return 'bg-danger';
        case 5: return 'bg-dark';
        default: return 'bg-secondary';
    }
}

/**
 * 获取严重级别文本
 */
function getSeverityText(severity) {
    switch (severity) {
        case 1: return '低危';
        case 2: return '中危';
        case 3: return '高危';
        case 4: return '严重';
        case 5: return '紧急';
        default: return '未知';
    }
}

/**
 * 显示通知消息
 */
function showNotification(message, type = 'info') {
    // 创建通知元素
    const notification = document.createElement('div');
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    // 添加到页面
    document.body.appendChild(notification);
    
    // 3秒后自动移除
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

/**
 * 显示加载状态
 */
function showLoading(element) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    if (element) {
        element.innerHTML = `
            <div class="text-center">
                <div class="spinner-border" role="status">
                    <span class="visually-hidden">加载中...</span>
                </div>
            </div>
        `;
    }
}

/**
 * 隐藏加载状态
 */
function hideLoading(element) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    // 加载状态会被实际内容替换，这里不需要特殊处理
}

// 全局错误处理
window.addEventListener('error', function(e) {
    console.error('全局错误:', e.error);
});

window.addEventListener('unhandledrejection', function(e) {
    console.error('未处理的Promise拒绝:', e.reason);
});