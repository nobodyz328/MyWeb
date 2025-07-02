package com.myweb.website_core.demos.web.blog;

import com.myweb.website_core.demos.web.blog.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    //自动提供CRUD操作
}
