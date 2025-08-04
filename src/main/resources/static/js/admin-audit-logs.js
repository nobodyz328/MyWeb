// 审计日志管理JavaScript

let currentPage = 0;
let currentSize = 20;
let currentFilters = {};

document.addEventListener('DOMContentLoaded', function() {
    loadAuditLogs();
    
    // 绑定搜索表单事件
    document.getElementById('filter-form').addEventListener('submit', function(e) {
        e.preventDefault();
        currentPage = 0;
        loadAuditLogs();
    });
});

/**
 * 加载审计日志
 */
function loadAuditLogs() {
    showLoading('logs-table-body');
    
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
    
    fetch(`/blog/admin/api/audit-logs?${params.toString()}`)
        .then(response => response.json())
        .then(data => {
            renderAuditLogsTable(data.content);
            renderPagination(data);
        })
        .catch(error => {
            handleError(error, '加载审计日志');
            document.getElementById('logs-table-body').innerHTML = 
                '<tr><td colspan="7" class="text-center text-danger">加载失败</td></tr>';
        });
}

/**
 * 渲染审计日志表格
 */
function renderAuditLogsTable(logs) {
    const tbody = document.getElementById('logs-table-body');
    
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">暂无数据</td></tr>';
        return;
    }
    
    let html = '';
    logs.forEach(log => {
        const resultClass = log.result === 'SUCCESS' ? 'status-success' : 'status-danger';
        const operationText = formatOperation(log.operation);
        const timeText = formatDateTime(log.timestamp);
        const resourceText = formatResource(log.resourceType, log.resourceId);
        
        html += `
            <tr>
                <td>${timeText}</td>
                <td>${log.username || '未知'}</td>
                <td>${operationText}</td>
                <td>${resourceText}</td>
                <td>${log.ipAddress || '未知'}</td>
                <td><span class="status-badge ${resultClass}">${log.result}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary btn-action" 
                            onclick="showLogDetail(${log.id})">
                        <i class="bi bi-eye"></i> 详情
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
    loadAuditLogs();
}

/**
 * 显示日志详情
 */
function showLogDetail(logId) {
    fetch(`/blog/api/admin/audit-logs/${logId}`)
        .then(response => response.json())
        .then(log => {
            const content = document.getElementById('log-detail-content');
            content.innerHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <h6>基本信息</h6>
                        <table class="table table-sm">
                            <tr><td><strong>ID:</strong></td><td>${log.id}</td></tr>
                            <tr><td><strong>时间:</strong></td><td>${formatDateTime(log.timestamp)}</td></tr>
                            <tr><td><strong>用户:</strong></td><td>${log.username || '未知'}</td></tr>
                            <tr><td><strong>操作:</strong></td><td>${formatOperation(log.operation)}</td></tr>
                            <tr><td><strong>结果:</strong></td><td><span class="status-badge ${log.result === 'SUCCESS' ? 'status-success' : 'status-danger'}">${log.result}</span></td></tr>
                        </table>
                    </div>
                    <div class="col-md-6">
                        <h6>网络信息</h6>
                        <table class="table table-sm">
                            <tr><td><strong>IP地址:</strong></td><td>${log.ipAddress || '未知'}</td></tr>
                            <tr><td><strong>用户代理:</strong></td><td>${log.userAgent || '未知'}</td></tr>
                            <tr><td><strong>会话ID:</strong></td><td>${log.sessionId || '未知'}</td></tr>
                            <tr><td><strong>执行时间:</strong></td><td>${log.executionTime || 0}ms</td></tr>
                        </table>
                    </div>
                </div>
                <div class="row mt-3">
                    <div class="col-12">
                        <h6>资源信息</h6>
                        <table class="table table-sm">
                            <tr><td><strong>资源类型:</strong></td><td>${log.resourceType || '未知'}</td></tr>
                            <tr><td><strong>资源ID:</strong></td><td>${log.resourceId || '未知'}</td></tr>
                        </table>
                    </div>
                </div>
                ${log.errorMessage ? `
                <div class="row mt-3">
                    <div class="col-12">
                        <h6>错误信息</h6>
                        <div class="alert alert-danger">
                            <pre>${log.errorMessage}</pre>
                        </div>
                    </div>
                </div>
                ` : ''}
                ${log.requestData ? `
                <div class="row mt-3">
                    <div class="col-12">
                        <h6>请求数据</h6>
                        <pre class="bg-light p-3 rounded"><code>${JSON.stringify(JSON.parse(log.requestData), null, 2)}</code></pre>
                    </div>
                </div>
                ` : ''}
                ${log.responseData ? `
                <div class="row mt-3">
                    <div class="col-12">
                        <h6>响应数据</h6>
                        <pre class="bg-light p-3 rounded"><code>${JSON.stringify(JSON.parse(log.responseData), null, 2)}</code></pre>
                    </div>
                </div>
                ` : ''}
            `;
            
            const modal = new bootstrap.Modal(document.getElementById('logDetailModal'));
            modal.show();
        })
        .catch(error => {
            handleError(error, '获取日志详情');
        });
}

/**
 * 刷新日志
 */
function refreshLogs() {
    loadAuditLogs();
    showNotification('日志已刷新', 'success');
}

/**
 * 导出日志
 */
function exportLogs() {
    const formData = new FormData(document.getElementById('filter-form'));
    const filters = {};
    
    for (let [key, value] of formData.entries()) {
        if (value.trim()) {
            filters[key] = value.trim();
        }
    }
    
    // 显示导出选项
    const exportModal = document.createElement('div');
    exportModal.className = 'modal fade';
    exportModal.innerHTML = `
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">导出审计日志</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">导出格式</label>
                        <div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="exportFormat" id="formatCsv" value="CSV" checked>
                                <label class="form-check-label" for="formatCsv">CSV格式</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="exportFormat" id="formatExcel" value="EXCEL">
                                <label class="form-check-label" for="formatExcel">Excel格式</label>
                            </div>
                        </div>
                    </div>
                    <div class="alert alert-info">
                        <i class="bi bi-info-circle"></i>
                        将导出当前筛选条件下的所有日志记录（最多10000条）
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="button" class="btn btn-primary" onclick="doExport()">开始导出</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(exportModal);
    const modal = new bootstrap.Modal(exportModal);
    modal.show();
    
    // 导出完成后清理
    exportModal.addEventListener('hidden.bs.modal', function() {
        document.body.removeChild(exportModal);
    });
    
    window.doExport = function() {
        const format = document.querySelector('input[name="exportFormat"]:checked').value;
        
        fetch('/blog/api/admin/audit-logs/export', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                ...filters,
                exportFormat: format
            })
        })
        .then(response => {
            if (response.ok) {
                return response.blob();
            }
            throw new Error('导出失败');
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `audit_logs_${new Date().toISOString().slice(0, 10)}.${format.toLowerCase()}`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            
            modal.hide();
            showNotification('导出成功', 'success');
        })
        .catch(error => {
            handleError(error, '导出日志');
        });
    };
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
        'SECURITY_EVENT_VIEW': '查看安全事件',
        'USER_MANAGEMENT_VIEW': '查看用户管理',
        'ROLE_MANAGEMENT_VIEW': '查看角色管理'
    };
    return operationMap[operation] || operation;
}

/**
 * 格式化资源信息
 */
function formatResource(resourceType, resourceId) {
    if (!resourceType) return '未知';
    
    const typeMap = {
        'POST': '帖子',
        'COMMENT': '评论',
        'USER': '用户',
        'ROLE': '角色',
        'PERMISSION': '权限',
        'AUDIT_LOG': '审计日志',
        'SECURITY_EVENT': '安全事件',
        'ADMIN': '管理功能',
        'SYSTEM': '系统'
    };
    
    const typeName = typeMap[resourceType] || resourceType;
    return resourceId ? `${typeName}:${resourceId}` : typeName;
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
                <td colspan="7" class="text-center">
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