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
 * 对类进行通知增强 Created by vlinux on 15/5/17.
 * @author hengyunabc
 */
public class Enhancer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(Enhancer.class);

    private final AdviceListener listener;
    private final boolean isTracing;
    private final boolean skipJDKTrace;
    private final Matcher classNameMatcher;
    private final Matcher classNameExcludeMatcher;
    private final Matcher methodNameMatcher;
    private final EnhancerAffect affect;
    private Set<Class<?>> matchingClasses = null;
    private static final ClassLoader selfClassLoader = Enhancer.class.getClassLoader();

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
            Matcher classNameExcludeMatcher,
            Matcher methodNameMatcher) {
        this.listener = listener;
        this.isTracing = isTracing;
        this.skipJDKTrace = skipJDKTrace;
        this.classNameMatcher = classNameMatcher;
        this.classNameExcludeMatcher = classNameExcludeMatcher;
        this.methodNameMatcher = methodNameMatcher;
        this.affect = new EnhancerAffect();
        affect.setListenerId(listener.id());
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
                        inClassLoader.getClass().getName(), className, e);
                return null;
            }

            // 这里要再次过滤一次，为啥？因为在transform的过程中，有可能还会再诞生新的类
            // 所以需要将之前需要转换的类集合传递下来，再次进行判断
            if (matchingClasses != null && !matchingClasses.contains(classBeingRedefined)) {
                return null;
            }

            //keep origin class reader for bytecode optimizations, avoiding JVM metaspace OOM.
            ClassNode classNode = new ClassNode(Opcodes.ASM9);
            ClassReader classReader = AsmUtils.toClassNode(classfileBuffer, classNode);
            // remove JSR https://github.com/alibaba/arthas/issues/1304
            classNode = AsmUtils.removeJSRInstructions(classNode);

            // 生成增强字节码
            DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

            final List<InterceptorProcessor> interceptorProcessors = new ArrayList<InterceptorProcessor>();

            interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyInterceptor1.class));
            interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyInterceptor2.class));
            interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyInterceptor3.class));

            if (this.isTracing) {
                if (!this.skipJDKTrace) {
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceInterceptor1.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceInterceptor2.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceInterceptor3.class));
                } else {
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceExcludeJDKInterceptor1.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceExcludeJDKInterceptor2.class));
                    interceptorProcessors.addAll(defaultInterceptorClassParser.parse(SpyTraceExcludeJDKInterceptor3.class));
                }
            }

            List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
            for (MethodNode methodNode : classNode.methods) {
                if (!isIgnore(methodNode, methodNameMatcher)) {
                    matchedMethods.add(methodNode);
                }
            }

            // https://github.com/alibaba/arthas/issues/1690
            if (AsmUtils.isEnhancerByCGLIB(className)) {
                for (MethodNode methodNode : matchedMethods) {
                    if (AsmUtils.isConstructor(methodNode)) {
                        AsmUtils.fixConstructorExceptionTable(methodNode);
                    }
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
                if (AsmUtils.isNative(methodNode)) {
                    logger.info("ignore native method: {}",
                            AsmUtils.methodDeclaration(Type.getObjectType(classNode.name), methodNode));
                    continue;
                }
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

            // https://github.com/alibaba/arthas/issues/1223 , V1_5 的major version是49
            if (AsmUtils.getMajorVersion(classNode.version) < 49) {
                classNode.version = AsmUtils.setMajorVersion(classNode.version, 49);
            }

            byte[] enhanceClassByteArray = AsmUtils.toBytes(classNode, inClassLoader, classReader);

            // 增强成功，记录类
            classBytesCache.put(classBeingRedefined, new Object());

            // dump the class
            dumpClassIfNecessary(className, enhanceClassByteArray, affect);

            // 成功计数
            affect.cCnt(1);

            return enhanceClassByteArray;
        } catch (Throwable t) {
            logger.warn("transform loader[{}]:class[{}] failed.", inClassLoader, className, t);
            affect.setThrowable(t);
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
    private List<Pair<Class<?>, String>> filter(Set<Class<?>> classes) {
        List<Pair<Class<?>, String>> filteredClasses = new ArrayList<Pair<Class<?>, String>>();
        final Iterator<Class<?>> it = classes.iterator();
        while (it.hasNext()) {
            final Class<?> clazz = it.next();
            boolean removeFlag = false;
            if (null == clazz) {
                removeFlag = true;
            } else if (isSelf(clazz)) {
                filteredClasses.add(new Pair<Class<?>, String>(clazz, "class loaded by arthas itself"));
                removeFlag = true;
            } else if (isUnsafeClass(clazz)) {
                filteredClasses.add(new Pair<Class<?>, String>(clazz, "class loaded by Bootstrap Classloader, try to execute `options unsafe true`"));
                removeFlag = true;
            } else if (isExclude(clazz)) {
                filteredClasses.add(new Pair<Class<?>, String>(clazz, "class is excluded"));
                removeFlag = true;
            } else {
                Pair<Boolean, String> unsupportedResult = isUnsupportedClass(clazz);
                if (unsupportedResult.getFirst()) {
                    filteredClasses.add(new Pair<Class<?>, String>(clazz, unsupportedResult.getSecond()));
                    removeFlag = true;
                }
            }
            if (removeFlag) {
                it.remove();
            }
        }
        return filteredClasses;
    }

    private boolean isExclude(Class<?> clazz) {
        if (this.classNameExcludeMatcher != null) {
            return classNameExcludeMatcher.matching(clazz.getName());
        }
        return false;
    }

    /**
     * 是否过滤Arthas加载的类
     */
    private static boolean isSelf(Class<?> clazz) {
        return null != clazz && isEquals(clazz.getClassLoader(), selfClassLoader);
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
    private static Pair<Boolean, String> isUnsupportedClass(Class<?> clazz) {
        if (ClassUtils.isLambdaClass(clazz)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is lambda");
        }

        if (clazz.isInterface() && !GlobalOptions.isSupportDefaultMethod) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is interface");
        }

        if (clazz.equals(Integer.class)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is java.lang.Integer");
        }

        if (clazz.equals(Class.class)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is java.lang.Class");
        }

        if (clazz.equals(Method.class)) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is java.lang.Method");
        }

        if (clazz.isArray()) {
            return new Pair<Boolean, String>(Boolean.TRUE, "class is array");
        }
        return new Pair<Boolean, String>(Boolean.FALSE, "");
    }

    /**
     * 对象增强
     *
     * @param inst              inst
     * @param maxNumOfMatchedClass 匹配的class最大数量
     * @return 增强影响范围
     * @throws UnmodifiableClassException 增强失败
     */
    public synchronized EnhancerAffect enhance(final Instrumentation inst, int maxNumOfMatchedClass) throws UnmodifiableClassException {
        // 获取需要增强的类集合
        this.matchingClasses = GlobalOptions.isDisableSubClass
                ? SearchUtils.searchClass(inst, classNameMatcher)
                : SearchUtils.searchSubClass(inst, SearchUtils.searchClass(inst, classNameMatcher));

        if (matchingClasses.size() > maxNumOfMatchedClass) {
            affect.setOverLimitMsg("The number of matched classes is " +matchingClasses.size()+ ", greater than the limit value " + maxNumOfMatchedClass + ". Try to change the limit with option '-m <arg>'.");
            return affect;
        }
        // 过滤掉无法被增强的类
        List<Pair<Class<?>, String>> filtedList = filter(matchingClasses);
        if (!filtedList.isEmpty()) {
            for (Pair<Class<?>, String> filted : filtedList) {
                logger.info("ignore class: {}, reason: {}", filted.getFirst().getName(), filted.getSecond());
            }
        }

        logger.info("enhance matched classes: {}", matchingClasses);

        affect.setTransformer(this);

        try {
            ArthasBootstrap.getInstance().getTransformerManager().addTransformer(this, isTracing);

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
        } catch (Throwable e) {
            logger.error("Enhancer error, matchingClasses: {}", matchingClasses, e);
            affect.setThrowable(e);
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

        try {
            enhance(inst, enhanceClassSet);
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
    private static void enhance(Instrumentation inst, Set<Class<?>> classes)
            throws UnmodifiableClassException {
        int size = classes.size();
        Class<?>[] classArray = new Class<?>[size];
        arraycopy(classes.toArray(), 0, classArray, 0, size);
        if (classArray.length > 0) {
            inst.retransformClasses(classArray);
        }
    }
}
