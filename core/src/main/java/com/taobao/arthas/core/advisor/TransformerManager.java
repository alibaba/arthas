package com.taobao.arthas.core.advisor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * @author hengyunabc 2020-05-18
 *
 */
public class TransformerManager {

    private Instrumentation instrumentation;
    private List<ClassFileTransformer> watchTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();
    private List<ClassFileTransformer> traceTransformers = new CopyOnWriteArrayList<ClassFileTransformer>();

    private ClassFileTransformer classFileTransformer;

    public TransformerManager(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;

        classFileTransformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

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
    }

    public void addTransformer(ClassFileTransformer transformer, boolean isTracing) {
        if (isTracing) {
            traceTransformers.add(transformer);
        } else {
            watchTransformers.add(transformer);
        }
    }

    public void removeTransformer(ClassFileTransformer transformer) {
        watchTransformers.remove(transformer);
        traceTransformers.remove(transformer);
    }

    public void destroy() {
        watchTransformers.clear();
        traceTransformers.clear();
        instrumentation.removeTransformer(classFileTransformer);
    }

}
