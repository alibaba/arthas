package com.taobao.arthas.core.util.reflect;

import com.taobao.arthas.core.util.StringUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 字段工具类
 * 提供对Java字段（Field）的反射操作工具方法
 *
 * @author ralf0131 2016-12-28 14:39.
 */
public class FieldUtils {

    /**
     * 访问权限测试掩码
     * 用于检测字段是否为包访问权限
     * 包含了public、protected、private三种访问修饰符
     */
    private static final int ACCESS_TEST = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;

    /**
     * 读取指定的public字段值
     * 仅考虑指定对象本身的类，不考虑其父类
     *
     * @param target 要反射的对象，不能为{@code null}
     * @param fieldName 要获取的字段名称
     * @return 字段的值
     * @throws IllegalArgumentException 如果{@code target}为{@code null}，或字段名为空或空白，或找不到字段
     * @throws IllegalAccessException 如果指定的字段不是{@code public}
     */
    public static Object readDeclaredField(final Object target, final String fieldName) throws IllegalAccessException {
        return readDeclaredField(target, fieldName, false);
    }

    /**
     * 根据名称获取字段值
     * 仅考虑指定对象本身的类，不考虑其父类
     *
     * @param target 要反射的对象，不能为{@code null}
     * @param fieldName 要获取的字段名称
     * @param forceAccess 是否打破访问限制
     *            使用{@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}方法。
     *            {@code false}将只匹配public字段
     * @return 字段对象
     * @throws IllegalArgumentException 如果{@code target}为{@code null}，或字段名为空或空白，或找不到字段
     * @throws IllegalAccessException 如果字段无法被访问
     */
    public static Object readDeclaredField(final Object target, final String fieldName, final boolean forceAccess) throws IllegalAccessException {
        // 验证目标对象不为空
        isTrue(target != null, "target object must not be null");
        // 获取目标对象的类
        final Class<?> cls = target.getClass();
        // 获取指定字段
        final Field field = getDeclaredField(cls, fieldName, forceAccess);
        // 验证字段存在
        isTrue(field != null, "Cannot locate declared field %s.%s", cls, fieldName);
        // 上面已经强制设置了访问权限，这里不再重复：
        return readField(field, target, false);
    }

    /**
     * 根据名称获取可访问的字段，可选择是否打破访问限制
     * 仅考虑指定的类本身，不考虑其父类
     *
     * @param cls 要反射的{@link Class}，不能为{@code null}
     * @param fieldName 要获取的字段名称
     * @param forceAccess 是否打破访问限制
     *            使用{@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}方法。
     *            {@code false}将只匹配{@code public}字段
     * @return 字段对象
     * @throws IllegalArgumentException 如果类为{@code null}，或字段名为空或空白
     */
    public static Field getDeclaredField(final Class<?> cls, final String fieldName, final boolean forceAccess) {
        // 验证类不为空
        isTrue(cls != null, "The class must not be null");
        // 验证字段名不为空
        isTrue(!StringUtils.isBlank(fieldName), "The field name must not be blank/empty");
        try {
            // 使用getDeclaredField()只考虑指定的类本身
            final Field field = cls.getDeclaredField(fieldName);
            // 检查字段是否可访问
            if (!isAccessible(field)) {
                if (forceAccess) {
                    // 如果强制访问，则设置为可访问
                    field.setAccessible(true);
                } else {
                    // 否则返回null
                    return null;
                }
            }
            return field;
        } catch (final NoSuchFieldException e) { // NOPMD
            // 字段不存在，忽略异常
        }
        return null;
    }

    /**
     * 读取字段的值
     *
     * @param field 要使用的字段
     * @param target 要调用的对象，对于{@code static}字段可以为{@code null}
     * @param forceAccess 是否打破访问限制
     *            使用{@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}方法
     * @return 字段的值
     * @throws IllegalArgumentException 如果字段为{@code null}
     * @throws IllegalAccessException 如果字段无法被访问
     */
    public static Object readField(final Field field, final Object target, final boolean forceAccess) throws IllegalAccessException {
        // 验证字段不为空
        isTrue(field != null, "The field must not be null");
        // 如果需要强制访问且字段当前不可访问
        if (forceAccess && !field.isAccessible()) {
            // 设置字段为可访问
            field.setAccessible(true);
        } else {
            // 否则尝试使用变通方法设置可访问
            setAccessibleWorkaround(field);
        }
        // 获取并返回字段的值
        return field.get(target);
    }

    /**
     * 读取可访问的静态字段值
     *
     * @param field 要读取的字段
     * @return 字段的值
     * @throws IllegalArgumentException 如果字段为{@code null}或不是{@code static}
     * @throws IllegalAccessException 如果字段无法被访问
     */
    public static Object readStaticField(final Field field) throws IllegalAccessException {
        return readStaticField(field, false);
    }

    /**
     * 读取静态字段的值
     *
     * @param field 要读取的字段
     * @param forceAccess 是否打破访问限制
     *            使用{@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}方法
     * @return 字段的值
     * @throws IllegalArgumentException 如果字段为{@code null}或不是{@code static}
     * @throws IllegalAccessException 如果字段无法被访问
     */
    public static Object readStaticField(final Field field, final boolean forceAccess) throws IllegalAccessException {
        // 验证字段不为空
        isTrue(field != null, "The field must not be null");
        // 验证字段是静态的
        isTrue(Modifier.isStatic(field.getModifiers()), "The field '%s' is not static", field.getName());
        // 读取静态字段，目标对象传null
        return readField(field, (Object) null, forceAccess);
    }

    /**
     * 写入public静态字段的值
     *
     * @param field 要写入的字段
     * @param value 要设置的值
     * @throws IllegalArgumentException 如果字段为{@code null}或不是{@code static}，或值不可赋值
     * @throws IllegalAccessException 如果字段不是{@code public}或是{@code final}
     */
    public static void writeStaticField(final Field field, final Object value) throws IllegalAccessException {
        writeStaticField(field, value, false);
    }

    /**
     * 写入静态字段的值
     *
     * @param field 要写入的字段
     * @param value 要设置的值
     * @param forceAccess 是否打破访问限制
     *            使用{@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}方法。
     *            {@code false}将只匹配{@code public}字段
     * @throws IllegalArgumentException 如果字段为{@code null}或不是{@code static}，或值不可赋值
     * @throws IllegalAccessException 如果字段无法被访问或是{@code final}
     */
    public static void writeStaticField(final Field field, final Object value, final boolean forceAccess) throws IllegalAccessException {
        // 验证字段不为空
        isTrue(field != null, "The field must not be null");
        // 验证字段是静态的
        isTrue(Modifier.isStatic(field.getModifiers()), "The field %s.%s is not static", field.getDeclaringClass().getName(),
                field.getName());
        // 写入静态字段，目标对象传null
        writeField(field, (Object) null, value, forceAccess);
    }

    /**
     * 写入字段的值
     *
     * @param field 要写入的字段
     * @param target 要调用的对象，对于{@code static}字段可以为{@code null}
     * @param value 要设置的值
     * @param forceAccess 是否打破访问限制
     *            使用{@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}方法。
     *            {@code false}将只匹配{@code public}字段
     * @throws IllegalArgumentException 如果字段为{@code null}或值不可赋值
     * @throws IllegalAccessException 如果字段无法被访问或是{@code final}
     */
    public static void writeField(final Field field, final Object target, final Object value, final boolean forceAccess)
            throws IllegalAccessException {
        // 验证字段不为空
        isTrue(field != null, "The field must not be null");
        // 如果需要强制访问且字段当前不可访问
        if (forceAccess && !field.isAccessible()) {
            // 设置字段为可访问
            field.setAccessible(true);
        } else {
            // 否则尝试使用变通方法设置可访问
            setAccessibleWorkaround(field);
        }
        // 设置字段的值
        field.set(target, value);
    }

    /**
     * 获取给定类及其所有父类（如果有）的所有字段
     *
     * @param cls 要查询的{@link Class}
     * @return 字段数组（可能为空）
     * @throws IllegalArgumentException 如果类为{@code null}
     * @since 3.2
     */
    public static Field[] getAllFields(final Class<?> cls) {
        // 获取所有字段的列表形式
        final List<Field> allFieldsList = getAllFieldsList(cls);
        // 转换为数组返回
        return allFieldsList.toArray(new Field[0]);
    }

    /**
     * 获取给定类及其所有父类（如果有）的所有字段
     *
     * @param cls 要查询的{@link Class}
     * @return 字段列表（可能为空）
     * @throws IllegalArgumentException 如果类为{@code null}
     * @since 3.2
     */
    public static List<Field> getAllFieldsList(final Class<?> cls) {
        // 验证类不为空
        isTrue(cls != null, "The class must not be null");
        // 创建用于存储所有字段的列表
        final List<Field> allFields = new ArrayList<Field>();
        // 从当前类开始向上遍历
        Class<?> currentClass = cls;
        while (currentClass != null) {
            // 获取当前类声明的所有字段
            final Field[] declaredFields = currentClass.getDeclaredFields();
            // 将字段添加到列表中
            allFields.addAll(Arrays.asList(declaredFields));
            // 移动到父类
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

    /**
     * 根据名称获取可访问的字段，遵循访问权限
     * 会考虑父类和接口
     *
     * @param cls 要反射的{@link Class}，不能为{@code null}
     * @param fieldName 要获取的字段名称
     * @return 字段对象
     * @throws IllegalArgumentException 如果类为{@code null}，或字段名为空或空白
     */
    public static Field getField(final Class<?> cls, final String fieldName) {
        // 获取字段，不强制访问
        final Field field = getField(cls, fieldName, false);
        // 尝试使用变通方法设置可访问
        setAccessibleWorkaround(field);
        return field;
    }

    /**
     * 根据名称获取可访问的字段，可选择是否打破访问限制
     * 会考虑父类和接口
     *
     * @param cls 要反射的{@link Class}，不能为{@code null}
     * @param fieldName 要获取的字段名称
     * @param forceAccess 是否打破访问限制
     *            使用{@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}方法。
     *            {@code false}将只匹配{@code public}字段
     * @return 字段对象
     * @throws IllegalArgumentException 如果类为{@code null}，或字段名为空或空白，
     *             或在继承层次结构中多个位置匹配到字段
     */
    public static Field getField(final Class<?> cls, final String fieldName, final boolean forceAccess) {
        // 验证类不为空
        isTrue(cls != null, "The class must not be null");
        // 验证字段名不为空
        isTrue(!StringUtils.isBlank(fieldName), "The field name must not be blank/empty");
        // FIXME 这个变通方法是否还需要？lang需要Java 6
        // Sun Java 1.3的getField实现有bug，所以我们要自己写代码

        // getField()会返回字段对象，其声明的类正确设置为声明该字段的类
        // 因此在子类上请求字段将返回来自父类的字段
        //
        // 查找的优先级顺序：
        // 搜索类的private/protected/package/public字段
        // 父类的protected/package/public字段
        // private/不同的包会阻止对更上层父类的访问
        // 实现的接口的public字段

        // 向上检查父类层次结构
        for (Class<?> acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                // 获取当前类声明的字段
                final Field field = acls.getDeclaredField(fieldName);
                // getDeclaredField也会检查非public作用域，并返回准确结果
                if (!Modifier.isPublic(field.getModifiers())) {
                    if (forceAccess) {
                        // 如果强制访问，设置可访问
                        field.setAccessible(true);
                    } else {
                        // 否则继续向上查找
                        continue;
                    }
                }
                return field;
            } catch (final NoSuchFieldException ex) { // NOPMD
                // 字段不存在，忽略
            }
        }
        // 检查public接口的情况。这必须手动搜索
        // 以防有public的超超类字段被private/package父类字段隐藏
        Field match = null;
        // 遍历所有接口
        for (final Class<?> class1 : getAllInterfaces(cls)) {
            try {
                // 获取接口的public字段
                final Field test = class1.getField(fieldName);
                // 验证字段引用不模糊（即不在多个接口中同时存在）
                isTrue(match == null, "Reference to field %s is ambiguous relative to %s"
                        + "; a matching field exists on two or more implemented interfaces.", fieldName, cls);
                match = test;
            } catch (final NoSuchFieldException ex) { // NOPMD
                // 字段不存在，忽略
            }
        }
        return match;
    }

    /**
     * 获取给定类及其父类实现的所有接口列表
     *
     * 顺序是通过依次查看源文件中声明的每个接口并跟踪其层次结构来确定的
     * 然后以相同的方式考虑每个父类。后面的重复项被忽略，因此顺序得以保持
     *
     * @param cls 要查找的类，可能为{@code null}
     * @return 按顺序排列的接口列表，如果输入为null则返回{@code null}
     */
    public static List<Class<?>> getAllInterfaces(final Class<?> cls) {
        // 如果类为null，返回null
        if (cls == null) {
            return null;
        }

        // 使用LinkedHashSet保持接口的发现顺序
        final LinkedHashSet<Class<?>> interfacesFound = new LinkedHashSet<Class<?>>();
        // 递归获取所有接口
        getAllInterfaces(cls, interfacesFound);

        // 返回包含所有接口的列表
        return new ArrayList<Class<?>>(interfacesFound);
    }

    /**
     * 获取指定类的所有接口
     * 这是一个递归方法，会获取类及其父类的所有接口，包括接口继承的接口
     *
     * @param cls 要查找的类，可能为{@code null}
     * @param interfacesFound 类的接口集合
     */
    private static void getAllInterfaces(Class<?> cls, final HashSet<Class<?>> interfacesFound) {
        // 遍历类层次结构
        while (cls != null) {
            // 获取当前类直接实现的接口
            final Class<?>[] interfaces = cls.getInterfaces();

            // 遍历每个接口
            for (final Class<?> i : interfaces) {
                // 如果接口是新发现的（避免重复）
                if (interfacesFound.add(i)) {
                    // 递归获取该接口继承的接口
                    getAllInterfaces(i, interfacesFound);
                }
            }

            // 移动到父类继续查找
            cls = cls.getSuperclass();
        }
    }


    /**
     * 默认访问权限父类的变通方法
     *
     * 当一个{@code public}类有一个默认访问权限（包访问）的父类，且该父类有{@code public}成员时，
     * 这些成员是可访问的。从编译代码中调用它们可以正常工作。
     * 不幸的是，在某些JVM上，使用反射调用这些成员似乎（错误地）阻止访问，即使修饰符是{@code public}。
     * 调用{@code setAccessible(true)}可以解决这个问题，但只对具有足够权限的代码有效。
     * 如果有更好的变通方法，我们将非常感激。
     *
     * @param o 要设置为可访问的AccessibleObject
     * @return 布尔值，指示对象的可访问性是否已设置为true
     */
    static boolean setAccessibleWorkaround(final AccessibleObject o) {
        // 如果对象为null或已经可访问，无需处理
        if (o == null || o.isAccessible()) {
            return false;
        }
        // 转换为Member接口
        final Member m = (Member) o;
        // 如果成员是public，但声明类是包访问权限，需要特殊处理
        if (!o.isAccessible() && Modifier.isPublic(m.getModifiers()) && isPackageAccess(m.getDeclaringClass().getModifiers())) {
            try {
                // 尝试设置为可访问
                o.setAccessible(true);
                return true;
            } catch (final SecurityException e) { // NOPMD
                // 忽略安全异常，后续会有IllegalAccessException
            }
        }
        return false;
    }

    /**
     * 判断给定的修饰符是否表示包访问权限
     * 包访问权限是指没有public、protected、private修饰符
     *
     * @param modifiers 要测试的修饰符
     * @return {@code true}除非检测到{@code package}/{@code protected}/{@code private}修饰符
     */
    static boolean isPackageAccess(final int modifiers) {
        // 如果modifiers与ACCESS_TEST的按位与结果为0，说明没有public、protected、private修饰符
        // 即为包访问权限
        return (modifiers & ACCESS_TEST) == 0;
    }

    /**
     * 判断一个成员是否可访问
     * 可访问的条件是：成员不为null、是public的、不是合成的（编译器自动生成的）
     *
     * @param m 要检查的成员
     * @return 如果<code>m</code>可访问则返回{@code true}
     */
    static boolean isAccessible(final Member m) {
        // 成员不为null、修饰符是public、不是合成成员
        return m != null && Modifier.isPublic(m.getModifiers()) && !m.isSynthetic();
    }

    /**
     * 断言表达式为true
     * 如果表达式为false，抛出带有格式化消息的IllegalArgumentException
     *
     * @param expression 要测试的布尔表达式
     * @param message 异常消息格式字符串
     * @param values 消息格式化的参数值
     * @throws IllegalArgumentException 如果表达式为false
     */
    static void isTrue(final boolean expression, final String message, final Object... values) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }
}
