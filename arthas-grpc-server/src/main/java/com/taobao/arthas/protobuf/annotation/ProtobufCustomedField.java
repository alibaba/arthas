package com.taobao.arthas.protobuf.annotation;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:21
 */

import com.baidu.bjf.remoting.protobuf.FieldType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:21
 * @description: ProtobufField 用于自定义标识字段；当类上添加 ProtobufClass 时，该注解不生效
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufCustomedField {
    int order() default 0;
    FieldType fieldType() default FieldType.DEFAULT;
}
