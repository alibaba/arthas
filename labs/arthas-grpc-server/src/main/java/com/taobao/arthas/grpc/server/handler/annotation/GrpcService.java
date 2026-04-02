package com.taobao.arthas.grpc.server.handler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * gRPC 服务注解
 *
 * <p>该注解用于标记一个类为 gRPC 服务实现类。
 * 被 @GrpcService 注解的类会被 gRPC 服务器扫描并注册为可用的服务。
 *
 * <p>使用示例：
 * <pre>{@code
 * @GrpcService("com.example.ArthasService")
 * public class ArthasServiceImpl {
 *     // 服务实现代码
 * }
 * }</pre>
 *
 * <p>注解属性说明：
 * <ul>
 *   <li>value: 指定服务的全限定名，用于服务路由和识别</li>
 * </ul>
 *
 * @author FengYe
 * @date 2024/9/6 01:57
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcService {
    /**
     * 服务名称
     *
     * <p>指定 gRPC 服务的全限定名，格式通常为：package.ServiceName
     * 例如：com.taobao.arthas.ArthasService
     *
     * <p>该值用于：
     * <ul>
     *   <li>生成 gRPC 方法的唯一键（service.method）</li>
     *   <li>路由请求到正确的服务实现</li>
     *   <li>构建 Proto 文件中定义的 service 名称</li>
 * </ul>
     *
     * @return 服务名称字符串，默认为空字符串
     */
    String value() default "";
}
