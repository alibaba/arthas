package com.taobao.arthas.protobuf;/**
 * @author: 風楪
 * @date: 2024/7/17 下午9:57
 */


import com.taobao.arthas.Main;
import com.taobao.arthas.protobuf.utils.MiniTemplator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: FengYe
 * @date: 2024/7/17 下午9:57
 * @description: ProtoBufProxy
 */
public class ProtobufProxyFactory {
    private static final String TEMPLATE_FILE = "/class_template.tpl";

    private static final Map<String, ProtobufCodec> codecCache = new ConcurrentHashMap<String, ProtobufCodec>();

    public static ProtobufCodec getCodec(Class<?> clazz) throws IOException {
        ProtobufCodec codec = codecCache.get(clazz.getName());
        if (codec != null) {
            return codec;
        }
        return create(clazz);
    }

    public static ProtobufCodec create(Class<?> clazz) throws IOException {
        String path = Objects.requireNonNull(clazz.getResource(TEMPLATE_FILE)).getPath();
        MiniTemplator miniTemplator = new MiniTemplator(path);

        return null;
    }
}
