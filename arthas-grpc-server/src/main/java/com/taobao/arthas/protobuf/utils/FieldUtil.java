package com.taobao.arthas.protobuf.utils;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:33
 */

import com.baidu.bjf.remoting.protobuf.annotation.Ignore;
import com.baidu.bjf.remoting.protobuf.code.CodedConstant;
import com.baidu.bjf.remoting.protobuf.utils.ClassHelper;
import com.baidu.bjf.remoting.protobuf.utils.FieldInfo;
import com.baidu.bjf.remoting.protobuf.utils.FieldUtils;
import com.taobao.arthas.protobuf.ProtobufField;
import com.taobao.arthas.protobuf.ProtobufFieldTypeEnum;
import com.taobao.arthas.protobuf.annotation.ProtobufCustomizedField;
import com.taobao.arthas.protobuf.annotation.ProtobufIgnore;
import com.taobao.arthas.service.req.ArthasSampleRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:33
 * @description: FieldUtil
 */
public class FieldUtil {

    public static final String PACKAGE_SEPARATOR = ".";

    public static final Map<Class<?>, ProtobufFieldTypeEnum> TYPE_MAPPER;

    static {
        TYPE_MAPPER = new HashMap<Class<?>, ProtobufFieldTypeEnum>();

        TYPE_MAPPER.put(int.class, ProtobufFieldTypeEnum.INT32);
        TYPE_MAPPER.put(Integer.class, ProtobufFieldTypeEnum.INT32);
        TYPE_MAPPER.put(short.class, ProtobufFieldTypeEnum.INT32);
        TYPE_MAPPER.put(Short.class, ProtobufFieldTypeEnum.INT32);
        TYPE_MAPPER.put(Byte.class, ProtobufFieldTypeEnum.INT32);
        TYPE_MAPPER.put(byte.class, ProtobufFieldTypeEnum.INT32);
        TYPE_MAPPER.put(long.class, ProtobufFieldTypeEnum.INT64);
        TYPE_MAPPER.put(Long.class, ProtobufFieldTypeEnum.INT64);
        TYPE_MAPPER.put(String.class, ProtobufFieldTypeEnum.STRING);
        TYPE_MAPPER.put(byte[].class, ProtobufFieldTypeEnum.BYTES);
        TYPE_MAPPER.put(Byte[].class, ProtobufFieldTypeEnum.BYTES);
        TYPE_MAPPER.put(Float.class, ProtobufFieldTypeEnum.FLOAT);
        TYPE_MAPPER.put(float.class, ProtobufFieldTypeEnum.FLOAT);
        TYPE_MAPPER.put(double.class, ProtobufFieldTypeEnum.DOUBLE);
        TYPE_MAPPER.put(Double.class, ProtobufFieldTypeEnum.DOUBLE);
        TYPE_MAPPER.put(Boolean.class, ProtobufFieldTypeEnum.BOOL);
        TYPE_MAPPER.put(boolean.class, ProtobufFieldTypeEnum.BOOL);
        TYPE_MAPPER.put(Date.class, ProtobufFieldTypeEnum.DATE);
        TYPE_MAPPER.put(BigDecimal.class, ProtobufFieldTypeEnum.BIGDECIMAL);
        TYPE_MAPPER.put(BigInteger.class, ProtobufFieldTypeEnum.BIGINTEGER);
    }


    /**
     * 将指定类的所有 java 字段转化为 protobuf 字段
     * 字段编号逻辑：优先处理自定义的字段，其余字段在最大的自定义字段基础上递增
     *
     * @param clazz
     * @param enableZigZap
     * @return
     */
    public static List<ProtobufField> getProtobufFieldList(Class<?> clazz, boolean enableZigZap) {
        // 获取所有的 java field
        List<Field> fields = new ArrayList<>();
        Field[] fieldsArray = clazz.getDeclaredFields();
        for (Field field : fieldsArray) {
            if (field.getAnnotation(ProtobufIgnore.class) == null) {
                fields.add(field);
            }
        }

        // 转化为 protobuf field
        List<ProtobufField> res = new ArrayList<>();
        List<ProtobufField> unOrderFields = new ArrayList<>();
        Set<Integer> orders = new HashSet<>();
        int maxOrder = 0;

        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();
            String filedTypeName = fieldType.getName();
            ProtobufCustomizedField customizedField = field.getAnnotation(ProtobufCustomizedField.class);
            int order = 0;

            if (field.getAnnotation(Ignore.class) != null || Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            // protobuf 不支持除字节数组以外任何数组
            if (filedTypeName.startsWith("[")) {
                if ((!filedTypeName.equals(byte[].class.getName())) && (!filedTypeName.equals(Byte[].class.getName()))) {
                    throw new RuntimeException("Array type of field '" + fieldName + "' on class '"
                            + field.getDeclaringClass().getName() + "' is not support,  please use List instead.");
                }
            }

            ProtobufField protobufField = new ProtobufField();
            protobufField.parseListOrMap(field);
            protobufField.setJavaField(field);

            ProtobufFieldTypeEnum protobufFieldType = ProtobufFieldTypeEnum.DEFAULT;
            if (customizedField != null) {
                order = customizedField.order();
                protobufFieldType = customizedField.protoBufFieldType();
            }

            // 如果不是自定义字段，则通过 javaType 和 protobufType 映射关系来决定
            if (protobufFieldType == ProtobufFieldTypeEnum.DEFAULT) {
                if (protobufField.isList()) {
                    fieldType = protobufField.getGenericKeyType();
                }
                if (fieldType == null) {
                    fieldType = Object.class;
                }

                protobufFieldType = TYPE_MAPPER.get(fieldType);
                if (protobufFieldType == null) {
                    if (Enum.class.isAssignableFrom(fieldType)) {
                        protobufFieldType = ProtobufFieldTypeEnum.ENUM;
                    } else if (protobufField.isMap()) {
                        protobufFieldType = ProtobufFieldTypeEnum.MAP;
                    } else {
                        protobufFieldType = ProtobufFieldTypeEnum.OBJECT;
                    }
                }

                // 处理 zigzap 编码
                if (enableZigZap) {
                    if (protobufFieldType == ProtobufFieldTypeEnum.INT32) {
                        protobufFieldType = ProtobufFieldTypeEnum.SINT32; // to convert to sint32 to enable zagzip
                    } else if (protobufFieldType == ProtobufFieldTypeEnum.INT64) {
                        protobufFieldType = ProtobufFieldTypeEnum.SINT64; // to convert to sint64 to enable zagzip
                    }
                }
            }
            protobufField.setProtobufFieldType(protobufFieldType);

            // 如果是自定义字段，则取自定义值中的order，否则记录到未排序的 list 中，等待后续处理
            if (order > 0) {
                if (orders.contains(order)) {
                    throw new RuntimeException(
                            "order id '" + order + "' from field name '" + fieldName + "'  is duplicate");
                }
                orders.add(order);
                protobufField.setOrder(order);
                maxOrder = Math.max(maxOrder, order);
            } else {
                unOrderFields.add(protobufField);
            }

            res.add(protobufField);
        }

        if (unOrderFields.isEmpty()) {
            return res;
        }

        for (ProtobufField protobufField : unOrderFields) {
            protobufField.setOrder(++maxOrder);
        }

        return res;
    }

    public static Field findField(Class clazz, String name, Class type) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (name == null && type == null) {
            throw new IllegalArgumentException(
                    "Either name or type of the field must be specified");
        }
        Class searchType = clazz;
        while (!Object.class.equals(searchType) && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if ((name == null || name.equals(field.getName()))
                        && (type == null || type.equals(field.getType()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    public static Object getField(Object t, String name) {
        Field field = findField(t.getClass(), name, null);
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        try {
            return field.get(t);
        } catch (Exception e) {
            //todo log
        }
        return null;
    }

    /**
     * 获取目标的访问方法字符串，如果目标已经声明 getter 则返回 getter，否则使用 FieldUtil.getField
     *
     * @param target
     * @param field
     * @param clazz
     * @param wildcardType
     * @return
     */
    public static String getAccessMethod(String target, Field field, Class<?> clazz, boolean wildcardType) {
        if (field.getModifiers() == Modifier.PUBLIC && !wildcardType) {
            return target + PACKAGE_SEPARATOR + field.getName();
        }

        String getter;
        if ("boolean".equalsIgnoreCase(field.getType().getCanonicalName())) {
            getter = "is" + CodedConstant.capitalize(field.getName());
        } else {
            getter = "get" + CodedConstant.capitalize(field.getName());
        }

        try {
            clazz.getMethod(getter, new Class<?>[0]);
            return target + PACKAGE_SEPARATOR + getter + "()";
        } catch (Exception e) {
            //todo log
        }

        String type = field.getType().getCanonicalName();
        if ("[B".equals(type) || "[Ljava.lang.Byte;".equals(type) || "java.lang.Byte[]".equals(type)) {
            type = "byte[]";
        }

        // use reflection to get value
        String code = "(" + FieldUtils.toObjectType(type) + ") ";
        code += "FieldUtils.getField(" + target + ", \"" + field.getName() + "\")";

        return code;
    }

    public static String getMappedTypeSize(){
        //todo
        return null;
    }
}
