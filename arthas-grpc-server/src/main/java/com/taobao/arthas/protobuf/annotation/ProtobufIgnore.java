package com.taobao.arthas.protobuf.annotation;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:44
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:44
 * @description: ProtobufIgnore
 */
@Target({ ElementType.TYPE,  ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtobufIgnore {
}
