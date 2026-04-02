
package com.taobao.arthas.core.env.convert;

import java.lang.reflect.Array;

import com.taobao.arthas.core.env.ConversionService;
import com.taobao.arthas.core.util.StringUtils;

/**
 * 字符串到数组的转换器
 * <p>
 * 该转换器负责将逗号分隔的字符串转换为指定类型的数组。
 * 支持泛型，可以转换为任何基本类型或对象类型的数组。
 * </p>
 * <p>
 * 转换规则：
 * <ul>
 * <li>输入字符串按逗号（","）分隔</li>
 * <li>每个分隔后的子字符串会通过ConversionService转换为指定类型</li>
 * <li>自动处理数组类型的反射创建</li>
 * </ul>
 * </p>
 * <p>
 * 示例：
 * <pre>
 * "1,2,3" -> Integer[]{1, 2, 3}
 * "a,b,c" -> String[]{"a", "b", "c"}
 * "true,false" -> Boolean[]{true, false}
 * </pre>
 * </p>
 *
 * @param <T> 数组元素的类型
 */
final class StringToArrayConverter<T> implements Converter<String, T[]> {

    /**
     * 类型转换服务
     * <p>
     * 用于将字符串转换为具体的数组元素类型。
     * 该服务支持多种类型的转换，包括基本类型和复杂对象类型。
     * </p>
     */
    private ConversionService conversionService;

    /**
     * 构造函数
     * <p>
     * 创建字符串到数组的转换器实例。
     * </p>
     *
     * @param conversionService 类型转换服务，用于转换数组中的每个元素
     */
    public StringToArrayConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * 将字符串转换为目标类型的数组
     * <p>
     * 转换过程：
     * <ol>
     * <li>将输入字符串按逗号分隔成字符串数组</li>
     * <li>获取目标数组的组件类型（元素类型）</li>
     * <li>创建目标类型的新数组</li>
     * <li>遍历字符串数组，逐个转换为元素类型</li>
     * <li>返回填充好的数组</li>
     * </ol>
     * </p>
     *
     * @param source 源字符串，逗号分隔的值，例如 "1,2,3" 或 "a,b,c"
     * @param targetType 目标数组类型，例如 Integer[].class 或 String[].class
     * @return 转换后的数组，包含所有转换后的元素
     * @throws IllegalArgumentException 如果元素转换失败
     * @throws ArrayStoreException 如果类型不兼容
     */
    @Override
    public T[] convert(String source, Class<T[]> targetType) {
        // 步骤1: 使用工具类将字符串按逗号分隔成字符串数组
        // 例如: "1,2,3" -> ["1", "2", "3"]
        String[] strings = StringUtils.tokenizeToStringArray(source, ",");

        // 步骤2: 获取目标数组的组件类型（元素类型）
        // 例如: Integer[].class 的组件类型是 Integer.class
        // 步骤3: 使用反射创建目标类型的新数组，长度与字符串数组相同
        @SuppressWarnings("unchecked")
        T[] values = (T[]) Array.newInstance(targetType.getComponentType(), strings.length);

        // 步骤4: 遍历字符串数组，逐个转换为元素类型
        for (int i = 0; i < strings.length; ++i) {
            // 使用转换服务将字符串转换为元素类型
            // 例如: 将 "1" 转换为 Integer 1
            @SuppressWarnings("unchecked")
            T value = (T) conversionService.convert(strings[i], targetType.getComponentType());

            // 将转换后的值存入结果数组
            values[i] = value;
        }

        // 步骤5: 返回填充好的数组
        return values;
    }

}
