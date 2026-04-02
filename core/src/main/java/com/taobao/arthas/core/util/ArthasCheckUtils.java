package com.taobao.arthas.core.util;

/**
 * Arthas检查工具类
 * 提供各种检查和判断方法，用于判断元素是否在数组中、两个元素是否相等等
 *
 * Created by vlinux on 15/5/19.
 */
public class ArthasCheckUtils {

    /**
     * 检查一个元素是否在数组中
     * 使用equals方法进行比较，支持null值判断
     *
     * @param e   要检查的元素
     * @param s   数组
     * @param <E> 元素类型
     * @return 如果元素在数组中返回true，否则返回false
     *
     * 示例：
     * (1,1,2,3)        == true  (1在数组中)
     * (1,2,3,4)        == false (5不在数组中)
     * (null,1,null,2)  == true  (null在数组中)
     * (1,null)         == false (1不在null数组中)
     */
    public static <E> boolean isIn(E e, E... s) {

        // 如果数组不为null，遍历数组检查是否有相等的元素
        if (null != s) {
            for (E es : s) {
                // 使用isEquals方法进行相等判断（支持null值）
                if (isEquals(e, es)) {
                    return true;
                }
            }
        }

        // 未找到相等的元素，返回false
        return false;

    }

    /**
     * 检查两个元素是否相等
     * 支持null值的安全比较，避免NullPointerException
     *
     * @param src    源元素
     * @param target 目标元素
     * @param <E>    元素类型
     * @return 如果两个元素都为null，或都不为null且equals返回true，则返回true
     *
     * 示例：
     * (null, null)    == true  (两个null相等)
     * (1L,2L)         == false (值不同)
     * (1L,1L)         == true  (值相同)
     * ("abc",null)    == false (一个为null)
     * (null,"abc")    == false (一个为null)
     */
    public static <E> boolean isEquals(E src, E target) {

        // 使用逻辑表达式判断：
        // 1. 两者都为null -> true
        // 2. 两者都不为null 且 equals返回true -> true
        // 其他情况 -> false
        return null == src
                && null == target
                || null != src
                && null != target
                && src.equals(target);

    }
}
