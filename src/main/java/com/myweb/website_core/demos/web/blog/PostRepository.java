package com.myweb.website_core.demos.web.blog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 可扩展自定义查询
    List<Post> findByAuthorId(Long authorId);
}
