/**
 * JWT认证工具类
 * 提供统一的JWT令牌管理和API请求功能
 */
class AuthUtils {
    
    /**
     * 获取访问令牌
     */
    static getAccessToken() {
        return localStorage.getItem('accessToken');
    }
    
    /**
     * 获取刷新令牌
     */
    static getRefreshToken() {
        return localStorage.getItem('refreshToken');
    }
    
    /**
     * 获取当前用户ID
     */
    static getUserId() {
        return localStorage.getItem('userId');
    }
    
    /**
     * 获取当前用户名
     */
    static getUsername() {
        return localStorage.getItem('username');
    }
    
    /**
     * 检查用户是否已登录
     */
    static isLoggedIn() {
        const accessToken = this.getAccessToken();
        return !!(accessToken && !this.isTokenExpired());
    }
    
    /**
     * 检查令牌是否过期
     */
    static isTokenExpired() {
        const accessToken = this.getAccessToken();
        if (!accessToken) return true;
        
        try {
            // 解析JWT令牌的payload部分
            const payload = JSON.parse(atob(accessToken.split('.')[1]));
            const currentTime = Math.floor(Date.now() / 1000);
            return payload.exp < currentTime;
        } catch (error) {
            console.error('解析令牌失败:', error);
            return true;
        }
    }
    
    /**
     * 清除所有认证信息
     */
    static clearAuth() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        localStorage.removeItem('tokenType');
        localStorage.removeItem('expiresIn');
    }
    
    /**
     * 刷新访问令牌
     */
    static async refreshAccessToken() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            throw new Error('没有刷新令牌');
        }
        
        try {
            const response = await fetch('/blog/users/refresh-token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });
            
            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('accessToken', data.accessToken);
                localStorage.setItem('refreshToken', data.refreshToken);
                localStorage.setItem('tokenType', data.tokenType || 'Bearer');
                localStorage.setItem('expiresIn', data.expiresIn);
                return data.accessToken;
            } else {
                throw new Error('刷新令牌失败');
            }
        } catch (error) {
            console.error('刷新令牌失败:', error);
            this.clearAuth();
            throw error;
        }
    }
    
    /**
     * 获取认证头
     */
    static getAuthHeaders() {
        const accessToken = this.getAccessToken();
        const tokenType = localStorage.getItem('tokenType') || 'Bearer';
        
        if (accessToken) {
            return {
                'Authorization': `${tokenType} ${accessToken}`
            };
        }
        return {};
    }
    
    /**
     * 发送认证请求
     * 自动处理令牌刷新和重试，并支持CSRF保护
     */
    static async authenticatedFetch(url, options = {}) {
        // 检查是否需要登录
        if (!this.isLoggedIn()) {
            throw new Error('用户未登录');
        }
        
        // 准备请求头
        const headers = {
            ...this.getAuthHeaders(),
            ...(options.headers || {})
        };
        
        // 只有在没有FormData时才设置Content-Type
        if (!(options.body instanceof FormData) && !headers['Content-Type']) {
            headers['Content-Type'] = 'application/json';
        }
        
        // 如果是非API请求且需要CSRF保护，添加CSRF令牌
        if (AuthUtils.needsCsrfProtection(options.method || 'GET', url)) {
            try {
                await AuthUtils.addCsrfToken(headers);
            } catch (csrfError) {
                console.warn('添加CSRF令牌失败:', csrfError);
            }
        }
        
        const requestOptions = {
            ...options,
            headers
        };
        
        try {
            let response = await fetch(url, requestOptions);
            
            // 如果令牌过期，尝试刷新
            if (response.status === 401) {
                console.log('访问令牌过期，尝试刷新...');
                
                try {
                    await this.refreshAccessToken();
                    
                    // 更新请求头并重试
                    requestOptions.headers = {
                        ...requestOptions.headers,
                        ...this.getAuthHeaders()
                    };
                    
                    response = await fetch(url, requestOptions);
                } catch (refreshError) {
                    console.error('刷新令牌失败，跳转到登录页:', refreshError);
                    this.redirectToLogin();
                    throw refreshError;
                }
            }
            
            // 如果是CSRF错误，尝试刷新CSRF令牌并重试
            if (response.status === 403) {
                try {
                    const errorData = await response.clone().json();
                    if (errorData.message && errorData.message.includes('CSRF')) {
                        console.log('CSRF令牌无效，尝试刷新...');
                        
                        // 刷新CSRF令牌并重试
                        await AuthUtils.addCsrfToken(requestOptions.headers, true);
                        response = await fetch(url, requestOptions);
                    }
                } catch (e) {
                    // 忽略JSON解析错误
                }
            }
            
            return response;
        } catch (error) {
            console.error('认证请求失败:', error);
            throw error;
        }
    }
    
    /**
     * 判断请求是否需要CSRF保护
     */
    static needsCsrfProtection(method, url) {
        // GET、HEAD、OPTIONS、TRACE请求不需要CSRF保护
        const safeMethods = ['GET', 'HEAD', 'OPTIONS', 'TRACE'];
        if (safeMethods.includes((method).toUpperCase())) {
            return false;
        }
        
        // API接口不需要CSRF保护（使用JWT）
        if (url && (
            url.includes('/api/')
        )) {
            return false;
        }
        
        // CSRF令牌获取接口不需要CSRF保护
        // if (url && url.includes('/api/csrf/token')) {
        //     return false;
        // }
        
        return true;
    }
    
    /**
     * 添加CSRF令牌到请求头
     */
    static async addCsrfToken(headers, forceRefresh = false) {
        try {
            // 首先尝试从Cookie中获取CSRF令牌
            const cookieToken = this.getCsrfTokenFromCookie();
            if (cookieToken && !forceRefresh) {
                headers['X-XSRF-TOKEN'] = cookieToken;
                return;
            }
            
            // 如果Cookie中没有或需要强制刷新，从服务器获取
            const response = await fetch('/blog/api/csrf/token', {
                method: 'GET',
                credentials: 'same-origin'
            });
            
            if (response.ok) {
                const data = await response.json();
                if (data.success && data.data) {
                    headers[data.data.headerName] = data.data.token;
                }
            }
        } catch (error) {
            console.error('获取CSRF令牌失败:', error);
        }
    }
    
    /**
     * 从Cookie中获取CSRF令牌
     */
    static getCsrfTokenFromCookie() {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; XSRF-TOKEN=`);
        if (parts.length === 2) {
            return parts.pop().split(';').shift();
        }
        return null;
    }

    
    /**
     * 跳转到登录页
     */
    static redirectToLogin() {
        this.clearAuth();
        window.location.href = '/blog/login';
    }
    
    /**
     * 退出登录
     */
    static async logout() {
        try {
            // 调用后端退出接口
            await this.authenticatedFetch('/blog/users/logout', {
                method: 'POST'
            });
        } catch (error) {
            console.error('退出登录请求失败:', error);
        } finally {
            // 无论后端请求是否成功，都清除本地认证信息
            this.clearAuth();
            window.location.href = '/blog/view';
        }
    }
    
    /**
     * 检查认证状态并在需要时跳转到登录页
     */
    static requireAuth() {
        if (!this.isLoggedIn()) {
            console.log('用户未登录，跳转到登录页');
            this.redirectToLogin();
            return false;
        }
        return true;
    }
    
    /**
     * 初始化认证检查
     * 在页面加载时调用，检查令牌有效性
     */
    static async initAuth() {
        if (!this.isLoggedIn()) {
            return false;
        }
        
        // 如果令牌即将过期，尝试刷新
        try {
            const payload = JSON.parse(atob(this.getAccessToken().split('.')[1]));
            const currentTime = Math.floor(Date.now() / 1000);
            const timeUntilExpiry = payload.exp - currentTime;
            
            if (timeUntilExpiry < 300) { // 5分钟
                console.log('令牌即将过期，预先刷新...');
                await this.refreshAccessToken();
            }
            
            return true;
        } catch (error) {
            console.error('初始化认证检查失败:', error);
            this.clearAuth();
            return false;
        }
    }
}

// 全局可用
window.AuthUtils = AuthUtils;