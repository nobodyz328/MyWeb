/**
 * CSRF令牌工具类
 * 提供CSRF令牌的获取、设置和Ajax请求集成功能
 * 
 * @author Kiro
 * @since 1.0.0
 */
class CsrfUtils {
    
    constructor() {
        this.tokenInfo = null;
        this.refreshPromise = null;
        this.init();
    }
    
    /**
     * 初始化CSRF工具
     */
    init() {
        // 从Cookie中读取初始令牌
        this.loadTokenFromCookie();
        
        // 设置Ajax请求拦截器
        this.setupAjaxInterceptors();
        
        // 设置表单提交拦截器
        this.setupFormInterceptors();
    }
    
    /**
     * 从Cookie中加载令牌
     */
    loadTokenFromCookie() {
        const token = this.getCookie('XSRF-TOKEN');
        if (token) {
            this.tokenInfo = {
                token: token,
                headerName: 'X-XSRF-TOKEN',
                parameterName: '_csrf',
                timestamp: Date.now()
            };
        }
    }
    
    /**
     * 获取Cookie值
     */
    getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) {
            return parts.pop().split(';').shift();
        }
        return null;
    }
    
    /**
     * 获取CSRF令牌
     */
    async getToken() {
        if (this.tokenInfo && this.isTokenValid()) {
            return this.tokenInfo;
        }
        
        return await this.refreshToken();
    }
    
    /**
     * 检查令牌是否有效
     */
    isTokenValid() {
        if (!this.tokenInfo) {
            return false;
        }
        
        // 检查令牌是否过期（2小时）
        const now = Date.now();
        const tokenAge = now - (this.tokenInfo.timestamp || 0);
        const maxAge = 2 * 60 * 60 * 1000; // 2小时
        
        return tokenAge < maxAge;
    }
    
    /**
     * 刷新CSRF令牌
     */
    async refreshToken() {
        // 防止并发刷新
        if (this.refreshPromise) {
            return await this.refreshPromise;
        }
        
        this.refreshPromise = this.doRefreshToken();
        
        try {
            const result = await this.refreshPromise;
            return result;
        } finally {
            this.refreshPromise = null;
        }
    }
    
    /**
     * 执行令牌刷新
     */
    async doRefreshToken() {
        try {
            const response = await fetch('/blog/api/csrf/token', {
                method: 'GET',
                credentials: 'same-origin',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const result = await response.json();
            
            if (result.success && result.data) {
                this.tokenInfo = {
                    ...result.data,
                    timestamp: Date.now()
                };
                
                console.debug('CSRF token refreshed successfully');
                return this.tokenInfo;
            } else {
                throw new Error(result.message || 'Failed to refresh CSRF token');
            }
            
        } catch (error) {
            console.error('Failed to refresh CSRF token:', error);
            this.tokenInfo = null;
            throw error;
        }
    }
    
    /**
     * 设置Ajax请求拦截器
     */
    setupAjaxInterceptors() {
        // jQuery Ajax拦截器
        if (typeof $ !== 'undefined' && $.ajaxSetup) {
            const self = this;
            
            $.ajaxSetup({
                beforeSend: async function(xhr, settings) {
                    // 只对需要CSRF保护的请求添加令牌
                    if (self.needsCsrfProtection(settings.type, settings.url)) {
                        try {
                            const tokenInfo = await self.getToken();
                            if (tokenInfo) {
                                xhr.setRequestHeader(tokenInfo.headerName, tokenInfo.token);
                            }
                        } catch (error) {
                            console.error('Failed to add CSRF token to request:', error);
                        }
                    }
                },
                
                error: function(xhr, status, error) {
                    // 处理CSRF错误
                    if (xhr.status === 403 && xhr.responseJSON && xhr.responseJSON.error === 'csrf_token_invalid') {
                        self.handleCsrfError(xhr.responseJSON);
                    }
                }
            });
        }
        
        // Fetch API拦截器
        if (typeof window.fetch !== 'undefined') {
            const originalFetch = window.fetch;
            const self = this;
            
            window.fetch = async function(url, options = {}) {
                // 只对需要CSRF保护的请求添加令牌
                if (self.needsCsrfProtection(options.method || 'GET', url)) {
                    try {
                        const tokenInfo = await self.getToken();
                        if (tokenInfo) {
                            options.headers = options.headers || {};
                            options.headers[tokenInfo.headerName] = tokenInfo.token;
                        }
                    } catch (error) {
                        console.error('Failed to add CSRF token to fetch request:', error);
                    }
                }
                
                const response = await originalFetch(url, options);
                
                // 处理CSRF错误
                if (response.status === 403) {
                    try {
                        const errorData = await response.clone().json();
                        if (errorData.error === 'csrf_token_invalid') {
                            self.handleCsrfError(errorData);
                        }
                    } catch (e) {
                        // 忽略JSON解析错误
                    }
                }
                
                return response;
            };
        }
    }
    
    /**
     * 设置表单提交拦截器
     */
    setupFormInterceptors() {
        const self = this;
        
        // 监听表单提交事件
        document.addEventListener('submit', async function(event) {
            const form = event.target;
            
            // 检查是否需要CSRF保护
            if (self.needsCsrfProtection(form.method || 'POST', form.action)) {
                // 检查表单中是否已有CSRF令牌字段
                let csrfInput = form.querySelector('input[name="_csrf"]');
                
                try {
                    const tokenInfo = await self.getToken();
                    
                    if (tokenInfo) {
                        if (!csrfInput) {
                            // 创建隐藏的CSRF令牌字段
                            csrfInput = document.createElement('input');
                            csrfInput.type = 'hidden';
                            csrfInput.name = tokenInfo.parameterName;
                            form.appendChild(csrfInput);
                        }
                        
                        // 设置令牌值
                        csrfInput.value = tokenInfo.token;
                    }
                } catch (error) {
                    console.error('Failed to add CSRF token to form:', error);
                    // 可以选择阻止表单提交
                    // event.preventDefault();
                }
            }
        });
    }
    
    /**
     * 判断请求是否需要CSRF保护
     */
    needsCsrfProtection(method, url) {
        // GET、HEAD、OPTIONS、TRACE请求不需要CSRF保护
        const safeMethods = ['GET', 'HEAD', 'OPTIONS', 'TRACE'];
        if (safeMethods.includes((method || 'GET').toUpperCase())) {
            return false;
        }
        
        // 检查URL是否在忽略列表中
        const ignoredPaths = [
            '/blog/login',
            '/blog/register',
            '/blog/static/',
            '/blog/css/',
            '/blog/js/',
            '/blog/images/',
            '/blog/',
            '/blog/view/',
            '/blog/users/register',
            '/blog/users/login',
            '/blog/users/register/code',
            '/blog/post/',
            '/blog/api/posts',
            '/blog/api/images/',
            '/blog/posts/top-liked',
            '/blog/search',
            '/blog/announcements',
            '/blog/api/csrf/token'
        ];
        
        if (typeof url === 'string') {
            for (const ignoredPath of ignoredPaths) {
                if (url.startsWith(ignoredPath)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 处理CSRF错误
     */
    handleCsrfError(errorData) {
        console.warn('CSRF token validation failed:', errorData.message);
        
        // 清除无效的令牌
        this.tokenInfo = null;
        
        // 显示用户友好的错误消息
        if (typeof window.showNotification === 'function') {
            window.showNotification('安全令牌已过期，请刷新页面后重试', 'warning');
        } else {
            alert('安全令牌已过期，请刷新页面后重试');
        }
        
        // 可以选择自动刷新页面
        // setTimeout(() => window.location.reload(), 2000);
    }
    
    /**
     * 手动添加CSRF令牌到请求头
     */
    async addTokenToHeaders(headers = {}) {
        try {
            const tokenInfo = await this.getToken();
            if (tokenInfo) {
                headers[tokenInfo.headerName] = tokenInfo.token;
            }
        } catch (error) {
            console.error('Failed to add CSRF token to headers:', error);
        }
        return headers;
    }
    
    /**
     * 手动添加CSRF令牌到表单数据
     */
    async addTokenToFormData(formData) {
        try {
            const tokenInfo = await this.getToken();
            if (tokenInfo) {
                if (formData instanceof FormData) {
                    formData.append(tokenInfo.parameterName, tokenInfo.token);
                } else if (typeof formData === 'object') {
                    formData[tokenInfo.parameterName] = tokenInfo.token;
                }
            }
        } catch (error) {
            console.error('Failed to add CSRF token to form data:', error);
        }
        return formData;
    }
}

// 创建全局实例
window.csrfUtils = new CsrfUtils();

// 导出为模块（如果支持）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CsrfUtils;
}