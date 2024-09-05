package com.taobao.arthas.grpc.server.protobuf;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:14
 */

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:14
 * @description: ProtobufField
 */
public class ProtobufField {

    /**
     * 序号
     */
    private int order;

    /**
     * protobuf 字段类型
     */
    private ProtobufFieldTypeEnum protobufFieldType;

    /**
     * java 字段类型
     */
    private Field javaField;

    /**
     * 是否为 List
     */
    private boolean isList;

    /**
     * 是否为 Map
     */
    private boolean isMap;

    /**
     * List & Map key 的泛型类型
     */
    private Class<?> genericKeyType;

    /**
     * Map value 的泛型类型
     */
    private Class<?> genericValueType;

    /**
     * 是否是通配符类型
     */
    private boolean wildcardType;

    private boolean packed;

    /**
     * 处理 List 和 Map 类型字段
     *
     * @param field
     */
    public void parseListOrMap(Field field) {
        Class<?> cls = field.getType();
        boolean needCheckGenericType = false;
        if (List.class.isAssignableFrom(cls) || Set.class.isAssignableFrom(cls)) {
            isList = true;
            needCheckGenericType = true;
        }
        if (Map.class.isAssignableFrom(cls)) {
            isMap = true;
            needCheckGenericType = true;
        }

        if (!needCheckGenericType) {
            return;
        }

        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;

            Type[] actualTypeArguments = ptype.getActualTypeArguments();

            if (actualTypeArguments != null) {

                int length = actualTypeArguments.length;
                if (isList) {
                    if (length != 1) {
                        throw new RuntimeException(
                                "List must use generic definiation like List<String>, please check  field name '"
                                        + field.getName() + " at class " + field.getDeclaringClass().getName());
                    }
                } else if (isMap) {
                    if (length != 2) {
                        throw new RuntimeException(
                                "Map must use generic definiation like Map<String, String>, please check  field name '"
                                        + field.getName() + " at class " + field.getDeclaringClass().getName());
                    }
                }

                Type targetType = actualTypeArguments[0];
                if (targetType instanceof Class) {
                    genericKeyType = (Class) targetType;
                } else if (targetType instanceof ParameterizedType) {
                    boolean mapKey = false;
                    if (isMap) {
                        mapKey = true;
                    }
                    throw new RuntimeException(noSubParameterizedType(field, mapKey));
                } else if (WildcardType.class.isAssignableFrom(targetType.getClass())) {
                    wildcardType = true;
                    WildcardType wildcardType = (WildcardType) targetType;

                    Type[] upperBounds = wildcardType.getUpperBounds();
                    if (upperBounds != null && upperBounds.length == 1) {
                        if (upperBounds[0] instanceof Class) {
                            genericKeyType = (Class) upperBounds[0];
                        }
                    }
                }

                if (actualTypeArguments.length > 1) {
                    targetType = actualTypeArguments[1];
                    if (targetType instanceof Class) {
                        genericValueType = (Class) targetType;
                    } else if (targetType instanceof ParameterizedType) {
                        boolean mapKey = false;
                        if (isMap) {
                            mapKey = true;
                        }
                        throw new RuntimeException(noSubParameterizedType(field, mapKey));
                    } else if (WildcardType.class.isAssignableFrom(targetType.getClass())) {
                        wildcardType = true;
                        WildcardType wildcardType = (WildcardType) targetType;

                        Type[] upperBounds = wildcardType.getUpperBounds();
                        if (upperBounds != null && upperBounds.length == 1) {
                            if (upperBounds[0] instanceof Class) {
                                genericValueType = (Class) upperBounds[0];
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * No sub parameterized type.
     *
     * @param field     the field
     * @param listOrMap the list or map
     * @return the string
     */
    private String noSubParameterizedType(Field field, boolean listOrMap) {
        String key = "List";
        if (listOrMap) {
            key = "Map";
        }
        return key + " can not has sub parameterized type  please check  field name '" + field.getName() + " at class "
                + field.getDeclaringClass().getName();

    }

    public boolean isEnumValueType() {
        if (genericValueType != null) {
            return Enum.class.isAssignableFrom(genericValueType);
        }
        return false;
    }

    public boolean isEnumKeyType() {
        if (genericKeyType != null) {
            return Enum.class.isAssignableFrom(genericKeyType);
        }
        return false;
    }

    public static boolean isListType(Field field) {
        return List.class.isAssignableFrom(field.getType());
    }

    public static boolean isSetType(Field field) {
        return Set.class.isAssignableFrom(field.getType());
    }

    public static boolean isPrimitiveType(Class c) {
        if (c.isPrimitive()) {
            return true;
        }
        if (c.getName().equals(String.class.getName())) {
            return true;
        }
        return false;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public ProtobufFieldTypeEnum getProtobufFieldType() {
        return protobufFieldType;
    }

    public void setProtobufFieldType(ProtobufFieldTypeEnum protobufFieldType) {
        this.protobufFieldType = protobufFieldType;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public boolean isMap() {
        return isMap;
    }

    public void setMap(boolean map) {
        isMap = map;
    }

    public Class<?> getGenericKeyType() {
        return genericKeyType;
    }

    public void setGenericKeyType(Class<?> genericKeyType) {
        this.genericKeyType = genericKeyType;
    }

    public Class<?> getGenericValueType() {
        return genericValueType;
    }

    public void setGenericValueType(Class<?> genericValueType) {
        this.genericValueType = genericValueType;
    }

    public Field getJavaField() {
        return javaField;
    }

    public void setJavaField(Field javaField) {
        this.javaField = javaField;
    }

    public boolean isWildcardType() {
        return wildcardType;
    }

    public void setWildcardType(boolean wildcardType) {
        this.wildcardType = wildcardType;
    }

    public boolean isPacked() {
        return packed;
    }

    public void setPacked(boolean packed) {
        this.packed = packed;
    }

    @Override
    public String toString() {
        return "ProtobufField{" +
                "order=" + order +
                ", protobufFieldType=" + protobufFieldType +
                ", isList=" + isList +
                ", isMap=" + isMap +
                ", genericKeyType=" + genericKeyType +
                ", genericValueType=" + genericValueType +
                ", wildcardType=" + wildcardType +
                '}';
    }
}
