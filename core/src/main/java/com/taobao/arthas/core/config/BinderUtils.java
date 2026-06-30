package com.taobao.arthas.core.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.taobao.arthas.core.env.Environment;

/**
 * 
 * @author hengyunabc 2020-01-10
 *
 */
public class BinderUtils {

    public static void inject(Environment environment, Object instance) {
        inject(environment, null, null, instance);
    }

    public static void inject(Environment environment, String prefix, Object instance) {
        inject(environment, null, prefix, instance);
    }

    public static void inject(Environment environment, String parentPrefix, String prefix, Object instance) {
        if (prefix == null) {
            prefix = "";
        }
        Class<? extends Object> type = instance.getClass();
        try {
            Config annotation = type.getAnnotation(Config.class);

            if (annotation == null) {
                prefix = parentPrefix + '.' + prefix;
            } else {
                prefix = annotation.prefix();
                if (prefix != null) {
                    if (parentPrefix != null && parentPrefix.length() > 0) {
                        prefix = parentPrefix + '.' + prefix;
                    }
                }
            }

            Method[] declaredMethods = type.getDeclaredMethods();
            // 获取到所有setter方法，再提取出field。根据前缀从 properties里取出值，再尝试用setter方法注入到对象里
            for (Method method : declaredMethods) {
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterTypes.length == 1 && methodName.startsWith("set") && methodName.length() > "set".length()) {

                    String field = getFieldNameFromSetterMethod(methodName);
                    String configKey = prefix + '.' + field;
                    if (environment.containsProperty(configKey)) {
                        Object reslovedValue = environment.getProperty(prefix + '.' + field, parameterTypes[0]);
                        if (reslovedValue != null) {
                            method.invoke(instance, new Object[] { reslovedValue });
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("inject error. prefix: " + prefix + ", instance: " + instance, e);
        }

        // process @NestedConfig
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            NestedConfig nestedConfig = field.getAnnotation(NestedConfig.class);
            if (nestedConfig != null) {
                String prefixForField = field.getName();
                if (parentPrefix != null && prefix.length() > 0) {
                    prefixForField = prefix + '.' + prefixForField;
                }

                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(instance);
                    if (fieldValue == null) {
                        fieldValue = field.getType().newInstance();
                    }
                    inject(environment, prefix, prefixForField, fieldValue);

                    field.set(instance, fieldValue);
                } catch (Exception e) {
                    throw new RuntimeException("process @NestedConfig error, field: " + field + ", prefix: "
                            + prefix + ", instance: " + instance, e);
                }
            }
        }
    }

    /**
     * 从setter方法获取到field的String。比如 setHost， 则获取到的是host。
     *
     * @param methodName
     * @return
     */
    private static String getFieldNameFromSetterMethod(String methodName) {
        String field = methodName.substring("set".length());
        String startPart = field.substring(0, 1).toLowerCase();
        String endPart = field.substring(1);

        field = startPart + endPart;
        return field;
    }

}
