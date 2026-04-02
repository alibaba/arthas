package com.taobao.arthas.core.env.convert;

import java.lang.reflect.Array;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的类型转换服务实现类
 *
 * 这是 {@link ConfigurableConversionService} 接口的默认实现，提供了常见的类型转换功能。
 * 该类使用线程安全的 ConcurrentHashMap 来存储和管理各种类型转换器。
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>注册和管理类型转换器</li>
 *   <li>检查是否支持特定的类型转换</li>
 *   <li>执行实际的类型转换操作</li>
 *   <li>处理基本类型和包装类型的转换</li>
 *   <li>支持数组和枚举类型的转换</li>
 * </ul>
 *
 * <p>支持的默认转换：</p>
 * <ul>
 *   <li>String -> Integer</li>
 *   <li>String -> Long</li>
 *   <li>String -> Boolean</li>
 *   <li>String -> InetAddress</li>
 *   <li>String -> Enum</li>
 *   <li>String -> Array</li>
 * </ul>
 *
 * <p>线程安全性：此类使用 ConcurrentHashMap 存储转换器，因此是线程安全的。</p>
 */
public class DefaultConversionService implements ConfigurableConversionService {

    /**
     * 转换器缓存映射表
     *
     * 使用 ConcurrentHashMap 存储类型转换器，键为 {@link ConvertiblePair}（源类型到目标类型的配对），
     * 值为对应的 {@link Converter} 转换器实现。
     *
     * 使用 ConcurrentHashMap 的原因：
     * <ul>
     *   <li>线程安全：支持多线程并发访问</li>
     *   <li>高性能：读操作不需要锁，写操作使用细粒度锁</li>
     *   <li>动态性：可以在运行时添加新的转换器</li>
     * </ul>
     */
    private static ConcurrentHashMap<ConvertiblePair, Converter> converters = new ConcurrentHashMap<ConvertiblePair, Converter>();

    /**
     * 默认构造函数
     *
     * 创建一个 DefaultConversionService 实例，并自动注册所有默认的类型转换器。
     * 这些转换器涵盖了最常见的类型转换场景。
     */
    public DefaultConversionService() {
        // 调用私有方法添加所有默认的转换器
        addDefaultConverter();

    }

    /**
     * 添加所有默认的类型转换器
     *
     * <p>此方法在构造函数中被调用，用于预注册所有系统内置的转换器。
     * 这些转换器涵盖了最常见的类型转换场景：</p>
     *
     * <ul>
     *   <li>数字转换：String -> Integer, String -> Long</li>
     *   <li>布尔转换：String -> Boolean</li>
     *   <li>网络地址转换：String -> InetAddress</li>
     *   <li>枚举转换：String -> Enum</li>
     *   <li>数组转换：String -> Array</li>
     * </ul>
     *
     * <p>注意：所有转换器都是单向的，从 String 到其他类型。</p>
     */
    private void addDefaultConverter() {
        // 注册 String 到 Integer 的转换器
        converters.put(new ConvertiblePair(String.class, Integer.class), new StringToIntegerConverter());

        // 注册 String 到 Long 的转换器
        converters.put(new ConvertiblePair(String.class, Long.class), new StringToLongConverter());

        // 注册 String 到 Boolean 的转换器
        converters.put(new ConvertiblePair(String.class, Boolean.class), new StringToBooleanConverter());

        // 注册 String 到 InetAddress 的转换器（用于 IP 地址转换）
        converters.put(new ConvertiblePair(String.class, InetAddress.class), new StringToInetAddressConverter());

        // 注册 String 到 Enum 的转换器（通用枚举转换器）
        converters.put(new ConvertiblePair(String.class, Enum.class), new StringToEnumConverter());

        // 注册 String 到 Array 的转换器（需要传入 this 以便递归处理数组元素）
        converters.put(new ConvertiblePair(String.class, Arrays.class), new StringToArrayConverter(this));

    }

    /**
     * 检查是否支持从源类型到目标类型的转换
     *
     * <p>此方法实现了类型转换能力的检查逻辑，判断系统中是否存在能够执行指定类型转换的转换器。</p>
     *
     * <p>检查策略：</p>
     * <ol>
     *   <li>如果源类型和目标类型相同，直接返回 true（无需转换）</li>
     *   <li>如果目标类型是基本类型，先转换为对应的包装类型</li>
     *   <li>检查是否存在精确匹配的转换器</li>
     *   <li>如果目标类型是枚举，检查是否存在通用的枚举转换器</li>
     *   <li>如果目标类型是数组，默认返回 true（支持数组转换）</li>
     * </ol>
     *
     * @param sourceType 源类型的 Class 对象，表示要转换的对象类型
     * @param targetType 目标类型的 Class 对象，表示希望转换到的类型
     * @return 如果支持转换返回 true，否则返回 false
     */
    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        // 如果源类型和目标类型相同，不需要转换
        if (sourceType == targetType) {
            return true;
        }

        // 如果目标类型是基本类型（如 int, long 等），转换为对应的包装类型
        // 这样可以统一处理基本类型和包装类型
        if (targetType.isPrimitive()) {
            targetType = objectiveClass(targetType);
        }

        // 检查是否存在精确匹配的转换器
        if (converters.containsKey(new ConvertiblePair(sourceType, targetType))) {
            return true;
        }

        // 如果目标类型是枚举，检查是否存在通用的 String -> Enum 转换器
        // 这允许处理任何具体的枚举类型转换
        if (targetType.isEnum()) {
            if (converters.containsKey(new ConvertiblePair(sourceType, Enum.class))) {
                return true;
            }
        }

        // 如果目标类型是数组，默认返回 true
        // 数组转换由专门的转换器处理
        if (targetType.isArray()) {
            return true;
        }

        // 不支持该类型的转换
        return false;
    }

    /**
     * 将源对象转换为目标类型
     *
     * <p>此方法执行实际的类型转换操作。它会查找合适的转换器并执行转换。</p>
     *
     * <p>转换策略：</p>
     * <ol>
     *   <li>如果目标类型是基本类型，先转换为对应的包装类型</li>
     *   <li>查找精确匹配的转换器（源类型 -> 目标类型）</li>
     *   <li>如果找不到且目标是数组，使用通用数组转换器</li>
     *   <li>如果找不到且目标是枚举，使用通用枚举转换器</li>
     *   <li>如果找不到转换器，直接返回源对象（假设类型兼容）</li>
     * </ol>
     *
     * @param <T> 目标类型的泛型参数
     * @param source 要转换的源对象，不能为 null
     * @param targetType 目标类型的 Class 对象，指定转换后的类型
     * @return 转换后的对象，类型为 T
     */
    @Override
    public <T> T convert(Object source, Class<T> targetType) {

        // 如果目标类型是基本类型（如 int, long 等），转换为对应的包装类型
        // 这样可以统一处理基本类型和包装类型的转换
        if (targetType.isPrimitive()) {
            targetType = (Class<T>) objectiveClass(targetType);
        }

        // 尝试获取精确匹配的转换器
        Converter converter = converters.get(new ConvertiblePair(source.getClass(), targetType));

        // 如果找不到精确匹配的转换器，且目标类型是数组
        // 尝试使用通用的数组转换器
        if (converter == null && targetType.isArray()) {
            converter = converters.get(new ConvertiblePair(source.getClass(), Arrays.class));
        }

        // 如果仍然找不到转换器，且目标类型是枚举
        // 尝试使用通用的枚举转换器
        if (converter == null && targetType.isEnum()) {
            converter = converters.get(new ConvertiblePair(source.getClass(), Enum.class));
        }

        // 如果找到了合适的转换器，执行转换操作
        if (converter != null) {
            return (T) converter.convert(source, targetType);
        }

        // 如果找不到转换器，直接返回源对象
        // 这种情况下假设源类型和目标类型是兼容的
        return (T) source;
    }

    /**
     * 获取指定类的数组类型的 Class 对象
     *
     * <p>此方法用于将普通类型转换为对应的数组类型。
     * 例如，传入 String.class 将返回 String[].class。</p>
     *
     * <p>实现原理：</p>
     * <ul>
     *   <li>使用反射创建一个长度为 0 的数组</li>
     *   <li>获取这个数组的 Class 对象</li>
     *   <li>返回这个 Class 对象，它就是数组类型的 Class</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * Class<String[]> stringArrayClass = arrayClass(String.class);
     * // stringArrayClass 就是 String[].class
     * </pre>
     *
     * @param <C> 泛型参数，表示输入的类类型
     * @param klass 要获取数组类型的 Class 对象，不能为 null
     * @return 指定类的数组类型的 Class 对象
     */
    public static <C> Class<C[]> arrayClass(Class<C> klass) {
        // 使用 Array.newInstance 创建一个长度为 0 的数组实例
        // 然后获取这个实例的 Class 对象，这就是数组类型的 Class
        return (Class<C[]>) Array.newInstance(klass, 0).getClass();
    }

    /**
     * 获取表示给定类的对应的包装类型或对象类型
     *
     * <p>此方法主要用于处理基本类型和包装类型的转换关系。
     * 在 Java 中，每个基本类型都有对应的包装类型（例如 int 对应 Integer）。
     * 这个方法将基本类型转换为对应的包装类型，方便统一处理。</p>
     *
     * <p>处理逻辑：</p>
     * <ol>
     *   <li>如果是数组类型，递归处理其组件类型</li>
     *   <li>如果是基本类型，返回对应的包装类型</li>
     *   <li>否则返回原类型</li>
     * </ol>
     *
     * <p>基本类型与包装类型的对应关系：</p>
     * <ul>
     *   <li>char -> Character</li>
     *   <li>int -> Integer</li>
     *   <li>boolean -> Boolean</li>
     *   <li>byte -> Byte</li>
     *   <li>double -> Double</li>
     *   <li>float -> Float</li>
     *   <li>long -> Long</li>
     *   <li>short -> Short</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>
     * objectiveClass(int.class)      // 返回 Integer.class
     * objectiveClass(boolean.class)  // 返回 Boolean.class
     * objectiveClass(String.class)   // 返回 String.class（非基本类型，返回原类型）
     * objectiveClass(int[].class)    // 返回 Integer[].class（数组类型，递归处理）
     * </pre>
     *
     * @param klass 要获取对象类型的 Class 对象，不能为 null
     * @return 继承自 Object 的 Class 对象，表示给定类的包装类型或对象类型
     */
    public static Class<?> objectiveClass(Class<?> klass) {
        // 获取数组的组件类型（如果该类是数组）
        Class<?> component = klass.getComponentType();

        // 如果是数组类型
        if (component != null) {
            // 如果组件类型是基本类型或数组类型
            // 递归处理组件类型，然后转换为数组类型
            if (component.isPrimitive() || component.isArray())
                return arrayClass(objectiveClass(component));
        }
        // 如果是基本类型（非数组）
        else if (klass.isPrimitive()) {
            // 根据不同的基本类型返回对应的包装类型
            if (klass == char.class)
                return Character.class;
            if (klass == int.class)
                return Integer.class;
            if (klass == boolean.class)
                return Boolean.class;
            if (klass == byte.class)
                return Byte.class;
            if (klass == double.class)
                return Double.class;
            if (klass == float.class)
                return Float.class;
            if (klass == long.class)
                return Long.class;
            if (klass == short.class)
                return Short.class;
        }

        // 如果不是基本类型也不是数组，直接返回原类型
        return klass;
    }

}
