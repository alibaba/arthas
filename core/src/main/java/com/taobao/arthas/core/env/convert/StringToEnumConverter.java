
package com.taobao.arthas.core.env.convert;

/**
 * 字符串到枚举类型的转换器
 *
 * <p>该转换器用于将字符串值转换为指定的枚举类型。通过调用Enum.valueOf()方法实现转换，
 * 该方法会根据枚举常量的名称进行匹配。</p>
 *
 * <p>注意：这是一个泛型类，类型参数T必须继承自Enum类，表示目标枚举类型。</p>
 *
 * @param <T> 目标枚举类型，必须继承自Enum
 */
@SuppressWarnings("rawtypes") // 禁止原始类型警告，因为Enum类本身是泛型，但这里我们需要处理任意枚举类型
final class StringToEnumConverter<T extends Enum> implements Converter<String, T> {

    /**
     * 将字符串转换为指定的枚举类型
     *
     * <p>该方法通过调用Enum.valueOf()方法实现转换。valueOf()方法会根据传入的枚举类型和字符串名称，
     * 返回对应的枚举常量。如果字符串名称与枚举类型中的任何常量都不匹配，将抛出IllegalArgumentException异常。</p>
     *
     * <p>示例用法：</p>
     * <pre>
     * StringToEnumConverter&lt;DayOfWeek&gt; converter = new StringToEnumConverter&lt;DayOfWeek&gt;();
     * DayOfWeek monday = converter.convert("MONDAY", DayOfWeek.class);
     * </pre>
     *
     * @param source 源字符串，必须与目标枚举类型中某个枚举常量的名称完全匹配（区分大小写）
     * @param targetType 目标枚举类型的Class对象，用于指定要转换到的枚举类型
     * @return 与source字符串匹配的枚举常量
     * @throws IllegalArgumentException 如果source字符串与目标枚举类型中的任何常量名称都不匹配
     * @throws NullPointerException 如果source或targetType为null
     */
    @SuppressWarnings("unchecked") // 禁止未检查的类型转换警告，因为Enum.valueOf返回的是原始类型
    @Override
    public T convert(String source, Class<T> targetType) {
        // 调用Enum.valueOf方法获取枚举常量
        // 该方法会在targetType枚举类型中查找名称为source的常量
        // 如果找到则返回对应的枚举实例，否则抛出IllegalArgumentException
        return (T) Enum.valueOf(targetType, source);
    }

}
