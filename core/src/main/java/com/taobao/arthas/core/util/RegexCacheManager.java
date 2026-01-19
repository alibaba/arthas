package com.taobao.arthas.core.util;

import com.taobao.arthas.core.shell.term.impl.http.session.LRUCache;
import java.util.regex.Pattern;

/**
 * 正则表达式缓存管理器
 * 用于缓存编译后的正则表达式对象，避免重复编译的开销
 */
public class RegexCacheManager {
    private static final RegexCacheManager INSTANCE = new RegexCacheManager();
    
    // 使用LRUCache缓存编译后的正则表达式
    private final LRUCache<String, Pattern> regexCache;
    
    // 缓存大小限制
    private static final int MAX_CACHE_SIZE = 100;
    
    private RegexCacheManager() {
        // 初始化LRUCache，设置最大缓存大小
        this.regexCache = new LRUCache<>(MAX_CACHE_SIZE);
    }

    public static RegexCacheManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取正则表达式Pattern对象，优先从缓存获取，缓存未命中则编译并缓存
     */
    public Pattern getPattern(String regex) {
        if (regex == null) {
            return null;
        }
        
        // 从LRUCache获取
        Pattern pattern = regexCache.get(regex);
        if (pattern != null) {
            return pattern;
        }

        // 缓存未命中，编译正则表达式
        // 不捕获PatternSyntaxException，让异常向上抛出，以便及时发现无效的正则表达式
        pattern = Pattern.compile(regex);
        // 缓存编译结果
        regexCache.put(regex, pattern);
        
        return pattern;
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        regexCache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return regexCache.usedEntries();
    }

}
