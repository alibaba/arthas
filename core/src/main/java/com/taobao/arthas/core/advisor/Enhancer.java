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

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtEnter;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtInvoke;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtInvokeException;
import com.taobao.arthas.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.asm.location.LocationType;
import com.taobao.arthas.bytekit.asm.location.MethodInsnNodeWare;
import com.taobao.arthas.bytekit.asm.location.filter.GroupLocationFilter;
import com.taobao.arthas.bytekit.asm.location.filter.InvokeCheckLocationFilter;
import com.taobao.arthas.bytekit.asm.location.filter.InvokeContainLocationFilter;
import com.taobao.arthas.bytekit.asm.location.filter.LocationFilter;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.util.ArthasCheckUtils;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;

/**
 * 对类进行通知增强 Created by vlinux on 15/5/17.
 * @author hengyunabc
 */
public class Enhancer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(Enhancer.class);

    private final AdviceListener listener;
    private final boolean isTracing;
    private final boolean skipJDKTrace;
    private final Matcher classNameMatcher;
    private final Matcher methodNameMatcher;
    private final EnhancerAffect affect;
    private Set<Class<?>> matchingClasses = null;

    // 被增强的类的缓存
    private final static Map<Class<?>/* Class */, Object> classBytesCache = new WeakHashMap<Class<?>, Object>();
    private static SpyImpl spyImpl = new SpyImpl();

    static {
        SpyAPI.setSpy(spyImpl);
    }

    /**
     * @param adviceId          通知编号
     * @param isTracing         可跟踪方法调用
     * @param skipJDKTrace      是否忽略对JDK内部方法的跟踪
     * @param matchingClasses   匹配中的类
     * @param methodNameMatcher 方法名匹配
     * @param affect            影响统计
     */
    public Enhancer(AdviceListener listener, boolean isTracing, boolean skipJDKTrace, Matcher classNameMatcher,
            Matcher methodNameMatcher) {
        this.listener = listener;
        this.isTracing = isTracing;
        this.skipJDKTrace = skipJDKTrace;
        this.classNameMatcher = classNameMatcher;
        this.methodNameMatcher = methodNameMatcher;
        this.affect = new EnhancerAffect();
        affect.setListenerId(listener.id());
    }

    public static class SpyInterceptor {

        @AtEnter(inline = true)
        public static void atEnter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args) {
            SpyAPI.atEnter(clazz, methodInfo, target, args);
        }

        @AtExit(inline = true)
        public static void atExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args, @Binding.Return Object returnObj) {
            SpyAPI.atExit(clazz, methodInfo, target, args, returnObj);
        }

        @AtExceptionExit(inline = true)
        public static void atExceptionExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                @Binding.Throwable Throwable throwable) {
            SpyAPI.atExceptionExit(clazz, methodInfo, target, args, throwable);
        }
    }

    public static class SpyTraceExcludeJDKInterceptor {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = "java.**")
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }

        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = "java.**")
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }

        @AtInvokeException(name = "", inline = true, excludes = "java.**")
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
        }
    }

    public static class SpyTraceInterceptor {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = {"java.arthas.SpyAPI", "java.lang.Byte"
                , "java.lang.Boolean"
                , "java.lang.Short"
                , "java.lang.Character"
                , "java.lang.Integer"
                , "java.lang.Float"
                , "java.lang.Long"
                , "java.lang.Double"})
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }

        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = {"java.arthas.SpyAPI", "java.lang.Byte"
                , "java.lang.Boolean"
                , "java.lang.Short"
                , "java.lang.Character"
                , "java.lang.Integer"
                , "java.lang.Float"
                , "java.lang.Long"
                , "java.lang.Double"})
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }

        @AtInvokeException(name = "", inline = true, excludes = {"java.arthas.SpyAPI", "java.lang.Byte"
                , "java.lang.Boolean"
                , "java.lang.Short"
                , "java.lang.Character"
                , "java.lang.Integer"
                , "java.lang.Float"
                , "java.lang.Long"
                , "java.lang.Double"})
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
        }
    }

    @Override
    public byte[] transform(final ClassLoader inClassLoader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            // 检查classloader能否加载到 SpyAPI，如果不能，则放弃增强
            try {
                if (inClassLoader != null) {
                    inClassLoader.loadClass(SpyAPI.class.getName());
                }
            } catch (Throwable e) {
                logger.error("the classloader can not load SpyAPI, ignore it. classloader: {}, className: {}",
                        inClassLoader.getClass().getName(), className);
                return null;
            }

            // 这里要再次过滤一次，为啥？因为在transform的过程中，有可能还会再诞生新的类
            // 所以需要将之前需要转换的类集合传递下来，再次进行判断
            if (matchingClasses != null && !matchingClasses.contains(classBeingRedefined)) {
                return null;
            }

            ClassNode classNode = AsmUtils.toClassNode(classfileBuffer);

            // 生成增强字节码
            DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

            final List<InterceptorProcessor> interceptorProcessors = new ArrayList<InterceptorProcessor>();

            List<InterceptorProcessor> traceProcessors = defaultInterceptorClassParser.parse(SpyInterceptor.class);
            interceptorProcessors.addAll(traceProcessors);

            if (this.isTracing) {
                Class<?> spyTraceInterceptorClass = SpyTraceExcludeJDKInterceptor.class;
                if (this.skipJDKTrace == false) {
                    spyTraceInterceptorClass = SpyTraceInterceptor.class;
                }
                List<InterceptorProcessor> traceInvokeProcessors = defaultInterceptorClassParser
                        .parse(spyTraceInterceptorClass);
                interceptorProcessors.addAll(traceInvokeProcessors);
            }

            List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
            for (MethodNode methodNode : classNode.methods) {
                if (!isIgnore(methodNode, methodNameMatcher)) {
                    matchedMethods.add(methodNode);
                }
            }

            // 用于检查是否已插入了 spy函数，如果已有则不重复处理
            GroupLocationFilter groupLocationFilter = new GroupLocationFilter();

            LocationFilter enterFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atEnter",
                    LocationType.ENTER);
            LocationFilter existFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atExit",
                    LocationType.EXIT);
            LocationFilter exceptionFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atExceptionExit", LocationType.EXCEPTION_EXIT);

            groupLocationFilter.addFilter(enterFilter);
            groupLocationFilter.addFilter(existFilter);
            groupLocationFilter.addFilter(exceptionFilter);

            LocationFilter invokeBeforeFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atBeforeInvoke", LocationType.INVOKE);
            LocationFilter invokeAfterFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atInvokeException", LocationType.INVOKE_COMPLETED);
            LocationFilter invokeExceptionFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                    "atInvokeException", LocationType.INVOKE_EXCEPTION_EXIT);
            groupLocationFilter.addFilter(invokeBeforeFilter);
            groupLocationFilter.addFilter(invokeAfterFilter);
            groupLocationFilter.addFilter(invokeExceptionFilter);

            for (MethodNode methodNode : matchedMethods) {
                // 先查找是否有 atBeforeInvoke 函数，如果有，则说明已经有trace了，则直接不再尝试增强，直接插入 listener
                if(AsmUtils.containsMethodInsnNode(methodNode, Type.getInternalName(SpyAPI.class), "atBeforeInvoke")) {
                    for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode
                            .getNext()) {
                        if (insnNode instanceof MethodInsnNode) {
                            final MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            if(this.skipJDKTrace) {
                                if(methodInsnNode.owner.startsWith("java/")) {
                                    continue;
                                }
                            }
                            // 原始类型的box类型相关的都跳过
                            if(AsmOpUtils.isBoxType(Type.getObjectType(methodInsnNode.owner))) {
                                continue;
                            }
                            AdviceListenerManager.registerTraceAdviceListener(inClassLoader, className,
                                    methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, listener);
                        }
                    }
                }else {
                    MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
                    for (InterceptorProcessor interceptor : interceptorProcessors) {
                        try {
                            List<Location> locations = interceptor.process(methodProcessor);
                            for (Location location : locations) {
                                if (location instanceof MethodInsnNodeWare) {
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

                // enter/exist 总是要插入 listener
                AdviceListenerManager.registerAdviceListener(inClassLoader, className, methodNode.name, methodNode.desc,
                        listener);
                affect.addMethodAndCount(inClassLoader, className, methodNode.name, methodNode.desc);
            }

            byte[] enhanceClassByteArray = AsmUtils.toBytes(classNode);

            // 增强成功，记录类
            classBytesCache.put(classBeingRedefined, new Object());

            // dump the class
            dumpClassIfNecessary(className, enhanceClassByteArray, affect);

            // 成功计数
            affect.cCnt(1);

            return enhanceClassByteArray;
        } catch (Throwable t) {
            logger.warn("transform loader[{}]:class[{}] failed.", inClassLoader, className, t);
        }

        return null;
    }

    /**
     * 是否抽象属性
     */
    private boolean isAbstract(int access) {
        return (Opcodes.ACC_ABSTRACT & access) == Opcodes.ACC_ABSTRACT;
    }

    /**
     * 是否需要忽略
     */
    private boolean isIgnore(MethodNode methodNode, Matcher methodNameMatcher) {
        return null == methodNode || isAbstract(methodNode.access) || !methodNameMatcher.matching(methodNode.name)
                || ArthasCheckUtils.isEquals(methodNode.name, "<clinit>");
    }

    /**
     * dump class to file
     */
    private static void dumpClassIfNecessary(String className, byte[] data, EnhancerAffect affect) {
        if (!GlobalOptions.isDump) {
            return;
        }
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
            affect.addClassDumpFile(dumpClassFile);
            if (GlobalOptions.verbose) {
                logger.info("dump enhanced class: {}, path: {}", className, dumpClassFile);
            }
        } catch (IOException e) {
            logger.warn("dump class:{} to file {} failed.", className, dumpClassFile, e);
        }

    }

    /**
     * 是否需要过滤的类
     *
     * @param classes 类集合
     */
    private static void filter(Set<Class<?>> classes) {
        final Iterator<Class<?>> it = classes.iterator();
        while (it.hasNext()) {
            final Class<?> clazz = it.next();
            if (null == clazz || isSelf(clazz) || isUnsafeClass(clazz) || isUnsupportedClass(clazz)) {
                it.remove();
            }
        }
    }

    /**
     * 是否过滤Arthas加载的类
     */
    private static boolean isSelf(Class<?> clazz) {
        return null != clazz && isEquals(clazz.getClassLoader(), Enhancer.class.getClassLoader());
    }

    /**
     * 是否过滤unsafe类
     */
    private static boolean isUnsafeClass(Class<?> clazz) {
        return !GlobalOptions.isUnsafe && clazz.getClassLoader() == null;
    }

    /**
     * 是否过滤目前暂不支持的类
     */
    private static boolean isUnsupportedClass(Class<?> clazz) {
        return clazz.isArray() || (clazz.isInterface() && !GlobalOptions.isSupportDefaultMethod) || clazz.isEnum()
                || clazz.equals(Class.class) || clazz.equals(Integer.class) || clazz.equals(Method.class);
    }

    /**
     * 对象增强
     *
     * @param inst              inst
     * @param adviceId          通知ID
     * @param isTracing         可跟踪方法调用
     * @param skipJDKTrace      是否忽略对JDK内部方法的跟踪
     * @param classNameMatcher  类名匹配
     * @param methodNameMatcher 方法名匹配
     * @return 增强影响范围
     * @throws UnmodifiableClassException 增强失败
     */
    public synchronized EnhancerAffect enhance(final Instrumentation inst) throws UnmodifiableClassException {
        // 获取需要增强的类集合
        this.matchingClasses = GlobalOptions.isDisableSubClass
                ? SearchUtils.searchClass(inst, classNameMatcher)
                : SearchUtils.searchSubClass(inst, SearchUtils.searchClass(inst, classNameMatcher));

        // 过滤掉无法被增强的类
        filter(matchingClasses);

        affect.setTransformer(this);

        try {
            ArthasBootstrap.getInstance().getTransformerManager().addTransformer(this, isTracing);
            //inst.addTransformer(enhancer, true);

            // 批量增强
            if (GlobalOptions.isBatchReTransform) {
                final int size = matchingClasses.size();
                final Class<?>[] classArray = new Class<?>[size];
                arraycopy(matchingClasses.toArray(), 0, classArray, 0, size);
                if (classArray.length > 0) {
                    inst.retransformClasses(classArray);
                    logger.info("Success to batch transform classes: " + Arrays.toString(classArray));
                }
            } else {
                // for each 增强
                for (Class<?> clazz : matchingClasses) {
                    try {
                        inst.retransformClasses(clazz);
                        logger.info("Success to transform class: " + clazz);
                    } catch (Throwable t) {
                        logger.warn("retransform {} failed.", clazz, t);
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
        } finally {
            //inst.removeTransformer(enhancer);
        }

        return affect;
    }

    /**
     * 重置指定的Class
     *
     * @param inst             inst
     * @param classNameMatcher 类名匹配
     * @return 增强影响范围
     * @throws UnmodifiableClassException
     */
    public static synchronized EnhancerAffect reset(final Instrumentation inst, final Matcher classNameMatcher)
            throws UnmodifiableClassException {

        final EnhancerAffect affect = new EnhancerAffect();
        final Set<Class<?>> enhanceClassSet = new HashSet<Class<?>>();

        for (Class<?> classInCache : classBytesCache.keySet()) {
            if (classNameMatcher.matching(classInCache.getName())) {
                enhanceClassSet.add(classInCache);
            }
        }

        final ClassFileTransformer resetClassFileTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                return null;
            }
        };

        try {
            enhance(inst, resetClassFileTransformer, enhanceClassSet);
            logger.info("Success to reset classes: " + enhanceClassSet);
        } finally {
            for (Class<?> resetClass : enhanceClassSet) {
                classBytesCache.remove(resetClass);
                affect.cCnt(1);
            }
        }

        return affect;
    }

    // 批量增强
    private static void enhance(Instrumentation inst, ClassFileTransformer transformer, Set<Class<?>> classes)
            throws UnmodifiableClassException {
        try {
            inst.addTransformer(transformer, true);
            int size = classes.size();
            Class<?>[] classArray = new Class<?>[size];
            arraycopy(classes.toArray(), 0, classArray, 0, size);
            if (classArray.length > 0) {
                inst.retransformClasses(classArray);
            }
        } finally {
            inst.removeTransformer(transformer);
        }
    }
}
