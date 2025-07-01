package com.myweb.website_core.demos.web;

import com.myweb.website_core.demos.web.blog.BlogPost;
import com.myweb.website_core.demos.web.blog.BlogPostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    // 初始化一些测试数据
    @Bean
    public CommandLineRunner initData(BlogPostRepository repository) {
        return args -> {
            repository.save(new BlogPost("Spring Boot入门", "Spring Boot让Java开发变得简单...", "张三"));
            repository.save(new BlogPost("RESTful API设计", "如何设计良好的REST API...", "李四"));
            System.out.println("初始化了2篇测试博客文章");
        };
    }
}