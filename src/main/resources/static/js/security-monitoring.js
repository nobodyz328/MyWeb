// 安全监控JavaScript

// 全局变量
let securityTrendChart = null;
let threatTypeChart = null;
let userBehaviorChart = null;
let autoRefreshInterval = null;
let isAutoRefreshEnabled = false;

document.addEventListener('DOMContentLoaded', function() {
    initializeSecurityMonitoring();
});

/**
 * 初始化安全监控
 */
function initializeSecurityMonitoring() {
    loadAllSecurityData();
    initializeCharts();
    setupEventListeners();
    
    // 每30秒自动刷新实时数据
    setInterval(loadRealtimeData, 30000);
}

/**
 * 加载所有安全数据
 */
function loadAllSecurityData() {
    loadSystemStatistics();
    loadRealtimeAlerts();
    loadHotIpAddresses();
    loadUserBehaviorAnalysis();
    loadSecurityTrends('7d');
}

/**
 * 加载系统统计数据
 */
function loadSystemStatistics() {
    fetch('/blog/admin/api/statistics')
        .then(response => response.json())
        .then(data => {
            updateStatistics(data);
        })
        .catch(error => {
            console.error('加载系统统计失败:', error);
            showNotification('加载系统统计失败', 'danger');
        });
}

/**
 * 更新统计数据显示
 */
function updateStatistics(data) {
    // 更新今日审计日志
    updateElement('today-audit-logs', data.todayAuditLogs || 0);
    updateElement('today-failures', data.todayFailures || 0);
    
    // 更新今日登录次数
    updateElement('today-logins', data.todayLogins || 0);
    updateElement('today-login-failures', data.todayLoginFailures || 0);
    
    // 更新安全事件
    updateElement('today-security-events', data.todaySecurityEvents || 0);
    updateElement('high-risk-events', data.highRiskEvents || 0);
    
    // 更新用户统计
    updateElement('online-users', data.activeUsers || 0);
    updateElement('total-users', data.totalUsers || 0);
    
    // 更新系统负载
    if (data.systemLoad) {
        updateElement('system-load', data.systemLoad.status || '正常');
        updateElement('cpu-usage', data.systemLoad.cpuUsage || '0%');
        updateElement('memory-usage', data.systemLoad.memoryUsage || '0%');
    }
    
    // 更新安全状态
    updateSecurityStatus(data);
}

/**
 * 更新安全状态显示
 */
function updateSecurityStatus(data) {
    const systemSecurityStatus = document.getElementById('system-security-status');
    const threatLevel = document.getElementById('threat-level');
    
    if (systemSecurityStatus) {
        let statusHtml = '<span class="threat-level-indicator threat-level-1"></span>正常';
        let statusDetail = '所有系统运行正常';
        
        if (data.highRiskEvents > 0) {
            statusHtml = '<span class="threat-level-indicator threat-level-3"></span>警告';
            statusDetail = `检测到 ${data.highRiskEvents} 个高危事件`;
        }
        
        systemSecurityStatus.innerHTML = statusHtml;
        updateElement('security-status-detail', statusDetail);
    }
    
    if (threatLevel) {
        let level = '低危';
        let levelClass = 'threat-level-2';
        let detail = '检测到轻微异常';
        
        if (data.highRiskEvents > 5) {
            level = '高危';
            levelClass = 'threat-level-4';
            detail = '检测到多个高危事件';
        } else if (data.highRiskEvents > 0) {
            level = '中危';
            levelClass = 'threat-level-3';
            detail = '检测到安全事件';
        }
        
        threatLevel.innerHTML = `<span class="threat-level-indicator ${levelClass}"></span>${level}`;
        updateElement('threat-level-detail', detail);
    }
}

/**
 * 加载实时告警
 */
function loadRealtimeAlerts() {
    fetch('/blog/admin/api/realtime-alerts')
        .then(response => response.json())
        .then(data => {
            displayRealtimeAlerts(data);
        })
        .catch(error => {
            console.error('加载实时告警失败:', error);
        });
}

/**
 * 显示实时告警
 */
function displayRealtimeAlerts(alerts) {
    const container = document.getElementById('realtime-alerts');
    if (!container) return;
    
    if (alerts.length === 0) {
        container.innerHTML = `
            <div class="text-center text-muted">
                <i class="bi bi-shield-check fa-2x"></i>
                <p class="mt-2">暂无安全告警</p>
            </div>
        `;
        return;
    }
    
    let html = '';
    alerts.forEach(alert => {
        const severityClass = getSeverityAlertClass(alert.severity);
        const timeText = formatDateTime(alert.timestamp);
        
        html += `
            <div class="alert-item ${severityClass}">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <strong>${alert.title}</strong>
                        <p class="mb-1">${alert.description}</p>
                        <small class="text-muted">
                            用户: ${alert.username || '未知'} | 
                            IP: ${alert.sourceIp || '未知'} | 
                            时间: ${timeText}
                        </small>
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-secondary" 
                            onclick="dismissAlert(${alert.id})">
                        <i class="bi bi-x"></i>
                    </button>
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

/**
 * 加载热点IP地址
 */
function loadHotIpAddresses() {
    fetch('/blog/admin/api/hot-ips')
        .then(response => response.json())
        .then(data => {
            displayHotIpAddresses(data);
        })
        .catch(error => {
            console.error('加载热点IP失败:', error);
        });
}

/**
 * 显示热点IP地址
 */
function displayHotIpAddresses(ips) {
    const container = document.getElementById('hot-ip-addresses');
    if (!container) return;
    
    if (ips.length === 0) {
        container.innerHTML = '<div class="text-center text-muted">暂无热点IP数据</div>';
        return;
    }
    
    let html = '<div class="list-group list-group-flush">';
    ips.forEach(ip => {
        const riskClass = getRiskLevelClass(ip.riskLevel);
        const timeText = formatDateTime(ip.lastAccess);
        
        html += `
            <div class="list-group-item d-flex justify-content-between align-items-center">
                <div>
                    <strong>${ip.ip}</strong>
                    <br>
                    <small class="text-muted">${ip.location} | 访问: ${ip.requestCount}次</small>
                </div>
                <div class="text-end">
                    <span class="badge ${riskClass}">${getRiskLevelText(ip.riskLevel)}</span>
                    <br>
                    <small class="text-muted">${timeText}</small>
                </div>
            </div>
        `;
    });
    html += '</div>';
    
    container.innerHTML = html;
}

/**
 * 加载用户行为分析
 */
function loadUserBehaviorAnalysis() {
    fetch('/blog/admin/api/user-behavior')
        .then(response => response.json())
        .then(data => {
            updateUserBehaviorData(data);
            updateUserBehaviorChart(data.hourlyActivity);
        })
        .catch(error => {
            console.error('加载用户行为分析失败:', error);
        });
}

/**
 * 更新用户行为数据
 */
function updateUserBehaviorData(data) {
    updateElement('active-sessions', data.activeSessions || 0);
    updateElement('suspicious-users', data.suspiciousUsers || 0);
}

/**
 * 加载安全趋势数据
 */
function loadSecurityTrends(period) {
    const days = period === '7d' ? 7 : period === '30d' ? 30 : 90;
    
    fetch(`/blog/admin/api/security-trends?days=${days}`)
        .then(response => response.json())
        .then(data => {
            updateSecurityTrendChart(data.dailyTrends);
            updateThreatTypeChart(data.threatTypes);
            
            // 更新按钮状态
            document.querySelectorAll('.btn-group .btn').forEach(btn => {
                btn.classList.remove('active');
            });
            event.target.classList.add('active');
        })
        .catch(error => {
            console.error('加载安全趋势失败:', error);
        });
}

/**
 * 初始化图表
 */
function initializeCharts() {
    initSecurityTrendChart();
    initThreatTypeChart();
    initUserBehaviorChart();
}

/**
 * 初始化安全趋势图表
 */
function initSecurityTrendChart() {
    const ctx = document.getElementById('securityTrendChart');
    if (!ctx) return;
    
    securityTrendChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: '安全事件总数',
                data: [],
                borderColor: 'rgb(255, 99, 132)',
                backgroundColor: 'rgba(255, 99, 132, 0.2)',
                tension: 0.1
            }, {
                label: '高危事件',
                data: [],
                borderColor: 'rgb(255, 159, 64)',
                backgroundColor: 'rgba(255, 159, 64, 0.2)',
                tension: 0.1
            }, {
                label: '登录失败',
                data: [],
                borderColor: 'rgb(54, 162, 235)',
                backgroundColor: 'rgba(54, 162, 235, 0.2)',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

/**
 * 初始化威胁类型图表
 */
function initThreatTypeChart() {
    const ctx = document.getElementById('threatTypeChart');
    if (!ctx) return;
    
    threatTypeChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: [],
            datasets: [{
                data: [],
                backgroundColor: [
                    '#FF6384',
                    '#36A2EB',
                    '#FFCE56',
                    '#4BC0C0',
                    '#9966FF'
                ]
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

/**
 * 初始化用户行为图表
 */
function initUserBehaviorChart() {
    const ctx = document.getElementById('userBehaviorChart');
    if (!ctx) return;
    
    userBehaviorChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: Array.from({length: 24}, (_, i) => `${i}:00`),
            datasets: [{
                label: '活跃用户数',
                data: [],
                backgroundColor: 'rgba(54, 162, 235, 0.5)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

/**
 * 更新安全趋势图表
 */
function updateSecurityTrendChart(data) {
    if (!securityTrendChart || !data) return;
    
    const labels = data.map(item => item.date);
    const totalEvents = data.map(item => item.totalEvents);
    const highRiskEvents = data.map(item => item.highRiskEvents);
    const loginFailures = data.map(item => item.loginFailures);
    
    securityTrendChart.data.labels = labels;
    securityTrendChart.data.datasets[0].data = totalEvents;
    securityTrendChart.data.datasets[1].data = highRiskEvents;
    securityTrendChart.data.datasets[2].data = loginFailures;
    securityTrendChart.update();
}

/**
 * 更新威胁类型图表
 */
function updateThreatTypeChart(data) {
    if (!threatTypeChart || !data) return;
    
    const labels = Object.keys(data);
    const values = Object.values(data);
    
    threatTypeChart.data.labels = labels;
    threatTypeChart.data.datasets[0].data = values;
    threatTypeChart.update();
}

/**
 * 更新用户行为图表
 */
function updateUserBehaviorChart(data) {
    if (!userBehaviorChart || !data) return;
    
    const hourlyData = new Array(24).fill(0);
    data.forEach(item => {
        if (item.hour >= 0 && item.hour < 24) {
            hourlyData[item.hour] = item.userCount;
        }
    });
    
    userBehaviorChart.data.datasets[0].data = hourlyData;
    userBehaviorChart.update();
}

/**
 * 设置事件监听器
 */
function setupEventListeners() {
    // 趋势图表时间段按钮
    document.querySelectorAll('[onclick^="loadTrendChart"]').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const period = this.textContent.includes('7') ? '7d' : 
                          this.textContent.includes('30') ? '30d' : '90d';
            loadSecurityTrends(period);
        });
    });
}

/**
 * 加载实时数据
 */
function loadRealtimeData() {
    if (isAutoRefreshEnabled) {
        loadSystemStatistics();
        loadRealtimeAlerts();
    }
}

/**
 * 刷新数据
 */
function refreshData() {
    showNotification('正在刷新数据...', 'info');
    loadAllSecurityData();
}

/**
 * 导出安全报告
 */
function exportSecurityReport() {
    const reportType = 'SECURITY_SUMMARY';
    const days = 7;
    const format = 'PDF';
    
    fetch('/blog/admin/api/export-report', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `reportType=${reportType}&days=${days}&format=${format}`
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            showNotification(data.error, 'danger');
        } else {
            showNotification(`报告生成中，报告ID: ${data.reportId}`, 'success');
        }
    })
    .catch(error => {
        console.error('导出报告失败:', error);
        showNotification('导出报告失败', 'danger');
    });
}

/**
 * 切换自动刷新
 */
function toggleAutoRefresh() {
    const icon = document.getElementById('auto-refresh-icon');
    const text = document.getElementById('auto-refresh-text');
    
    if (isAutoRefreshEnabled) {
        // 停止自动刷新
        clearInterval(autoRefreshInterval);
        isAutoRefreshEnabled = false;
        icon.className = 'bi bi-play-circle';
        text.textContent = '自动刷新';
        showNotification('自动刷新已停止', 'info');
    } else {
        // 开始自动刷新
        autoRefreshInterval = setInterval(loadRealtimeData, 10000); // 10秒刷新一次
        isAutoRefreshEnabled = true;
        icon.className = 'bi bi-pause-circle';
        text.textContent = '停止刷新';
        showNotification('自动刷新已启动', 'success');
    }
}

/**
 * 清除所有告警
 */
function clearAllAlerts() {
    if (!confirm('确定要清除所有告警吗？')) {
        return;
    }
    
    fetch('/blog/admin/api/clear-alerts', {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showNotification(data.message, 'success');
            loadRealtimeAlerts(); // 重新加载告警
        } else {
            showNotification(data.error || '清除失败', 'danger');
        }
    })
    .catch(error => {
        console.error('清除告警失败:', error);
        showNotification('清除告警失败', 'danger');
    });
}

/**
 * 忽略单个告警
 */
function dismissAlert(alertId) {
    // 简单的前端移除，实际应该调用API
    const alertElement = event.target.closest('.alert-item');
    if (alertElement) {
        alertElement.remove();
    }
}

// 工具函数

/**
 * 更新元素内容
 */
function updateElement(id, value) {
    const element = document.getElementById(id);
    if (element) {
        element.textContent = value;
    }
}

/**
 * 获取严重级别告警样式类
 */
function getSeverityAlertClass(severity) {
    switch (severity) {
        case 1: return 'alert-low';
        case 2: return 'alert-low';
        case 3: return 'alert-medium';
        case 4: return 'alert-high';
        case 5: return 'alert-high';
        default: return 'alert-low';
    }
}

/**
 * 获取风险级别样式类
 */
function getRiskLevelClass(riskLevel) {
    switch (riskLevel) {
        case 'HIGH': return 'bg-danger';
        case 'MEDIUM': return 'bg-warning';
        case 'LOW': return 'bg-success';
        default: return 'bg-secondary';
    }
}

/**
 * 获取风险级别文本
 */
function getRiskLevelText(riskLevel) {
    switch (riskLevel) {
        case 'HIGH': return '高危';
        case 'MEDIUM': return '中危';
        case 'LOW': return '低危';
        default: return '未知';
    }
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

// 全局函数，供HTML调用
window.refreshData = refreshData;
window.exportSecurityReport = exportSecurityReport;
window.toggleAutoRefresh = toggleAutoRefresh;
window.clearAllAlerts = clearAllAlerts;
window.loadTrendChart = loadSecurityTrends;