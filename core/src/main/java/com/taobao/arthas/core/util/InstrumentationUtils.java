package com.taobao.arthas.core.util;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 * 
 * @author hengyunabc 2020-05-25
 *
 */
public class InstrumentationUtils {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentationUtils.class);

    public static void retransformClasses(Instrumentation inst, ClassFileTransformer transformer,
            Set<Class<?>> classes) {
        try {
            inst.addTransformer(transformer, true);

            for (Class<?> clazz : classes) {
                try {
                    inst.retransformClasses(clazz);
                } catch (Throwable e) {
                    String errorMsg = "retransformClasses class error, name: " + clazz.getName();
                    if (ClassUtils.isLambdaClass(clazz) && e instanceof VerifyError) {
                        errorMsg += ", Please ignore lambda class VerifyError: https://github.com/alibaba/arthas/issues/675";
                    }
                    logger.error(errorMsg, e);
                }
            }
        } finally {
            inst.removeTransformer(transformer);
        }
    }
}
