package com.taobao.arthas.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.taobao.arthas.core.env.Environment;

/**
 * 配置绑定工具类
 * 负责将配置属性注入到对象中，支持嵌套配置和前缀管理
 *
 * @author hengyunabc 2020-01-10
 *
 */
public class BinderUtils {

    /**
     * 将配置注入到对象实例中（无前缀）
     *
     * @param environment 环境配置对象，包含所有配置属性
     * @param instance 需要注入配置的目标对象实例
     */
    public static void inject(Environment environment, Object instance) {
        inject(environment, null, null, instance);
    }

    /**
     * 将配置注入到对象实例中（带前缀）
     *
     * @param environment 环境配置对象，包含所有配置属性
     * @param prefix 配置属性的前缀
     * @param instance 需要注入配置的目标对象实例
     */
    public static void inject(Environment environment, String prefix, Object instance) {
        inject(environment, null, prefix, instance);
    }

    /**
     * 将配置注入到对象实例中（完整版本）
     * 支持通过setter方法注入配置，支持嵌套配置对象
     *
     * @param environment 环境配置对象，包含所有配置属性
     * @param parentPrefix 父级前缀
     * @param prefix 当前配置前缀
     * @param instance 需要注入配置的目标对象实例
     */
    public static void inject(Environment environment, String parentPrefix, String prefix, Object instance) {
        // 如果前缀为null，设置为空字符串
        if (prefix == null) {
            prefix = "";
        }

        // 获取目标对象的类型
        Class<? extends Object> type = instance.getClass();
        try {
            // 获取类上的@Config注解
            Config annotation = type.getAnnotation(Config.class);

            // 如果没有@Config注解，使用父前缀+当前前缀的组合
            if (annotation == null) {
                prefix = parentPrefix + '.' + prefix;
            } else {
                // 如果有@Config注解，使用注解中定义的前缀
                prefix = annotation.prefix();
                if (prefix != null) {
                    // 如果存在父前缀，将父前缀和当前前缀拼接
                    if (parentPrefix != null && parentPrefix.length() > 0) {
                        prefix = parentPrefix + '.' + prefix;
                    }
                }
            }

            // 获取类中声明的所有方法
            Method[] declaredMethods = type.getDeclaredMethods();

            // 遍历所有方法，找到setter方法进行配置注入
            // 获取到所有setter方法，再提取出field。根据前缀从 properties里取出值，再尝试用setter方法注入到对象里
            for (Method method : declaredMethods) {
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();

                // 判断是否为setter方法：参数个数为1，方法名以"set"开头，且方法名长度大于"set"
                if (parameterTypes.length == 1 && methodName.startsWith("set") && methodName.length() > "set".length()) {

                    // 从setter方法名中提取字段名
                    String field = getFieldNameFromSetterMethod(methodName);

                    // 构建完整的配置键：前缀.字段名
                    String configKey = prefix + '.' + field;

                    // 如果环境中存在该配置键
                    if (environment.containsProperty(configKey)) {
                        // 从环境中获取配置值，并转换为setter方法的参数类型
                        Object reslovedValue = environment.getProperty(prefix + '.' + field, parameterTypes[0]);
                        if (reslovedValue != null) {
                            // 通过反射调用setter方法，将配置值注入到对象中
                            method.invoke(instance, new Object[] { reslovedValue });
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("inject error. prefix: " + prefix + ", instance: " + instance, e);
        }

        // 处理嵌套配置对象（@NestedConfig注解的字段）
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            // 检查字段是否有@NestedConfig注解
            NestedConfig nestedConfig = field.getAnnotation(NestedConfig.class);
            if (nestedConfig != null) {
                // 构建嵌套字段的配置前缀：当前前缀.字段名
                String prefixForField = prefixForField = field.getName();
                if (parentPrefix != null && prefix.length() > 0) {
                    prefixForField = prefix + '.' + prefixForField;
                }

                // 设置字段可访问（即使是私有字段）
                field.setAccessible(true);
                try {
                    // 获取字段当前值
                    Object fieldValue = field.get(instance);
                    // 如果字段值为null，创建新实例
                    if (fieldValue == null) {
                        fieldValue = field.getType().newInstance();
                    }
                    // 递归调用inject方法，为嵌套对象注入配置
                    inject(environment, prefix, prefixForField, fieldValue);

                    // 将配置后的嵌套对象设置回原字段
                    field.set(instance, fieldValue);
                } catch (Exception e) {
                    throw new RuntimeException("process @NestedConfig error, field: " + field + ", prefix: "
                            + prefix + ", instance: " + instance, e);
                }
            }
        }
    }

    /**
     * 从setter方法名中提取字段名
     * 例如：setHost -> host，setUserName -> userName
     *
     * @param methodName setter方法名，必须以"set"开头
     * @return 字段名（首字母小写）
     */
    private static String getFieldNameFromSetterMethod(String methodName) {
        // 去掉"set"前缀，得到剩余部分
        String field = methodName.substring("set".length());

        // 将首字母转换为小写
        String startPart = field.substring(0, 1).toLowerCase();
        // 获取除首字母外的剩余部分
        String endPart = field.substring(1);

        // 拼接得到最终的字段名
        field = startPart + endPart;
        return field;
    }

}
