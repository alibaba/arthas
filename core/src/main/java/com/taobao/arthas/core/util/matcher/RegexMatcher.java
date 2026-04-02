package com.taobao.arthas.core.util.matcher;

import com.taobao.arthas.core.util.RegexCacheManager;
import java.util.regex.Pattern;

/**
 * 正则表达式匹配器
 *
 * 该类实现了基于正则表达式的字符串匹配功能，用于判断目标字符串是否匹配指定的正则表达式模式。
 * 采用延迟编译策略，在第一次执行匹配时才编译正则表达式，并通过缓存管理器提高性能。
 *
 * @author ralf0131 2017-01-06 13:16.
 */
public class RegexMatcher implements Matcher<String> {

    /**
     * 正则表达式模式字符串
     * 保存用户提供的正则表达式原始字符串
     */
    private final String pattern;

    /**
     * 编译后的正则表达式对象
     * 使用volatile关键字确保多线程环境下的可见性
     * 采用延迟初始化策略，在第一次匹配时才编译
     */
    private volatile Pattern compiledPattern;

    /**
     * 构造函数
     *
     * @param pattern 正则表达式模式字符串，用于后续的匹配操作
     */
    public RegexMatcher(String pattern) {
        this.pattern = pattern;
    }

    /**
     * 执行匹配操作
     *
     * 判断目标字符串是否匹配当前的正则表达式模式。
     * 该方法采用延迟编译策略，在第一次调用时才编译正则表达式，
     * 并通过RegexCacheManager缓存编译结果以提高性能。
     *
     * @param target 待匹配的目标字符串
     * @return 如果目标字符串匹配正则表达式模式返回true，否则返回false。
     *         如果目标字符串或模式字符串为null，也返回false
     */
    @Override
    public boolean matching(String target) {
        // 参数校验：如果目标字符串或模式字符串为null，直接返回不匹配
        if (null == target || null == pattern) {
            return false;
        }

        // 延迟编译：在第一次matching时才编译正则表达式
        // 使用双重检查锁定模式（在RegexCacheManager内部实现）确保线程安全
        if (compiledPattern == null) {
            // 通过缓存管理器获取编译后的Pattern对象
            // 缓存管理器会复用已编译的Pattern，提高性能
            compiledPattern = RegexCacheManager.getInstance().getPattern(pattern);
        }

        // 执行匹配操作：使用编译后的Pattern对象对目标字符串进行全匹配
        // matches()方法要求整个字符串都匹配正则表达式
        return compiledPattern != null && compiledPattern.matcher(target).matches();
    }
}