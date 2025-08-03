-- 创建安全事件表
-- 用于记录系统中发生的各种安全事件，支持安全监控和告警
-- 符合GB/T 22239-2019二级等保要求

CREATE TABLE security_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    severity INTEGER NOT NULL,
    user_id BIGINT,
    username VARCHAR(50),
    source_ip VARCHAR(45),
    user_agent VARCHAR(500),
    request_uri VARCHAR(500),
    request_method VARCHAR(10),
    session_id VARCHAR(100),
    event_data TEXT,
    event_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    alerted BOOLEAN NOT NULL DEFAULT FALSE,
    alert_time TIMESTAMP,
    handled_by VARCHAR(50),
    handled_time TIMESTAMP,
    handle_notes TEXT,
    risk_score INTEGER,
    related_event_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 创建索引以优化查询性能
CREATE INDEX idx_security_event_type ON security_events(event_type);
CREATE INDEX idx_security_event_user ON security_events(user_id);
CREATE INDEX idx_security_event_ip ON security_events(source_ip);
CREATE INDEX idx_security_event_time ON security_events(event_time);
CREATE INDEX idx_security_event_severity ON security_events(severity);
CREATE INDEX idx_security_event_status ON security_events(status);
CREATE INDEX idx_security_event_alerted ON security_events(alerted);
CREATE INDEX idx_security_event_created_at ON security_events(created_at);

-- 创建复合索引以支持常见查询模式
CREATE INDEX idx_security_event_user_type_time ON security_events(user_id, event_type, event_time);
CREATE INDEX idx_security_event_ip_time ON security_events(source_ip, event_time);
CREATE INDEX idx_security_event_severity_time ON security_events(severity, event_time);
CREATE INDEX idx_security_event_status_time ON security_events(status, event_time);

-- 添加表注释
COMMENT ON TABLE security_events IS '安全事件表 - 记录系统安全事件，支持监控和告警';
COMMENT ON COLUMN security_events.id IS '主键ID';
COMMENT ON COLUMN security_events.event_type IS '事件类型';
COMMENT ON COLUMN security_events.title IS '事件标题';
COMMENT ON COLUMN security_events.description IS '事件描述';
COMMENT ON COLUMN security_events.severity IS '严重级别(1-5)';
COMMENT ON COLUMN security_events.user_id IS '相关用户ID';
COMMENT ON COLUMN security_events.username IS '相关用户名';
COMMENT ON COLUMN security_events.source_ip IS '源IP地址';
COMMENT ON COLUMN security_events.user_agent IS '用户代理';
COMMENT ON COLUMN security_events.request_uri IS '请求URI';
COMMENT ON COLUMN security_events.request_method IS '请求方法';
COMMENT ON COLUMN security_events.session_id IS '会话ID';
COMMENT ON COLUMN security_events.event_data IS '事件详细数据(JSON格式)';
COMMENT ON COLUMN security_events.event_time IS '事件发生时间';
COMMENT ON COLUMN security_events.status IS '事件状态(NEW/PROCESSING/RESOLVED/IGNORED)';
COMMENT ON COLUMN security_events.alerted IS '是否已告警';
COMMENT ON COLUMN security_events.alert_time IS '告警时间';
COMMENT ON COLUMN security_events.handled_by IS '处理人员';
COMMENT ON COLUMN security_events.handled_time IS '处理时间';
COMMENT ON COLUMN security_events.handle_notes IS '处理备注';
COMMENT ON COLUMN security_events.risk_score IS '风险评分(0-100)';
COMMENT ON COLUMN security_events.related_event_count IS '相关事件数量';
COMMENT ON COLUMN security_events.created_at IS '创建时间';
COMMENT ON COLUMN security_events.updated_at IS '更新时间';

-- 创建更新时间触发器
CREATE OR REPLACE FUNCTION update_security_events_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_security_events_updated_at
    BEFORE UPDATE ON security_events
    FOR EACH ROW
    EXECUTE FUNCTION update_security_events_updated_at();

-- 插入一些示例数据用于测试（可选）
INSERT INTO security_events (
    event_type, title, description, severity, username, source_ip, 
    user_agent, request_uri, request_method, event_time, risk_score
) VALUES 
(
    'MULTIPLE_LOGIN_FAILURES', 
    '多次登录失败', 
    '用户在短时间内多次登录失败', 
    4, 
    'testuser', 
    '192.168.1.100', 
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    '/api/auth/login',
    'POST',
    CURRENT_TIMESTAMP,
    75
),
(
    'UNAUTHORIZED_ACCESS_ATTEMPT', 
    '未授权访问尝试', 
    '用户尝试访问未授权资源', 
    4, 
    'normaluser', 
    '192.168.1.101', 
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
    '/api/admin/users',
    'GET',
    CURRENT_TIMESTAMP,
    80
),
(
    'ABNORMAL_ACCESS_FREQUENCY', 
    '访问频率异常', 
    '检测到异常的访问频率', 
    3, 
    'botuser', 
    '192.168.1.102', 
    'Python-requests/2.28.1',
    '/api/posts',
    'GET',
    CURRENT_TIMESTAMP,
    60
);