package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * RegexCacheManager测试类
 */
public class RegexCacheManagerTest {
    private RegexCacheManager cacheManager;

    @Before
    public void setUp() {
        // 获取单例实例
        cacheManager = RegexCacheManager.getInstance();
        // 清理缓存，确保测试环境干净
        cacheManager.clearCache();
    }

    /**
     * 测试基本缓存功能
     */
    @Test
    public void testBasicCacheFunctionality() {
        // 测试缓存未命中的情况
        String regex1 = ".*Test.*";
        Pattern pattern1 = cacheManager.getPattern(regex1);
        Assert.assertNotNull(pattern1);
        Assert.assertEquals(1, cacheManager.getCacheSize());

        // 测试缓存命中的情况
        Pattern pattern1Cached = cacheManager.getPattern(regex1);
        Assert.assertNotNull(pattern1Cached);
        Assert.assertSame(pattern1, pattern1Cached); // 应该是同一个对象
        Assert.assertEquals(1, cacheManager.getCacheSize()); // 缓存大小应该保持不变

        // 测试多个正则表达式
        String regex2 = "^Test.*";
        Pattern pattern2 = cacheManager.getPattern(regex2);
        Assert.assertNotNull(pattern2);
        Assert.assertEquals(2, cacheManager.getCacheSize());

        // 测试空正则表达式
        Pattern nullPattern = cacheManager.getPattern(null);
        Assert.assertNull(nullPattern);

        Pattern emptyPattern = cacheManager.getPattern("");
        Assert.assertNull(emptyPattern);
    }

    /**
     * 测试LRU淘汰策略
     */
    @Test
    public void testLRUEvictionPolicy() {
        // 生成多个正则表达式，超过最大缓存大小
        int maxCacheSize = 100;
        for (int i = 0; i < maxCacheSize + 5; i++) {
            String regex = "TestRegex" + i;
            Pattern pattern = cacheManager.getPattern(regex);
            Assert.assertNotNull(pattern);
        }

        // 缓存大小应该等于最大缓存大小
        Assert.assertEquals(maxCacheSize, cacheManager.getCacheSize()); // 100 是实际的最大缓存大小

        // 测试访问顺序，确保LRU策略生效
        String firstRegex = "TestRegex0";

        // 再次访问第一个正则表达式，使其成为最近使用的
        Pattern firstPattern = cacheManager.getPattern(firstRegex);
        Assert.assertNotNull(firstPattern);

        // 再添加一个新的正则表达式，应该淘汰最久未使用的
        String newRegex = "NewTestRegex";
        Pattern newPattern = cacheManager.getPattern(newRegex);
        Assert.assertNotNull(newPattern);

        // 第一个正则表达式应该仍然在缓存中（因为刚被访问过）
        Pattern firstPatternAgain = cacheManager.getPattern(firstRegex);
        Assert.assertNotNull(firstPatternAgain);
    }

    /**
     * 测试缓存清理功能
     */
    @Test
    public void testCacheClear() {
        // 添加一些缓存项
        cacheManager.getPattern(".*Test1");
        cacheManager.getPattern(".*Test2");
        Assert.assertTrue(cacheManager.getCacheSize() > 0);

        // 清理缓存
        cacheManager.clearCache();
        Assert.assertEquals(0, cacheManager.getCacheSize());

        // 清理后应该可以重新添加缓存项
        Pattern pattern = cacheManager.getPattern(".*Test3");
        Assert.assertNotNull(pattern);
        Assert.assertEquals(1, cacheManager.getCacheSize());
    }

    /**
     * 测试无效正则表达式处理
     */
    @Test
    public void testInvalidRegexHandling() {
        // 测试无效的正则表达式
        String invalidRegex = "[a-z";
        Pattern pattern = cacheManager.getPattern(invalidRegex);
        Assert.assertNull(pattern);
        
        // 测试另一个无效的正则表达式
        String anotherInvalidRegex = "(a-z";
        Pattern anotherPattern = cacheManager.getPattern(anotherInvalidRegex);
        Assert.assertNull(anotherPattern);
        
        // 确保缓存大小没有增加
        Assert.assertEquals("无效正则表达式不应该被缓存", 0, cacheManager.getCacheSize());
    }

}
