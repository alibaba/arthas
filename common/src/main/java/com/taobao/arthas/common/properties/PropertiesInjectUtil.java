package com.taobao.arthas.common.properties;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * 把properties配置注入对象里。
 *
 * <pre>
 *  在对象上可以用 {@link Config} 来指明要注入的属性的前缀。
 *
 *  在对象内部可以用 {@link NestedConfig} 来指明要注入的内部对象。
 * </pre>
 *
 * @author hengyunabc
 *
 */
public abstract class PropertiesInjectUtil {

    /**
     *
     * @param systemPropertiesFirst
     *            是否使用system properties来覆盖用户的配置
     * @param properties
     * @param object
     */
    public static void inject(boolean systemPropertiesFirst, Properties properties, Object object) {
        if (systemPropertiesFirst) {
            overrideBySystemProperties(properties);
        }
        inject(properties, "", object);
    }

    /**
     *
     * @param properties
     * @param object
     */
    public static void inject(Properties properties, Object object) {
        inject(true, properties, object);
    }

    public static void inject(Properties properties, String parentPrefix, Object object) {
        Class<? extends Object> clazz = object.getClass();

        Config annotation = clazz.getAnnotation(Config.class);

        String prefix = "";

        if (annotation == null) {
            prefix = parentPrefix;
        } else {
            prefix = annotation.prefix();
            if (prefix != null) {

                if (parentPrefix != null && parentPrefix.length() > 0) {
                    prefix = parentPrefix + '.' + prefix;
                }

            }
        }

        Map<String, String> prefixMap = getPrefixMap(prefix, properties);

        Method[] declaredMethods = clazz.getDeclaredMethods();
        if (declaredMethods != null) {
            // 获取到所有setter方法，再提取出field。根据前缀从 properties里取出值，再尝试用setter方法注入到对象里
            for (Method method : declaredMethods) {
                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();

                if (parameterTypes != null && parameterTypes.length == 1 && methodName.startsWith("set")
                                && methodName.length() > "set".length()) {
                    try {
                        String field = getFieldNameFromSetterMethod(methodName);

                        String value = prefixMap.get(prefix.isEmpty()? field : prefix + '.' + field);

                        Object methodParaValue = null;

                        if (value != null) {
                            Class<?> paraClazz = parameterTypes[0];
                            if (paraClazz.equals(String.class)) {
                                methodParaValue = value;
                            } else if (paraClazz.equals(int.class) || paraClazz.equals(Integer.class)) {
                                methodParaValue = Integer.parseInt(value);
                            } else if (paraClazz.equals(long.class) || paraClazz.equals(Long.class)) {
                                methodParaValue = Long.parseLong(value);
                            } else if (paraClazz.equals(float.class) || paraClazz.equals(Float.class)) {
                                methodParaValue = Float.parseFloat(value);
                            } else if (paraClazz.equals(double.class) || paraClazz.equals(Double.class)) {
                                methodParaValue = Double.parseDouble(value);
                            } else if (paraClazz.equals(boolean.class) || paraClazz.equals(Boolean.class)) {
                                methodParaValue = Boolean.parseBoolean(value);
                            } else if (paraClazz.equals(String[].class)) {
                                // TODO 支持 int ,long 等的数组
                                String[] split = value.split(",");
                                if (split != null) {
                                    for (int i = 0; i < split.length; ++i) {
                                        split[i] = split[i].trim();
                                    }
                                    methodParaValue = split;
                                }
                            } else if (paraClazz.isEnum()) {
                                methodParaValue = Enum.valueOf((Class<? extends Enum>) paraClazz, value);
                            } else if (paraClazz.equals(InetAddress.class)) {
                                methodParaValue = InetAddress.getByName(value);
                            } else if (paraClazz.equals(Charset.class)) {
                                methodParaValue = Charset.forName(value);
                            } else if (paraClazz.equals(File.class)) {
                                methodParaValue = new File(value);
                            } else {
                                continue;
                            }

                            method.invoke(object, new Object[] { methodParaValue });
                        }

                    } catch (Exception e) {
                        throw new RuntimeException("PropertiesInjectUtil inject properties error.", e);
                    }
                }
            }
        }

        // process @NestedConfig
        Field[] fields = clazz.getDeclaredFields();
        if (fields != null) {
            for (Field field : fields) {
                NestedConfig nestedConfig = field.getAnnotation(NestedConfig.class);
                if (nestedConfig != null) {
                    String prefixForField = field.getName();
                    if (parentPrefix != null && prefix.length() > 0) {
                        prefixForField = prefix + '.' + prefixForField;
                    }

                    if (!getPrefixMap(prefixForField, properties).isEmpty()) {
                        field.setAccessible(true);
                        try {
                            Object fieldValue = field.get(object);
                            if (fieldValue == null) {
                                fieldValue = field.getType().newInstance();
                            }
                            inject(properties, prefixForField, fieldValue);

                            field.set(object, fieldValue);

                        } catch (Exception e) {
                            throw new RuntimeException("PropertiesInjectUtil inject properties error.", e);
                        }
                    }
                }
            }
        }

    }

    /**
     * 从properties里取出以特定前缀为开头的key/value。
     *
     * @param prefix
     * @param properties
     * @return
     */
    private static Map<String, String> getPrefixMap(String prefix, Properties properties) {
        Map<String, String> result = new HashMap<String, String>(16);

        Set<Entry<Object, Object>> entrySet = properties.entrySet();

        for (Entry<Object, Object> entry : entrySet) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String && value != null) {
                if (prefix.isEmpty() || ((String) key).startsWith(prefix + '.')) {
                    result.put((String) key, value.toString());
                }
            }
        }

        return result;
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

    private static void overrideBySystemProperties(Properties properties) {
        for (Object key : properties.keySet()) {
            if (key instanceof String) {
                String keyStr = (String) key;
                String value = System.getProperty(keyStr, properties.getProperty(keyStr));
                if (value != null) {
                    properties.setProperty(keyStr, value);
                }
            }
        }
    }
}
