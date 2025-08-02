package com.myweb.website_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MyWeb 博客系统主应用类
 * <p>
 * 功能特性：
 * - 异步博客社区系统
 * - 用户注册/登录/关注
 * - 帖子发布/编辑/点赞/收藏
 * - 评论系统
 * - 公告管理
 * - 消息队列和缓存支持
 * <p>
 * 技术栈：
 * - Java 21
 * - Spring Boot 3.2.0
 * - Spring Security
 * - PostgreSQL
 * - RabbitMQ
 * - Redis
 * - MyBatis
 * - Thymeleaf
 */
@SpringBootApplication
@EnableAsync
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}