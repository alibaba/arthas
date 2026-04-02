package com.taobao.arthas.core.util;

/**
 * 数组工具类
 * 提供数组操作的静态方法，主要用于对象数组与基本类型数组之间的转换
 *
 * @author ralf0131 2016-12-28 14:57.
 */
public class ArrayUtils {

    /**
     * 空的不可变 long 类型数组
     * 用于在需要返回空数组时避免创建新对象，提高性能
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];

    /**
     * 将 Long 对象数组转换为基本类型 long 数组
     *
     * <p>此方法用于将包装类型 Long 数组转换为基本类型 long 数组。
     * 如果输入数组为 null，则返回 null。
     * 如果输入数组为空，则返回预定义的空数组常量。</p>
     *
     * @param array Long 对象数组，可以为 null
     * @return long 基本类型数组，如果输入为 null 则返回 null
     * @throws NullPointerException 当数组中包含 null 元素时抛出
     */
    public static long[] toPrimitive(final Long[] array) {
        // 检查输入数组是否为 null
        if (array == null) {
            return null;
        // 检查输入数组是否为空，返回预定义的空数组常量
        } else if (array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }

        // 创建与输入数组长度相同的基本类型数组
        final long[] result = new long[array.length];

        // 遍历输入数组，逐个转换元素
        for (int i = 0; i < array.length; i++) {
            // 调用 Long 对象的 longValue() 方法获取基本类型值
            result[i] = array[i].longValue();
        }

        // 返回转换后的数组
        return result;
    }
}
