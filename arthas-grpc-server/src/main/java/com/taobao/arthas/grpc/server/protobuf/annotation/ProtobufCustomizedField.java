package com.taobao.arthas.grpc.server.protobuf.annotation;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:21
 */

import com.taobao.arthas.grpc.server.protobuf.ProtobufFieldTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:21
 * @description: ProtobufField 用于自定义标识字段
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufCustomizedField {
    int order() default 0;
    ProtobufFieldTypeEnum protoBufFieldType() default ProtobufFieldTypeEnum.DEFAULT;
}
