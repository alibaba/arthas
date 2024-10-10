package com.taobao.arthas.grpc.server.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: FengYe
 * @date: 2024/7/30 上午2:01
 * @description: Packed 是否将 List 打包。对于基础类型的 List，打包可以减少无意义的 tag，提高压缩率
 *
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufPacked {
}
