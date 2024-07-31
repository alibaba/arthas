package com.taobao.arthas.protobuf.utils;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:33
 */


import com.baidu.bjf.remoting.protobuf.EnumReadable;
import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.code.ICodeGenerator;
import com.baidu.bjf.remoting.protobuf.utils.ClassHelper;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MapEntry;
import com.google.protobuf.WireFormat;
import com.taobao.arthas.protobuf.ProtobufCodec;
import com.taobao.arthas.protobuf.ProtobufField;
import com.taobao.arthas.protobuf.ProtobufFieldTypeEnum;
import com.taobao.arthas.protobuf.ProtobufProxy;
import com.taobao.arthas.protobuf.annotation.ProtobufPacked;
import com.taobao.arthas.protobuf.annotation.ProtobufCustomizedField;
import com.taobao.arthas.protobuf.annotation.ProtobufIgnore;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:33
 * @description: FieldUtil
 */
public class FieldUtil {

    public static final String PACKAGE_SEPARATOR = ".";

    public static final Map<Class<?>, ProtobufFieldTypeEnum> TYPE_MAPPER;

    private static final Map<String, String> PRIMITIVE_TYPE_MAPPING;

    static {

        PRIMITIVE_TYPE_MAPPING = new HashMap<String, String>();

        PRIMITIVE_TYPE_MAPPING.put(int.class.getSimpleName(), Integer.class.getSimpleName());
        PRIMITIVE_TYPE_MAPPING.put(long.class.getSimpleName(), Long.class.getSimpleName());
        PRIMITIVE_TYPE_MAPPING.put(short.class.getSimpleName(), Short.class.getSimpleName());
        PRIMITIVE_TYPE_MAPPING.put(boolean.class.getSimpleName(), Boolean.class.getSimpleName());
        PRIMITIVE_TYPE_MAPPING.put(double.class.getSimpleName(), Double.class.getSimpleName());
        PRIMITIVE_TYPE_MAPPING.put(float.class.getSimpleName(), Float.class.getSimpleName());
        PRIMITIVE_TYPE_MAPPING.put(char.class.getSimpleName(), Character.class.getSimpleName());
        PRIMITIVE_TYPE_MAPPING.put(byte.class.getSimpleName(), Byte.class.getSimpleName());
    }

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

            if (field.getAnnotation(ProtobufIgnore.class) != null || Modifier.isTransient(field.getModifiers())) {
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

            // 如果使用 packed 注解则打包
            if (protobufField.isList() && (protobufField.getProtobufFieldType().isPrimitive() || protobufField.getProtobufFieldType().isEnum())) {
                protobufField.setPacked(field.getAnnotation(ProtobufPacked.class) != null);
            }
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
    public static String getGetterDynamicString(String target, Field field, Class<?> clazz, boolean wildcardType) {
        if (field.getModifiers() == Modifier.PUBLIC && !wildcardType) {
            return target + PACKAGE_SEPARATOR + field.getName();
        }

        String getter;
        if ("boolean".equalsIgnoreCase(field.getType().getCanonicalName())) {
            getter = "is" + capitalize(field.getName());
        } else {
            getter = "get" + capitalize(field.getName());
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

        String code = "(" + toObjectType(type) + ") ";
        code += "FieldUtil.getField(" + target + ", \"" + field.getName() + "\")";

        return code;
    }

    public static String getSizeDynamicString(ProtobufField field) {
        ProtobufFieldTypeEnum protobufFieldType = field.getProtobufFieldType();
        int order = field.getOrder();
        boolean isList = field.isList();
        boolean isMap = field.isMap();
        boolean packed = field.isPacked();
        String typeName = protobufFieldType.getType().toUpperCase();
        String dynamicFieldName = getDynamicFieldName(order);


        if (isList) {
            return "FieldUtil.getListSize(" + order + "," + dynamicFieldName + "," + ProtobufFieldTypeEnum.class.getName() + "." + typeName
                    + "," + field.isPacked() + ");\n";
        } else if (isMap) {
            return "FieldUtil.getMapSize(" + order + "," + dynamicFieldName + "," + getMapFieldGenericParameterString(field) + ");\n";
        }

        if (protobufFieldType == ProtobufFieldTypeEnum.OBJECT) {
            return "FieldUtil.getObjectSize(" + order + "," + dynamicFieldName + ", " + ProtobufFieldTypeEnum.class.getName() + "."
                    + typeName + ");\n";
        }

        String javaType = protobufFieldType.getType();
        if (protobufFieldType == ProtobufFieldTypeEnum.STRING) {
            javaType = "String";
        }

        if (protobufFieldType == ProtobufFieldTypeEnum.BYTES) {
            javaType = "ByteArray";
        }
        javaType = capitalize(javaType);
        dynamicFieldName = dynamicFieldName + protobufFieldType.getToPrimitiveType();
        //todo check 感觉上面这个有点问题，测试的时候看下
        return "com.google.protobuf.CodedOutputStream.compute" + javaType + "Size(" + order + "," + dynamicFieldName + ")"
                + ");\n";
    }

    private static String getMapFieldGenericParameterString(ProtobufField field) {
        String wireFormatClassName = WireFormat.FieldType.class.getCanonicalName();
        ProtobufFieldTypeEnum fieldType = TYPE_MAPPER.get(field.getGenericKeyType());
        String keyClass;
        String defaultKeyValue;
        if (fieldType == null) {
            if (Enum.class.isAssignableFrom(field.getGenericKeyType())) {
                keyClass = wireFormatClassName + ".ENUM";
                Class<?> declaringClass = field.getGenericKeyType();
                Field[] fields = declaringClass.getFields();
                if (fields.length > 0) {
                    defaultKeyValue = field.getGenericKeyType().getCanonicalName() + "."
                            + fields[0].getName();
                } else {
                    defaultKeyValue = "0";
                }

            } else {
                keyClass = wireFormatClassName + ".MESSAGE";
                boolean hasDefaultConstructor = hasDefaultConstructor(field.getGenericKeyType());
                if (!hasDefaultConstructor) {
                    throw new IllegalArgumentException("Class '" + field.getGenericKeyType().getCanonicalName()
                            + "' must has default constructor method with no parameters.");
                }
                defaultKeyValue =
                        "new " + field.getGenericKeyType().getCanonicalName() + "()";
            }
        } else {
            keyClass = wireFormatClassName + "." + fieldType.toString();

            defaultKeyValue = fieldType.getDefaultValue();
        }

        fieldType = TYPE_MAPPER.get(field.getGenericValueType());
        String valueClass;
        String defaultValueValue;
        if (fieldType == null) {
            if (Enum.class.isAssignableFrom(field.getGenericValueType())) {
                valueClass = wireFormatClassName + ".ENUM";
                Class<?> declaringClass = field.getGenericValueType();
                Field[] fields = declaringClass.getFields();
                if (fields.length > 0) {
                    defaultValueValue = field.getGenericValueType().getCanonicalName()
                            + "." + fields[0].getName();
                } else {
                    defaultValueValue = "0";
                }

            } else {
                valueClass = wireFormatClassName + ".MESSAGE";
                // check constructor
                boolean hasDefaultConstructor = hasDefaultConstructor(field.getGenericValueType());
                if (!hasDefaultConstructor) {
                    throw new IllegalArgumentException("Class '" + field.getGenericValueType().getCanonicalName()
                            + "' must has default constructor method with no parameters.");
                }
                defaultValueValue =
                        "new " + field.getGenericValueType().getCanonicalName() + "()";
            }
        } else {
            valueClass = wireFormatClassName + "." + fieldType;
            defaultValueValue = fieldType.getDefaultValue();
        }
        String joinedSentence = keyClass + "," + defaultKeyValue + "," + valueClass + "," + defaultValueValue;
        return joinedSentence;
    }

    public static boolean hasDefaultConstructor(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        try {
            cls.getConstructor(new Class<?>[0]);
        } catch (NoSuchMethodException e) {
            return false;
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return true;
    }

    /**
     * 通过 order 获取动态生成的字段名
     *
     * @param order
     * @return
     */
    public static String getDynamicFieldName(int order) {
        return "f_" + order;
    }

    public static int getListSize(int order, Collection<?> list, ProtobufFieldTypeEnum type, boolean packed) {
        int size = 0;
        if (list == null || list.isEmpty()) {
            return size;
        }

        int dataSize = 0;
        for (Object object : list) {
            dataSize += getObjectSize(order, object, type);
        }
        size += dataSize;
        if (type != ProtobufFieldTypeEnum.OBJECT) {
            if (packed) {
                size += com.google.protobuf.CodedOutputStream.computeInt32SizeNoTag(dataSize);
                int tag = makeTag(order, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                size += com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(tag);
            } else {
                size += list.size() * CodedOutputStream.computeTagSize(order);
            }
        }
        return size;
    }

    public static <K, V> int getMapSize(int order, Map<K, V> map, com.google.protobuf.WireFormat.FieldType keyType,
                                        K defaultKey, com.google.protobuf.WireFormat.FieldType valueType, V defalutValue) {
        int size = 0;
        for (java.util.Map.Entry<K, V> entry : map.entrySet()) {
            MapEntry<K, V> valuesDefaultEntry = MapEntry
                    .<K, V>newDefaultInstance(null, keyType, defaultKey, valueType, defalutValue);

            MapEntry<K, V> values =
                    valuesDefaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();

            size += com.google.protobuf.CodedOutputStream.computeMessageSize(order, values);
        }
        return size;
    }

    public static int getObjectSize(int order, Object object, ProtobufFieldTypeEnum type) {
        int size = 0;
        if (object == null) {
            return size;
        }

        if (type == ProtobufFieldTypeEnum.OBJECT) {
            try {
                Class cls = object.getClass();
                ProtobufCodec target = ProtobufProxy.create(cls);
                size = target.size(object);
                size = size + CodedOutputStream.computeRawVarint32Size(size);
                return size + CodedOutputStream.computeTagSize(order);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        if (type == ProtobufFieldTypeEnum.STRING) {
            size = CodedOutputStream.computeStringSizeNoTag(String.valueOf(object));
        } else if (type == ProtobufFieldTypeEnum.BOOL) {
            size = CodedOutputStream.computeBoolSizeNoTag(Boolean.valueOf(String.valueOf(object)));
        } else if (type == ProtobufFieldTypeEnum.BYTES) {
            byte[] bb = (byte[]) object;
            size = CodedOutputStream.computeBytesSizeNoTag(ByteString.copyFrom(bb));
        } else if (type == ProtobufFieldTypeEnum.DOUBLE) {
            size = CodedOutputStream.computeDoubleSizeNoTag(Double.valueOf(object.toString()));
        } else if (type == ProtobufFieldTypeEnum.FIXED32 || type == ProtobufFieldTypeEnum.SFIXED32) {
            size = CodedOutputStream.computeFixed32SizeNoTag(Integer.valueOf(object.toString()));
        } else if (type == ProtobufFieldTypeEnum.INT32 || type == ProtobufFieldTypeEnum.SINT32 || type == ProtobufFieldTypeEnum.UINT32) {
            size = CodedOutputStream.computeInt32SizeNoTag(Integer.valueOf(object.toString()));
        } else if (type == ProtobufFieldTypeEnum.FIXED64 || type == ProtobufFieldTypeEnum.SFIXED64) {
            size = CodedOutputStream.computeSFixed64SizeNoTag(Long.valueOf(object.toString()));
        } else if (type == ProtobufFieldTypeEnum.INT64 || type == ProtobufFieldTypeEnum.SINT64 || type == ProtobufFieldTypeEnum.UINT64) {
            size = CodedOutputStream.computeInt64SizeNoTag(Long.valueOf(object.toString()));
        } else if (type == ProtobufFieldTypeEnum.FLOAT) {
            size = CodedOutputStream.computeFloatSizeNoTag(Float.valueOf(object.toString()));
        } else if (type == ProtobufFieldTypeEnum.ENUM) {
            size = CodedOutputStream.computeInt32SizeNoTag(((Enum) object).ordinal());
        }
        return size;
    }

    /**
     * 首字母大写
     *
     * @param str
     * @return
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toTitleCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 生成 protobuf tag
     *
     * @param fieldNumber
     * @param wireType
     * @return
     */
    public static int makeTag(final int fieldNumber, final int wireType) {
        return (fieldNumber << 3) | wireType;
    }

    /**
     * 基础类型转为包装对象
     *
     * @param primitiveType
     * @return
     */
    public static String toObjectType(String primitiveType) {
        if (PRIMITIVE_TYPE_MAPPING.containsKey(primitiveType)) {
            return PRIMITIVE_TYPE_MAPPING.get(primitiveType);
        }
        return primitiveType;
    }
}
