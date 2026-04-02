package com.taobao.arthas.core.util;

import com.taobao.arthas.core.shell.term.impl.http.session.LRUCache;
import java.util.regex.Pattern;

/**
 * 正则表达式缓存管理器
 * 用于缓存编译后的正则表达式Pattern对象，避免重复编译带来的性能开销
 * 使用单例模式和LRU缓存策略
 */
public class RegexCacheManager {
    // 单例实例
    private static final RegexCacheManager INSTANCE = new RegexCacheManager();

    // 使用LRUCache缓存编译后的正则表达式Pattern对象
    // LRU（Least Recently Used）策略：当缓存满时，优先移除最久未使用的条目
    private final LRUCache<String, Pattern> regexCache;

    // 缓存大小限制，最多缓存100个正则表达式
    private static final int MAX_CACHE_SIZE = 100;

    /**
     * 私有构造函数，实现单例模式
     * 初始化LRU缓存，设置最大缓存大小为100
     */
    private RegexCacheManager() {
        // 初始化LRUCache，设置最大缓存大小
        this.regexCache = new LRUCache<>(MAX_CACHE_SIZE);
    }

    /**
     * 获取RegexCacheManager的单例实例
     * @return RegexCacheManager单例实例
     */
    public static RegexCacheManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取正则表达式Pattern对象
     * 优先从缓存获取，如果缓存未命中则编译正则表达式并放入缓存
     * 采用缓存优先策略，显著提升正则表达式的匹配性能
     * @param regex 正则表达式字符串
     * @return 编译后的Pattern对象，如果regex为null则返回null
     */
    public Pattern getPattern(String regex) {
        // 参数校验：如果正则表达式为null，直接返回null
        if (regex == null) {
            return null;
        }

        // 从LRUCache中尝试获取已编译的Pattern对象
        Pattern pattern = regexCache.get(regex);
        if (pattern != null) {
            // 缓存命中，直接返回
            return pattern;
        }

        // 缓存未命中，需要编译正则表达式
        // 不捕获PatternSyntaxException，让异常向上抛出，以便及时发现无效的正则表达式
        pattern = Pattern.compile(regex);
        // 将编译结果放入缓存，供下次使用
        regexCache.put(regex, pattern);

        return pattern;
    }
    
    /**
     * 清理缓存
     * 清空缓存中所有的正则表达式Pattern对象
     * 适用于需要释放内存或重置缓存状态的场景
     */
    public void clearCache() {
        regexCache.clear();
    }

    /**
     * 获取当前缓存的使用大小
     * @return 当前缓存中已存储的Pattern对象数量
     */
    public int getCacheSize() {
        return regexCache.usedEntries();
    }

}
