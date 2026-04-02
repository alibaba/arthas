package com.taobao.arthas.grpc.server.handler;


import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import com.taobao.arthas.grpc.server.utils.ReflectUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * gRPC调度器
 *
 * <p>负责gRPC服务的加载、注册和请求分发，是gRPC服务处理的核心组件。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>扫描并加载指定包路径下的所有gRPC服务类</li>
 *   <li>解析服务类中的方法，生成方法句柄(MethodHandle)并缓存</li>
 *   <li>处理Protobuf消息的序列化和反序列化</li>
 *   <li>根据不同的gRPC调用类型(一元调用、客户端流、服务端流、双向流)分发请求到对应的处理器</li>
 * </ul>
 *
 * <p>支持的gRPC调用类型：</p>
 * <ul>
 *   <li>UNARY - 一元调用：客户端发送一个请求，服务端返回一个响应</li>
 *   <li>CLIENT_STREAM - 客户端流：客户端发送多个请求，服务端返回一个响应</li>
 *   <li>SERVER_STREAM - 服务端流：客户端发送一个请求，服务端返回多个响应</li>
 *   <li>BI_STREAM - 双向流：客户端和服务端可以双向发送多个消息</li>
 * </ul>
 *
 * @author: FengYe
 * @date: 2024/9/6 01:12
 * @description: GrpcDelegrate
 */
public class GrpcDispatcher {

    /**
     * 日志记录器
     * 使用MethodHandles.lookup()获取当前类的查找对象，确保日志记录的准确性
     */
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * 默认的gRPC服务包名
     * 当未指定包名时，将扫描此默认包下的所有gRPC服务实现类
     */
    public static final String DEFAULT_GRPC_SERVICE_PACKAGE_NAME = "com.taobao.arthas.grpc.server.service.impl";

    /**
     * gRPC方法调用映射表
     * Key: 服务名.方法名 (例如: "ArthasService.executeCommand")
     * Value: 绑定到服务实例的方法句柄(MethodHandle)，用于快速调用对应的服务方法
     */
    public static Map<String, MethodHandle> grpcInvokeMap = new HashMap<>();

    // 客户端流调用的观察者映射表(当前未使用，保留用于后续扩展)
//    public static Map<String, StreamObserver> clientStreamInvokeMap = new HashMap<>();

    /**
     * 请求对象的parseFrom方法映射表
     * Key: 服务名.方法名
     * Value: 请求类的静态方法parseFrom的方法句柄，用于将字节数组反序列化为请求对象
     * parseFrom是Protobuf生成的标准方法，用于从二进制数据解析消息对象
     */
    public static Map<String, MethodHandle> requestParseFromMap = new HashMap<>();

    /**
     * 请求对象的toByteArray方法映射表
     * Key: 服务名.方法名
     * Value: 请求实例方法的toByteArray的方法句柄，用于将请求对象序列化为字节数组
     * toByteArray是Protobuf生成的标准方法，用于将消息对象序列化为二进制数据
     */
    public static Map<String, MethodHandle> requestToByteArrayMap = new HashMap<>();

    /**
     * 响应对象的parseFrom方法映射表
     * Key: 服务名.方法名
     * Value: 响应类的静态方法parseFrom的方法句柄，用于将字节数组反序列化为响应对象
     */
    public static Map<String, MethodHandle> responseParseFromMap = new HashMap<>();

    /**
     * 响应对象的toByteArray方法映射表
     * Key: 服务名.方法名
     * Value: 响应实例方法的toByteArray的方法句柄，用于将响应对象序列化为字节数组
     */
    public static Map<String, MethodHandle> responseToByteArrayMap = new HashMap<>();

    /**
     * gRPC调用类型映射表
     * Key: 服务名.方法名
     * Value: 该方法对应的gRPC调用类型(UNARY/CLIENT_STREAM/SERVER_STREAM/BI_STREAM)
     * 用于在运行时确定使用哪种方式处理请求
     */
    public static Map<String, GrpcInvokeTypeEnum> grpcInvokeTypeMap = new HashMap<>();

    /**
     * 加载gRPC服务
     *
     * <p>扫描指定包名下的所有类，加载带有@GrpcService注解的服务类，
     * 并解析服务类中带有@GrpcMethod注解的方法，注册到调度器中。</p>
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>使用反射工具扫描指定包下的所有类</li>
     *   <li>过滤出带有@GrpcService注解的类</li>
     *   <li>创建服务实例</li>
     *   <li>解析服务类中的所有方法</li>
     *   <li>对带有@GrpcMethod注解的方法进行注册：
     *     <ul>
     *       <li>生成方法句柄并绑定到服务实例</li>
     *       <li>根据gRPC调用类型解析请求和响应的类型</li>
     *       <li>注册Protobuf消息的序列化和反序列化方法</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param grpcServicePackageName gRPC服务所在的包名，如果为null则使用默认包名
     */
    public void loadGrpcService(String grpcServicePackageName) {
        // 使用反射工具扫描指定包下的所有类，如果未指定包名则使用默认包名
        List<Class<?>> classes = ReflectUtil.findClasses(Optional.ofNullable(grpcServicePackageName).orElse(DEFAULT_GRPC_SERVICE_PACKAGE_NAME));
        // 遍历所有扫描到的类
        for (Class<?> clazz : classes) {
            // 检查类是否带有@GrpcService注解
            if (clazz.isAnnotationPresent(GrpcService.class)) {
                try {
                    // 处理 service
                    // 获取服务类的注解实例
                    GrpcService grpcService = clazz.getAnnotation(GrpcService.class);
                    // 使用无参构造函数创建服务实例
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    // 处理 method
                    // 创建方法句柄查找对象，用于生成高效的方法调用句柄
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    // 获取服务类声明的所有方法（包括私有方法）
                    Method[] declaredMethods = clazz.getDeclaredMethods();
                    // 遍历服务类中的所有方法
                    for (Method method : declaredMethods) {
                        // 检查方法是否带有@GrpcMethod注解
                        if (method.isAnnotationPresent(GrpcMethod.class)) {
                            // 获取方法的@GrpcMethod注解实例
                            GrpcMethod grpcMethod = method.getAnnotation(GrpcMethod.class);
                            // 创建未绑定的方法句柄（方法句柄是Java 7引入的高效方法调用机制）
                            MethodHandle grpcInvoke = lookup.unreflect(method);
                            // 生成gRPC方法的唯一标识键：服务名.方法名
                            String grpcMethodKey = generateGrpcMethodKey(grpcService.value(), grpcMethod.value());
                            // 将gRPC调用类型注册到映射表
                            grpcInvokeTypeMap.put(grpcMethodKey, grpcMethod.grpcType());
                            // 将方法句柄绑定到服务实例并注册到映射表
                            // bindTo(instance)将方法句柄绑定到具体的对象实例，后续调用时无需再传递对象
                            grpcInvokeMap.put(grpcMethodKey, grpcInvoke.bindTo(instance));


                            // 准备解析请求和响应类型的变量
                            Class<?> requestClass = null;
                            Class<?> responseClass = null;
                            // 根据不同的gRPC调用类型，解析请求和响应的Class对象
                            if (GrpcInvokeTypeEnum.UNARY.equals(grpcMethod.grpcType())) {
                                // 一元调用：请求类型是方法句柄的第二个参数（第一个参数是this，已绑定），响应类型是返回值类型
                                requestClass = grpcInvoke.type().parameterType(1);
                                responseClass = grpcInvoke.type().returnType();
                            } else if (GrpcInvokeTypeEnum.CLIENT_STREAM.equals(grpcMethod.grpcType()) || GrpcInvokeTypeEnum.BI_STREAM.equals(grpcMethod.grpcType())) {
                                // 客户端流或双向流：
                                // 请求类型是方法返回值泛型的实际类型（StreamObserver<Request>中的Request）
                                requestClass = getInnerGenericClass(method.getGenericReturnType());
                                // 响应类型是方法第一个参数泛型的实际类型（StreamObserver<Response>中的Response）
                                responseClass = getInnerGenericClass(method.getGenericParameterTypes()[0]);
                            } else if (GrpcInvokeTypeEnum.SERVER_STREAM.equals(grpcMethod.grpcType())) {
                                // 服务端流：
                                // 请求类型是方法第一个参数泛型的实际类型
                                // 响应类型是方法第二个参数泛型的实际类型
                                requestClass = getInnerGenericClass(method.getGenericParameterTypes()[0]);
                                responseClass = getInnerGenericClass(method.getGenericParameterTypes()[1]);
                            }
                            // 查找请求类的静态方法parseFrom，用于从字节数组反序列化请求对象
                            // parseFrom(byte[])是Protobuf生成的标准静态方法
                            MethodHandle requestParseFrom = lookup.findStatic(requestClass, "parseFrom", MethodType.methodType(requestClass, byte[].class));
                            // 查找响应类的静态方法parseFrom，用于从字节数组反序列化响应对象
                            MethodHandle responseParseFrom = lookup.findStatic(responseClass, "parseFrom", MethodType.methodType(responseClass, byte[].class));
                            // 查找请求实例的toByteArray方法，用于将请求对象序列化为字节数组
                            // toByteArray()是Protobuf生成的标准实例方法
                            MethodHandle requestToByteArray = lookup.findVirtual(requestClass, "toByteArray", MethodType.methodType(byte[].class));
                            // 查找响应实例的toByteArray方法，用于将响应对象序列化为字节数组
                            MethodHandle responseToByteArray = lookup.findVirtual(responseClass, "toByteArray", MethodType.methodType(byte[].class));
                            // 将请求的parseFrom方法句柄注册到映射表
                            requestParseFromMap.put(grpcMethodKey, requestParseFrom);
                            // 将响应的parseFrom方法句柄注册到映射表
                            responseParseFromMap.put(grpcMethodKey, responseParseFrom);
                            // 将请求的toByteArray方法句柄注册到映射表
                            requestToByteArrayMap.put(grpcMethodKey, requestToByteArray);
                            // 将响应的toByteArray方法句柄注册到映射表
                            responseToByteArrayMap.put(grpcMethodKey, responseToByteArray);


                            // 以下是原始的switch-case处理逻辑，已注释保留
                            // 该逻辑根据不同的gRPC类型分别处理，但当前实现使用统一的方式处理
//                            switch (grpcMethod.grpcType()) {
//                                case UNARY:
//                                    // 一元调用：直接绑定方法句柄
//                                    unaryInvokeMap.put(grpcMethodKey, grpcInvoke.bindTo(instance));
//                                    return;
//                                case CLIENT_STREAM:
//                                    // 客户端流：调用方法获取StreamObserver并缓存
//                                    Object invoke = grpcInvoke.bindTo(instance).invoke();
//                                    if (!(invoke instanceof StreamObserver)) {
//                                        throw new RuntimeException(grpcMethodKey + " return class is not StreamObserver!");
//                                    }
//                                    clientStreamInvokeMap.put(grpcMethodKey, (StreamObserver) invoke);
//                                    return;
//                                case SERVER_STREAM:
//                                    // 服务端流：待实现
//                                    return;
//                                case BI_STREAM:
//                                    // 双向流：待实现
//                                    return;
//                            }
                        }
                    }
                } catch (Throwable e) {
                    // 记录服务加载过程中的错误，继续处理其他服务
                    logger.error("GrpcDispatcher loadGrpcService error.", e);
                }
            }
        }
    }

    /**
     * 执行一元gRPC调用（旧版本方法）
     *
     * <p>处理简单的一元调用，客户端发送一个请求，服务端返回一个响应。</p>
     *
     * @param service 服务名称
     * @param method 方法名称
     * @param arg 请求参数的字节数组
     * @return gRPC响应对象，包含响应数据和元信息
     * @throws Throwable 方法调用过程中可能抛出的任何异常
     */
    public GrpcResponse doUnaryExecute(String service, String method, byte[] arg) throws Throwable {
        // 获取方法句柄
        MethodHandle methodHandle = grpcInvokeMap.get(generateGrpcMethodKey(service, method));
        // 获取方法句柄的类型信息（用于后续获取返回值类型）
        MethodType type = grpcInvokeMap.get(generateGrpcMethodKey(service, method)).type();
        // 使用parseFrom方法将字节数组反序列化为请求对象
        Object req = requestParseFromMap.get(generateGrpcMethodKey(service, method)).invoke(arg);
        // 调用服务方法，传入请求对象
        Object execute = methodHandle.invoke(req);
        // 创建响应对象
        GrpcResponse grpcResponse = new GrpcResponse();
        // 设置响应的Class类型（用于序列化）
        grpcResponse.setClazz(type.returnType());
        // 设置服务名称
        grpcResponse.setService(service);
        // 设置方法名称
        grpcResponse.setMethod(method);
        // 将执行结果写入响应对象
        grpcResponse.writeResponseData(execute);
        // 返回响应对象
        return grpcResponse;
    }

    /**
     * 执行一元gRPC调用（推荐使用的新版本方法）
     *
     * <p>处理简单的一元调用，客户端发送一个请求，服务端返回一个响应。
     * 相比doUnaryExecute方法，此方法接收封装后的GrpcRequest对象，使用更方便。</p>
     *
     * @param request gRPC请求对象，包含服务名、方法名和请求数据
     * @return gRPC响应对象，包含响应数据和元信息
     * @throws Throwable 方法调用过程中可能抛出的任何异常
     */
    public GrpcResponse unaryExecute(GrpcRequest request) throws Throwable {
        // 从请求对象中获取方法句柄
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        // 获取方法句柄的类型信息
        MethodType type = grpcInvokeMap.get(request.getGrpcMethodKey()).type();
        // 从请求对象中读取字节数组，并使用parseFrom方法反序列化为请求对象
        Object req = requestParseFromMap.get(request.getGrpcMethodKey()).invoke(request.readData());
        // 调用服务方法，传入请求对象
        Object execute = methodHandle.invoke(req);
        // 创建响应对象
        GrpcResponse grpcResponse = new GrpcResponse();
        // 设置响应的Class类型
        grpcResponse.setClazz(type.returnType());
        // 从请求中复制服务名称到响应
        grpcResponse.setService(request.getService());
        // 从请求中复制方法名称到响应
        grpcResponse.setMethod(request.getMethod());
        // 将执行结果写入响应对象
        grpcResponse.writeResponseData(execute);
        // 返回响应对象
        return grpcResponse;
    }

    /**
     * 执行客户端流式gRPC调用
     *
     * <p>客户端流调用场景：客户端可以发送多个请求，服务端返回一个响应。
     * 该方法返回一个StreamObserver，客户端可以通过它发送多个请求消息。</p>
     *
     * @param request gRPC请求对象，包含服务名和方法名
     * @param responseObserver 响应观察者，用于服务端返回响应
     * @return 请求观察者，客户端通过它发送多个请求消息
     * @throws Throwable 方法调用过程中可能抛出的任何异常
     */
    public StreamObserver<GrpcRequest> clientStreamExecute(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) throws Throwable {
        // 获取方法句柄
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        // 调用服务方法，传入响应观察者，返回请求观察者
        // 服务方法通常签名是：StreamObserver<Request> method(StreamObserver<Response> responseObserver)
        return (StreamObserver<GrpcRequest>) methodHandle.invoke(responseObserver);
    }

    /**
     * 执行服务端流式gRPC调用
     *
     * <p>服务端流调用场景：客户端发送一个请求，服务端可以返回多个响应。
     * 该方法会立即返回，服务端通过responseObserver异步发送多个响应消息。</p>
     *
     * @param request gRPC请求对象，包含服务名、方法名和请求数据
     * @param responseObserver 响应观察者，服务端通过它发送多个响应
     * @throws Throwable 方法调用过程中可能抛出的任何异常
     */
    public void serverStreamExecute(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) throws Throwable {
        // 获取方法句柄
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        // 从请求对象中读取字节数组，并反序列化为请求对象
        Object req = requestParseFromMap.get(request.getGrpcMethodKey()).invoke(request.readData());
        // 调用服务方法，传入请求对象和响应观察者
        // 服务方法通常签名是：void method(Request request, StreamObserver<Response> responseObserver)
        methodHandle.invoke(req, responseObserver);
    }

    /**
     * 执行双向流式gRPC调用
     *
     * <p>双向流调用场景：客户端和服务端可以双向发送多个消息。
     * 该方法返回一个StreamObserver，客户端可以通过它发送多个请求消息，
     * 同时服务端也可以通过responseObserver发送多个响应消息。</p>
     *
     * @param request gRPC请求对象，包含服务名和方法名
     * @param responseObserver 响应观察者，服务端通过它发送多个响应
     * @return 请求观察者，客户端通过它发送多个请求消息
     * @throws Throwable 方法调用过程中可能抛出的任何异常
     */
    public StreamObserver<GrpcRequest> biStreamExecute(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) throws Throwable {
        // 获取方法句柄
        MethodHandle methodHandle = grpcInvokeMap.get(request.getGrpcMethodKey());
        // 调用服务方法，传入响应观察者，返回请求观察者
        // 服务方法通常签名是：StreamObserver<Request> method(StreamObserver<Response> responseObserver)
        // 与客户端流类似，但双方都可以持续发送消息
        return (StreamObserver<GrpcRequest>) methodHandle.invoke(responseObserver);
    }

    /**
     * 获取指定服务方法的请求类型
     *
     * <p>根据服务名和方法名，获取该方法的请求参数类型。
     * 按照Protobuf规范，gRPC方法只能有单个请求参数。</p>
     *
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @return 请求参数的Class对象
     * @throws RuntimeException 如果指定的gRPC方法不存在则抛出异常
     */
    public static Class<?> getRequestClass(String serviceName, String methodName) {
        // 获取方法句柄，如果不存在则抛出异常
        // protobuf 规范只能有单入参
        return Optional.ofNullable(grpcInvokeMap.get(generateGrpcMethodKey(serviceName, methodName))).orElseThrow(() -> new RuntimeException("The specified grpc method does not exist")).type().parameterArray()[0];
    }

    /**
     * 生成gRPC方法的唯一键
     *
     * <p>将服务名和方法名组合成一个唯一的键，格式为：服务名.方法名
     * 这个键用于在各个映射表中存储和检索方法相关的信息。</p>
     *
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @return 格式为"serviceName.methodName"的键字符串
     */
    public static String generateGrpcMethodKey(String serviceName, String methodName) {
        // 使用点号连接服务名和方法名，形成唯一标识
        return serviceName + "." + methodName;
    }

    /**
     * 检查并设置gRPC请求的类型
     *
     * <p>根据服务名和方法名，从映射表中查找对应的gRPC调用类型，
     * 并设置到请求对象中。如果找不到，则默认使用UNARY类型。</p>
     *
     * <p>同时将请求标记为流式数据的第一个数据包，用于流式调用的处理。</p>
     *
     * @param request gRPC请求对象，会修改其grpcType和streamFirstData字段
     */
    public static void checkGrpcType(GrpcRequest request) {
        // 从映射表中查找gRPC调用类型，如果不存在则默认为UNARY类型
        request.setGrpcType(
                Optional.ofNullable(grpcInvokeTypeMap.get(generateGrpcMethodKey(request.getService(), request.getMethod())))
                        .orElse(GrpcInvokeTypeEnum.UNARY)
        );
        // 标记这是流式数据的第一个数据包
        request.setStreamFirstData(true);
    }

    /**
     * 获取泛型类型的实际类型参数
     *
     * <p>从泛型类型中提取实际类型参数，用于处理StreamObserver<Request>这样的泛型类型。
     * 例如，对于StreamObserver<String>，此方法会返回String.class。</p>
     *
     * <p>该方法支持递归解析嵌套的泛型类型。</p>
     *
     * @param type 要解析的类型，可以是Class或ParameterizedType
     * @return 泛型的实际类型参数的Class对象，如果无法解析则返回null
     */
    public static Class<?> getInnerGenericClass(Type type) {
        // 如果是普通Class类型，直接返回
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        // 如果是参数化类型（泛型类型）
        if (type instanceof ParameterizedType) {
            // 强制转换为参数化类型
            ParameterizedType paramType = (ParameterizedType) type;
            // 获取泛型的实际类型参数数组
            Type[] actualTypeArguments = paramType.getActualTypeArguments();
            // 如果存在类型参数
            if (actualTypeArguments.length > 0) {
                // 获取第一个实际类型参数（通常这就是我们需要的）
                Type innerType = actualTypeArguments[0]; // 获取第一个实际类型参数
                // 如果内部类型还是参数化类型（嵌套泛型），递归调用获取最内层类型
                if (innerType instanceof ParameterizedType) {
                    return getInnerGenericClass(innerType); // 递归调用获取最内层类型
                // 如果内部类型是Class，直接返回
                } else if (innerType instanceof Class) {
                    return (Class<?>) innerType; // 直接返回 Class 类型
                }
            }
        }
        // 如果没有找到对应的类型，返回null
        return null; // 如果没有找到对应的类型
    }
}
