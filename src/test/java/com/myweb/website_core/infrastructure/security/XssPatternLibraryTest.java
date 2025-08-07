package com.myweb.website_core.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XSS攻击模式库单元测试
 * 
 * 测试XssPatternLibrary的各种功能：
 * 1. 各种XSS攻击模式检测
 * 2. 模式分类功能
 * 3. 攻击类型识别
 * 4. 边界情况处理
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
class XssPatternLibraryTest {
    
    @Test
    void testContainsXssPattern_ScriptInjection_ShouldDetect() {
        // Given
        String[] scriptAttacks = {
            "<script>alert('XSS')</script>",
            "<script type='text/javascript'>alert('XSS')</script>",
            "<SCRIPT>alert('XSS')</SCRIPT>",
            "<script>",
            "</script>",
            "<script/src='http://evil.com/xss.js'>"
        };
        
        // When & Then
        for (String attack : scriptAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到脚本注入攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_JavascriptProtocol_ShouldDetect() {
        // Given
        String[] jsProtocolAttacks = {
            "javascript:alert('XSS')",
            "JAVASCRIPT:alert('XSS')",
            "vbscript:msgbox('XSS')",
            "data:text/html,<script>alert('XSS')</script>",
            "data:text/javascript,alert('XSS')"
        };
        
        // When & Then
        for (String attack : jsProtocolAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到JavaScript协议攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_EventHandlers_ShouldDetect() {
        // Given
        String[] eventHandlerAttacks = {
            "onload=alert('XSS')",
            "onerror='alert(1)'",
            "onclick=\"alert('XSS')\"",
            "onmouseover=alert(1)",
            "onfocus=alert('XSS')",
            "onblur=alert('XSS')",
            "onchange=alert('XSS')",
            "onsubmit=alert('XSS')",
            "onkeydown=alert('XSS')",
            "onmousedown=alert('XSS')"
        };
        
        // When & Then
        for (String attack : eventHandlerAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到事件处理器攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_Expressions_ShouldDetect() {
        // Given
        String[] expressionAttacks = {
            "expression(alert('XSS'))",
            "eval(alert('XSS'))",
            "setTimeout(alert('XSS'), 1000)",
            "setInterval(alert('XSS'), 1000)",
            "Function('alert(1)')()",
            "execScript('alert(1)')"
        };
        
        // When & Then
        for (String attack : expressionAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到表达式攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_StyleAttacks_ShouldDetect() {
        // Given
        String[] styleAttacks = {
            "<style>body{background:url('javascript:alert(1)')}</style>",
            "@import 'javascript:alert(1)'",
            "@charset 'javascript:alert(1)'",
            "behavior:url('javascript:alert(1)')",
            "-moz-binding:url('javascript:alert(1)')"
        };
        
        // When & Then
        for (String attack : styleAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到样式攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_LinkAttacks_ShouldDetect() {
        // Given
        String[] linkAttacks = {
            "src='javascript:alert(1)'",
            "href=\"javascript:alert(1)\"",
            "src=javascript:alert(1)",
            "href=vbscript:msgbox(1)",
            "action='javascript:alert(1)'",
            "formaction=\"javascript:alert(1)\""
        };
        
        // When & Then
        for (String attack : linkAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到链接攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_FrameAttacks_ShouldDetect() {
        // Given
        String[] frameAttacks = {
            "<iframe src='javascript:alert(1)'></iframe>",
            "<frame src='javascript:alert(1)'>",
            "<frameset><frame src='javascript:alert(1)'></frameset>",
            "<object data='javascript:alert(1)'></object>",
            "<embed src='javascript:alert(1)'>",
            "<applet code='javascript:alert(1)'></applet>"
        };
        
        // When & Then
        for (String attack : frameAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到框架攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_FormAttacks_ShouldDetect() {
        // Given
        String[] formAttacks = {
            "<form action='javascript:alert(1)'>",
            "<input type='text' onfocus='alert(1)'>",
            "<textarea onchange='alert(1)'></textarea>",
            "<select onchange='alert(1)'></select>",
            "<button onclick='alert(1)'>Click</button>",
            "formaction='javascript:alert(1)'"
        };
        
        // When & Then
        for (String attack : formAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到表单攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_MetaAttacks_ShouldDetect() {
        // Given
        String[] metaAttacks = {
            "<meta http-equiv='refresh' content='0;url=javascript:alert(1)'>",
            "<meta http-equiv='Set-Cookie' content='test=value'>",
            "<base href='javascript:alert(1)'>",
            "<link rel='stylesheet' href='javascript:alert(1)'>"
        };
        
        // When & Then
        for (String attack : metaAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到Meta标签攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_EncodingBypass_ShouldDetect() {
        // Given
        String[] encodingAttacks = {
            "&#60;script&#62;alert('XSS')&#60;/script&#62;",
            "%3Cscript%3Ealert('XSS')%3C/script%3E",
            "\\x3Cscript\\x3Ealert('XSS')\\x3C/script\\x3E",
            "\\u003Cscript\\u003Ealert('XSS')\\u003C/script\\u003E"
        };
        
        // When & Then
        for (String attack : encodingAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到编码绕过攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_Comments_ShouldDetect() {
        // Given
        String[] commentAttacks = {
            "<!-- <script>alert('XSS')</script> -->",
            "/* <script>alert('XSS')</script> */",
            "// alert('XSS')",
            "<![CDATA[<script>alert('XSS')</script>]]>"
        };
        
        // When & Then
        for (String attack : commentAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到注释攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_SafeContent_ShouldNotDetect() {
        // Given
        String[] safeContent = {
            "This is normal text content",
            "User input with numbers 12345",
            "Content with special chars: !@#$%^&*()",
            "Unicode content: 中文内容测试",
            "Email: user@example.com",
            "URL: https://example.com/path?param=value",
            "HTML entities: &lt;div&gt;safe content&lt;/div&gt;",
            ""
        };
        
        // When & Then
        for (String content : safeContent) {
            assertFalse(XssPatternLibrary.containsXssPattern(content), 
                "不应该检测到XSS攻击: " + content);
        }
    }
    
    @Test
    void testContainsXssPattern_NullAndEmpty_ShouldNotDetect() {
        // When & Then
        assertFalse(XssPatternLibrary.containsXssPattern(null));
        assertFalse(XssPatternLibrary.containsXssPattern(""));
    }
    
    @Test
    void testGetMatchedPatternTypes_ScriptInjection_ShouldReturnCorrectType() {
        // Given
        String scriptAttack = "<script>alert('XSS')</script>";
        
        // When
        List<String> matchedTypes = XssPatternLibrary.getMatchedPatternTypes(scriptAttack);
        
        // Then
        assertNotNull(matchedTypes);
        assertTrue(matchedTypes.contains("SCRIPT_INJECTION"));
    }
    
    @Test
    void testGetMatchedPatternTypes_MultipleTypes_ShouldReturnAllTypes() {
        // Given
        String combinedAttack = "<script>alert('XSS')</script>" +
                               "javascript:alert('XSS')" +
                               "onload=alert('XSS')";
        
        // When
        List<String> matchedTypes = XssPatternLibrary.getMatchedPatternTypes(combinedAttack);
        
        // Then
        assertNotNull(matchedTypes);
        assertTrue(matchedTypes.contains("SCRIPT_INJECTION"));
        assertTrue(matchedTypes.contains("JAVASCRIPT_PROTOCOL"));
        assertTrue(matchedTypes.contains("EVENT_HANDLER"));
    }
    
    @Test
    void testGetMatchedPatternTypes_SafeContent_ShouldReturnEmptyList() {
        // Given
        String safeContent = "This is safe content";
        
        // When
        List<String> matchedTypes = XssPatternLibrary.getMatchedPatternTypes(safeContent);
        
        // Then
        assertNotNull(matchedTypes);
        assertTrue(matchedTypes.isEmpty());
    }
    
    @Test
    void testGetMatchedPatternTypes_NullInput_ShouldReturnEmptyList() {
        // When
        List<String> matchedTypes = XssPatternLibrary.getMatchedPatternTypes(null);
        
        // Then
        assertNotNull(matchedTypes);
        assertTrue(matchedTypes.isEmpty());
    }
    
    @Test
    void testGetAllPatternCategories_ShouldReturnAllCategories() {
        // When
        List<List<java.util.regex.Pattern>> allCategories = XssPatternLibrary.getAllPatternCategories();
        
        // Then
        assertNotNull(allCategories);
        assertEquals(11, allCategories.size()); // 应该有11个分类
        
        // 验证每个分类都不为空
        for (List<java.util.regex.Pattern> category : allCategories) {
            assertNotNull(category);
            assertFalse(category.isEmpty());
        }
    }
    
    @Test
    void testContainsXssPattern_CaseInsensitive_ShouldDetect() {
        // Given
        String[] caseVariations = {
            "<SCRIPT>alert('XSS')</SCRIPT>",
            "<Script>alert('XSS')</Script>",
            "JAVASCRIPT:alert('XSS')",
            "JavaScript:alert('XSS')",
            "ONLOAD=alert('XSS')",
            "OnLoad=alert('XSS')"
        };
        
        // When & Then
        for (String attack : caseVariations) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到大小写变体攻击: " + attack);
        }
    }
    
    @Test
    void testContainsXssPattern_ComplexRealWorldAttacks_ShouldDetect() {
        // Given
        String[] realWorldAttacks = {
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "<body onload=alert('XSS')>",
            "<input onfocus=alert('XSS') autofocus>",
            "<select onfocus=alert('XSS') autofocus>",
            "<textarea onfocus=alert('XSS') autofocus>",
            "<keygen onfocus=alert('XSS') autofocus>",
            "<video><source onerror=alert('XSS')>",
            "<audio src=x onerror=alert('XSS')>",
            "<details open ontoggle=alert('XSS')>"
        };
        
        // When & Then
        for (String attack : realWorldAttacks) {
            assertTrue(XssPatternLibrary.containsXssPattern(attack), 
                "应该检测到真实世界攻击: " + attack);
        }
    }
}