package com.taobao.arthas.grpc.server.protobuf.utils;/**
 * @author: 風楪
 * @date: 2024/8/6 上午1:19
 */

/**
 * @author: FengYe
 * @date: 2024/8/6 上午1:19
 * @description: EnumHandler 处理 enum 泛型类型
 */
public interface EnumHandler<V> {
    V handle(int value);
}
