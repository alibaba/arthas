package com.taobao.arthas.protobuf;/**
 * @author: 風楪
 * @date: 2024/7/17 下午9:57
 */


import com.baidu.bjf.remoting.protobuf.Codec;
import com.taobao.arthas.protobuf.annotation.ProtobufClass;
import com.taobao.arthas.protobuf.utils.MiniTemplator;

import java.io.IOException;
import java.lang.reflect.Field;
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

    private Class<?> clazz;

    private MiniTemplator miniTemplator;

    private List<ProtobufField> protobufFields;

    public ProtobufProxy(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        this.clazz = clazz;
        loadProtobufField();
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

    public ProtobufCodec create(Class<?> clazz) throws Exception {
        String path = Objects.requireNonNull(clazz.getResource(TEMPLATE_FILE)).getPath();
        miniTemplator = new MiniTemplator(path);

        miniTemplator.setVariable("package", "package " + clazz.getPackage().getName() + ";");

        processImportBlock();

        miniTemplator.setVariable("className", clazz.getName() + "$$ProxyClass");
        miniTemplator.setVariable("codecClassName", ProtobufCodec.class.getName());
        miniTemplator.setVariable("targetProxyClassName", clazz.getName());


        return null;
    }

    private void processImportBlock() {
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

    public void processEncodeBlock() {

    }


    private void loadProtobufField() {
        List<Field> fields = new ArrayList<>();
        ProtobufClass annotation = clazz.getAnnotation(ProtobufClass.class);
        if (annotation != null) {

        } else {
            //todo 添加指定字段映射关系
        }
    }

}
