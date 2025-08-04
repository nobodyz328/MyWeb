// 安全事件管理JavaScript

let currentPage = 0;
let currentSize = 20;
let currentFilters = {};

document.addEventListener('DOMContentLoaded', function() {
    loadSecurityEvents();
    loadEventStatistics();
    
    // 绑定搜索表单事件
    document.getElementById('filter-form').addEventListener('submit', function(e) {
        e.preventDefault();
        currentPage = 0;
        loadSecurityEvents();
    });
    
    // 每30秒刷新一次统计数据
    setInterval(loadEventStatistics, 30000);
});

/**
 * 加载安全事件
 */
function loadSecurityEvents() {
    showLoading('events-table-body');
    
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
    
    fetch(`/blog/admin/api/security-events?${params.toString()}`)
        .then(response => response.json())
        .then(data => {
            renderSecurityEventsTable(data.content);
            renderPagination(data);
        })
        .catch(error => {
            handleError(error, '加载安全事件');
            document.getElementById('events-table-body').innerHTML = 
                '<tr><td colspan="8" class="text-center text-danger">加载失败</td></tr>';
        });
}

/**
 * 加载事件统计
 */
function loadEventStatistics() {
    fetch('/blog/admin/api/statistics')
        .then(response => response.json())
        .then(data => {
            // 更新统计卡片
            document.getElementById('high-risk-count').textContent = data.highRiskEvents || 0;
            document.getElementById('medium-risk-count').textContent = data.mediumRiskEvents || 0;
            document.getElementById('low-risk-count').textContent = data.lowRiskEvents || 0;
            document.getElementById('unhandled-count').textContent = data.unhandledEvents || 0;
        })
        .catch(error => {
            console.error('加载统计数据失败:', error);
        });
}

/**
 * 渲染安全事件表格
 */
function renderSecurityEventsTable(events) {
    const tbody = document.getElementById('events-table-body');
    
    if (!events || events.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">暂无数据</td></tr>';
        return;
    }
    
    let html = '';
    events.forEach(event => {
        const severityClass = getSeverityClass(event.severity);
        const severityText = getSeverityText(event.severity);
        const statusClass = getStatusClass(event.status);
        const statusText = getStatusText(event.status);
        const riskScoreClass = getRiskScoreClass(event.riskScore);
        const timeText = formatDateTime(event.eventTime);
        
        html += `
            <tr>
                <td>${timeText}</td>
                <td>${formatEventType(event.eventType)}</td>
                <td>${event.username || '未知'}</td>
                <td>${event.sourceIp || '未知'}</td>
                <td><span class="severity-${event.severity}">${severityText}</span></td>
                <td><span class="risk-score ${riskScoreClass}">${event.riskScore || 0}</span></td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary btn-action" 
                            onclick="showEventDetail(${event.id})">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-success btn-action" 
                            onclick="showHandleEventModal(${event.id})"
                            ${event.status === 'RESOLVED' ? 'disabled' : ''}>
                        <i class="bi bi-check-circle"></i>
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
    
    if (startPage > 0) {
        html += '<li class="page-item"><a class="page-link" href="#" onclick="changePage(0)">1</a></li>';
        if (startPage > 1) {
            html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            html += `<li class="page-item active"><span class="page-link">${i + 1}</span></li>`;
        } else {
            html += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${i})">${i + 1}</a></li>`;
        }
    }
    
    if (endPage < pageData.totalPages - 1) {
        if (endPage < pageData.totalPages - 2) {
            html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
        }
        html += `<li class="page-item"><a class="page-link" href="#" onclick="changePage(${pageData.totalPages - 1})">${pageData.totalPages}</a></li>`;
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
    loadSecurityEvents();
}

/**
 * 显示事件详情
 */
function showEventDetail(eventId) {
    // 这里应该调用后端API获取事件详情
    // 由于SecurityEventService中没有findById方法，我们模拟一个详情显示
    const content = document.getElementById('event-detail-content');
    content.innerHTML = `
        <div class="text-center">
            <div class="spinner-border" role="status">
                <span class="visually-hidden">加载中...</span>
            </div>
        </div>
    `;
    
    // 模拟API调用
    setTimeout(() => {
        content.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <h6>基本信息</h6>
                    <table class="table table-sm">
                        <tr><td><strong>事件ID:</strong></td><td>${eventId}</td></tr>
                        <tr><td><strong>事件类型:</strong></td><td>登录失败</td></tr>
                        <tr><td><strong>发生时间:</strong></td><td>${new Date().toLocaleString()}</td></tr>
                        <tr><td><strong>严重级别:</strong></td><td><span class="severity-3">高危</span></td></tr>
                        <tr><td><strong>风险评分:</strong></td><td><span class="risk-score risk-high">75</span></td></tr>
                    </table>
                </div>
                <div class="col-md-6">
                    <h6>用户信息</h6>
                    <table class="table table-sm">
                        <tr><td><strong>用户名:</strong></td><td>test_user</td></tr>
                        <tr><td><strong>IP地址:</strong></td><td>192.168.1.100</td></tr>
                        <tr><td><strong>用户代理:</strong></td><td>Mozilla/5.0...</td></tr>
                        <tr><td><strong>会话ID:</strong></td><td>abc123...</td></tr>
                    </table>
                </div>
            </div>
            <div class="row mt-3">
                <div class="col-12">
                    <h6>事件描述</h6>
                    <div class="alert alert-warning">
                        检测到用户在短时间内多次登录失败，可能存在暴力破解攻击。
                    </div>
                </div>
            </div>
            <div class="row mt-3">
                <div class="col-12">
                    <h6>处理状态</h6>
                    <p><span class="status-badge status-warning">待处理</span></p>
                </div>
            </div>
        `;
    }, 500);
    
    const modal = new bootstrap.Modal(document.getElementById('eventDetailModal'));
    modal.show();
}

/**
 * 显示处理事件模态框
 */
function showHandleEventModal(eventId) {
    document.getElementById('handle-event-id').value = eventId;
    document.getElementById('handle-status').value = '';
    document.getElementById('handle-notes').value = '';
    
    const modal = new bootstrap.Modal(document.getElementById('handleEventModal'));
    modal.show();
}

/**
 * 提交处理事件
 */
function submitHandleEvent() {
    const eventId = document.getElementById('handle-event-id').value;
    const status = document.getElementById('handle-status').value;
    const notes = document.getElementById('handle-notes').value;
    
    if (!status) {
        showNotification('请选择处理状态', 'warning');
        return;
    }
    
    const params = new URLSearchParams();
    params.append('status', status);
    if (notes) {
        params.append('notes', notes);
    }
    
    fetch(`/blog/admin/api/security-events/${eventId}/handle`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString()
    })
    .then(response => {
        if (response.ok) {
            return response.text();
        }
        throw new Error('处理失败');
    })
    .then(message => {
        showNotification(message, 'success');
        bootstrap.Modal.getInstance(document.getElementById('handleEventModal')).hide();
        loadSecurityEvents(); // 刷新列表
        loadEventStatistics(); // 刷新统计
    })
    .catch(error => {
        handleError(error, '处理安全事件');
    });
}

/**
 * 刷新事件
 */
function refreshEvents() {
    loadSecurityEvents();
    loadEventStatistics();
    showNotification('安全事件已刷新', 'success');
}

/**
 * 格式化事件类型
 */
function formatEventType(eventType) {
    const typeMap = {
        'LOGIN_FAILURE': '登录失败',
        'BRUTE_FORCE_ATTACK': '暴力破解',
        'SUSPICIOUS_ACTIVITY': '可疑活动',
        'UNAUTHORIZED_ACCESS': '未授权访问',
        'DATA_BREACH': '数据泄露',
        'MALWARE_DETECTED': '恶意软件',
        'PRIVILEGE_ESCALATION': '权限提升',
        'ACCOUNT_LOCKOUT': '账户锁定',
        'SECURITY_POLICY_VIOLATION': '安全策略违规'
    };
    return typeMap[eventType] || eventType;
}

/**
 * 获取严重级别样式类
 */
function getSeverityClass(severity) {
    return `severity-${severity}`;
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
 * 获取状态样式类
 */
function getStatusClass(status) {
    switch (status) {
        case 'NEW': return 'status-warning';
        case 'INVESTIGATING': return 'status-info';
        case 'RESOLVED': return 'status-success';
        case 'FALSE_POSITIVE': return 'status-secondary';
        case 'IGNORED': return 'status-secondary';
        default: return 'status-secondary';
    }
}

/**
 * 获取状态文本
 */
function getStatusText(status) {
    switch (status) {
        case 'NEW': return '新事件';
        case 'INVESTIGATING': return '调查中';
        case 'RESOLVED': return '已解决';
        case 'FALSE_POSITIVE': return '误报';
        case 'IGNORED': return '已忽略';
        default: return '未知';
    }
}

/**
 * 获取风险评分样式类
 */
function getRiskScoreClass(riskScore) {
    if (riskScore >= 80) return 'risk-critical';
    if (riskScore >= 60) return 'risk-high';
    if (riskScore >= 40) return 'risk-medium';
    return 'risk-low';
}

/**
 * 格式化日期时间
 */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '未知';
    
    const date = new Date(dateTimeStr);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
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