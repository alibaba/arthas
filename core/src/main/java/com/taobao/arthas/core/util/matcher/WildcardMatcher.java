package com.taobao.arthas.core.util.matcher;

/**
 * 通配符匹配器
 *
 * 支持通配符模式的字符串匹配器。支持以下通配符：
 * - * : 匹配零个或多个字符
 * - ? : 匹配任意单个字符
 * - \\ : 转义字符，用于转义 * 和 ?
 *
 * @author ralf0131 2017-01-06 13:17.
 */
public class WildcardMatcher implements Matcher<String> {

    /**
     * 匹配模式字符串
     */
    private final String pattern;

    /**
     * 星号通配符，匹配零个或多个字符
     */
    private static final Character ASTERISK = '*';

    /**
     * 问号通配符，匹配任意单个字符
     */
    private static final Character QUESTION_MARK = '?';

    /**
     * 转义字符，用于转义通配符
     */
    private static final Character ESCAPE = '\\';


    /**
     * 构造通配符匹配器
     *
     * 创建一个使用指定模式进行匹配的通配符匹配器。
     *
     * @param pattern 通配符匹配模式字符串
     */
    public WildcardMatcher(String pattern) {
        this.pattern = pattern;
    }

    /**
     * 匹配目标字符串
     *
     * 使用通配符模式匹配目标字符串。
     *
     * @param target 要匹配的目标字符串
     * @return 如果目标字符串匹配模式则返回 true，否则返回 false
     */
    @Override
    public boolean matching(String target) {
        // 调用内部递归匹配方法，从字符串和模式的起始位置开始匹配
        return match(target, pattern, 0, 0);
    }

    /**
     * 内部递归匹配方法
     *
     * 递归地检查目标字符串是否匹配通配符模式。这是核心匹配算法，
     * 支持星号(*)、问号(?)和转义字符(\\)的通配符匹配。
     *
     * @param target 要匹配的目标字符串
     * @param pattern 通配符模式字符串
     * @param stringStartNdx 目标字符串的起始匹配位置
     * @param patternStartNdx 模式字符串的起始匹配位置
     * @return 如果从指定位置开始匹配则返回 true，否则返回 false
     */
    private boolean match(String target, String pattern, int stringStartNdx, int patternStartNdx) {
        // #135: 修复空指针问题，如果目标字符串或模式为 null，则不匹配
        if(target==null || pattern==null){
            return false;
        }
        // 初始化模式字符串和目标字符串的索引位置
        int pNdx = patternStartNdx;  // 模式字符串当前位置索引
        int sNdx = stringStartNdx;   // 目标字符串当前位置索引
        int pLen = pattern.length(); // 模式字符串长度

        // 性能优化：如果模式只有一个字符且是星号，直接返回 true
        if (pLen == 1) {
            // 单个星号可以匹配任意字符串，包括空字符串
            if (pattern.charAt(0) == ASTERISK) {
                return true;
            }
        }

        int sLen = target.length();   // 目标字符串长度
        boolean nextIsNotWildcard = false; // 标记下一个字符是否被转义（不是通配符）

        // 主匹配循环
        while (true) {

            // 检查是否到达目标字符串或模式字符串的末尾
            if ((sNdx >= sLen)) {
                // 已到达目标字符串末尾，但模式可能还有剩余的星号
                // 星号可以匹配空字符串，所以跳过所有连续的星号
                while ((pNdx < pLen) && (pattern.charAt(pNdx) == ASTERISK)) {
                    pNdx++;
                }
                // 如果模式也到达末尾，则匹配成功；否则匹配失败
                return pNdx >= pLen;
            }

            // 模式已到达末尾，但目标字符串未到达末尾，匹配失败
            if (pNdx >= pLen) {
                return false;
            }

            // 获取模式字符串当前位置的字符
            char p = pattern.charAt(pNdx);

            // 执行匹配逻辑
            if (!nextIsNotWildcard) {
                // 当前字符没有被转义，按通配符处理

                // 处理转义字符
                if (p == ESCAPE) {
                    // 标记下一个字符为非通配符（被转义）
                    pNdx++;
                    nextIsNotWildcard = true;
                    continue;
                }

                // 处理问号通配符：匹配任意单个字符
                if (p == QUESTION_MARK) {
                    sNdx++;
                    pNdx++;
                    continue;
                }

                // 处理星号通配符：匹配零个或多个字符
                if (p == ASTERISK) {
                    // 获取模式中的下一个字符
                    char pnext = 0;
                    if (pNdx + 1 < pLen) {
                        pnext = pattern.charAt(pNdx + 1);
                    }

                    // 连续的双星号效果等同于单星号，跳过重复的星号
                    if (pnext == ASTERISK) {
                        pNdx++;
                        continue;
                    }

                    int i;
                    pNdx++;

                    // 递归查找：从字符串末尾开始，寻找是否有子串能匹配剩余的模式
                    // 星号可以匹配任意长度的字符串，所以需要尝试所有可能的匹配位置
                    for (i = target.length(); i >= sNdx; i--) {
                        // 递归调用：检查从位置 i 开始的子串是否能匹配剩余的模式
                        if (match(target, pattern, i, pNdx)) {
                            return true;
                        }
                    }
                    // 所有尝试都失败，返回不匹配
                    return false;
                }
            } else {
                // 当前字符被转义，作为普通字符处理
                nextIsNotWildcard = false;
            }

            // 检查模式字符和目标字符串字符是否相等
            if (p != target.charAt(sNdx)) {
                // 字符不匹配，返回失败
                return false;
            }

            // 当前字符匹配成功，继续匹配下一对字符
            sNdx++;
            pNdx++;
        }
    }
}
