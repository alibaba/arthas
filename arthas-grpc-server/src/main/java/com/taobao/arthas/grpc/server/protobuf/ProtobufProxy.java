package com.taobao.arthas.grpc.server.protobuf;

import com.google.protobuf.WireFormat;
import com.taobao.arthas.grpc.server.protobuf.annotation.ProtobufEnableZigZap;
import com.taobao.arthas.grpc.server.protobuf.annotation.ProtobufClass;
import com.taobao.arthas.grpc.server.protobuf.utils.ProtoBufUtil;
import com.taobao.arthas.grpc.server.protobuf.utils.MiniTemplator;
import com.taobao.arthas.grpc.server.protobuf.utils.ProtoBufClassCompiler;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: FengYe
 * @date: 2024/7/17 下午9:57
 * @description: ProtoBufProxy
 */
@Deprecated
public class ProtobufProxy {
    private static final String TEMPLATE_FILE = "/class_template.tpl";

    private static final Map<String, ProtobufCodec> codecCache = new ConcurrentHashMap<>();

    private static Class<?> clazz;

    private static MiniTemplator miniTemplator;

    private static List<ProtobufField> protobufFields;

    public static <T> ProtobufCodec<T> getCodecCacheSide(Class<T> clazz) {
        ProtobufCodec<T> codec = codecCache.get(clazz.getName());
        if (codec != null) {
            return codec;
        }
        try {
            codec = create(clazz);
            codecCache.put(clazz.getName(), codec);
            return codec;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static <T> ProtobufCodec<T> create(Class<T> clazz) {
        Objects.requireNonNull(clazz);
        if (clazz.getAnnotation(ProtobufClass.class) == null) {
            throw new IllegalArgumentException(clazz + "class is not annotated with @ProtobufClass");
        }
        ProtobufProxy.clazz = clazz;
        loadProtobufField();

        String path = Objects.requireNonNull(clazz.getResource(TEMPLATE_FILE)).getPath();
        try {
            miniTemplator = new MiniTemplator(path);
        } catch (Exception e) {
            throw new RuntimeException("miniTemplator init failed. " + path, e);
        }

        miniTemplator.setVariable("package", "package " + clazz.getPackage().getName() + ";");

        processImportBlock();

        miniTemplator.setVariable("className", ProtoBufUtil.getClassName(clazz) + "$$ProxyClass");
        miniTemplator.setVariable("codecClassName", ProtobufCodec.class.getName());
        miniTemplator.setVariable("targetProxyClassName", clazz.getCanonicalName());
        processEncodeBlock();
        processDecodeBlock();

        String code = miniTemplator.generateOutput();

        ProtoBufClassCompiler protoBufClassCompiler = new ProtoBufClassCompiler(ProtoBufClassCompiler.class.getClassLoader());
        String fullClassName = ProtoBufUtil.getFullClassName(clazz) + "$$ProxyClass";
        Class<?> newClass = protoBufClassCompiler.compile(fullClassName, code, clazz.getClassLoader());

        try {
            ProtobufCodec<T> newInstance = (ProtobufCodec<T>) newClass.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            return newInstance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void processImportBlock() {
        Set<String> imports = new HashSet<>();
        imports.add("java.util.*");
        imports.add("java.io.IOException");
        imports.add("java.lang.reflect.*");
        imports.add("com.taobao.arthas.grpc.server.protobuf.*");
        imports.add("com.taobao.arthas.grpc.server.protobuf.utils.*");
        imports.add("com.taobao.arthas.grpc.server.protobuf.annotation.*");
        imports.add("com.google.protobuf.*");
        imports.add(clazz.getCanonicalName());
        for (String pkg : imports) {
            miniTemplator.setVariable("importBlock", pkg);
            miniTemplator.addBlock("imports");
        }
    }

    private static void processEncodeBlock() {
        for (ProtobufField protobufField : protobufFields) {
            String dynamicFieldGetter = ProtoBufUtil.getGetterDynamicString(protobufField, clazz);
            String dynamicFieldType = protobufField.isList() ? "Collection" : protobufField.getProtobufFieldType().getJavaType();
            String dynamicFieldName = ProtoBufUtil.getDynamicFieldName(protobufField.getOrder());

            miniTemplator.setVariable("dynamicFieldType", dynamicFieldType);
            miniTemplator.setVariable("dynamicFieldName", dynamicFieldName);
            miniTemplator.setVariable("dynamicFieldGetter", dynamicFieldGetter);
            String sizeDynamicString = ProtoBufUtil.getSizeDynamicString(protobufField);
            miniTemplator.setVariable("sizeDynamicString", sizeDynamicString);
            miniTemplator.setVariable("encodeWriteFieldValue", ProtoBufUtil.getWriteByteDynamicString(protobufField));
            miniTemplator.addBlock("encodeFields");
        }
    }

    private static void processDecodeBlock() {
        // 初始化 list、map、enum
        StringBuilder initListMapFields = new StringBuilder();
        for (ProtobufField protobufField : protobufFields) {
            boolean isList = protobufField.isList();
            boolean isMap = protobufField.isMap();
            String express = "";
            if (isList) {
                if (ProtobufField.isListType(protobufField.getJavaField())) {
                    express = "new ArrayList()";
                } else if (ProtobufField.isSetType(protobufField.getJavaField())) {
                    express = "new HashSet()";
                }
            } else if (isMap) {
                express = "new HashMap()";
            }
            if (isList || isMap) {
                initListMapFields.append(ProtoBufUtil.getInitListMapFieldDynamicString(protobufField, express));
            }

            if (protobufField.getProtobufFieldType() == ProtobufFieldTypeEnum.ENUM) {
                String clsName = protobufField.getJavaField().getType().getCanonicalName();
                if (!isList) {
                    express = "ProtoBufUtil.getEnumValue(" + clsName + ".class, " + clsName + ".values()[0].name())";
                    // add set get method
                    String setToField = ProtoBufUtil.getSetFieldDynamicString(protobufField, clazz, express);
                    miniTemplator.setVariable("enumInitialize", setToField);
                    miniTemplator.addBlock("enumFields");
                }
            }
        }
        miniTemplator.setVariable("initListMapFields", initListMapFields.toString());

        //处理字段赋值
        StringBuilder code = new StringBuilder();
        // 处理field解析
        for (ProtobufField protobufField : protobufFields) {
            boolean isList = protobufField.isList();
            String t = protobufField.getProtobufFieldType().getType();
            t = ProtoBufUtil.capitalize(t);

            boolean listTypeCheck = false;
            String express;
            String objectDecodeExpress = "";
            String objectDecodeExpressSuffix = "";

            String decodeOrder = "-1";
            if (protobufField.getProtobufFieldType() != ProtobufFieldTypeEnum.DEFAULT) {
                decodeOrder = ProtoBufUtil.makeTag(protobufField.getOrder(),
                        protobufField.getProtobufFieldType().getInternalFieldType().getWireType()) + "";
            } else {
                decodeOrder = "ProtoBufUtil.makeTag(" + protobufField.getOrder() + ",WireFormat."
                        + protobufField.getProtobufFieldType().getWireFormat() + ")";
            }
            miniTemplator.setVariable("decodeOrder", decodeOrder);

            // enumeration type
            if (protobufField.getProtobufFieldType() == ProtobufFieldTypeEnum.ENUM) {
                String clsName = protobufField.getJavaField().getType().getCanonicalName();
                if (isList) {
                    if (protobufField.getGenericKeyType() != null) {
                        Class cls = protobufField.getGenericKeyType();
                        clsName = cls.getCanonicalName();
                    }
                }
                express = "ProtoBufUtil.getEnumValue(" + clsName + ".class, ProtoBufUtil.getEnumName(" + clsName
                        + ".values()," + "input.read" + t + "()))";
            } else {
                // here is the trick way to process BigDecimal and BigInteger
                if (protobufField.getProtobufFieldType() == ProtobufFieldTypeEnum.BIGDECIMAL || protobufField.getProtobufFieldType() == ProtobufFieldTypeEnum.BIGINTEGER) {
                    express = "new " + protobufField.getProtobufFieldType().getJavaType() + "(input.read" + t + "())";
                } else {
                    express = "input.read" + t + "()";
                }

            }

            // if List type and element is object message type
            if (isList && protobufField.getProtobufFieldType() == ProtobufFieldTypeEnum.OBJECT) {
                if (protobufField.getGenericKeyType() != null) {
                    Class cls = protobufField.getGenericKeyType();

                    checkObjectType(protobufField, cls);

                    code.append("codec = ProtobufProxy.create(").append(cls.getCanonicalName()).append(".class");
                    code.append(")").append(ProtoBufUtil.JAVA_LINE_BREAK);
                    objectDecodeExpress = code.toString();
                    code.setLength(0);

                    objectDecodeExpress += "int length = input.readRawVarint32()" + ProtoBufUtil.JAVA_LINE_BREAK;
                    objectDecodeExpress += "final int oldLimit = input.pushLimit(length)" + ProtoBufUtil.JAVA_LINE_BREAK;
                    listTypeCheck = true;
                    express = "(" + cls.getCanonicalName() + ") codec.readFrom(input)";

                }
            } else if (protobufField.isMap()) {

                String getMapCommand = getMapCommand(protobufField);

                if (protobufField.isEnumKeyType()) {
                    String enumClassName = protobufField.getGenericKeyType().getCanonicalName();
                    code.append("EnumHandler<").append(enumClassName).append("> keyhandler");
                    code.append("= new EnumHandler");
                    code.append("<").append(enumClassName).append(">() {");
                    code.append(ProtoBufUtil.LINE_BREAK);
                    code.append("public ").append(enumClassName).append(" handle(int value) {");
                    code.append(ProtoBufUtil.LINE_BREAK);
                    code.append("String enumName = ProtoBufUtil.getEnumName(").append(enumClassName)
                            .append(".values(), value)");
                    code.append(ProtoBufUtil.JAVA_LINE_BREAK);
                    code.append("return ").append(enumClassName).append(".valueOf(enumName)");
                    code.append(ProtoBufUtil.JAVA_LINE_BREAK);
                    code.append("}}");
                    code.append(ProtoBufUtil.JAVA_LINE_BREAK);
                }

                if (protobufField.isEnumValueType()) {
                    String enumClassName = protobufField.getGenericValueType().getCanonicalName();
                    code.append("EnumHandler<").append(enumClassName).append("> handler");
                    code.append("= new EnumHandler");
                    code.append("<").append(enumClassName).append(">() {");
                    code.append(ProtoBufUtil.LINE_BREAK);
                    code.append("public ").append(enumClassName).append(" handle(int value) {");
                    code.append(ProtoBufUtil.LINE_BREAK);
                    code.append("String enumName = ProtoBufUtil.getEnumName(").append(enumClassName)
                            .append(".values(), value)");
                    code.append(ProtoBufUtil.JAVA_LINE_BREAK);
                    code.append("return ").append(enumClassName).append(".valueOf(enumName)");
                    code.append(ProtoBufUtil.JAVA_LINE_BREAK);
                    code.append("}}");
                    code.append(ProtoBufUtil.JAVA_LINE_BREAK);
                }

                objectDecodeExpress = code.toString();
                code.setLength(0);

                express = "ProtoBufUtil.putMapValue(input, " + getMapCommand + ",";
                express += ProtoBufUtil.getMapFieldGenericParameterString(protobufField);
                if (protobufField.isEnumKeyType()) {
                    express += ", keyhandler";
                } else {
                    express += ", null";
                }
                if (protobufField.isEnumValueType()) {
                    express += ", handler";
                } else {
                    express += ", null";
                }
                express += ")";

            } else if (protobufField.getProtobufFieldType() == ProtobufFieldTypeEnum.OBJECT) { // if object
                // message
                // type
                Class cls = protobufField.getJavaField().getType();
                checkObjectType(protobufField, cls);
                String name = cls.getCanonicalName(); // need
                // to
                // parse
                // nested
                // class
                code.append("codec = ProtobufProxy.create(").append(name).append(".class");

                code.append(")").append(ProtoBufUtil.JAVA_LINE_BREAK);
                objectDecodeExpress = code.toString();
                code.setLength(0);

                objectDecodeExpress += "int length = input.readRawVarint32()" + ProtoBufUtil.JAVA_LINE_BREAK;
                objectDecodeExpress += "final int oldLimit = input.pushLimit(length)" + ProtoBufUtil.JAVA_LINE_BREAK;

                listTypeCheck = true;
                express = "(" + name + ") codec.readFrom(input)";
            }

            if (protobufField.getProtobufFieldType() == ProtobufFieldTypeEnum.BYTES) {
                express += ".toByteArray()";
            }

            String decodeFieldSetValue = ProtoBufUtil.getSetFieldDynamicString(protobufField, clazz, express) + ProtoBufUtil.JAVA_LINE_BREAK;

            if (listTypeCheck) {
                objectDecodeExpressSuffix += "input.checkLastTagWas(0)" + ProtoBufUtil.JAVA_LINE_BREAK;
                objectDecodeExpressSuffix += "input.popLimit(oldLimit)" + ProtoBufUtil.JAVA_LINE_BREAK;
            }

            String objectPackedDecodeExpress = "";
            // read packed type
            if (isList) {
                ProtobufFieldTypeEnum protobufFieldType = protobufField.getProtobufFieldType();
                if (protobufFieldType.isPrimitive() || protobufFieldType.isEnum()) {
                    code.append("if (tag == ")
                            .append(ProtoBufUtil.makeTag(protobufField.getOrder(), WireFormat.WIRETYPE_LENGTH_DELIMITED));
                    code.append(") {").append(ProtoBufUtil.LINE_BREAK);

                    code.append("int length = input.readRawVarint32()").append(ProtoBufUtil.JAVA_LINE_BREAK);
                    code.append("int limit = input.pushLimit(length)").append(ProtoBufUtil.JAVA_LINE_BREAK);

                    code.append(ProtoBufUtil.getSetFieldDynamicString(protobufField, clazz, express));

                    code.append("input.popLimit(limit)").append(ProtoBufUtil.JAVA_LINE_BREAK);

                    code.append("continue").append(ProtoBufUtil.JAVA_LINE_BREAK);
                    code.append("}").append(ProtoBufUtil.LINE_BREAK);

                    objectPackedDecodeExpress = code.toString();
                }
            }
            miniTemplator.setVariable("objectPackedDecodeExpress", objectPackedDecodeExpress);
            miniTemplator.setVariable("objectDecodeExpress", objectDecodeExpress);
            miniTemplator.setVariable("objectDecodeExpressSuffix", objectDecodeExpressSuffix);
            miniTemplator.setVariable("decodeFieldSetValue", decodeFieldSetValue);
            miniTemplator.addBlock("decodeFields");
        }

    }

    private static void checkObjectType(ProtobufField protobufField, Class cls) {
        if (ProtobufField.isPrimitiveType(cls)) {
            throw new RuntimeException("invalid generic type for List as Object type, current type is '" + cls.getName()
                    + "'  on field name '" + protobufField.getJavaField().getDeclaringClass().getName() + "#"
                    + protobufField.getJavaField().getName());
        }
    }

    private static String getMapCommand(ProtobufField protobufField) {
        String keyGeneric;
        keyGeneric = protobufField.getGenericKeyType().getCanonicalName();

        String valueGeneric;
        valueGeneric = protobufField.getGenericValueType().getCanonicalName();
        String getMapCommand = "(Map<" + keyGeneric;
        getMapCommand = getMapCommand + ", " + valueGeneric + ">)";
        getMapCommand = getMapCommand + ProtoBufUtil.getGetterDynamicString(protobufField, clazz);
        return getMapCommand;
    }

    private static void loadProtobufField() {
        protobufFields = ProtoBufUtil.getProtobufFieldList(clazz,
                clazz.getAnnotation(ProtobufEnableZigZap.class) != null
        );
    }
}
