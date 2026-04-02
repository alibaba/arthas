package com.taobao.arthas.grpc.server.handler.executor;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import com.taobao.arthas.grpc.server.utils.ReflectUtil;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * gRPC执行器工厂
 *
 * <p>负责扫描、加载和管理所有gRPC执行器实现类，并根据gRPC调用类型提供对应的执行器实例。
 * 采用工厂模式，将执行器的创建和使用解耦。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>扫描指定包下的所有GrpcExecutor实现类</li>
 *   <li>根据执行器的类型创建实例并注册</li>
 *   <li>根据gRPC调用类型提供对应的执行器</li>
 * </ul>
 *
 * <p>支持的执行器类型：</p>
 * <ul>
 *   <li>继承自AbstractGrpcExecutor的执行器（需要注入GrpcDispatcher）</li>
 *   <li>直接实现GrpcExecutor接口的执行器（无参构造）</li>
 * </ul>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>扫描指定包下的所有类</li>
 *   <li>筛选出GrpcExecutor接口的实现类</li>
 *   <li>根据构造函数类型创建实例：
 *     <ul>
 *       <li>如果有GrpcDispatcher参数的构造函数，注入dispatcher创建实例</li>
 *       <li>如果有无参构造函数，直接创建实例</li>
 *     </ul>
 *   </li>
 *   <li>将执行器实例注册到映射表中（key为支持的gRPC类型）</li>
 *   <li>使用时根据gRPC类型获取对应的执行器</li>
 * </ol>
 *
 * @author: FengYe
 * @date: 2024/10/24 01:56
 * @description: GrpcExecutorFactory
 */
public class GrpcExecutorFactory {

    /**
     * 日志记录器
     * 使用MethodHandles.lookup()获取当前类的查找对象，确保日志记录的准确性
     */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * 默认的gRPC执行器包名
     * 工厂将扫描此包下的所有GrpcExecutor实现类
     */
    public static final String DEFAULT_GRPC_EXECUTOR_PACKAGE_NAME = "com.taobao.arthas.grpc.server.handler.executor";

    /**
     * 执行器映射表
     * Key: gRPC调用类型枚举（UNARY/CLIENT_STREAM/SERVER_STREAM/BI_STREAM）
     * Value: 对应的执行器实例
     *
     * 这个映射表是工厂的核心，根据不同的gRPC调用类型快速找到对应的执行器
     */
    private final Map<GrpcInvokeTypeEnum, GrpcExecutor> map = new HashMap<>();

    /**
     * 加载所有gRPC执行器
     *
     * <p>扫描默认包下的所有类，筛选出GrpcExecutor的实现类，并根据构造函数类型创建实例。</p>
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>使用反射工具扫描默认包下的所有类</li>
     *   <li>筛选出GrpcExecutor接口的实现类</li>
     *   <li>跳过接口类和抽象基类</li>
     *   <li>根据实现类的构造函数类型创建实例：
     *     <ul>
     *       <li>如果是AbstractGrpcExecutor的子类，使用带GrpcDispatcher参数的构造函数</li>
     *       <li>如果是直接实现GrpcExecutor接口的类，使用无参构造函数</li>
     *     </ul>
     *   </li>
     *   <li>将执行器实例注册到映射表中（key为supportGrpcType()返回的类型）</li>
     * </ol>
     *
     * <p>注意事项：</p>
     * <ul>
     *   <li>每个gRPC类型只能有一个执行器实现，如果有多个，后者会覆盖前者</li>
     *   <li>执行器的加载是惰性的，在首次调用getExecutor时才会触发</li>
     *   <li>如果某个执行器加载失败，会记录错误日志但不影响其他执行器的加载</li>
     * </ul>
     *
     * @param dispatcher gRPC调度器实例，会被注入到需要它的执行器中
     *                  执行器通过dispatcher来调用实际的服务方法
     */
    public void loadExecutor(GrpcDispatcher dispatcher) {
        // 扫描默认包下的所有类
        List<Class<?>> classes = ReflectUtil.findClasses(DEFAULT_GRPC_EXECUTOR_PACKAGE_NAME);
        // 遍历所有扫描到的类
        for (Class<?> clazz : classes) {
            // 检查类是否是GrpcExecutor接口的实现
            if (GrpcExecutor.class.isAssignableFrom(clazz)) {
                try {
                    // 跳过抽象基类和接口本身，这些类不应该被实例化
                    if (AbstractGrpcExecutor.class.equals(clazz) || GrpcExecutor.class.equals(clazz)) {
                        continue;
                    }
                    // 处理继承自AbstractGrpcExecutor的执行器
                    // 这类执行器需要注入GrpcDispatcher来调用服务方法
                    if (AbstractGrpcExecutor.class.isAssignableFrom(clazz)) {
                        // 获取带GrpcDispatcher参数的构造函数
                        Constructor<?> constructor = clazz.getConstructor(GrpcDispatcher.class);
                        // 通过构造函数创建实例，并注入dispatcher
                        GrpcExecutor executor = (GrpcExecutor) constructor.newInstance(dispatcher);
                        // 将执行器注册到映射表，key为该执行器支持的gRPC类型
                        map.put(executor.supportGrpcType(), executor);
                    // 处理直接实现GrpcExecutor接口的执行器
                    // 这类执行器通常不需要dispatcher，有自己的处理逻辑
                    } else {
                        // 获取无参构造函数
                        Constructor<?> constructor = clazz.getConstructor();
                        // 通过无参构造函数创建实例
                        GrpcExecutor executor = (GrpcExecutor) constructor.newInstance();
                        // 将执行器注册到映射表
                        map.put(executor.supportGrpcType(), executor);
                    }
                } catch (Exception e) {
                    // 记录加载错误，但继续处理其他执行器
                    // 这样即使某个执行器加载失败，也不会影响整个系统的运行
                    logger.error("GrpcExecutorFactory loadExecutor error", e);
                }
            }
        }
    }

    /**
     * 根据gRPC调用类型获取对应的执行器
     *
     * <p>这是工厂的核心方法，根据请求的gRPC类型返回对应的执行器实例。</p>
     *
     * <p>使用场景：</p>
     * <ul>
     *   <li>当收到gRPC请求时，根据请求中的gRPC类型获取对应的执行器</li>
     *   <li>然后调用执行器的execute方法处理请求</li>
     * </ul>
     *
     * <p>注意事项：</p>
     * <ul>
     *   <li>如果请求的gRPC类型没有对应的执行器，返回null</li>
     *   <li>调用方需要处理返回null的情况</li>
     *   <li>此方法是线程安全的，可以并发调用</li>
     * </ul>
     *
     * @param grpcType gRPC调用类型枚举，指定要获取哪种类型的执行器
     *                 可选值：
     *                 <ul>
     *                   <li>GrpcInvokeTypeEnum.UNARY - 获取一元调用执行器</li>
     *                   <li>GrpcInvokeTypeEnum.CLIENT_STREAM - 获取客户端流执行器</li>
     *                   <li>GrpcInvokeTypeEnum.SERVER_STREAM - 获取服务端流执行器</li>
     *                   <li>GrpcInvokeTypeEnum.BI_STREAM - 获取双向流执行器</li>
     *                 </ul>
     * @return 对应类型的gRPC执行器实例，如果不存在则返回null
     */
    public GrpcExecutor getExecutor(GrpcInvokeTypeEnum grpcType) {
        // 从映射表中获取对应类型的执行器实例
        return map.get(grpcType);
    }
}
