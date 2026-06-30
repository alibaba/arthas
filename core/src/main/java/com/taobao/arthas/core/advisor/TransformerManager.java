package com.taobao.arthas.core.advisor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * <pre>
 * * 统一管理 ClassFileTransformer
 * * 每个增强命令对应一个 Enhancer ，也统一在这里管理
 * </pre>
 * 
 * @see com.taobao.arthas.core.advisor.Enhancer
 * @author hengyunabc 2020-05-18
 *
 */
public class TransformerManager {

    private Instrumentation instrumentation;
    private List<ClassFileTransformer> watchTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();
    private List<ClassFileTransformer> traceTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();
    
    /**
     * 先于 watch/trace的 Transformer TODO 改进为全部用 order 排序？
     */
    private List<ClassFileTransformer> reTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    /**
     * 懒加载模式的 Transformer，用于在类首次加载时增强
     * 这些 transformer 需要用 addTransformer(transformer, false) 注册才能在类首次加载时工作
     */
    private List<ClassFileTransformer> lazyTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    private ClassFileTransformer classFileTransformer;
    
    /**
     * 用于处理类首次加载的 transformer（非 retransform-capable）
     * 只有这种 transformer 才会在类首次定义时被调用
     */
    private ClassFileTransformer lazyClassFileTransformer;

    public TransformerManager(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;

        classFileTransformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                for (ClassFileTransformer classFileTransformer : reTransformers) {
                    byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                for (ClassFileTransformer classFileTransformer : watchTransformers) {
                    byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                for (ClassFileTransformer classFileTransformer : traceTransformers) {
                    byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                return classfileBuffer;
            }

        };
        instrumentation.addTransformer(classFileTransformer, true);
        
        // 懒加载 transformer，用于在类首次加载时增强
        // 注意：必须用 addTransformer(transformer, false) 才能在类首次定义时被调用
        lazyClassFileTransformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                // 只处理类首次加载的情况（classBeingRedefined == null）
                if (classBeingRedefined != null) {
                    return null;
                }
                
                for (ClassFileTransformer transformer : lazyTransformers) {
                    byte[] transformResult = transformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                return classfileBuffer;
            }

        };
        // 使用 false 参数，这样才会在类首次定义时被调用
        instrumentation.addTransformer(lazyClassFileTransformer, false);
    }

    public void addTransformer(ClassFileTransformer transformer, boolean isTracing) {
        if (isTracing) {
            traceTransformers.add(transformer);
        } else {
            watchTransformers.add(transformer);
        }
    }
    
    /**
     * 添加懒加载 transformer，用于在类首次加载时增强
     */
    public void addLazyTransformer(ClassFileTransformer transformer) {
        lazyTransformers.add(transformer);
    }

    public void addRetransformer(ClassFileTransformer transformer) {
        reTransformers.add(transformer);
    }

    public void removeTransformer(ClassFileTransformer transformer) {
        reTransformers.remove(transformer);
        watchTransformers.remove(transformer);
        traceTransformers.remove(transformer);
        lazyTransformers.remove(transformer);
    }

    public void destroy() {
        reTransformers.clear();
        watchTransformers.clear();
        traceTransformers.clear();
        lazyTransformers.clear();
        instrumentation.removeTransformer(classFileTransformer);
        instrumentation.removeTransformer(lazyClassFileTransformer);
    }

}
