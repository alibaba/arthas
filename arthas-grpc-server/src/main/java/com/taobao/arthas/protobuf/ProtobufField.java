package com.taobao.arthas.protobuf;/**
 * @author: 風楪
 * @date: 2024/7/25 上午12:14
 */

import com.google.protobuf.WireFormat;

import java.lang.reflect.Field;

/**
 * @author: FengYe
 * @date: 2024/7/25 上午12:14
 * @description: ProtobufField
 */
public class ProtobufField {

    private int order;

    private Field javaField;

    private ProtobufFieldType protobufFieldType;
}
