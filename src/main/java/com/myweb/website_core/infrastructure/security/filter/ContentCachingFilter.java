package com.myweb.website_core.infrastructure.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 *  请求体包装过滤器
 *  <p>
 *  功能：
 *  缓存请求体，以便在处理请求后访问原始数据。
 * @author Myweb
 * @version 1.2
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentCachingFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
        log.info("ContentCaching过滤器初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        //  检查请求是否是 HttpServletRequest
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查请求类型是否包含请求体（例如 POST, PUT, PATCH）
        String method = httpRequest.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 创建 ContentCachingRequestWrapper 来包装原始请求
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(httpRequest);

        try {
            // 将包装后的请求传递给过滤器链中的下一个过滤器
            chain.doFilter(cachingRequest, response);
        } finally {
            // byte[] requestBody = cachingRequest.getContentAsByteArray();
            // if (requestBody.length > 0) {
            //     String body = new String(requestBody, httpRequest.getCharacterEncoding());
            //     System.out.println("Request Body: " + body);
            // }
        }
    }
    /**
     * 过滤器销毁
     */
    @Override
    public void destroy() {
        log.info("ContentCaching过滤器销毁");
    }
}
