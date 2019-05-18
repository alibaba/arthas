package com.taobao.arthas.core.advisor;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.middleware.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.taobao.arthas.core.util.ArthasCheckUtils.isEquals;
import static java.lang.System.arraycopy;

/**
 * Date: 2019/5/17
 *
 * @author xuzhiyi
 */
public class EnhanceUtils {

    private static final Logger logger = LogUtil.getArthasLogger();

    /**
     * The active advices map.
     */
    private final static Map<Integer/*ADVICE_ID*/, AdviceListener> ADVICES
        = new ConcurrentHashMap<Integer, AdviceListener>();

    /**
     * The suspend advices map.
     */
    private final static Map<Integer/*ADVICE_ID*/, AdviceListener> SUSPEND_ADVICES
        = new ConcurrentHashMap<Integer, AdviceListener>();

    /**
     * The classes cache.
     */
    public final static Map<Class<?>/*Class*/, byte[]/*bytes of Class*/> CLASS_BYTES_CACHE
        = new WeakHashMap<Class<?>, byte[]>();

    /**
     * Is unregistered advice.
     *
     * @param adviceId the advice id.
     * @return is unregistered
     */
    public static boolean isUnRegistered(int adviceId) {
        return !ADVICES.containsKey(adviceId) && !SUSPEND_ADVICES.containsKey(adviceId);
    }

    /**
     * Get advice listener.
     *
     * @param adviceId the advice id.
     * @return the listener
     */
    public static AdviceListener getListener(int adviceId) {
        return ADVICES.get(adviceId);
    }

    /**
     * Register listener.
     *
     * @param adviceId the advice id
     * @param listener the listener
     */
    public static void register(int adviceId, AdviceListener listener) {

        listener.create();

        ADVICES.put(adviceId, listener);
    }

    /**
     * Unregister advice listener.
     *
     * @param adviceId the advice id
     * @param inst     the inst
     */
    public static void unRegister(int adviceId, Instrumentation inst) {

        final AdviceListener listener = ADVICES.remove(adviceId);

        if (null != listener) {
            // destroy
            listener.destroy();

            // trigger unEnhance
            if (listener.getListenClasses() != null) {
                UnEnhancer.unEnhance(inst, listener.getListenClasses());
            }
        }

    }

    /**
     * Resume advice listener.
     *
     * @param adviceId the advice id
     */
    public static void resume(int adviceId) {
        if (SUSPEND_ADVICES.get(adviceId) != null) {
            ADVICES.put(adviceId, SUSPEND_ADVICES.get(adviceId));
        }
        SUSPEND_ADVICES.remove(adviceId);
    }

    /**
     * Suspend advice listener.
     *
     * @param adviceId the advice id
     */
    public static AdviceListener suspend(int adviceId) {
        if (ADVICES.get(adviceId) != null) {
            SUSPEND_ADVICES.put(adviceId, ADVICES.get(adviceId));
        }
        return ADVICES.remove(adviceId);
    }

    public static void transform(Instrumentation inst, Set<Class<?>> enhanceClassSet) throws UnmodifiableClassException {
        EnhanceUtils.filter(enhanceClassSet);

        // batch enhance
        if (GlobalOptions.isBatchReTransform) {
            final int size = enhanceClassSet.size();
            final Class<?>[] classArray = new Class<?>[size];
            arraycopy(enhanceClassSet.toArray(), 0, classArray, 0, size);
            if (classArray.length > 0) {
                inst.retransformClasses(classArray);
                logger.info("Success to batch transform classes: " + Arrays.toString(classArray));
            }
        } else {
            // for each enhance
            for (Class<?> clazz : enhanceClassSet) {
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
    }

    /**
     * dump class to file
     */
    public static void dumpClassIfNecessary(String className, byte[] data, EnhancerAffect affect) {
        if (!GlobalOptions.isDump) {
            return;
        }
        final File dumpClassFile = new File("./arthas-class-dump/" + className + ".class");
        final File classPath = new File(dumpClassFile.getParent());

        // 创建类所在的包路径
        if (!classPath.mkdirs()
            && !classPath.exists()) {
            logger.warn("create dump classpath:{} failed.", classPath);
            return;
        }

        // 将类字节码写入文件
        try {
            FileUtils.writeByteArrayToFile(dumpClassFile, data);
            if (affect != null) {
                affect.getClassDumpFiles().add(dumpClassFile);
            }
            logger.info(null, "dump class:{} to file {}.", className, dumpClassFile.getAbsolutePath());
        } catch (IOException e) {
            logger.warn("dump class:{} to file {} failed.", className, dumpClassFile, e);
        }
    }

    /**
     * dump class to file
     */
    public static void dumpClassIfNecessary(String className, byte[] data) {
        dumpClassIfNecessary(className, data, null);
    }

    /**
     * 是否需要过滤的类
     *
     * @param classes 类集合
     */
    public static void filter(Set<Class<?>> classes) {
        final Iterator<Class<?>> it = classes.iterator();
        while (it.hasNext()) {
            final Class<?> clazz = it.next();
            if (null == clazz
                || isSelf(clazz)
                || isUnsafeClass(clazz)
                || isUnsupportedClass(clazz)) {
                it.remove();
            }
        }
    }

    /**
     * 是否过滤Arthas加载的类
     */
    private static boolean isSelf(Class<?> clazz) {
        return null != clazz
               && isEquals(clazz.getClassLoader(), Enhancer.class.getClassLoader());
    }

    /**
     * 是否过滤unsafe类
     */
    private static boolean isUnsafeClass(Class<?> clazz) {
        return !GlobalOptions.isUnsafe
               && clazz.getClassLoader() == null;
    }

    /**
     * 是否过滤目前暂不支持的类
     */
    private static boolean isUnsupportedClass(Class<?> clazz) {

        return clazz.isArray()
               || clazz.isInterface()
               || clazz.isEnum()
               || clazz.equals(Class.class) || clazz.equals(Integer.class) || clazz.equals(Method.class);
    }
}
