package com.taobao.arthas.core.advisor;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;
import static java.lang.System.arraycopy;

import java.arthas.SpyAPI;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.asm.location.LocationType;
import com.alibaba.bytekit.asm.location.MethodInsnNodeWare;
import com.alibaba.bytekit.asm.location.filter.GroupLocationFilter;
import com.alibaba.bytekit.asm.location.filter.InvokeCheckLocationFilter;
import com.alibaba.bytekit.asm.location.filter.InvokeContainLocationFilter;
import com.alibaba.bytekit.asm.location.filter.LocationFilter;
import com.alibaba.bytekit.utils.AsmOpUtils;
import com.alibaba.bytekit.utils.AsmUtils;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyInterceptor1;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyInterceptor2;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyInterceptor3;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyTraceExcludeJDKInterceptor1;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyTraceExcludeJDKInterceptor2;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyTraceExcludeJDKInterceptor3;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyTraceInterceptor1;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyTraceInterceptor2;
import com.taobao.arthas.core.advisor.SpyInterceptors.SpyTraceInterceptor3;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.util.ArthasCheckUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;

/**
 * 类增强器
 * 负责对Java类进行字节码增强，在方法调用前后插入监控代码
 * 实现了ClassFileTransformer接口，在类加载或重转换时进行增强
 *
 * Created by vlinux on 15/5/17.
 * @author hengyunabc
 */
public class Enhancer implements ClassFileTransformer {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(Enhancer.class);

    // 通知监听器，用于接收增强后的回调
    private final AdviceListener listener;
    // 是否启用追踪模式（trace命令）
    private final boolean isTracing;
    // 是否跳过JDK内部方法的追踪
    private final boolean skipJDKTrace;
    // 类名匹配器
    private final Matcher classNameMatcher;
    // 类名排除匹配器
    private final Matcher classNameExcludeMatcher;
    // 方法名匹配器
    private final Matcher methodNameMatcher;
    /**
     * 指定增强的 classloader hash，如果为空则不限制
     * 用于限制只对特定ClassLoader加载的类进行增强
     */
    private final String targetClassLoaderHash;
    // 增强影响范围统计对象
    private final EnhancerAffect affect;
    // 匹配的类集合
    private Set<Class<?>> matchingClasses = null;
    // 是否启用懒加载模式（在类首次加载时进行增强）
    private boolean isLazy = false;
    // Enhancer自身的类加载器
    private static final ClassLoader selfClassLoader = Enhancer.class.getClassLoader();

    // 被增强的类的缓存，使用WeakHashMap避免内存泄漏
    private final static Map<Class<?>/* Class */, Object> classBytesCache = new WeakHashMap<Class<?>, Object>();
    // SpyAPI的实现类，用于在增强代码中调用
    private static SpyImpl spyImpl = new SpyImpl();

    static {
        // 设置SpyAPI的实现，这是静态初始化块
        SpyAPI.setSpy(spyImpl);
    }

    /**
     * 构造函数（非懒加载模式）
     *
     * @param listener            通知监听器
     * @param isTracing           是否启用追踪模式
     * @param skipJDKTrace        是否跳过JDK内部方法的追踪
     * @param classNameMatcher    类名匹配器
     * @param classNameExcludeMatcher 类名排除匹配器
     * @param methodNameMatcher   方法名匹配器
     */
    public Enhancer(AdviceListener listener, boolean isTracing, boolean skipJDKTrace, Matcher classNameMatcher,
            Matcher classNameExcludeMatcher,
            Matcher methodNameMatcher) {
        this(listener, isTracing, skipJDKTrace, classNameMatcher, classNameExcludeMatcher, methodNameMatcher, false, null);
    }

    /**
     * 构造函数（懒加载模式，但不指定ClassLoader）
     *
     * @param listener            通知监听器
     * @param isTracing           是否启用追踪模式
     * @param skipJDKTrace        是否跳过JDK内部方法的追踪
     * @param classNameMatcher    类名匹配器
     * @param classNameExcludeMatcher 类名排除匹配器
     * @param methodNameMatcher   方法名匹配器
     * @param isLazy              是否启用懒加载模式
     */
    public Enhancer(AdviceListener listener, boolean isTracing, boolean skipJDKTrace, Matcher classNameMatcher,
            Matcher classNameExcludeMatcher,
            Matcher methodNameMatcher, boolean isLazy) {
        this(listener, isTracing, skipJDKTrace, classNameMatcher, classNameExcludeMatcher, methodNameMatcher, isLazy, null);
    }

    /**
     * 完整构造函数
     *
     * @param listener               通知监听器
     * @param isTracing              是否启用追踪模式
     * @param skipJDKTrace           是否跳过JDK内部方法的追踪
     * @param classNameMatcher       类名匹配器
     * @param classNameExcludeMatcher 类名排除匹配器
     * @param methodNameMatcher      方法名匹配器
     * @param isLazy                 是否启用懒加载模式
     * @param targetClassLoaderHash  目标ClassLoader的hash值
     */
    public Enhancer(AdviceListener listener, boolean isTracing, boolean skipJDKTrace, Matcher classNameMatcher,
            Matcher classNameExcludeMatcher,
            Matcher methodNameMatcher, boolean isLazy, String targetClassLoaderHash) {
        this.listener = listener;
        this.isTracing = isTracing;
        this.skipJDKTrace = skipJDKTrace;
        this.classNameMatcher = classNameMatcher;
        this.classNameExcludeMatcher = classNameExcludeMatcher;
        this.methodNameMatcher = methodNameMatcher;
        this.targetClassLoaderHash = targetClassLoaderHash;
        this.affect = new EnhancerAffect();
        affect.setListenerId(listener.id());
        this.isLazy = isLazy;
    }

    /**
     * 转换类的字节码
     * 这是ClassFileTransformer接口的核心方法，在类加载或重转换时被调用
     *
     * @param inClassLoader        类加载器
     * @param className            类名（内部名称格式，如java/lang/String）
     * @param classBeingRedefined  被重定义的类（如果是重转换操作）
     * @param protectionDomain     保护域
     * @param classfileBuffer      原始类文件字节码
     * @return 增强后的类字节码，如果返回null则表示不进行增强
     * @throws IllegalClassFormatException 类格式异常
     */
    @Override
    public byte[] transform(final ClassLoader inClassLoader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            // 检查classloader能否加载到 SpyAPI，如果不能，则放弃增强
            // 这是因为增强后的代码需要调用SpyAPI，如果类加载器无法访问SpyAPI，则无法进行增强
            try {
                if (inClassLoader != null) {
                    inClassLoader.loadClass(SpyAPI.class.getName());
                }
            } catch (Throwable e) {
                logger.error("the classloader can not load SpyAPI, ignore it. classloader: {}, className: {}",
                        inClassLoader.getClass().getName(), className, e);
                return null;
            }

            // 这里要再次过滤一次，为啥？因为在transform的过程中，有可能还会再诞生新的类
            // 所以需要将之前需要转换的类集合传递下来，再次进行判断
            if (matchingClasses != null && !matchingClasses.contains(classBeingRedefined)) {
                // 懒加载模式：当类首次加载时（classBeingRedefined == null），检查类名是否匹配
                if (isLazy && classBeingRedefined == null && className != null) {
                    // 将 className 从 internal name 转换为 binary name
                    // internal name: java/lang/String, binary name: java.lang.String
                    String classNameDot = className.replace('/', '.');
                    // 检查类名是否匹配
                    if (!classNameMatcher.matching(classNameDot)) {
                        return null;
                    }
                    // 检查是否被排除
                    if (classNameExcludeMatcher != null && classNameExcludeMatcher.matching(classNameDot)) {
                        return null;
                    }
                    // 检查 classloader 是否匹配（指定了 targetClassLoaderHash 时生效）
                    if (!isTargetClassLoader(inClassLoader)) {
                        return null;
                    }
                    // 检查是否是 arthas 自身的类
                    if (inClassLoader != null && isEquals(inClassLoader, selfClassLoader)) {
                        return null;
                    }
                    // 检查是否是 unsafe 类（Bootstrap ClassLoader加载的类）
                    if (!GlobalOptions.isUnsafe && inClassLoader == null) {
                        return null;
                    }
                    logger.info("Lazy mode: enhancing newly loaded class: {}", classNameDot);
                } else {
                    // 非懒加载模式，但不在匹配列表中，直接返回null
                    return null;
                }
            }

            // 保留原始的ClassReader，用于字节码优化，避免JVM元空间OOM
            ClassNode classNode = new ClassNode(Opcodes.ASM9);
            ClassReader classReader = AsmUtils.toClassNode(classfileBuffer, classNode);
            // 移除JSR指令，解决issue #1304
            classNode = AsmUtils.removeJSRInstructions(classNode);

            // 生成增强字节码
            // 创建拦截器类解析器，用于解析增强逻辑
            DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

            // 拦截器处理器列表，包含所有需要插入的增强逻辑
            final List<InterceptorProcessor> interceptorProcessors = new ArrayList<InterceptorProcessor>();

            // 添加基础的三个拦截器（enter/exit/exceptionExit）
            interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyInterceptor1.class));
            interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyInterceptor2.class));
            interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyInterceptor3.class));

            // 如果是追踪模式，添加追踪相关的拦截器
            if (this.isTracing) {
                if (!this.skipJDKTrace) {
                    // 不跳过JDK追踪，添加完整的追踪拦截器
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceInterceptor1.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceInterceptor2.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceInterceptor3.class));
                } else {
                    // 跳过JDK追踪，使用排除JDK的追踪拦截器
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceExcludeJDKInterceptor1.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceExcludeJDKInterceptor2.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceExcludeJDKInterceptor3.class));
                }
            }

            // 找出所有需要增强的方法
            List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
            for (MethodNode methodNode : classNode.methods) {
                // 检查方法是否应该被忽略
                if (!isIgnore(methodNode, methodNameMatcher)) {
                    matchedMethods.add(methodNode);
                }
            }

            // 处理CGLIB代理类的构造函数异常表问题
            // 参考issue #1690
            if (AsmUtils.isEnhancerByCGLIB(className)) {
                for (MethodNode methodNode : matchedMethods) {
                    if (AsmUtils.isConstructor(methodNode)) {
                        // 修复构造函数的异常表
                        AsmUtils.fixConstructorExceptionTable(methodNode);
                    }
                }
            }

            // 创建组位置过滤器，用于检查是否已经插入了spy函数
            // 如果已经插入过，则不重复处理，避免重复增强
            GroupLocationFilter groupLocationFilter = new GroupLocationFilter();

            // 创建入口位置过滤器（检查是否已经插入了atEnter调用）
            LocationFilter enterFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atEnter",
                    LocationType.ENTER);
            // 创建退出位置过滤器（检查是否已经插入了atExit调用）
            LocationFilter existFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atExit",
                    LocationType.EXIT);
            // 创建异常退出位置过滤器（检查是否已经插入了atExceptionExit调用）
            LocationFilter exceptionFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atExceptionExit", LocationType.EXCEPTION_EXIT);

            groupLocationFilter.addFilter(enterFilter);
            groupLocationFilter.addFilter(existFilter);
            groupLocationFilter.addFilter(exceptionFilter);

            // 创建方法调用前的位置过滤器（用于trace功能）
            LocationFilter invokeBeforeFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atBeforeInvoke", LocationType.INVOKE);
            // 创建方法调用完成的位置过滤器（用于trace功能）
            LocationFilter invokeAfterFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atInvokeException", LocationType.INVOKE_COMPLETED);
            // 创建方法调用异常的位置过滤器（用于trace功能）
            LocationFilter invokeExceptionFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atInvokeException", LocationType.INVOKE_EXCEPTION_EXIT);
            groupLocationFilter.addFilter(invokeBeforeFilter);
            groupLocationFilter.addFilter(invokeAfterFilter);
            groupLocationFilter.addFilter(invokeExceptionFilter);

            // 遍历所有匹配的方法，进行增强处理
            for (MethodNode methodNode : matchedMethods) {
                // 跳过native方法，无法增强native方法
                if (AsmUtils.isNative(methodNode)) {
                    logger.info("ignore native method: {}",
                            AsmUtils.methodDeclaration(Type.getObjectType(classNode.name), methodNode));
                    continue;
                }
                // 先查找是否有 atBeforeInvoke 函数，如果有，则说明已经有trace了
                // 此时不再尝试增强，直接插入listener到已有的trace点
                if(AsmUtils.containsMethodInsnNode(methodNode, Type.getInternalName(SpyAPI.class), "atBeforeInvoke")) {
                    // 遍历方法的所有指令
                    for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode
                            .getNext()) {
                        if (insnNode instanceof MethodInsnNode) {
                            final MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            // 如果设置了跳过JDK追踪，则忽略java包下的方法调用
                            if(this.skipJDKTrace) {
                                if(methodInsnNode.owner.startsWith("java/")) {
                                    continue;
                                }
                            }
                            // 跳过原始类型的装箱类型相关的方法
                            if(AsmOpUtils.isBoxType(Type.getObjectType(methodInsnNode.owner))) {
                                continue;
                            }
                            // 注册追踪监听器到这个方法调用点
                            AdviceListenerManager.registerTraceAdviceListener(inClassLoader, className,
                                    methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, listener);
                        }
                    }
                }else {
                    // 方法还没有被trace过，需要进行增强
                    MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
                    // 应用所有拦截器
                    for (InterceptorProcessor interceptor : interceptorProcessors) {
                        try {
                            // 处理方法，插入增强代码
                            List<Location> locations = interceptor.process(methodProcessor);
                            for (Location location : locations) {
                                if (location instanceof MethodInsnNodeWare) {
                                    // 如果是方法调用相关的位置，注册追踪监听器
                                    MethodInsnNodeWare methodInsnNodeWare = (MethodInsnNodeWare) location;
                                    MethodInsnNode methodInsnNode = methodInsnNodeWare.methodInsnNode();

                                    AdviceListenerManager.registerTraceAdviceListener(inClassLoader, className,
                                            methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, listener);
                                }
                            }

                        } catch (Throwable e) {
                            logger.error("enhancer error, class: {}, method: {}, interceptor: {}", classNode.name, methodNode.name, interceptor.getClass().getName(), e);
                        }
                    }
                }

                // 注册方法的enter/exit监听器（总是会注册）
                AdviceListenerManager.registerAdviceListener(inClassLoader, className, methodNode.name, methodNode.desc,
                        listener);
                // 统计增强的方法数量
                affect.addMethodAndCount(inClassLoader, className, methodNode.name, methodNode.desc);
            }

            // 处理类版本号问题
            // 参考 issue #1223，Java 5 (V1_5) 的major version是49
            // 如果类版本低于49，需要升级到49，因为增强后的代码需要较新的class版本
            if (AsmUtils.getMajorVersion(classNode.version) < 49) {
                classNode.version = AsmUtils.setMajorVersion(classNode.version, 49);
            }

            // 将ClassNode转换回字节数组
            byte[] enhanceClassByteArray = AsmUtils.toBytes(classNode, inClassLoader, classReader);

            // 增强成功，将类记录到缓存中
            classBytesCache.put(classBeingRedefined, new Object());

            // 如果需要，将增强后的类dump到文件
            dumpClassIfNecessary(className, enhanceClassByteArray, affect);

            // 成功计数：增强的类数量加1
            affect.cCnt(1);

            // 返回增强后的字节码
            return enhanceClassByteArray;
        } catch (Throwable t) {
            // 捕获所有异常，记录日志
            logger.warn("transform loader[{}]:class[{}] failed.", inClassLoader, className, t);
            // 将异常设置到affect对象中
            affect.setThrowable(t);
        }

        // 如果出现异常或不需要增强，返回null
        return null;
    }

    /**
     * 判断方法是否是抽象方法
     *
     * @param access 方法的访问标志
     * @return 如果是抽象方法返回true，否则返回false
     */
    private boolean isAbstract(int access) {
        return (Opcodes.ACC_ABSTRACT & access) == Opcodes.ACC_ABSTRACT;
    }

    /**
     * 判断方法是否应该被忽略（不进行增强）
     *
     * @param methodNode       方法节点
     * @param methodNameMatcher 方法名匹配器
     * @return 如果应该被忽略返回true，否则返回false
     */
    private boolean isIgnore(MethodNode methodNode, Matcher methodNameMatcher) {
        // 满足以下任一条件则忽略：
        // 1. 方法节点为null
        // 2. 方法是抽象方法
        // 3. 方法名不匹配
        // 4. 方法是静态初始化块<clinit>
        return null == methodNode || isAbstract(methodNode.access) || !methodNameMatcher.matching(methodNode.name)
                || ArthasCheckUtils.isEquals(methodNode.name, "<clinit>");
    }

    /**
     * 如果需要，将增强后的类dump到文件
     * 用于调试和查看增强后的字节码
     *
     * @param className 类名
     * @param data      类字节码数据
     * @param affect    影响范围对象
     */
    private static void dumpClassIfNecessary(String className, byte[] data, EnhancerAffect affect) {
        // 检查是否开启了dump选项
        if (!GlobalOptions.isDump) {
            return;
        }
        // 创建dump文件，路径为./arthas-class-dump/类名.class
        final File dumpClassFile = new File("./arthas-class-dump/" + className + ".class");
        final File classPath = new File(dumpClassFile.getParent());

        // 创建类所在的包路径
        if (!classPath.mkdirs() && !classPath.exists()) {
            logger.warn("create dump classpath:{} failed.", classPath);
            return;
        }

        // 将类字节码写入文件
        try {
            FileUtils.writeByteArrayToFile(dumpClassFile, data);
            // 记录dump的文件
            affect.addClassDumpFile(dumpClassFile);
            if (GlobalOptions.verbose) {
                logger.info("dump enhanced class: {}, path: {}", className, dumpClassFile);
            }
        } catch (IOException e) {
            logger.warn("dump class:{} to file {} failed.", className, dumpClassFile, e);
        }

    }

    /**
     * 过滤掉无法被增强的类
     * 返回被过滤的类及其原因
     *
     * @param classes 类集合
     * @return 被过滤的类列表，每个元素包含类对象和过滤原因
     */
    private List<Pair<Class<?>, String>> filter(Set<Class<?>> classes) {
        List<Pair<Class<?>, String>> filteredClasses = new ArrayList<Pair<Class<?>, String>>();
        final Iterator<Class<?>> it = classes.iterator();
        // 遍历所有类，检查是否应该被过滤
        while (it.hasNext()) {
            final Class<?> clazz = it.next();
            boolean removeFlag = false;
            // 检查1：类是否为null
            if (null == clazz) {
                removeFlag = true;
            // 检查2：ClassLoader是否匹配
            } else if (!isTargetClassLoader(clazz.getClassLoader())) {
                filteredClasses.add(new Pair<Class<?>, String>(clazz, "classloader is not matched"));
                removeFlag = true;
            // 检查3：是否是Arthas自身的类
            } else if (isSelf(clazz)) {
                filteredClasses.add(new Pair<Class<?>, String>(clazz, "class loaded by arthas itself"));
                removeFlag = true;
            // 检查4：是否是不安全的类（Bootstrap ClassLoader加载的类）
            } else if (isUnsafeClass(clazz)) {
                filteredClasses.add(new Pair<Class<?>, String>(clazz, "class loaded by Bootstrap Classloader, try to execute `options unsafe true`"));
                removeFlag = true;
            // 检查5：是否在排除列表中
            } else if (isExclude(clazz)) {
                filteredClasses.add(new Pair<Class<?>, String>(clazz, "class is excluded"));
                removeFlag = true;
            } else {
                // 检查6：是否是目前不支持的类类型
                Pair<Boolean, String> unsupportedResult = isUnsupportedClass(clazz);
                if (unsupportedResult.getFirst()) {
                    filteredClasses.add(new Pair<Class<?>, String>(clazz, unsupportedResult.getSecond()));
                    removeFlag = true;
                }
            }
            // 如果需要过滤，从集合中移除
            if (removeFlag) {
                it.remove();
            }
        }
        return filteredClasses;
    }

    /**
     * 判断类是否在排除列表中
     *
     * @param clazz 类对象
     * @return 如果应该被排除返回true，否则返回false
     */
    private boolean isExclude(Class<?> clazz) {
        if (this.classNameExcludeMatcher != null) {
            return classNameExcludeMatcher.matching(clazz.getName());
        }
        return false;
    }

    /**
     * 判断是否是Arthas自身加载的类
     * Arthas自身的类不应该被增强，否则会造成无限递归
     *
     * @param clazz 类对象
     * @return 如果是Arthas自身加载的类返回true，否则返回false
     */
    private static boolean isSelf(Class<?> clazz) {
        return null != clazz && isEquals(clazz.getClassLoader(), selfClassLoader);
    }

    /**
     * 判断是否是不安全的类
     * 不安全的类是指由Bootstrap ClassLoader加载的类
     *
     * @param clazz 类对象
     * @return 如果是不安全的类返回true，否则返回false
     */
    private static boolean isUnsafeClass(Class<?> clazz) {
        return !GlobalOptions.isUnsafe && clazz.getClassLoader() == null;
    }

    /**
     * 判断是否是目前暂不支持的类类型
     * 某些类型的类由于技术限制，暂时无法被增强
     *
     * @param clazz 类对象
     * @return 返回一个Pair，第一个元素表示是否不支持，第二个元素是不支持的原因
     */
    private static Pair<Boolean, String> isUnsupportedClass(Class<?> clazz) {
        // Lambda表达式类不支持
        if (ClassUtils.isLambdaClass(clazz)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is lambda");
        }

        // 接口不支持（除非开启了默认方法支持）
        if (clazz.isInterface() && !GlobalOptions.isSupportDefaultMethod) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is interface");
        }

        // 基本类型的包装类不支持
        if (clazz.equals(Integer.class)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is java.lang.Integer");
        }

        // Class类不支持
        if (clazz.equals(Class.class)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is java.lang.Class");
        }

        // Method类不支持
        if (clazz.equals(Method.class)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is java.lang.Method");
        }

        // 数组不支持
        if (clazz.isArray()) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is array");
        }
        // 支持增强
        return new Pair<Boolean, String>(Boolean.FALSE, "");
    }

    /**
     * 对匹配的类进行增强
     * 这是增强操作的主入口方法
     *
     * @param inst                  Java Instrumentation实例
     * @param maxNumOfMatchedClass  允许增强的最大类数量
     * @return 增强影响范围统计对象
     * @throws UnmodifiableClassException 如果类无法被修改则抛出此异常
     */
    public synchronized EnhancerAffect enhance(final Instrumentation inst, int maxNumOfMatchedClass) throws UnmodifiableClassException {
        // 获取需要增强的类集合
        // 如果禁用了子类匹配，只搜索精确匹配的类
        // 否则搜索所有匹配的类及其子类
        this.matchingClasses = GlobalOptions.isDisableSubClass
                ? SearchUtils.searchClass(inst, classNameMatcher)
                : SearchUtils.searchSubClass(inst, SearchUtils.searchClass(inst, classNameMatcher));

        // 过滤掉无法被增强的类
        List<Pair<Class<?>, String>> filtedList = filter(matchingClasses);
        // 输出被过滤的类及其原因
        if (!filtedList.isEmpty()) {
            for (Pair<Class<?>, String> filted : filtedList) {
                logger.info("ignore class: {}, reason: {}", filted.getFirst().getName(), filted.getSecond());
            }
        }

        // 检查匹配的类数量是否超过限制
        if (matchingClasses.size() > maxNumOfMatchedClass) {
            affect.setOverLimitMsg("The number of matched classes is " +matchingClasses.size()+ ", greater than the limit value " + maxNumOfMatchedClass + ". Try to change the limit with option '-m <arg>'.");
            return affect;
        }

        logger.info("enhance matched classes: {}", matchingClasses);

        // 设置转换器到affect对象
        affect.setTransformer(this);

        try {
            // 将当前Enhancer添加到TransformerManager
            ArthasBootstrap.getInstance().getTransformerManager().addTransformer(this, isTracing);

            // 懒加载模式：同时添加到懒加载 transformer 列表
            // 这样才能在类首次加载时被增强
            if (isLazy) {
                ArthasBootstrap.getInstance().getTransformerManager().addLazyTransformer(this);
                logger.info("Lazy mode enabled, transformer added to lazy transformer list");
            }

            // 执行类转换
            if (GlobalOptions.isBatchReTransform) {
                // 批量增强模式：一次性转换所有类
                final int size = matchingClasses.size();
                final Class<?>[] classArray = new Class<?>[size];
                arraycopy(matchingClasses.toArray(), 0, classArray, 0, size);
                if (classArray.length > 0) {
                    inst.retransformClasses(classArray);
                    logger.info("Success to batch transform classes: " + Arrays.toString(classArray));
                }
            } else {
                // 逐个增强模式：一个一个地转换类
                for (Class<?> clazz : matchingClasses) {
                    try {
                        inst.retransformClasses(clazz);
                        logger.info("Success to transform class: " + clazz);
                    } catch (Throwable t) {
                        logger.warn("retransform {} failed.", clazz, t);
                        // 根据异常类型进行不同的处理
                        if (t instanceof UnmodifiableClassException) {
                            throw (UnmodifiableClassException) t;
                        } else if (t instanceof RuntimeException) {
                            throw (RuntimeException) t;
                        } else {
                            throw new RuntimeException(t);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            // 捕获并记录所有异常
            logger.error("Enhancer error, matchingClasses: {}", matchingClasses, e);
            affect.setThrowable(e);
        }

        return affect;
    }

    /**
     * 判断ClassLoader是否是目标ClassLoader
     * 用于限制只对特定ClassLoader加载的类进行增强
     *
     * @param inClassLoader 类加载器
     * @return 如果是目标ClassLoader返回true，否则返回false
     */
    private boolean isTargetClassLoader(ClassLoader inClassLoader) {
        // 如果没有指定targetClassLoaderHash，则匹配所有ClassLoader
        if (targetClassLoaderHash == null || targetClassLoaderHash.isEmpty()) {
            return true;
        }
        // Bootstrap ClassLoader（null）不匹配
        if (inClassLoader == null) {
            return false;
        }
        // 比较ClassLoader的hash值
        return Integer.toHexString(inClassLoader.hashCode()).equalsIgnoreCase(targetClassLoaderHash);
    }

    /**
     * 重置指定的类
     * 移除类的增强，恢复到原始状态
     *
     * @param inst             Java Instrumentation实例
     * @param classNameMatcher 类名匹配器
     * @return 增强影响范围统计对象
     * @throws UnmodifiableClassException 如果类无法被修改则抛出此异常
     */
    public static synchronized EnhancerAffect reset(final Instrumentation inst, final Matcher classNameMatcher)
            throws UnmodifiableClassException {

        final EnhancerAffect affect = new EnhancerAffect();
        final Set<Class<?>> enhanceClassSet = new HashSet<Class<?>>();

        // 从缓存中找出需要重置的类
        for (Class<?> classInCache : classBytesCache.keySet()) {
            if (classNameMatcher.matching(classInCache.getName())) {
                enhanceClassSet.add(classInCache);
            }
        }

        try {
            // 执行类重转换（移除增强）
            enhance(inst, enhanceClassSet);
            logger.info("Success to reset classes: " + enhanceClassSet);
        } finally {
            // 从缓存中移除已重置的类
            for (Class<?> resetClass : enhanceClassSet) {
                classBytesCache.remove(resetClass);
                affect.cCnt(1);
            }
        }

        return affect;
    }

    /**
     * 批量增强类集合
     * 这是reset方法使用的内部辅助方法
     *
     * @param inst    Java Instrumentation实例
     * @param classes 要增强的类集合
     * @throws UnmodifiableClassException 如果类无法被修改则抛出此异常
     */
    private static void enhance(Instrumentation inst, Set<Class<?>> classes)
            throws UnmodifiableClassException {
        int size = classes.size();
        Class<?>[] classArray = new Class<?>[size];
        // 将Set转换为数组
        arraycopy(classes.toArray(), 0, classArray, 0, size);
        if (classArray.length > 0) {
            // 执行批量重转换
            inst.retransformClasses(classArray);
        }
    }
}
