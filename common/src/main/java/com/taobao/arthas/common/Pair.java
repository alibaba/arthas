package com.taobao.arthas.common;

/**
 * 泛型键值对类
 * 用于存储两个相关的对象，常用于需要返回两个值的方法
 * 这是一个不可变类，一旦创建就不能修改其包含的对象
 *
 * @param <X> 第一个值的类型
 * @param <Y> 第二个值的类型
 */
public class Pair<X, Y> {
    /**
     * 第一个值
     * 使用final修饰，确保不可变
     */
    private final X x;

    /**
     * 第二个值
     * 使用final修饰，确保不可变
     */
    private final Y y;

    /**
     * 构造函数
     * 创建一个新的键值对对象
     *
     * @param x 第一个值
     * @param y 第二个值
     */
    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 获取第一个值
     *
     * @return 第一个值
     */
    public X getFirst() {
        return x;
    }

    /**
     * 获取第二个值
     *
     * @return 第二个值
     */
    public Y getSecond() {
        return y;
    }

    /**
     * 静态工厂方法
     * 创建一个新的键值对对象，使用泛型推断，使代码更简洁
     *
     * @param a 第一个值
     * @param b 第二个值
     * @param <A> 第一个值的类型
     * @param <B> 第二个值的类型
     * @return 包含两个值的Pair对象
     */
    public static <A, B> Pair<A, B> make(A a, B b) {
        return new Pair<A, B>(a, b);
    }

    /**
     * 判断两个Pair对象是否相等
     * 两个Pair对象相等当且仅当它们的第一个值和第二个值都相等
     *
     * @param o 要比较的对象
     * @return 如果相等返回true，否则返回false
     */
    @Override
    public boolean equals(Object o) {
        // 如果是同一个对象，直接返回true
        if (o == this)
            return true;
        // 如果不是Pair类型，返回false
        if (!(o instanceof Pair))
            return false;

        // 强制转换为Pair类型
        Pair other = (Pair) o;

        // 比较第一个值
        if (x == null) {
            // 当前对象的x为null，检查另一个对象的x是否也为null
            if (other.x != null)
                return false;
        } else {
            // 当前对象的x不为null，使用equals方法比较
            if (!x.equals(other.x))
                return false;
        }
        // 比较第二个值
        if (y == null) {
            // 当前对象的y为null，检查另一个对象的y是否也为null
            if (other.y != null)
                return false;
        } else {
            // 当前对象的y不为null，使用equals方法比较
            if (!y.equals(other.y))
                return false;
        }
        // 两个值都相等，返回true
        return true;
    }

    /**
     * 计算Pair对象的哈希码
     * 基于第一个值和第二个值的哈希码计算
     *
     * @return 哈希码值
     */
    @Override
    public int hashCode() {
        // 初始哈希码为1
        int hashCode = 1;
        // 如果第一个值不为null，将其哈希码作为结果
        if (x != null)
            hashCode = x.hashCode();
        // 如果第二个值不为null，使用31作为乘数计算哈希码
        // 31是常用的质数乘数，可以减少哈希冲突
        if (y != null)
            hashCode = (hashCode * 31) + y.hashCode();
        return hashCode;
    }

    /**
     * 将Pair对象转换为字符串
     * 格式为：P[first,second]
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "P[" + x + "," + y + "]";
    }
}
