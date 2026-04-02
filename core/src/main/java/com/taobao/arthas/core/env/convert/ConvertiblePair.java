package com.taobao.arthas.core.env.convert;

/**
 * 可转换类型对
 *
 * 用于保存源类型到目标类型的配对信息。这是一个不可变类，
 * 用于在类型转换系统中标识一个特定的转换路径。
 *
 * <p>主要用途：</p>
 * <ul>
 *   <li>作为转换器的键，标识一个特定的转换器能够处理的类型对</li>
 *   <li>在转换器注册表中查找对应的转换器</li>
 *   <li>作为缓存键存储转换结果</li>
 * </ul>
 *
 * <p>示例：</p>
 * <pre>
 * ConvertiblePair pair = new ConvertiblePair(String.class, Integer.class);
 * // 表示从 String 到 Integer 的转换路径
 * </pre>
 */
public final class ConvertiblePair {

    /**
     * 源类型，即被转换对象的类型
     * 使用 final 修饰确保不可变性
     */
    private final Class<?> sourceType;

    /**
     * 目标类型，即转换后对象的类型
     * 使用 final 修饰确保不可变性
     */
    private final Class<?> targetType;

    /**
     * 创建一个新的源类型到目标类型的配对
     *
     * <p>构造函数参数：</p>
     * <ul>
     *   <li>sourceType: 源类型，表示从哪个类型进行转换</li>
     *   <li>targetType: 目标类型，表示转换到哪个类型</li>
     * </ul>
     *
     * @param sourceType 源类型的 Class 对象，不能为 null
     * @param targetType 目标类型的 Class 对象，不能为 null
     */
    public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    /**
     * 获取源类型
     *
     * @return 源类型的 Class 对象
     */
    public Class<?> getSourceType() {
        return this.sourceType;
    }

    /**
     * 获取目标类型
     *
     * @return 目标类型的 Class 对象
     */
    public Class<?> getTargetType() {
        return this.targetType;
    }

    /**
     * 判断两个 ConvertiblePair 对象是否相等
     *
     * <p>相等条件：</p>
     * <ul>
     *   <li>两个对象引用相同（使用 == 判断）</li>
     *   <li>或者源类型和目标类型都相等</li>
     * </ul>
     *
     * @param obj 要比较的对象
     * @return 如果相等返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object obj) {
        // 首先检查引用是否相同
        if (this == obj) {
            return true;
        }
        // 检查对象是否为 null 或类型不匹配
        if (obj == null || obj.getClass() != ConvertiblePair.class) {
            return false;
        }
        // 类型转换后比较源类型和目标类型
        ConvertiblePair other = (ConvertiblePair) obj;
        return this.sourceType.equals(other.sourceType) && this.targetType.equals(other.targetType);
    }

    /**
     * 计算对象的哈希值
     *
     * <p>使用源类型和目标类型的哈希值组合计算，
     * 算法为：sourceType.hashCode() * 31 + targetType.hashCode()</p>
     *
     * @return 对象的哈希值
     */
    @Override
    public int hashCode() {
        return this.sourceType.hashCode() * 31 + this.targetType.hashCode();
    }

    /**
     * 返回对象的字符串表示
     *
     * <p>格式为："源类型全限定名 -> 目标类型全限定名"</p>
     * <p>例如："java.lang.String -> java.lang.Integer"</p>
     *
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return this.sourceType.getName() + " -> " + this.targetType.getName();
    }
}