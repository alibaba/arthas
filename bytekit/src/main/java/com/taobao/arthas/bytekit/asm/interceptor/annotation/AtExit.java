package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExit.ExitInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.ExitLocationMatcher;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = ExitInterceptorProcessorParser.class)
public @interface AtExit {
    boolean inline() default true;
    Class<? extends Throwable> suppress() default None.class;
    Class<?> suppressHandler() default Void.class;

    class ExitInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            AtExit atExit = (AtExit) annotationOnMethod;

            LocationMatcher locationMatcher = new ExitLocationMatcher();

            return InterceptorParserUtils.createInterceptorProcessor(method,
                    locationMatcher,
                    atExit.inline(),
                    atExit.suppress(),
                    atExit.suppressHandler());

        }

    }
}
