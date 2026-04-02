package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2DataFrame;

/**
 * gRPC执行器接口
 *
 * <p>定义了gRPC请求执行的标准契约，所有具体的gRPC执行器都必须实现此接口。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>声明支持的gRPC调用类型</li>
 *   <li>定义执行gRPC请求的标准方法</li>
 * </ul>
 *
 * <p>设计模式：</p>
 * <ul>
 *   <li>策略模式：不同的实现类处理不同类型的gRPC调用（UNARY/CLIENT_STREAM/SERVER_STREAM/BI_STREAM）</li>
 *   <li>工厂模式：通过GrpcExecutorFactory根据调用类型创建对应的执行器实例</li>
 * </ul>
 *
 * <p>实现类包括：</p>
 * <ul>
 *   <li>UnaryGrpcExecutor - 处理一元调用</li>
 *   <li>ClientStreamGrpcExecutor - 处理客户端流调用</li>
 *   <li>ServerStreamGrpcExecutor - 处理服务端流调用</li>
 *   <li>BiStreamGrpcExecutor - 处理双向流调用</li>
 * </ul>
 *
 * @author: FengYe
 * @date: 2024/10/24 01:50
 * @description: GrpcProcessor
 */
public interface GrpcExecutor {

    /**
     * 获取该执行器支持的gRPC调用类型
     *
     * <p>每个执行器实现类只负责处理一种特定类型的gRPC调用。
     * 此方法用于声明该执行器支持哪种类型的调用。</p>
     *
     * <p>支持的调用类型：</p>
     * <ul>
     *   <li>GrpcInvokeTypeEnum.UNARY - 一元调用（一个请求，一个响应）</li>
     *   <li>GrpcInvokeTypeEnum.CLIENT_STREAM - 客户端流（多个请求，一个响应）</li>
     *   <li>GrpcInvokeTypeEnum.SERVER_STREAM - 服务端流（一个请求，多个响应）</li>
     *   <li>GrpcInvokeTypeEnum.BI_STREAM - 双向流（多个请求，多个响应）</li>
     * </ul>
     *
     * @return 该执行器支持的gRPC调用类型枚举值
     */
    GrpcInvokeTypeEnum supportGrpcType();

    /**
     * 执行gRPC请求
     *
     * <p>这是执行器的核心方法，负责处理传入的gRPC请求并产生相应的响应。
     * 具体的处理逻辑由各个实现类根据其支持的调用类型来实现。</p>
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>从GrpcRequest中提取服务名、方法名和请求数据</li>
     *   <li>通过GrpcDispatcher调用对应的服务方法</li>
     *   <li>处理服务方法的返回结果</li>
     *   <li>通过ChannelHandlerContext将响应写回客户端</li>
     * </ol>
     *
     * <p>不同类型的执行器实现差异：</p>
     * <ul>
     *   <li>一元调用：直接调用方法并返回单个响应</li>
     *   <li>客户端流：维护请求流，收集所有请求后返回单个响应</li>
     *   <li>服务端流：调用方法后，通过观察者模式返回多个响应</li>
     *   <li>双向流：同时维护请求流和响应流，支持双向通信</li>
     * </ul>
     *
     * @param request gRPC请求对象，包含：
     *                <ul>
     *                  <li>service - 服务名称</li>
     *                  <li>method - 方法名称</li>
     *                  <li>grpcType - gRPC调用类型</li>
     *                  <li>data - 请求的字节数据</li>
     *                  <li>streamFirstData - 是否是流式数据的第一个包</li>
     *                </ul>
     * @param frame HTTP/2数据帧对象，包含：
     *              <ul>
     *                <li>实际的请求内容数据</li>
     *                <li>流结束标志</li>
     *                <li>流相关的元信息</li>
     *              </ul>
     * @param context Netty通道处理器上下文，用于：
     *                <ul>
     *                  <li>获取当前通道信息</li>
     *                  <li>写入响应数据</li>
     *                  <li>管理流的生命周期</li>
     *                  <li>处理通道事件</li>
     *                </ul>
     * @throws Throwable 执行过程中可能抛出的任何异常，包括：
     *                   <ul>
     *                     <li>服务方法调用异常</li>
     *                     <li>序列化/反序列化异常</li>
     *                     <li>网络IO异常</li>
     *                   </ul>
     */
    void execute(GrpcRequest request, Http2DataFrame frame, ChannelHandlerContext context) throws Throwable;
}
