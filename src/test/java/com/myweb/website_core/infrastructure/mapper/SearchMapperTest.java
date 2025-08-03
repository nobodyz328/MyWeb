package com.myweb.website_core.infrastructure.mapper;

import com.myweb.website_core.domain.business.vo.PostSearchVO;
import com.myweb.website_core.domain.business.vo.UserSearchVO;
import com.myweb.website_core.infrastructure.persistence.mapper.PostMapper;
import com.myweb.website_core.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 搜索功能数据访问层测试
 * 
 * 测试PostMapper和UserMapper的搜索功能
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SearchMapperTest {
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 测试帖子搜索功能
     */
    @Test
    public void testPostSearch() {
        // 测试基本搜索功能
        String keyword = "test";
        List<PostSearchVO> results = postMapper.searchPostsWithPagination(keyword, "RELEVANCE", 0, 10);
        
        // 验证结果不为null
        assertNotNull(results);
        
        // 测试搜索结果计数
        Long count = postMapper.countSearchPosts(keyword);
        assertNotNull(count);
        assertTrue(count >= 0);
        
        // 测试按标题搜索
        List<PostSearchVO> titleResults = postMapper.searchPostsByTitle(keyword, 0, 10);
        assertNotNull(titleResults);
        
        // 测试按内容搜索
        List<PostSearchVO> contentResults = postMapper.searchPostsByContent(keyword, 0, 10);
        assertNotNull(contentResults);
        
        // 测试按作者搜索
        List<PostSearchVO> authorResults = postMapper.searchPostsByAuthor(keyword, 0, 10);
        assertNotNull(authorResults);
        
        // 测试获取热门帖子
        List<PostSearchVO> hotPosts = postMapper.getHotPosts(10);
        assertNotNull(hotPosts);
    }
    
    /**
     * 测试用户搜索功能
     */
    @Test
    public void testUserSearch() {
        // 测试基本搜索功能
        String keyword = "test";
        List<UserSearchVO> results = userMapper.searchUsersWithPagination(keyword, "RELEVANCE", 0, 10);
        
        // 验证结果不为null
        assertNotNull(results);
        
        // 测试搜索结果计数
        Long count = userMapper.countSearchUsers(keyword);
        assertNotNull(count);
        assertTrue(count >= 0);
        
        // 测试按用户名搜索
        List<UserSearchVO> usernameResults = userMapper.searchUsersByUsername(keyword, 0, 10);
        assertNotNull(usernameResults);
        
        // 测试按个人简介搜索
        List<UserSearchVO> bioResults = userMapper.searchUsersByBio(keyword, 0, 10);
        assertNotNull(bioResults);
        
        // 测试获取活跃用户
        List<UserSearchVO> activeUsers = userMapper.getActiveUsers(10);
        assertNotNull(activeUsers);
    }
    
    /**
     * 测试搜索排序功能
     */
    @Test
    public void testSearchSorting() {
        String keyword = "test";
        
        // 测试按相关性排序
        List<PostSearchVO> relevanceResults = postMapper.searchPostsWithPagination(keyword, "RELEVANCE", 0, 10);
        assertNotNull(relevanceResults);
        
        // 测试按时间排序
        List<PostSearchVO> timeResults = postMapper.searchPostsWithPagination(keyword, "TIME", 0, 10);
        assertNotNull(timeResults);
        
        // 测试按热度排序
        List<PostSearchVO> popularityResults = postMapper.searchPostsWithPagination(keyword, "POPULARITY", 0, 10);
        assertNotNull(popularityResults);
        
        // 测试用户按相关性排序
        List<UserSearchVO> userRelevanceResults = userMapper.searchUsersWithPagination(keyword, "RELEVANCE", 0, 10);
        assertNotNull(userRelevanceResults);
        
        // 测试用户按热度排序
        List<UserSearchVO> userPopularityResults = userMapper.searchUsersWithPagination(keyword, "POPULARITY", 0, 10);
        assertNotNull(userPopularityResults);
    }
    
    /**
     * 测试分页功能
     */
    @Test
    public void testPagination() {
        String keyword = "test";
        
        // 测试第一页
        List<PostSearchVO> page1 = postMapper.searchPostsWithPagination(keyword, "RELEVANCE", 0, 5);
        assertNotNull(page1);
        assertTrue(page1.size() <= 5);
        
        // 测试第二页
        List<PostSearchVO> page2 = postMapper.searchPostsWithPagination(keyword, "RELEVANCE", 5, 5);
        assertNotNull(page2);
        assertTrue(page2.size() <= 5);
        
        // 测试用户分页
        List<UserSearchVO> userPage1 = userMapper.searchUsersWithPagination(keyword, "RELEVANCE", 0, 5);
        assertNotNull(userPage1);
        assertTrue(userPage1.size() <= 5);
    }
    
    /**
     * 测试空关键词处理
     */
    @Test
    public void testEmptyKeyword() {
        // 测试空字符串
        List<PostSearchVO> emptyResults = postMapper.searchPostsWithPagination("", "RELEVANCE", 0, 10);
        assertNotNull(emptyResults);
        
        // 测试空格字符串
        List<PostSearchVO> spaceResults = postMapper.searchPostsWithPagination("   ", "RELEVANCE", 0, 10);
        assertNotNull(spaceResults);
    }
}