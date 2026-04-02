package com.taobao.arthas.grpc.server.handler.executor;

import com.taobao.arthas.grpc.server.handler.GrpcDispatcher;
import com.taobao.arthas.grpc.server.handler.GrpcRequest;
import com.taobao.arthas.grpc.server.handler.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC执行器抽象类
 *
 * <p>这是所有gRPC执行器的基类，提供了gRPC请求处理的通用功能。
 * 不同的gRPC调用模式（一元调用、客户端流、服务端流、双向流）都继承此类实现各自的处理逻辑。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>维护gRPC请求分发器，用于将请求分发到具体的处理方法</li>
 *   <li>管理请求流观察者的映射，支持流式调用场景</li>
 *   <li>为子类提供统一的请求处理框架</li>
 * </ul>
 *
 * @author: FengYe
 * @date: 2024/10/24 02:07
 * @description: AbstractGrpcExecutor - gRPC执行器抽象基类
 */
public abstract class AbstractGrpcExecutor implements GrpcExecutor{

    /**
     * gRPC请求分发器
     *
     * <p>负责将接收到的gRPC请求分发到对应的服务方法进行处理。
     * 分发器根据服务名和方法名找到对应的实现类并调用。</p>
     */
    protected GrpcDispatcher dispatcher;

    /**
     * 请求流观察者映射表
     *
     * <p>用于存储和管理每个流ID对应的请求观察者对象。
     * 在流式调用场景中，一个HTTP/2流会对应一个StreamObserver，
     * 用于接收客户端发送的多个请求消息。</p>
     *
     * <p>Key: 流ID（StreamId），用于标识HTTP/2连接中的单个流</p>
     * <p>Value: 请求流观察者，用于接收和处理流式请求</p>
     *
     * <p>使用ConcurrentHashMap保证并发安全，因为多个线程可能同时处理不同的流。</p>
     */
    protected ConcurrentHashMap<Integer, StreamObserver<GrpcRequest>> requestStreamObserverMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * <p>创建gRPC执行器实例，并初始化请求分发器。</p>
     *
     * @param dispatcher gRPC请求分发器，用于将请求分发到具体的服务方法
     */
    public AbstractGrpcExecutor(GrpcDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
