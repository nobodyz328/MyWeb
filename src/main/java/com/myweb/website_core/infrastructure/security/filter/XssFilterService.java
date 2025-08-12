package com.myweb.website_core.infrastructure.security.filter;

import com.myweb.website_core.application.service.security.XssMonitoringService;
import com.myweb.website_core.application.service.security.XssStatisticsService;
import com.myweb.website_core.infrastructure.config.XssFilterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 增强型XSS过滤器
 * <p>
 * 提供可配置的XSS过滤策略，支持HTML标签白名单、性能优化等高级功能。
 * <p>
 * 主要功能：
 * 1. 可配置的HTML标签白名单过滤
 * 2. 性能优化的缓存机制
 * 3. 统计和监控集成
 * 4. 自定义过滤规则支持
 * <p>
 * 符合需求：2.2, 2.4, 2.6 - 可配置XSS过滤
 * 
 * @author MyWeb
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XssFilterService {
    
    private final XssFilterConfig xssFilterConfig;
    private final XssStatisticsService xssStatisticsService;
    private final XssMonitoringService xssMonitoringService;
    
    // 缓存机制
    private final ConcurrentHashMap<String, String> filterCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // 路径匹配器
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 预编译的正则表达式模式
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)(\\s[^>]*)?>", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_ATTRIBUTE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*[\"']([^\"']*)[\"']", 
        Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS_PROTOCOL_PATTERN = Pattern.compile(
        "(javascript|vbscript|data|file|about):", Pattern.CASE_INSENSITIVE);
    
    /**
     * 检测是否包含XSS攻击代码（不修改内容）
     * 
     * @param input 输入内容
     * @param requestUri 请求URI
     * @param clientIp 客户端IP
     * @return 是否包含XSS攻击代码
     */
    public boolean containsXss(String input, String requestUri, String clientIp) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // 检查是否在白名单中
        if (isWhitelistedUrl(requestUri)) {
            return false;
        }
        
        try {
            // 执行过滤并比较结果
            String filteredContent = performXssFiltering(input, requestUri, clientIp);
            boolean hasXss = !input.equals(filteredContent);
            
            if (hasXss) {
                String attackType = detectAttackType(input);
                xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, null);
                xssMonitoringService.recordXssEvent(clientIp, requestUri, attackType, true, 0);
            }
            
            return hasXss;
            
        } catch (Exception e) {
            log.error("XSS检测处理异常", e);
            return false; // 异常情况下认为没有XSS
        }
    }
    
    /**
     * 过滤XSS攻击代码
     * 
     * @param input 输入内容
     * @param requestUri 请求URI
     * @param clientIp 客户端IP
     * @return 过滤后的内容
     */
    public String filterXss(String input, String requestUri, String clientIp) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查是否在白名单中
            if (isWhitelistedUrl(requestUri)) {
                log.debug("URL在白名单中，跳过XSS过滤: {}", requestUri);
                return input;
            }
            
            // 检查缓存
            String cachedResult = getCachedResult(input);
            if (cachedResult != null) {
                log.debug("使用缓存的XSS过滤结果");
                return cachedResult;
            }
            
            // 执行过滤
            String filteredContent = performXssFiltering(input, requestUri, clientIp);
            
            // 缓存结果
            cacheResult(input, filteredContent);
            
            // 记录监控数据
            long processingTime = System.currentTimeMillis() - startTime;
            boolean hasXss = !input.equals(filteredContent);
            
            if (hasXss) {
                String attackType = detectAttackType(input);
                xssStatisticsService.recordXssAttack(clientIp, requestUri, attackType, null);
                xssMonitoringService.recordXssEvent(clientIp, requestUri, attackType, true, processingTime);
            } else {
                xssMonitoringService.recordXssEvent(clientIp, requestUri, "none", false, processingTime);
            }
            
            return filteredContent;
            
        } catch (Exception e) {
            log.error("XSS过滤处理异常", e);
            return input; // 异常情况下返回原始输入
        }
    }
    
    /**
     * 执行XSS过滤
     * 
     * @param input 输入内容
     * @param requestUri 请求URI
     * @param clientIp 客户端IP
     * @return 过滤后的内容
     */
    private String performXssFiltering(String input, String requestUri, String clientIp) {
        String content = input;
        
        // 1. 长度限制
        if (content.length() > xssFilterConfig.getMaxContentLength()) {
            content = content.substring(0, xssFilterConfig.getMaxContentLength());
            log.warn("内容长度超限，已截断 - IP: {}, URI: {}", clientIp, requestUri);
        }
        
        // 2. 移除控制字符
        content = removeControlCharacters(content);
        
        // 3. 处理HTML标签
        content = filterHtmlTags(content);
        
        // 4. 处理危险协议
        content = filterDangerousProtocols(content);
        
        // 5. 应用自定义过滤规则
        content = applyCustomFilters(content);
        
        // 6. HTML编码特殊字符
        if (xssFilterConfig.isEncodeSpecialChars()) {
            content = encodeSpecialCharacters(content);
        }
        
        return content;
    }
    
    /**
     * 过滤HTML标签
     * 
     * @param content 内容
     * @return 过滤后的内容
     */
    private String filterHtmlTags(String content) {
        if (!xssFilterConfig.isRemoveUnknownTags()) {
            return content;
        }
        
        Set<String> allowedTags = xssFilterConfig.getAllowedTags();
        StringBuilder result = new StringBuilder();
        
        java.util.regex.Matcher matcher = HTML_TAG_PATTERN.matcher(content);
        int lastEnd = 0;
        
        while (matcher.find()) {
            // 添加标签前的内容
            result.append(content, lastEnd, matcher.start());
            
            String fullTag = matcher.group(0);
            String closingSlash = matcher.group(1);
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);
            
            // 检查标签是否在白名单中
            if (allowedTags.contains(tagName)) {
                // 过滤属性
                String filteredAttributes = filterAttributes(tagName, attributes);
                
                // 重构标签
                if (closingSlash != null && !closingSlash.isEmpty()) {
                    result.append("</").append(tagName).append(">");
                } else {
                    result.append("<").append(tagName);
                    if (filteredAttributes != null && !filteredAttributes.isEmpty()) {
                        result.append(filteredAttributes);
                    }
                    result.append(">");
                }
            } else {
                // 标签不在白名单中，记录并移除
                log.debug("移除不在白名单中的HTML标签: {}", tagName);
            }
            
            lastEnd = matcher.end();
        }
        
        // 添加剩余内容
        result.append(content.substring(lastEnd));
        
        return result.toString();
    }
    
    /**
     * 过滤HTML属性
     * 
     * @param tagName 标签名
     * @param attributes 属性字符串
     * @return 过滤后的属性字符串
     */
    private String filterAttributes(String tagName, String attributes) {
        if (attributes == null || attributes.trim().isEmpty()) {
            return "";
        }
        
        Set<String> allowedAttributes = xssFilterConfig.getAllowedAttributes();
        Set<String> tagSpecificAttributes = xssFilterConfig.getTagSpecificAttributes().get(tagName);
        
        StringBuilder result = new StringBuilder();
        java.util.regex.Matcher matcher = HTML_ATTRIBUTE_PATTERN.matcher(attributes);
        
        while (matcher.find()) {
            String attrName = matcher.group(1).toLowerCase();
            String attrValue = matcher.group(2);
            
            // 检查属性是否被允许
            boolean isAllowed = allowedAttributes.contains(attrName) || 
                (tagSpecificAttributes != null && tagSpecificAttributes.contains(attrName));
            
            if (isAllowed) {
                // 检查属性值是否安全
                if (isSafeAttributeValue(attrName, attrValue)) {
                    result.append(" ").append(attrName).append("=\"").append(attrValue).append("\"");
                } else {
                    log.debug("移除不安全的属性值: {}={}", attrName, attrValue);
                }
            } else {
                log.debug("移除不在白名单中的属性: {}", attrName);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 检查属性值是否安全
     * 
     * @param attrName 属性名
     * @param attrValue 属性值
     * @return 是否安全
     */
    private boolean isSafeAttributeValue(String attrName, String attrValue) {
        if (attrValue == null || attrValue.trim().isEmpty()) {
            return true;
        }
        
        // 检查危险协议
        if (DANGEROUS_PROTOCOL_PATTERN.matcher(attrValue).find()) {
            return false;
        }
        
        // 检查事件处理器
        if (attrName.startsWith("on")) {
            return false;
        }
        
        // 检查style属性中的危险内容
        if ("style".equals(attrName)) {
            return isSafeStyleValue(attrValue);
        }
        
        return true;
    }
    
    /**
     * 检查样式值是否安全
     * 
     * @param styleValue 样式值
     * @return 是否安全
     */
    private boolean isSafeStyleValue(String styleValue) {
        String lowerValue = styleValue.toLowerCase();
        
        // 检查危险的CSS函数
        String[] dangerousFunctions = {"expression", "javascript", "vbscript", "import", "url"};
        for (String func : dangerousFunctions) {
            if (lowerValue.contains(func + "(")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 过滤危险协议
     * 
     * @param content 内容
     * @return 过滤后的内容
     */
    private String filterDangerousProtocols(String content) {
        return DANGEROUS_PROTOCOL_PATTERN.matcher(content).replaceAll("blocked:");
    }
    
    /**
     * 应用自定义过滤规则
     * 
     * @param content 内容
     * @return 过滤后的内容
     */
    private String applyCustomFilters(String content) {
        String result = content;
        
        for (String patternStr : xssFilterConfig.getCustomXssPatterns()) {
            try {
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                result = pattern.matcher(result).replaceAll("");
            } catch (Exception e) {
                log.warn("应用自定义XSS过滤规则失败: {}", patternStr, e);
            }
        }
        
        return result;
    }
    
    /**
     * 移除控制字符
     * 
     * @param content 内容
     * @return 清理后的内容
     */
    private String removeControlCharacters(String content) {
        return content.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }
    
    /**
     * 编码特殊字符
     * 
     * @param content 内容
     * @return 编码后的内容
     */
    private String encodeSpecialCharacters(String content) {
        return content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
//            .replace("/", "&#x2F;");
    }
    
    /**
     * 检测攻击类型
     * 
     * @param input 输入内容
     * @return 攻击类型
     */
    private String detectAttackType(String input) {
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("<script")) {
            return "script_injection";
        } else if (lowerInput.contains("javascript:")) {
            return "javascript_protocol";
        } else if (lowerInput.contains("onload=") || lowerInput.contains("onerror=")) {
            return "event_handler";
        } else if (lowerInput.contains("<iframe") || lowerInput.contains("<frame")) {
            return "frame_injection";
        } else if (lowerInput.contains("expression(")) {
            return "css_expression";
        } else {
            return "unknown";
        }
    }
    
    /**
     * 检查URL是否在白名单中
     * 
     * @param requestUri 请求URI
     * @return 是否在白名单中
     */
    private boolean isWhitelistedUrl(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        
        for (String pattern : xssFilterConfig.getWhitelistUrlPatterns()) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取缓存结果
     * 
     * @param input 输入内容
     * @return 缓存的结果，如果不存在或过期则返回null
     */
    private String getCachedResult(String input) {
        if (!xssFilterConfig.getPerformance().isCacheEnabled()) {
            return null;
        }
        
        String cacheKey = generateCacheKey(input);
        String cachedResult = filterCache.get(cacheKey);
        
        if (cachedResult != null) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null) {
                long expirationTime = xssFilterConfig.getPerformance().getCacheExpirationMinutes() * 60 * 1000;
                if (System.currentTimeMillis() - timestamp < expirationTime) {
                    return cachedResult;
                } else {
                    // 缓存过期，清理
                    filterCache.remove(cacheKey);
                    cacheTimestamps.remove(cacheKey);
                }
            }
        }
        
        return null;
    }
    
    /**
     * 缓存过滤结果
     * 
     * @param input 输入内容
     * @param result 过滤结果
     */
    private void cacheResult(String input, String result) {
        if (!xssFilterConfig.getPerformance().isCacheEnabled()) {
            return;
        }
        
        // 检查缓存大小限制
        int cacheSize = xssFilterConfig.getPerformance().getCacheSize();
        if (filterCache.size() >= cacheSize) {
            // 清理最旧的缓存项
            cleanupOldestCacheEntries();
        }
        
        String cacheKey = generateCacheKey(input);
        filterCache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }
    
    /**
     * 生成缓存键
     * 
     * @param input 输入内容
     * @return 缓存键
     */
    private String generateCacheKey(String input) {
        return String.valueOf(input.hashCode());
    }
    
    /**
     * 清理最旧的缓存项
     */
    private void cleanupOldestCacheEntries() {
        // 清理25%的缓存项
        int itemsToRemove = Math.max(1, filterCache.size() / 4);
        
        cacheTimestamps.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByValue())
            .limit(itemsToRemove)
            .forEach(entry -> {
                String key = entry.getKey();
                filterCache.remove(key);
                cacheTimestamps.remove(key);
            });
    }
}