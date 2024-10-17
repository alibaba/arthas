package com.taobao.arthas.grpc.server.protobuf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:19
 * @description: ProtobufClass 用于标识 protobuf class
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface ProtobufClass {
}
