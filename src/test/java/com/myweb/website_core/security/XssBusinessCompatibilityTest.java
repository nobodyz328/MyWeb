package com.myweb.website_core.security;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import com.myweb.website_core.infrastructure.security.filter.XssFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * XSS防护业务功能兼容性测试
 * <p>
 * 验证XSS防护功能不会影响正常的业务功能，确保用户体验不受影响。
 * <p>
 * 测试场景：
 * 1. 博客文章发布功能
 * 2. 用户评论功能
 * 3. 富文本编辑器内容
 * 4. 用户个人资料编辑
 * 5. 文件上传描述
 * 6. 搜索功能
 * 7. 管理员操作
 * <p>
 * 符合需求：2.1, 2.2, 2.5 - 业务功能兼容性验证
 * 
 * @author MyWeb
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("XSS防护业务功能兼容性测试")
class XssBusinessCompatibilityTest {
    
    @Mock
    private XssFilterConfig xssFilterConfig;
    
    @Mock
    private XssStatisticsService xssStatisticsService;
    
    @Mock
    private XssMonitoringService xssMonitoringService;
    
    @Mock
    private XssFilterConfig.PerformanceConfig performanceConfig;
    
    @Mock
    private XssFilterConfig.StatisticsConfig statisticsConfig;
    
    @Mock
    private XssFilterConfig.MonitoringConfig monitoringConfig;
    
    private XssFilterService xssFilter;
    
    private static final String TEST_CLIENT_IP = "192.168.1.1";
    private static final String TEST_REQUEST_URI = "/api/posts";
    
    @BeforeEach
    void setUp() {
        setupConfiguration();
        xssFilter = new XssFilterService(xssFilterConfig, xssStatisticsService, xssMonitoringService);
    }
    
    private void setupConfiguration() {
        when(xssFilterConfig.isEnabled()).thenReturn(true);
        when(xssFilterConfig.isStrictMode()).thenReturn(false);
        when(xssFilterConfig.getAllowedTags()).thenReturn(Set.of("b", "i", "u", "strong", "em", "p", "br", "a", "img", "h1", "h2", "h3", "ul", "ol", "li", "blockquote", "code", "pre"));
        when(xssFilterConfig.getAllowedAttributes()).thenReturn(Set.of("href", "src", "alt", "title", "class", "id", "width", "height"));
        when(xssFilterConfig.getTagSpecificAttributes()).thenReturn(Map.of(
            "a", Set.of("href", "title", "target"),
            "img", Set.of("src", "alt", "title", "width", "height"),
            "p", Set.of("class", "id"),
            "div", Set.of("class", "id")
        ));
        when(xssFilterConfig.isRemoveUnknownTags()).thenReturn(true);
        when(xssFilterConfig.isEncodeSpecialChars()).thenReturn(true);
        when(xssFilterConfig.getMaxTagDepth()).thenReturn(10);
        when(xssFilterConfig.getMaxContentLength()).thenReturn(50000);
        when(xssFilterConfig.getCustomXssPatterns()).thenReturn(List.of());
        when(xssFilterConfig.getWhitelistUrlPatterns()).thenReturn(List.of());
        
        when(xssFilterConfig.getPerformance()).thenReturn(performanceConfig);
        when(performanceConfig.isCacheEnabled()).thenReturn(true);
        when(performanceConfig.getCacheSize()).thenReturn(1000);
        when(performanceConfig.getCacheExpirationMinutes()).thenReturn(30);
        
        when(xssFilterConfig.getStatistics()).thenReturn(statisticsConfig);
        when(statisticsConfig.isEnabled()).thenReturn(true);
        
        when(xssFilterConfig.getMonitoring()).thenReturn(monitoringConfig);
        when(monitoringConfig.isEnabled()).thenReturn(true);
    }
    
    @Test
    @DisplayName("测试博客文章发布功能兼容性")
    void testBlogPostPublishingCompatibility() {
        // Given - 典型的博客文章内容
        String blogPost = """
            <h1>Spring Boot 3.2 新特性详解</h1>
            <p>Spring Boot 3.2 版本带来了许多令人兴奋的新特性，本文将详细介绍这些特性。</p>
            
            <h2>主要新特性</h2>
            <ul>
                <li><strong>性能优化</strong>：启动速度提升30%</li>
                <li><strong>安全增强</strong>：新增多项安全配置</li>
                <li><em>开发体验</em>：更好的开发工具支持</li>
            </ul>
            
            <h2>代码示例</h2>
            <pre><code>
            @RestController
            public class HelloController {
                @GetMapping("/hello")
                public String hello() {
                    return "Hello, Spring Boot 3.2!";
                }
            }
            </code></pre>
            
            <p>更多信息请访问：<a href="https://spring.io/blog" title="Spring官方博客">Spring官方博客</a></p>
            
            <blockquote>
                <p>Spring Boot 3.2 是一个重要的里程碑版本。</p>
            </blockquote>
            
            <p>文章配图：</p>
            <img src="/uploads/spring-boot-3.2.jpg" alt="Spring Boot 3.2 特性图" width="600" height="400">
            """;
        
        // When
        String result = xssFilter.filterXss(blogPost, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then - 验证所有正常内容都被保留
        assertTrue(result.contains("<h1>Spring Boot 3.2 新特性详解</h1>"), "标题应被保留");
        assertTrue(result.contains("<h2>主要新特性</h2>"), "二级标题应被保留");
        assertTrue(result.contains("<ul>"), "无序列表应被保留");
        assertTrue(result.contains("<li>"), "列表项应被保留");
        assertTrue(result.contains("<strong>性能优化</strong>"), "强调标签应被保留");
        assertTrue(result.contains("<em>开发体验</em>"), "斜体标签应被保留");
        assertTrue(result.contains("<pre><code>"), "代码块应被保留");
        assertTrue(result.contains("<a href=\"https://spring.io/blog\""), "链接应被保留");
        assertTrue(result.contains("<blockquote>"), "引用块应被保留");
        assertTrue(result.contains("<img src=\"/uploads/spring-boot-3.2.jpg\""), "图片应被保留");
        assertTrue(result.contains("alt=\"Spring Boot 3.2 特性图\""), "图片属性应被保留");
        
        // 验证没有误报攻击
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试用户评论功能兼容性")
    void testUserCommentCompatibility() {
        // Given - 各种类型的用户评论
        String[] comments = {
            "很好的文章，感谢分享！",
            "这个<strong>解决方案</strong>很实用，已经在项目中使用了。",
            "补充一个链接：<a href=\"https://docs.spring.io\" title=\"Spring文档\">Spring官方文档</a>",
            "代码示例中有个小问题：<code>@GetMapping</code>应该是<code>@PostMapping</code>",
            "同意楼上的观点，<em>性能优化</em>确实很重要。",
            "<blockquote>引用原文：Spring Boot 3.2 是一个重要版本</blockquote>",
            "分享一张相关图片：<img src=\"/uploads/comment-image.jpg\" alt=\"评论图片\" width=\"200\">"
        };
        
        for (String comment : comments) {
            // When
            String result = xssFilter.filterXss(comment, "/api/comments", TEST_CLIENT_IP);
            
            // Then - 验证正常评论内容保持不变
            if (comment.contains("<strong>")) {
                assertTrue(result.contains("<strong>"), "强调标签应保留: " + comment);
            }
            if (comment.contains("<a href=")) {
                assertTrue(result.contains("<a href="), "链接应保留: " + comment);
            }
            if (comment.contains("<code>")) {
                assertTrue(result.contains("<code>"), "代码标签应保留: " + comment);
            }
            if (comment.contains("<em>")) {
                assertTrue(result.contains("<em>"), "斜体标签应保留: " + comment);
            }
            if (comment.contains("<blockquote>")) {
                assertTrue(result.contains("<blockquote>"), "引用块应保留: " + comment);
            }
            if (comment.contains("<img")) {
                assertTrue(result.contains("<img"), "图片应保留: " + comment);
            }
            
            // 验证文本内容完整
            assertTrue(result.length() > 0, "评论内容不应为空: " + comment);
        }
        
        // 验证没有误报攻击
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试富文本编辑器内容兼容性")
    void testRichTextEditorCompatibility() {
        // Given - 富文本编辑器生成的内容
        String richTextContent = """
            <p>这是一段包含多种格式的富文本内容。</p>
            <p><strong>粗体文本</strong>和<em>斜体文本</em>的组合。</p>
            <p><u>下划线文本</u>也应该被支持。</p>
            <h2>二级标题</h2>
            <h3>三级标题</h3>
            <ul>
                <li>无序列表项1</li>
                <li>无序列表项2</li>
            </ul>
            <ol>
                <li>有序列表项1</li>
                <li>有序列表项2</li>
            </ol>
            <blockquote>
                <p>这是一个引用块，通常用于引用他人的话。</p>
            </blockquote>
            <p>内联代码：<code>console.log('Hello World')</code></p>
            <pre><code>
            // 代码块
            function greet(name) {
                return `Hello, ${name}!`;
            }
            </code></pre>
            <p>链接示例：<a href="https://example.com" title="示例链接">点击这里</a></p>
            """;
        
        // When
        String result = xssFilter.filterXss(richTextContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then - 验证富文本格式完整保留
        assertTrue(result.contains("<p>"), "段落标签应保留");
        assertTrue(result.contains("<strong>"), "粗体标签应保留");
        assertTrue(result.contains("<em>"), "斜体标签应保留");
        assertTrue(result.contains("<u>"), "下划线标签应保留");
        assertTrue(result.contains("<h2>"), "二级标题应保留");
        assertTrue(result.contains("<h3>"), "三级标题应保留");
        assertTrue(result.contains("<ul>"), "无序列表应保留");
        assertTrue(result.contains("<ol>"), "有序列表应保留");
        assertTrue(result.contains("<li>"), "列表项应保留");
        assertTrue(result.contains("<blockquote>"), "引用块应保留");
        assertTrue(result.contains("<code>"), "内联代码应保留");
        assertTrue(result.contains("<pre>"), "代码块应保留");
        assertTrue(result.contains("<a href="), "链接应保留");
        
        // 验证内容完整性
        assertTrue(result.contains("这是一段包含多种格式的富文本内容"), "文本内容应完整");
        assertTrue(result.contains("console.log('Hello World')"), "代码内容应完整");
        assertTrue(result.contains("function greet(name)"), "代码块内容应完整");
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试用户个人资料编辑兼容性")
    void testUserProfileEditingCompatibility() {
        // Given - 用户个人资料内容
        String profileContent = """
            <p>大家好，我是一名<strong>Java开发工程师</strong>，专注于<em>Spring生态系统</em>的开发。</p>
            <p>技能栈包括：</p>
            <ul>
                <li>Java 17+</li>
                <li>Spring Boot 3.x</li>
                <li>MySQL & Redis</li>
                <li>Docker & Kubernetes</li>
            </ul>
            <p>个人博客：<a href="https://myblog.com" title="我的博客">https://myblog.com</a></p>
            <p>GitHub：<a href="https://github.com/myusername" title="我的GitHub">@myusername</a></p>
            <blockquote>
                <p>代码改变世界，技术创造未来。</p>
            </blockquote>
            """;
        
        // When
        String result = xssFilter.filterXss(profileContent, "/api/users/profile", TEST_CLIENT_IP);
        
        // Then
        assertTrue(result.contains("<p>大家好，我是一名<strong>Java开发工程师</strong>"), "个人介绍应保留");
        assertTrue(result.contains("<ul>"), "技能列表应保留");
        assertTrue(result.contains("<li>Java 17+</li>"), "技能项应保留");
        assertTrue(result.contains("<a href=\"https://myblog.com\""), "个人链接应保留");
        assertTrue(result.contains("<blockquote>"), "个人格言应保留");
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "Java开发经验分享",
        "Spring Boot <strong>最佳实践</strong>",
        "数据库优化<em>技巧</em>总结",
        "<code>SELECT * FROM users</code>查询优化",
        "微服务架构<u>设计模式</u>",
        "Docker容器化<strong>部署指南</strong>"
    })
    @DisplayName("测试搜索关键词兼容性")
    void testSearchKeywordCompatibility(String keyword) {
        // When
        String result = xssFilter.filterXss(keyword, "/api/search", TEST_CLIENT_IP);
        
        // Then - 搜索关键词应保持基本格式
        assertNotNull(result, "搜索结果不应为null");
        assertFalse(result.isEmpty(), "搜索结果不应为空");
        
        // 验证基本HTML标签保留
        if (keyword.contains("<strong>")) {
            assertTrue(result.contains("<strong>"), "强调标签应保留");
        }
        if (keyword.contains("<em>")) {
            assertTrue(result.contains("<em>"), "斜体标签应保留");
        }
        if (keyword.contains("<code>")) {
            assertTrue(result.contains("<code>"), "代码标签应保留");
        }
        if (keyword.contains("<u>")) {
            assertTrue(result.contains("<u>"), "下划线标签应保留");
        }
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试文件上传描述兼容性")
    void testFileUploadDescriptionCompatibility() {
        // Given - 文件上传时的描述信息
        String[] descriptions = {
            "项目截图 - 主页面设计",
            "<strong>重要文档</strong> - 系统架构图",
            "代码示例：<code>UserController.java</code>",
            "数据库设计图 - <em>用户表结构</em>",
            "API文档 - <a href=\"/docs/api.html\" title=\"API文档\">详细说明</a>",
            "配置文件：<code>application.yml</code> 示例"
        };
        
        for (String description : descriptions) {
            // When
            String result = xssFilter.filterXss(description, "/api/files/upload", TEST_CLIENT_IP);
            
            // Then
            assertNotNull(result, "描述不应为null: " + description);
            assertFalse(result.isEmpty(), "描述不应为空: " + description);
            
            // 验证基本格式保留
            if (description.contains("<strong>")) {
                assertTrue(result.contains("<strong>"), "强调标签应保留: " + description);
            }
            if (description.contains("<code>")) {
                assertTrue(result.contains("<code>"), "代码标签应保留: " + description);
            }
            if (description.contains("<em>")) {
                assertTrue(result.contains("<em>"), "斜体标签应保留: " + description);
            }
            if (description.contains("<a href=")) {
                assertTrue(result.contains("<a href="), "链接应保留: " + description);
            }
        }
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试管理员操作兼容性")
    void testAdminOperationCompatibility() {
        // Given - 管理员可能输入的内容
        String adminContent = """
            <h2>系统维护通知</h2>
            <p><strong>维护时间</strong>：2025年1月15日 02:00-04:00</p>
            <p><em>影响范围</em>：全站功能暂时不可用</p>
            <ul>
                <li>数据库升级</li>
                <li>安全补丁更新</li>
                <li>性能优化</li>
            </ul>
            <p>技术支持：<a href="mailto:admin@example.com" title="联系管理员">admin@example.com</a></p>
            <blockquote>
                <p>感谢您的理解与支持！</p>
            </blockquote>
            """;
        
        // When
        String result = xssFilter.filterXss(adminContent, "/api/admin/announcements", TEST_CLIENT_IP);
        
        // Then
        assertTrue(result.contains("<h2>系统维护通知</h2>"), "管理员标题应保留");
        assertTrue(result.contains("<strong>维护时间</strong>"), "重要信息标记应保留");
        assertTrue(result.contains("<em>影响范围</em>"), "强调信息应保留");
        assertTrue(result.contains("<ul>"), "维护项目列表应保留");
        assertTrue(result.contains("<a href=\"mailto:admin@example.com\""), "联系方式应保留");
        assertTrue(result.contains("<blockquote>"), "管理员留言应保留");
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试多语言内容兼容性")
    void testMultiLanguageContentCompatibility() {
        // Given - 多语言内容
        String multiLangContent = """
            <h2>Welcome / 欢迎 / ようこそ</h2>
            <p><strong>English</strong>: This is a multilingual blog system.</p>
            <p><strong>中文</strong>：这是一个多语言博客系统。</p>
            <p><strong>日本語</strong>：これは多言語ブログシステムです。</p>
            <ul>
                <li>Support for <em>multiple languages</em></li>
                <li>支持<em>多种语言</em></li>
                <li><em>複数の言語</em>をサポート</li>
            </ul>
            """;
        
        // When
        String result = xssFilter.filterXss(multiLangContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertTrue(result.contains("Welcome / 欢迎 / ようこそ"), "多语言标题应保留");
        assertTrue(result.contains("This is a multilingual blog system"), "英文内容应保留");
        assertTrue(result.contains("这是一个多语言博客系统"), "中文内容应保留");
        assertTrue(result.contains("これは多言語ブログシステムです"), "日文内容应保留");
        assertTrue(result.contains("<strong>"), "格式标签应保留");
        assertTrue(result.contains("<em>"), "强调标签应保留");
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试特殊字符内容兼容性")
    void testSpecialCharacterContentCompatibility() {
        // Given - 包含特殊字符的内容
        String specialCharContent = """
            <p>数学公式：E = mc²</p>
            <p>货币符号：$100, €50, ¥300, £25</p>
            <p>特殊符号：© 2025 MyBlog™ ® ℠</p>
            <p>编程符号：<code>if (x > 0 && y < 10) { return true; }</code></p>
            <p>引号测试："Hello" 'World' "你好" '世界'</p>
            <p>其他符号：→ ← ↑ ↓ ★ ☆ ♠ ♥ ♦ ♣</p>
            """;
        
        // When
        String result = xssFilter.filterXss(specialCharContent, TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertTrue(result.contains("E = mc²"), "数学符号应保留");
        assertTrue(result.contains("$100, €50, ¥300, £25"), "货币符号应保留");
        assertTrue(result.contains("© 2025 MyBlog™"), "版权符号应保留");
        assertTrue(result.contains("<code>if (x > 0 && y < 10)"), "代码中的特殊字符应保留");
        assertTrue(result.contains("→ ← ↑ ↓"), "箭头符号应保留");
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("测试长内容处理兼容性")
    void testLongContentProcessingCompatibility() {
        // Given - 长文章内容
        StringBuilder longContent = new StringBuilder();
        longContent.append("<h1>长文章标题</h1>");
        
        for (int i = 1; i <= 50; i++) {
            longContent.append("<h2>第").append(i).append("章</h2>");
            longContent.append("<p>这是第").append(i).append("章的内容，包含<strong>重要信息</strong>和<em>详细说明</em>。</p>");
            longContent.append("<ul>");
            for (int j = 1; j <= 5; j++) {
                longContent.append("<li>要点").append(j).append("：详细描述内容</li>");
            }
            longContent.append("</ul>");
            longContent.append("<p>参考链接：<a href=\"https://example.com/chapter").append(i).append("\" title=\"第").append(i).append("章参考\">详细资料</a></p>");
        }
        
        // When
        String result = xssFilter.filterXss(longContent.toString(), TEST_REQUEST_URI, TEST_CLIENT_IP);
        
        // Then
        assertTrue(result.contains("<h1>长文章标题</h1>"), "主标题应保留");
        assertTrue(result.contains("<h2>第1章</h2>"), "章节标题应保留");
        assertTrue(result.contains("<h2>第50章</h2>"), "最后章节标题应保留");
        assertTrue(result.contains("<strong>重要信息</strong>"), "强调内容应保留");
        assertTrue(result.contains("<em>详细说明</em>"), "斜体内容应保留");
        assertTrue(result.contains("<ul>"), "列表应保留");
        assertTrue(result.contains("<li>要点1"), "列表项应保留");
        assertTrue(result.contains("<a href=\"https://example.com/chapter"), "链接应保留");
        
        // 验证内容长度合理（应该与原内容长度相近）
        assertTrue(result.length() > longContent.length() * 0.9, "处理后内容长度应与原内容相近");
        
        verify(xssStatisticsService, never()).recordXssAttack(any(), any(), any(), any());
    }
}