package com.myweb.website_core.demos.web.blog;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BlogService {
    private final BlogPostRepository repository;

    public BlogService(BlogPostRepository repository) {
        this.repository = repository;
    }

    public BlogPost createPost(BlogPost  post){
        return repository.save(post);
    }
    public List<BlogPost> getAllPosts(){
        return repository.findAll();
    }
    public Optional<BlogPost> getPostById(Long id){
        return repository.findById(id);
    }
    public BlogPost updatePost(Long id, BlogPost updatedPost){
        return repository.findById(id)
                .map(post -> {
                    post.setTitle(updatedPost.getTitle());
                    post.setContent(updatedPost.getContent());
                    return repository.save(post);
                })
                .orElseGet(() -> {
                    updatedPost.setId(id);
                    return repository.save(updatedPost);
                });
    }
    public void deletePost(Long id) {
        repository.deleteById(id);
    }
}
