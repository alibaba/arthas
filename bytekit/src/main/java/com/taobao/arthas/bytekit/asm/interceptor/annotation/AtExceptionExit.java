package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;

import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExceptionExit.ExceptionExitInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.ExceptionExitLocationMatcher;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = ExceptionExitInterceptorProcessorParser.class)
public @interface AtExceptionExit {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    Class<? extends Throwable> onException() default Throwable.class;

    class ExceptionExitInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            AtExceptionExit atExceptionExit = (AtExceptionExit) annotationOnMethod;

            LocationMatcher locationMatcher = new ExceptionExitLocationMatcher(Type.getInternalName(atExceptionExit.onException()));;

            return InterceptorParserUtils.createInterceptorProcessor(method,
                    locationMatcher,
                    atExceptionExit.inline(),
                    atExceptionExit.suppress(),
                    atExceptionExit.suppressHandler());

        }

    }
}
