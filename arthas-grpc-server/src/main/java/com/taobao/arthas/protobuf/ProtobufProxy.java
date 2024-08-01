package com.taobao.arthas.protobuf;/**
 * @author: 風楪
 * @date: 2024/7/17 下午9:57
 */


import com.taobao.arthas.protobuf.annotation.ProtobufEnableZigZap;
import com.taobao.arthas.protobuf.annotation.ProtobufClass;
import com.taobao.arthas.protobuf.utils.FieldUtil;
import com.taobao.arthas.protobuf.utils.MiniTemplator;
import com.taobao.arthas.service.req.ArthasSampleRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: FengYe
 * @date: 2024/7/17 下午9:57
 * @description: ProtoBufProxy
 */
public class ProtobufProxy {
    private static final String TEMPLATE_FILE = "/class_template.tpl";

    private static final Map<String, ProtobufCodec> codecCache = new ConcurrentHashMap<String, ProtobufCodec>();

    private static Class<?> clazz;

    private static MiniTemplator miniTemplator;

    private static List<ProtobufField> protobufFields;

    public ProtobufProxy(Class<?> clazz) {

    }

    public ProtobufCodec getCodecCacheSide(Class<?> clazz) {
        ProtobufCodec codec = codecCache.get(clazz.getName());
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

    public static ProtobufCodec create(Class<?> clazz) throws Exception {
        Objects.requireNonNull(clazz);
        if (clazz.getAnnotation(ProtobufClass.class) == null) {
            throw new IllegalArgumentException("class is not annotated with @ProtobufClass");
        }
        ProtobufProxy.clazz = clazz;
        loadProtobufField();

        String path = Objects.requireNonNull(clazz.getResource(TEMPLATE_FILE)).getPath();
        miniTemplator = new MiniTemplator(path);

        miniTemplator.setVariable("package", "package " + clazz.getPackage().getName() + ";");

        processImportBlock();

        miniTemplator.setVariable("className", clazz.getName() + "$$ProxyClass");
        miniTemplator.setVariable("codecClassName", ProtobufCodec.class.getName());
        miniTemplator.setVariable("targetProxyClassName", clazz.getName());


        return null;
    }

    private static void processImportBlock() {
        Set<String> imports = new HashSet<>();
        imports.add("java.util.*");
        imports.add("java.io.IOException");
        imports.add("java.lang.reflect.*");
//        imports.add("com.baidu.bjf.remoting.protobuf.FieldType"); // fix the class ambiguous of FieldType
//        imports.add("com.baidu.bjf.remoting.protobuf.code.*");
//        imports.add("com.baidu.bjf.remoting.protobuf.utils.*");
//        imports.add("com.baidu.bjf.remoting.protobuf.*");
        imports.add("com.google.protobuf.*");
        imports.add(clazz.getName());
        for (String pkg : imports) {
            miniTemplator.setVariable("importBlock", pkg);
            miniTemplator.addBlock("imports");
        }
    }

    private static void processEncodeBlock() {
        for (ProtobufField protobufField : protobufFields) {
            boolean isList = protobufField.isList();
            boolean isMap = protobufField.isMap();

            ProtobufFieldTypeEnum protobufFieldType = protobufField.getProtobufFieldType();
            String dynamicFieldGetter = FieldUtil.getGetterDynamicString("target", protobufField.getJavaField(), clazz, protobufField.isWildcardType());
            String dynamicFieldType = isList ? "Collection" : protobufField.getProtobufFieldType().getJavaType();
            String dynamicFieldName = FieldUtil.getDynamicFieldName(protobufField.getOrder());

            miniTemplator.setVariable("dynamicFieldType", dynamicFieldType);
            miniTemplator.setVariable("dynamicFieldName", dynamicFieldName);
            miniTemplator.setVariable("dynamicFieldGetter", dynamicFieldGetter);
            String sizeDynamicString = FieldUtil.getSizeDynamicString(protobufField);
            miniTemplator.setVariable("sizeDynamicString", sizeDynamicString);

            //todo

            miniTemplator.addBlock("encodeFields");
        }
    }


    private static void loadProtobufField() {
        protobufFields = FieldUtil.getProtobufFieldList(clazz,
                clazz.getAnnotation(ProtobufEnableZigZap.class) != null
        );
    }

    public static void main(String[] args) {
        List<ProtobufField> protobufFieldList = FieldUtil.getProtobufFieldList(ArthasSampleRequest.class, false);
        for (ProtobufField protobufField : protobufFieldList) {
            String target = FieldUtil.getGetterDynamicString("target", protobufField.getJavaField(), ArthasSampleRequest.class, protobufField.isWildcardType());
            System.out.println(target);
        }
    }

}
