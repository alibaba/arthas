package com.taobao.arthas.core.advisor;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;

import com.taobao.arthas.core.util.reflect.FieldUtils;
import com.taobao.middleware.logger.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;
import static java.lang.System.arraycopy;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * 对类进行通知增强
 * Created by vlinux on 15/5/17.
 */
public class Enhancer implements ClassFileTransformer {

    private static final Logger logger = LogUtil.getArthasLogger();

    private final int adviceId;
    private final boolean isTracing;
    private final boolean skipJDKTrace;
    private final Set<Class<?>> matchingClasses;
    private final Matcher methodNameMatcher;
    private final EnhancerAffect affect;


    /**
     * @param adviceId          通知编号
     * @param isTracing         可跟踪方法调用
     * @param skipJDKTrace      是否忽略对JDK内部方法的跟踪
     * @param matchingClasses   匹配中的类
     * @param methodNameMatcher 方法名匹配
     * @param affect            影响统计
     */
    private Enhancer(int adviceId,
                     boolean isTracing,
                     boolean skipJDKTrace,
                     Set<Class<?>> matchingClasses,
                     Matcher methodNameMatcher,
                     EnhancerAffect affect) {
        this.adviceId = adviceId;
        this.isTracing = isTracing;
        this.skipJDKTrace = skipJDKTrace;
        this.matchingClasses = matchingClasses;
        this.methodNameMatcher = methodNameMatcher;
        this.affect = affect;
    }

	private void spy(final ClassLoader targetClassLoader) throws Exception {
        if (targetClassLoader == null) {
            // 增强JDK自带的类,targetClassLoader为null
            return;
        }
        // 因为 Spy 是被bootstrap classloader加载的，所以一定可以被找到，如果找不到的话，说明应用方的classloader实现有问题
        Class<?> spyClass = targetClassLoader.loadClass(Constants.SPY_CLASSNAME);

        final ClassLoader arthasClassLoader = Enhancer.class.getClassLoader();

        // 初始化间谍, AgentLauncher会把各种hook设置到ArthasClassLoader当中
        // 这里我们需要把这些hook取出来设置到目标classloader当中
        Method initMethod = spyClass.getMethod("init", ClassLoader.class, Method.class,
                Method.class, Method.class, Method.class, Method.class, Method.class);
        initMethod.invoke(null, arthasClassLoader,
                FieldUtils.getField(spyClass, "ON_BEFORE_METHOD").get(null),
                FieldUtils.getField(spyClass, "ON_RETURN_METHOD").get(null),
                FieldUtils.getField(spyClass, "ON_THROWS_METHOD").get(null),
                FieldUtils.getField(spyClass, "BEFORE_INVOKING_METHOD").get(null),
                FieldUtils.getField(spyClass, "AFTER_INVOKING_METHOD").get(null),
                FieldUtils.getField(spyClass, "THROW_INVOKING_METHOD").get(null));
	}

    @Override
    public byte[] transform(final ClassLoader inClassLoader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            // 这里要再次过滤一次，为啥？因为在transform的过程中，有可能还会再诞生新的类
            // 所以需要将之前需要转换的类集合传递下来，再次进行判断
            if (!matchingClasses.contains(classBeingRedefined)) {
                return null;
            }

            final ClassReader cr;

            // 首先先检查是否在缓存中存在Class字节码
            // 因为要支持多人协作,存在多人同时增强的情况
            final byte[] byteOfClassInCache = EnhanceUtils.CLASS_BYTES_CACHE.get(classBeingRedefined);
            if (null != byteOfClassInCache) {
                cr = new ClassReader(byteOfClassInCache);
            }

            // 如果没有命中缓存,则从原始字节码开始增强
            else {
                cr = new ClassReader(classfileBuffer);
            }

            // 字节码增强
            final ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS) {

                /*
                 * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
                 * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
                 * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
                 * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
                 *
                 * 通过重写 getCommonSuperClass() 方法，更正获取ClassLoader的方式，改成使用指定ClassLoader的方式进行。
                 * 规避了原有代码采用Object.class.getClassLoader()的方式
                 */
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    Class<?> c, d;
                    final ClassLoader classLoader = inClassLoader;
                    try {
                        c = Class.forName(type1.replace('/', '.'), false, classLoader);
                        d = Class.forName(type2.replace('/', '.'), false, classLoader);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (c.isAssignableFrom(d)) {
                        return type1;
                    }
                    if (d.isAssignableFrom(c)) {
                        return type2;
                    }
                    if (c.isInterface() || d.isInterface()) {
                        return "java/lang/Object";
                    } else {
                        do {
                            c = c.getSuperclass();
                        } while (!c.isAssignableFrom(d));
                        return c.getName().replace('.', '/');
                    }
                }

            };

            // 生成增强字节码
            cr.accept(new AdviceWeaver(adviceId, isTracing, skipJDKTrace, cr.getClassName(), methodNameMatcher, affect,
                            cw), EXPAND_FRAMES);
            final byte[] enhanceClassByteArray = cw.toByteArray();

            // 生成成功,推入缓存
            EnhanceUtils.CLASS_BYTES_CACHE.put(classBeingRedefined, enhanceClassByteArray);

            // dump the class
            EnhanceUtils.dumpClassIfNecessary(className, enhanceClassByteArray, affect);

            // 成功计数
            affect.cCnt(1);

            // 派遣间谍
            try {
                spy(inClassLoader);
            } catch (Throwable t) {
                logger.warn("print spy failed. classname={};loader={};", className, inClassLoader, t);
                throw t;
            }

            return enhanceClassByteArray;
        } catch (Throwable t) {
            logger.warn("transform loader[{}]:class[{}] failed.", inClassLoader, className, t);
        }

        return null;
    }

    /**
     * Enhance classes.
     *
     * @param inst              inst
     * @param adviceId          the advice id
     * @param isTracing         is tracing
     * @param skipJDKTrace      is skip JDK inner method
     * @param enhanceClassSet   the class set
     * @param methodNameMatcher the method matcher
     * @return the affect
     * @throws UnmodifiableClassException the fail exception
     */
    public static synchronized EnhancerAffect enhance(
            final Instrumentation inst,
            final int adviceId,
            final boolean isTracing,
            final boolean skipJDKTrace,
            final Set<Class<?>> enhanceClassSet,
            final Matcher methodNameMatcher) throws UnmodifiableClassException {

        final EnhancerAffect affect = new EnhancerAffect();

        // 构建增强器
        final Enhancer enhancer = new Enhancer(adviceId, isTracing, skipJDKTrace, enhanceClassSet, methodNameMatcher, affect);
        try {
            inst.addTransformer(enhancer, true);
            EnhanceUtils.transform(inst, enhanceClassSet);
        } finally {
            inst.removeTransformer(enhancer);
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
    public static synchronized EnhancerAffect reset(
            final Instrumentation inst,
            final Matcher classNameMatcher) throws UnmodifiableClassException {

        final EnhancerAffect affect = new EnhancerAffect();
        final Set<Class<?>> enhanceClassSet = new HashSet<Class<?>>();

        for (Class<?> classInCache : EnhanceUtils.CLASS_BYTES_CACHE.keySet()) {
            if (classNameMatcher.matching(classInCache.getName())) {
                enhanceClassSet.add(classInCache);
            }
        }

        final ClassFileTransformer resetClassFileTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) throws IllegalClassFormatException {
                return null;
            }
        };

        try {
            enhance(inst, resetClassFileTransformer, enhanceClassSet);
            logger.info("Success to reset classes: " + enhanceClassSet);
        } finally {
            for (Class<?> resetClass : enhanceClassSet) {
                EnhanceUtils.CLASS_BYTES_CACHE.remove(resetClass);
                affect.cCnt(1);
            }
        }

        return affect;
    }

    // 批量增强
    public static void enhance(Instrumentation inst, ClassFileTransformer transformer, Set<Class<?>> classes)
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
