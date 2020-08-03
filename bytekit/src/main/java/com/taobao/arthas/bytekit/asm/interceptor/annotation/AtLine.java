package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtLine.LineInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.LineLocationMatcher;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = LineInterceptorProcessorParser.class)
public @interface AtLine {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    int[] lines();

    class LineInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            AtLine atLine = (AtLine) annotationOnMethod;

            LocationMatcher locationMatcher = new LineLocationMatcher(atLine.lines());

            return InterceptorParserUtils.createInterceptorProcessor(method,
                    locationMatcher,
                    atLine.inline(),
                    atLine.suppress(),
                    atLine.suppressHandler());
        }

    }
}
