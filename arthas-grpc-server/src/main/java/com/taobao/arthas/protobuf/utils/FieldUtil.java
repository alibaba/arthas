package com.taobao.arthas.protobuf.utils;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:33
 */


import com.baidu.bjf.remoting.protobuf.EnumReadable;
import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.utils.ClassHelper;
import com.baidu.bjf.remoting.protobuf.utils.FieldInfo;
import com.baidu.bjf.remoting.protobuf.utils.ProtobufProxyUtils;
import com.baidu.bjf.remoting.protobuf.utils.StringUtils;
import com.google.protobuf.*;
import com.taobao.arthas.protobuf.ProtobufCodec;
import com.taobao.arthas.protobuf.ProtobufField;
import com.taobao.arthas.protobuf.ProtobufFieldTypeEnum;
import com.taobao.arthas.protobuf.ProtobufProxy;
import com.taobao.arthas.protobuf.annotation.ProtobufPacked;
import com.taobao.arthas.protobuf.annotation.ProtobufCustomizedField;
import com.taobao.arthas.protobuf.annotation.ProtobufIgnore;


import java.io.IOException;
import java.lang.Enum;
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


    public static final Map<Class<?>, ProtobufFieldTypeEnum> TYPE_MAPPER;

    public static final Map<String, String> PRIMITIVE_TYPE_MAPPING;

    public static final String DYNAMIC_TARGET = "target";

    public static final String PACKAGE_SEPARATOR = ".";

    public static final String LINE_BREAK = "\n";

    public static final String JAVA_LINE_BREAK = ";" + LINE_BREAK;

    public static final String CODE_OUTPUT_STREAM_OBJ_NAME = "output";

    public static final String WIREFORMAT_CLSNAME = com.google.protobuf.WireFormat.FieldType.class.getCanonicalName();

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

    /**
     * 在 clazz 上寻找字段名为 name 的字段
     *
     * @param clazz
     * @param name
     * @return
     */
    public static Field findField(Class clazz, String name) {
        return findField(clazz, name, null);
    }

    /**
     * 在 clazz 上寻找字段名为 name、字段类型为 type 的字段
     *
     * @param clazz
     * @param name
     * @param type
     * @return
     */
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

    /**
     * 获取对象 t 上的字段名为 name 的字段值
     *
     * @param t
     * @param name
     * @return
     */
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

    public static void setField(Object t, String name, Object value) {
        Field field = findField(t.getClass(), name);
        if (field == null) {
            return;
        }
        field.setAccessible(true);
        try {
            field.set(t, value);
        } catch (Exception e) {
            //todo log
        }
    }

    /**
     * 读取 input put 进 map
     * @param input
     * @param map
     * @param keyType
     * @param defaultKey
     * @param valueType
     * @param defalutValue
     * @param <K>
     * @param <V>
     * @throws IOException
     */
    public static <K, V> void putMapValue(CodedInputStream input, Map<K, V> map,
                                          com.google.protobuf.WireFormat.FieldType keyType, K defaultKey,
                                          com.google.protobuf.WireFormat.FieldType valueType, V defalutValue) throws IOException {
        putMapValue(input, map, keyType, defaultKey, valueType, defalutValue, null);
    }

    public static <K, V> void putMapValue(CodedInputStream input, Map<K, V> map,
                                          com.google.protobuf.WireFormat.FieldType keyType, K defaultKey,
                                          com.google.protobuf.WireFormat.FieldType valueType, V defalutValue, EnumHandler<V> handler)
            throws IOException {
        putMapValue(input, map, keyType, defaultKey, valueType, defalutValue, null, handler);

    }

    public static <K, V> void putMapValue(CodedInputStream input, Map<K, V> map,
                                          com.google.protobuf.WireFormat.FieldType keyType, K defaultKey,
                                          com.google.protobuf.WireFormat.FieldType valueType, V defalutValue, EnumHandler<K> keyHandler, EnumHandler<V> valHandler)
            throws IOException {
        MapEntry<K, V> valuesDefaultEntry = MapEntry
                .<K, V> newDefaultInstance(null, keyType, defaultKey, valueType, defalutValue);

        MapEntry<K, V> values =
                input.readMessage(valuesDefaultEntry.getParserForType(), null);

        Object value = values.getValue();
        Object key = values.getKey();
        if (keyHandler != null) {
            key = keyHandler.handle((int) key);
        }

        if (valHandler != null) {
            value = valHandler.handle((int) value);
        }
        map.put((K) key, (V) value);
    }


    public static String getGetterDynamicString(ProtobufField protobufField, Class<?> dynamicTargetClass) {
        Field field = protobufField.getJavaField();
        boolean wildcardType = protobufField.isWildcardType();

        if (field.getModifiers() == Modifier.PUBLIC && !wildcardType) {
            return DYNAMIC_TARGET + PACKAGE_SEPARATOR + field.getName();
        }

        String getter;
        if ("boolean".equalsIgnoreCase(field.getType().getCanonicalName())) {
            getter = "is" + capitalize(field.getName());
        } else {
            getter = "get" + capitalize(field.getName());
        }

        try {
            dynamicTargetClass.getMethod(getter, new Class<?>[0]);
            return DYNAMIC_TARGET + PACKAGE_SEPARATOR + getter + "()";
        } catch (Exception e) {
            //todo log
        }

        String type = field.getType().getCanonicalName();
        if ("[B".equals(type) || "[Ljava.lang.Byte;".equals(type) || "java.lang.Byte[]".equals(type)) {
            type = "byte[]";
        }

        String code = "(" + toObjectType(type) + ") ";
        code += "FieldUtil.getField(" + DYNAMIC_TARGET + ", \"" + field.getName() + "\")";

        return code;
    }

    /**
     * 获取计算 size 动态字符串
     *
     * @param field
     * @return
     */
    public static String getSizeDynamicString(ProtobufField field) {
        ProtobufFieldTypeEnum protobufFieldType = field.getProtobufFieldType();
        int order = field.getOrder();
        boolean isList = field.isList();
        boolean isMap = field.isMap();
        String typeName = protobufFieldType.getType().toUpperCase();
        String dynamicFieldName = getDynamicFieldName(order);


        if (isList) {
            return "FieldUtil.getListSize(" + order + "," + dynamicFieldName + "," + ProtobufFieldTypeEnum.class.getName() + "." + typeName
                    + "," + field.isPacked() + ");" + LINE_BREAK;
        } else if (isMap) {
            return "FieldUtil.getMapSize(" + order + "," + dynamicFieldName + "," + getMapFieldGenericParameterString(field) + ");" + LINE_BREAK;
        }

        if (protobufFieldType == ProtobufFieldTypeEnum.OBJECT) {
            return "FieldUtil.getObjectSize(" + order + "," + dynamicFieldName + ", " + ProtobufFieldTypeEnum.class.getName() + "."
                    + typeName + ");" + LINE_BREAK;
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
                + ");" + LINE_BREAK;
    }

    /**
     * 获取写入 CodedOutputStream 动态字符串
     *
     * @param protobufField
     * @return
     */
    public static String getWriteByteDynamicString(ProtobufField protobufField) {
        ProtobufFieldTypeEnum protobufFieldType = protobufField.getProtobufFieldType();
        int order = protobufField.getOrder();
        String dynamicFieldName = getDynamicFieldName(protobufField.getOrder());
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(dynamicFieldName).append(" != null){").append(LINE_BREAK);

        if (protobufField.isList()) {
            String typeString = protobufFieldType.getType().toUpperCase();
            sb.append("Field.writeList(").append(CODE_OUTPUT_STREAM_OBJ_NAME).append(",");
            sb.append(order).append(",").append(ProtobufFieldTypeEnum.class.getName()).append(".").append(typeString);
            sb.append(",").append(dynamicFieldName).append(",").append(Boolean.valueOf(protobufField.isPacked())).append(")")
                    .append(JAVA_LINE_BREAK).append("}").append(LINE_BREAK);
            return sb.toString();
        } else if (protobufField.isMap()) {
            sb.append("Field.writeMap(").append(CODE_OUTPUT_STREAM_OBJ_NAME).append(",");
            sb.append(order).append(",").append(dynamicFieldName);

            String joinedSentence = getMapFieldGenericParameterString(protobufField);
            sb.append(",").append(joinedSentence);

            sb.append(")").append(JAVA_LINE_BREAK).append("}").append(LINE_BREAK);
            return sb.toString();
        } else {
            dynamicFieldName = dynamicFieldName + protobufFieldType.getToPrimitiveType();
        }

        if (protobufFieldType == ProtobufFieldTypeEnum.OBJECT) {
            String typeString = protobufFieldType.getType().toUpperCase();
            sb.append("Field.writeObject(").append(CODE_OUTPUT_STREAM_OBJ_NAME).append(",");
            sb.append(order).append(",").append(ProtobufFieldTypeEnum.class.getName()).append(".").append(typeString);
            sb.append(",").append(dynamicFieldName).append(", false)").append(JAVA_LINE_BREAK).append("}")
                    .append(LINE_BREAK);
            return sb.toString();
        }

        if (protobufFieldType == ProtobufFieldTypeEnum.STRING) {
            sb.append(CODE_OUTPUT_STREAM_OBJ_NAME).append(".writeString(").append(order);
            sb.append(", ").append(dynamicFieldName).append(")").append(JAVA_LINE_BREAK).append("}")
                    .append(LINE_BREAK);
            return sb.toString();
        }

        if (protobufFieldType == ProtobufFieldTypeEnum.BYTES) {
            sb.append(CODE_OUTPUT_STREAM_OBJ_NAME).append(".writeByteArray(").append(order);
            sb.append(", ").append(dynamicFieldName).append(")").append(JAVA_LINE_BREAK).append("}")
                    .append(LINE_BREAK);
            return sb.toString();
        }

        String t = protobufFieldType.getType();
        t = capitalize(t);

        sb.append(CODE_OUTPUT_STREAM_OBJ_NAME).append(".write").append(t).append("(").append(order);
        sb.append(", ").append(dynamicFieldName).append(")").append(JAVA_LINE_BREAK).append("}")
                .append(LINE_BREAK);
        return sb.toString();
    }

    public static void writeList(CodedOutputStream out, int order, ProtobufFieldTypeEnum type, Collection list)
            throws IOException {
        writeList(out, order, type, list, false);
    }

    /**
     * java list 写入 CodedOutputStream
     */
    public static void writeList(CodedOutputStream out, int order, ProtobufFieldTypeEnum type, Collection list, boolean packed)
            throws IOException {
        if (list == null || list.isEmpty()) {
            return;
        }

        CodedOutputStreamCache output = CodedOutputStreamCache.get();
        for (Object object : list) {
            if (object == null) {
                throw new NullPointerException("List can not include Null value.");
            }
            writeObject(output.getCodedOutputStream(), order, type, object, true, !packed);
        }
        byte[] byteArray = output.getData();

        if (packed) {
            out.writeUInt32NoTag(makeTag(order, WireFormat.WIRETYPE_LENGTH_DELIMITED));
            out.writeUInt32NoTag(byteArray.length);
        }

        out.write(byteArray, 0, byteArray.length);

    }

    /**
     * java map 写入 output
     */
    public static <K, V> void writeMap(CodedOutputStream output, int order, Map<K, V> map,
                                       com.google.protobuf.WireFormat.FieldType keyType, K defaultKey,
                                       com.google.protobuf.WireFormat.FieldType valueType, V defalutValue) throws IOException {
        MapEntry<K, V> valuesDefaultEntry = MapEntry
                .<K, V>newDefaultInstance(null, keyType, defaultKey, valueType, defalutValue);
        for (java.util.Map.Entry<K, V> entry : map.entrySet()) {
            MapEntry<K, V> values =
                    valuesDefaultEntry.newBuilderForType().setKey(entry.getKey()).setValue(entry.getValue()).build();
            output.writeMessage(order, values);
        }
    }

    /**
     * java object 写入 CodedOutputStream
     */
    public static void writeObject(CodedOutputStream out, int order, ProtobufFieldTypeEnum type, Object o, boolean list,
                                   boolean withTag) throws IOException {
        if (o == null) {
            return;
        }

        if (type == ProtobufFieldTypeEnum.OBJECT) {

            Class cls = o.getClass();
            ProtobufCodec target = ProtobufProxy.create(cls);

            if (withTag) {
                out.writeUInt32NoTag(makeTag(order, WireFormat.WIRETYPE_LENGTH_DELIMITED));
            }

            byte[] byteArray = target.encode(o);
            out.writeUInt32NoTag(byteArray.length);
            out.write(byteArray, 0, byteArray.length);

            return;
        }

        if (type == ProtobufFieldTypeEnum.BOOL) {
            if (withTag) {
                out.writeBool(order, (Boolean) o);
            } else {
                out.writeBoolNoTag((Boolean) o);
            }
        } else if (type == ProtobufFieldTypeEnum.BYTES) {
            byte[] bb = (byte[]) o;
            if (withTag) {
                out.writeBytes(order, ByteString.copyFrom(bb));
            } else {
                out.writeBytesNoTag(ByteString.copyFrom(bb));
            }
        } else if (type == ProtobufFieldTypeEnum.DOUBLE) {
            if (withTag) {
                out.writeDouble(order, (Double) o);
            } else {
                out.writeDoubleNoTag((Double) o);
            }
        } else if (type == ProtobufFieldTypeEnum.FIXED32) {
            if (withTag) {
                out.writeFixed32(order, (Integer) o);
            } else {
                out.writeFixed32NoTag((Integer) o);
            }
        } else if (type == ProtobufFieldTypeEnum.FIXED64) {
            if (withTag) {
                out.writeFixed64(order, (Long) o);
            } else {
                out.writeFixed64NoTag((Long) o);
            }
        } else if (type == ProtobufFieldTypeEnum.FLOAT) {
            if (withTag) {
                out.writeFloat(order, (Float) o);
            } else {
                out.writeFloatNoTag((Float) o);
            }
        } else if (type == ProtobufFieldTypeEnum.INT32) {
            if (withTag) {
                out.writeInt32(order, (Integer) o);
            } else {
                out.writeInt32NoTag((Integer) o);
            }
        } else if (type == ProtobufFieldTypeEnum.INT64) {
            if (withTag) {
                out.writeInt64(order, (Long) o);
            } else {
                out.writeInt64NoTag((Long) o);
            }
        } else if (type == ProtobufFieldTypeEnum.SFIXED32) {
            if (withTag) {
                out.writeSFixed32(order, (Integer) o);
            } else {
                out.writeSFixed32NoTag((Integer) o);
            }
        } else if (type == ProtobufFieldTypeEnum.SFIXED64) {
            if (withTag) {
                out.writeSFixed64(order, (Long) o);
            } else {
                out.writeSFixed64NoTag((Long) o);
            }
        } else if (type == ProtobufFieldTypeEnum.SINT32) {
            if (withTag) {
                out.writeSInt32(order, (Integer) o);
            } else {
                out.writeSInt32NoTag((Integer) o);
            }
        } else if (type == ProtobufFieldTypeEnum.SINT64) {
            if (withTag) {
                out.writeSInt64(order, (Long) o);
            } else {
                out.writeSInt64NoTag((Long) o);
            }
        } else if (type == ProtobufFieldTypeEnum.STRING) {
            if (withTag) {
                out.writeBytes(order, ByteString.copyFromUtf8(String.valueOf(o)));
            } else {
                out.writeBytesNoTag(ByteString.copyFromUtf8(String.valueOf(o)));
            }
        } else if (type == ProtobufFieldTypeEnum.UINT32) {
            if (withTag) {
                out.writeUInt32(order, (Integer) o);
            } else {
                out.writeUInt32NoTag((Integer) o);
            }
        } else if (type == ProtobufFieldTypeEnum.UINT64) {
            if (withTag) {
                out.writeUInt64(order, (Long) o);
            } else {
                out.writeUInt64NoTag((Long) o);
            }
        } else if (type == ProtobufFieldTypeEnum.ENUM) {
            int value;
            value = ((Enum) o).ordinal();
            if (withTag) {
                out.writeEnum(order, value);
            } else {
                out.writeEnumNoTag(value);
            }
        }
    }

    public static String getMapFieldGenericParameterString(ProtobufField field) {
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

    /**
     * 获取 set 指定对象指定字段的方法
     *
     * @param protobufField
     * @param dynamicTargetClass
     * @param express
     * @return
     */
    public static String getSetFieldDynamicString(ProtobufField protobufField, Class<?> dynamicTargetClass, String express) {
        StringBuilder sb = new StringBuilder();
        boolean isMap = protobufField.isMap();
        boolean isList = protobufField.isList();
        boolean isWildcardType = protobufField.isWildcardType();
        boolean isPacked = protobufField.isPacked();
        Field javaField = protobufField.getJavaField();


        if (isList || isMap) {
            sb.append("if ((").append(getGetterDynamicString(protobufField, dynamicTargetClass)).append(") == null) {")
                    .append(LINE_BREAK);
        }

        String collectionTypetoCreate = "";
        String collectionType = "";
        if (List.class.isAssignableFrom(javaField.getType())) {
            collectionTypetoCreate = "new ArrayList()";
            collectionType = "List";
        } else if (Set.class.isAssignableFrom(javaField.getType())) {
            collectionTypetoCreate = "new HashSet()";
            collectionType = "Set";
        }

        // if field of public modifier we can access directly
        if (Modifier.isPublic(javaField.getModifiers()) && !isWildcardType) {
            if (isList) {
                // should initialize list
                sb.append(DYNAMIC_TARGET).append(PACKAGE_SEPARATOR).append(javaField.getName()).append("= ")
                        .append(collectionTypetoCreate).append(JAVA_LINE_BREAK).append("}")
                        .append(LINE_BREAK);
                if (express != null) {
                    if (isPacked) {
                        sb.append("while (input.getBytesUntilLimit() > 0) {").append(LINE_BREAK);
                    }
                    sb.append(DYNAMIC_TARGET).append(PACKAGE_SEPARATOR).append(javaField.getName()).append(".add(")
                            .append(express).append(")");
                    if (isPacked) {
                        sb.append(";}").append(LINE_BREAK);
                    }
                }
                return sb.toString();
            } else if (isMap) {
                sb.append(DYNAMIC_TARGET).append(PACKAGE_SEPARATOR).append(javaField.getName())
                        .append("= new HashMap()").append(JAVA_LINE_BREAK).append("}")
                        .append(LINE_BREAK);
                return sb.append(express).toString();
            }
            // if date type
            if (javaField.getType().equals(Date.class)) {
                express = "new Date(" + express + ")";
            }
            return DYNAMIC_TARGET + PACKAGE_SEPARATOR + javaField.getName() + "=" + express + LINE_BREAK;
        }
        String setter = "set" + capitalize(javaField.getName());
        // check method exist
        try {
            dynamicTargetClass.getMethod(setter, new Class<?>[]{javaField.getType()});
            if (isList) {
                sb.append(collectionType).append(" __list = ").append(collectionTypetoCreate)
                        .append(JAVA_LINE_BREAK);
                sb.append(DYNAMIC_TARGET).append(PACKAGE_SEPARATOR).append(setter).append("(__list)")
                        .append(JAVA_LINE_BREAK).append("}").append(LINE_BREAK);

                if (express != null) {
                    if (isPacked) {
                        sb.append("while (input.getBytesUntilLimit() > 0) {").append(LINE_BREAK);
                    }
                    sb.append("(").append(getGetterDynamicString(protobufField, dynamicTargetClass)).append(").add(")
                            .append(express).append(")");
                    if (isPacked) {
                        sb.append(";}").append(LINE_BREAK);
                    }
                }
                return sb.toString();
            } else if (isMap) {
                sb.append("Map __map = new HashMap()").append(JAVA_LINE_BREAK);
                sb.append(DYNAMIC_TARGET).append(PACKAGE_SEPARATOR).append(setter).append("(__map)")
                        .append(JAVA_LINE_BREAK).append("}").append(LINE_BREAK);
                return sb + express;
            }

            // fix date type
            if (javaField.getType().equals(Date.class)) {
                express = "new Date(" + express + ")";
            }

            return DYNAMIC_TARGET + PACKAGE_SEPARATOR + setter + "(" + express + ")\n";
        } catch (Exception e) {
            //todo log
        }

        if (isList) {
            sb.append(collectionType).append(" __list = ").append(collectionTypetoCreate)
                    .append(JAVA_LINE_BREAK);
            sb.append("FieldUtil.setField(").append(DYNAMIC_TARGET).append(", \"").append(javaField.getName())
                    .append("\", __list)").append(JAVA_LINE_BREAK).append("}").append(LINE_BREAK);
            if (express != null) {
                if (isPacked) {
                    sb.append("while (input.getBytesUntilLimit() > 0) {").append(LINE_BREAK);
                }
                sb.append("(").append(getGetterDynamicString(protobufField, dynamicTargetClass)).append(").add(")
                        .append(express).append(")");
                if (isPacked) {
                    sb.append(";}").append(LINE_BREAK);
                }
            }
            return sb.toString();
        } else if (isMap) {
            sb.append("Map __map = new HashMap()").append(JAVA_LINE_BREAK);
            sb.append("FieldUtil.setField(").append(DYNAMIC_TARGET).append(", \"").append(javaField.getName())
                    .append("\", __map)").append(JAVA_LINE_BREAK).append("}").append(LINE_BREAK);
            return sb + express;
        }

        // use reflection to get value
        String code = "";
        if (express != null) {
            // if date type
            if (javaField.getType().equals(Date.class)) {
                express = "new Date(" + express + ")";
            }

            code = "FieldUtil.setField(" + DYNAMIC_TARGET + ", \"" + javaField.getName() + "\", " + express + ")"
                    + LINE_BREAK;
        }
        return code;
    }

    public static int getEnumOrdinal(Enum en) {
        if (en != null) {
            return en.ordinal();
        }
        return -1;
    }

    public static <T extends Enum<T>> T getEnumValue(Class<T> enumType, String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        try {
            T v = Enum.valueOf(enumType, name);
            return v;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getEnumName(Enum[] e, int value) {
        if (e != null) {
            int toCompareValue;
            for (Enum en : e) {
                toCompareValue = en.ordinal();
                if (value == toCompareValue) {
                    return en.name();
                }
            }
        }
        return "";
    }

    /**
     * 获取初始化 list、map 字段的动态字符串
     *
     * @param protobufField
     * @return
     */
    public static String getInitListMapFieldDynamicString(ProtobufField protobufField, String express) {
        return "FieldUtil.setField(" + DYNAMIC_TARGET + ", \"" + protobufField.getJavaField().getName() + "\", " + express + ");"
                + LINE_BREAK;
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

    /**
     * 获取 object protobuf size
     *
     * @param order
     * @param object
     * @param type
     * @return
     */
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

    public static boolean isNull(Object o) {
        return o == null;
    }

    public static boolean isNull(double o) {
        return false;
    }

    public static boolean isNull(int o) {
        return false;
    }

    public static boolean isNull(byte o) {
        return false;
    }

    public static boolean isNull(short o) {
        return false;
    }

    public static boolean isNull(long o) {
        return false;
    }

    public static boolean isNull(float o) {
        return false;
    }

    public static boolean isNull(char o) {
        return false;
    }
}
