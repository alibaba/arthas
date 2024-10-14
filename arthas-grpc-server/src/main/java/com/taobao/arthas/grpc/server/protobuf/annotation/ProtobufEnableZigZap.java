package com.taobao.arthas.grpc.server.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: FengYe
 * @date: 2024/7/28 下午7:27
 * @description: EnableZigZap 是否启用 zigzap 编码
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface ProtobufEnableZigZap {
}
