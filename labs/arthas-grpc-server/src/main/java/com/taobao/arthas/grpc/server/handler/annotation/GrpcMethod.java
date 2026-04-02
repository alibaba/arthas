package com.taobao.arthas.grpc.server.handler.annotation;

import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * gRPC方法注解
 *
 * <p>用于标注gRPC服务类中的方法，提供gRPC方法的元数据信息。
 * 该注解是gRPC服务注册的核心，GrpcDispatcher通过扫描此注解来发现和注册gRPC方法。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>标识方法为gRPC服务方法</li>
 *   <li>指定方法的调用名称（可以与实际方法名不同）</li>
 *   <li>声明方法支持的gRPC调用类型</li>
 *   <li>标记方法是否为流式方法（已废弃，使用grpcType替代）</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @GrpcService("ArthasService")
 * public class ArthasServiceImpl {
 *
 *     // 一元调用示例
 *     @GrpcMethod(value = "executeCommand", grpcType = GrpcInvokeTypeEnum.UNARY)
 *     public CommandResponse execute(CommandRequest request) {
 *         // 处理逻辑
 *     }
 *
 *     // 客户端流调用示例
 *     @GrpcMethod(value = "executeCommands", grpcType = GrpcInvokeTypeEnum.CLIENT_STREAM)
 *     public StreamObserver<CommandRequest> executeCommands(StreamObserver<CommandResponse> responseObserver) {
 *         // 处理逻辑
 *     }
 *
 *     // 服务端流调用示例
 *     @GrpcMethod(value = "watchCommand", grpcType = GrpcInvokeTypeEnum.SERVER_STREAM)
 *     public void watchCommand(CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
 *         // 处理逻辑
 *     }
 *
 *     // 双向流调用示例
 *     @GrpcMethod(value = "interactiveCommand", grpcType = GrpcInvokeTypeEnum.BI_STREAM)
 *     public StreamObserver<CommandRequest> interactiveCommand(StreamObserver<CommandResponse> responseObserver) {
 *         // 处理逻辑
 *     }
 * }
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>此注解只能用于方法上</li>
 *   <li>注解会在运行时保留，通过反射读取</li>
 *   <li>使用此注解的类必须同时带有@GrpcService注解</li>
 *   <li>方法签名必须符合对应gRPC类型的要求</li>
 * </ul>
 *
 * @author: FengYe
 * @date: 2024/9/6 01:57
 * @description: GrpcMethod
 */
@Target({ElementType.METHOD})  // 指定此注解只能用于方法上
@Retention(RetentionPolicy.RUNTIME)  // 注解在运行时保留，可以通过反射读取
public @interface GrpcMethod {

    /**
     * 方法名称
     *
     * <p>指定gRPC方法的调用名称，这个名称会被用于生成gRPC方法的唯一键。</p>
     *
     * <p>使用场景：</p>
     * <ul>
     *   <li>可以指定一个与Java方法名不同的gRPC方法名</li>
     *   <li>如果为空字符串，则使用Java方法名作为gRPC方法名</li>
     *   <li>最终生成的键格式为：服务名.方法名</li>
     * </ul>
     *
     * <p>示例：</p>
     * <pre>{@code
     * // Java方法名为executeCommand，gRPC方法名为execute
     * @GrpcMethod(value = "execute")
     * public CommandResponse executeCommand(CommandRequest request) { ... }
     *
     * // 使用默认值，gRPC方法名也是executeCommand
     * @GrpcMethod
     * public CommandResponse executeCommand(CommandRequest request) { ... }
     * }</pre>
     *
     * @return gRPC方法的名称，默认为空字符串（表示使用Java方法名）
     */
    String value() default "";

    /**
     * 是否为流式方法（已废弃）
     *
     * <p>此字段已废弃，请使用grpcType()来明确指定gRPC调用类型。</p>
     *
     * <p>保留此字段是为了向后兼容，旧代码可能使用了此字段。
     * 新代码应该直接使用grpcType()来指定更精确的调用类型。</p>
     *
     * <p>流式方法的分类：</p>
     * <ul>
     *   <li>非流式（false）：一元调用（UNARY）</li>
     *   <li>流式（true）：可能是客户端流、服务端流或双向流</li>
     * </ul>
     *
     * @deprecated 使用 {@link #grpcType()} 替代，它可以更精确地指定调用类型
     * @return 是否为流式方法，默认为false（非流式）
     */
    @Deprecated
    boolean stream() default false;

    /**
     * gRPC调用类型
     *
     * <p>明确指定该方法的gRPC调用类型，这是最重要的属性。
     * 不同类型的调用决定了方法的签名和处理方式。</p>
     *
     * <p>支持的类型：</p>
     * <ul>
     *   <li><b>UNARY</b> - 一元调用（默认值）
     *     <ul>
     *       <li>客户端发送一个请求，服务端返回一个响应</li>
     *       <li>方法签名：Response method(Request request)</li>
     *       <li>示例：简单的命令执行</li>
     *     </ul>
     *   </li>
     *   <li><b>CLIENT_STREAM</b> - 客户端流调用
     *     <ul>
     *       <li>客户端发送多个请求，服务端返回一个响应</li>
     *       <li>方法签名：StreamObserver<Request> method(StreamObserver<Response> responseObserver)</li>
     *       <li>示例：批量命令执行</li>
     *     </ul>
     *   </li>
     *   <li><b>SERVER_STREAM</b> - 服务端流调用
     *     <ul>
     *       <li>客户端发送一个请求，服务端返回多个响应</li>
     *       <li>方法签名：void method(Request request, StreamObserver<Response> responseObserver)</li>
     *       <li>示例：命令输出流式返回</li>
     *     </ul>
     *   </li>
     *   <li><b>BI_STREAM</b> - 双向流调用
     *     <ul>
     *       <li>客户端和服务端可以双向发送多个消息</li>
     *       <li>方法签名：StreamObserver<Request> method(StreamObserver<Response> responseObserver)</li>
     *       <li>示例：交互式命令会话</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>方法签名示例：</p>
     * <pre>{@code
     * // UNARY - 一元调用
     * @GrpcMethod(value = "execute", grpcType = GrpcInvokeTypeEnum.UNARY)
     * public CommandResponse execute(CommandRequest request) {
     *     // 处理单个请求，返回单个响应
     * }
     *
     * // CLIENT_STREAM - 客户端流
     * @GrpcMethod(value = "batchExecute", grpcType = GrpcInvokeTypeEnum.CLIENT_STREAM)
     * public StreamObserver<CommandRequest> batchExecute(StreamObserver<CommandResponse> responseObserver) {
     *     // 返回一个观察者，接收多个请求，最后返回一个响应
     * }
     *
     * // SERVER_STREAM - 服务端流
     * @GrpcMethod(value = "tailLog", grpcType = GrpcInvokeTypeEnum.SERVER_STREAM)
     * public void tailLog(LogRequest request, StreamObserver<LogResponse> responseObserver) {
     *     // 接收一个请求，通过responseObserver返回多个响应
     * }
     *
     * // BI_STREAM - 双向流
     * @GrpcMethod(value = "interactiveSession", grpcType = GrpcInvokeTypeEnum.BI_STREAM)
     * public StreamObserver<CommandRequest> interactiveSession(StreamObserver<CommandResponse> responseObserver) {
     *     // 双向流式通信，可以接收多个请求并返回多个响应
     * }
     * }</pre>
     *
     * @return gRPC调用类型枚举，默认为UNARY（一元调用）
     */
    GrpcInvokeTypeEnum grpcType() default GrpcInvokeTypeEnum.UNARY;
}
