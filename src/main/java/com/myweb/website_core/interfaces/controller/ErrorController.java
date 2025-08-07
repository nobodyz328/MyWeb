package com.myweb.website_core.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 错误页面控制器
 * 处理各种错误页面的显示
 * 
 * @author MyWeb Team
 * @version 1.0
 * @since 2025-01-01
 */
@Controller
@RequestMapping("/error")
public class ErrorController {
    
    /**
     * 访问频率限制错误页面
     * 
     * @return 错误页面模板
     */
    @GetMapping("/rate-limit")
    public String rateLimitError() {
        return "error/rate-limit";
    }
}