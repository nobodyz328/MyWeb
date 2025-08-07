package com.myweb.website_core.infrastructure.security;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * XSS攻击模式库
 * 
 * 定义和管理各种XSS攻击向量的检测模式，支持模式的动态更新和扩展。
 * 
 * 主要功能：
 * 1. 维护XSS攻击模式库
 * 2. 提供模式匹配和检测功能
 * 3. 支持自定义模式添加
 * 4. 提供模式分类管理
 * 
 * 符合需求：4.2, 4.4 - 入侵防范机制
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-01-01
 */
public class XssPatternLibrary {
    
    /**
     * 脚本注入攻击模式
     */
    public static final List<Pattern> SCRIPT_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script\\s*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script/[^>]*>", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * JavaScript协议攻击模式
     */
    public static final List<Pattern> JAVASCRIPT_PROTOCOL_PATTERNS = Arrays.asList(
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:text/javascript", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:application/javascript", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 事件处理器攻击模式
     */
    public static final List<Pattern> EVENT_HANDLER_PATTERNS = Arrays.asList(
        Pattern.compile("onload\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onerror\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onclick\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseover\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseout\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmousedown\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmouseup\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onmousemove\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onfocus\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onblur\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onchange\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onsubmit\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onreset\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onselect\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeydown\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeypress\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onkeyup\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onabort\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("oncanplay\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("oncanplaythrough\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ondurationchange\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onemptied\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onended\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onloadeddata\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onloadedmetadata\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onloadstart\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onpause\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onplay\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onplaying\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onprogress\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onratechange\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onseeked\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onseeking\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onstalled\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onsuspend\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ontimeupdate\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onvolumechange\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onwaiting\\s*=", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 表达式和函数调用攻击模式
     */
    public static final List<Pattern> EXPRESSION_PATTERNS = Arrays.asList(
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("setTimeout\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("setInterval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Function\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("execScript\\s*\\(", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 样式表攻击模式
     */
    public static final List<Pattern> STYLE_PATTERNS = Arrays.asList(
        Pattern.compile("<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@charset", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@namespace", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@media", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@page", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@font-face", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@keyframes", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@supports", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@document", Pattern.CASE_INSENSITIVE),
        Pattern.compile("behavior\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("-moz-binding\\s*:", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 链接和资源攻击模式
     */
    public static final List<Pattern> LINK_PATTERNS = Arrays.asList(
        Pattern.compile("src\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("href\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src\\s*=\\s*['\"]?vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("href\\s*=\\s*['\"]?vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src\\s*=\\s*['\"]?data:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("href\\s*=\\s*['\"]?data:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("action\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("formaction\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 框架和嵌入攻击模式
     */
    public static final List<Pattern> FRAME_PATTERNS = Arrays.asList(
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<frame[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<frameset[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<applet[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<param[^>]*>", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 表单攻击模式
     */
    public static final List<Pattern> FORM_PATTERNS = Arrays.asList(
        Pattern.compile("<form[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<input[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<textarea[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<select[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<button[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("formaction\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("formmethod\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("formtarget\\s*=", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Meta标签攻击模式
     */
    public static final List<Pattern> META_PATTERNS = Arrays.asList(
        Pattern.compile("<meta[^>]*http-equiv", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<meta[^>]*refresh", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<meta[^>]*content\\s*=\\s*['\"]?[^'\"]*javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<base[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 编码绕过攻击模式
     */
    public static final List<Pattern> ENCODING_BYPASS_PATTERNS = Arrays.asList(
        Pattern.compile("&#x?[0-9a-f]+;?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%[0-9a-f]{2}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\\x[0-9a-f]{2}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\\u[0-9a-f]{4}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\\\[0-7]{1,3}", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 注释攻击模式
     */
    public static final List<Pattern> COMMENT_PATTERNS = Arrays.asList(
        Pattern.compile("<!--.*?-->", Pattern.DOTALL),
        Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL),
        Pattern.compile("//.*$", Pattern.MULTILINE),
        Pattern.compile("<!\\[CDATA\\[.*?\\]\\]>", Pattern.DOTALL)
    );
    
    /**
     * 获取所有XSS攻击模式
     * 
     * @return 所有XSS攻击模式的列表
     */
    public static List<List<Pattern>> getAllPatternCategories() {
        return Arrays.asList(
            SCRIPT_INJECTION_PATTERNS,
            JAVASCRIPT_PROTOCOL_PATTERNS,
            EVENT_HANDLER_PATTERNS,
            EXPRESSION_PATTERNS,
            STYLE_PATTERNS,
            LINK_PATTERNS,
            FRAME_PATTERNS,
            FORM_PATTERNS,
            META_PATTERNS,
            ENCODING_BYPASS_PATTERNS,
            COMMENT_PATTERNS
        );
    }
    
    /**
     * 检查输入是否包含XSS攻击模式
     * 
     * @param input 输入字符串
     * @return 如果包含XSS攻击模式返回true
     */
    public static boolean containsXssPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        for (List<Pattern> patternCategory : getAllPatternCategories()) {
            for (Pattern pattern : patternCategory) {
                if (pattern.matcher(input).find()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取匹配的XSS攻击模式类型
     * 
     * @param input 输入字符串
     * @return 匹配的攻击模式类型列表
     */
    public static List<String> getMatchedPatternTypes(String input) {
        if (input == null || input.isEmpty()) {
            return Arrays.asList();
        }
        
        List<String> matchedTypes = Arrays.asList();
        
        if (SCRIPT_INJECTION_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("SCRIPT_INJECTION");
        }
        if (JAVASCRIPT_PROTOCOL_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("JAVASCRIPT_PROTOCOL");
        }
        if (EVENT_HANDLER_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("EVENT_HANDLER");
        }
        if (EXPRESSION_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("EXPRESSION");
        }
        if (STYLE_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("STYLE");
        }
        if (LINK_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("LINK");
        }
        if (FRAME_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("FRAME");
        }
        if (FORM_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("FORM");
        }
        if (META_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("META");
        }
        if (ENCODING_BYPASS_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("ENCODING_BYPASS");
        }
        if (COMMENT_PATTERNS.stream().anyMatch(p -> p.matcher(input).find())) {
            matchedTypes.add("COMMENT");
        }
        
        return matchedTypes;
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private XssPatternLibrary() {
        // 工具类，不允许实例化
    }
}