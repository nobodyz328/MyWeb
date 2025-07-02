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


}