package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;

import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtSyncExit.SyncExitInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;
import com.taobao.arthas.bytekit.asm.location.SyncLocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = SyncExitInterceptorProcessorParser.class)
public @interface AtSyncExit {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    int count() default -1;
    boolean whenComplete() default false;

    class SyncExitInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            AtSyncExit atSyncExit = (AtSyncExit) annotationOnMethod;

            LocationMatcher locationMatcher = new SyncLocationMatcher(Opcodes.MONITOREXIT, atSyncExit.count(), atSyncExit.whenComplete());

            return InterceptorParserUtils.createInterceptorProcessor(method,
                    locationMatcher,
                    atSyncExit.inline(),
                    atSyncExit.suppress(),
                    atSyncExit.suppressHandler());
        }

    }
}
