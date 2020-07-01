package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtInvokeException.InvokeExceptionInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.InvokeLocationMatcher;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;

/**
 * 
 * @author hengyunabc 2020-05-03
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = InvokeExceptionInterceptorProcessorParser.class)
public @interface AtInvokeException {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    Class<?> owner() default Void.class;

    /**
     * method name
     * 
     * @return
     */
    String name();

    String desc() default "";

    int count() default -1;

    String[] excludes() default {};

    class InvokeExceptionInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            AtInvokeException atInvokeException = (AtInvokeException) annotationOnMethod;

            String owner = null;
            String desc = null;
            if (!atInvokeException.owner().equals(Void.class)) {
                owner = Type.getType(atInvokeException.owner()).getInternalName();
            }
            if (atInvokeException.desc().isEmpty()) {
                desc = null;
            }

            List<String> excludes = new ArrayList<String>();
            for (String exclude : atInvokeException.excludes()) {
                excludes.add(exclude);
            }

            LocationMatcher locationMatcher = new InvokeLocationMatcher(owner, atInvokeException.name(), desc,
                    atInvokeException.count(), true, excludes, true);

            return InterceptorParserUtils.createInterceptorProcessor(method,
                    locationMatcher,
                    atInvokeException.inline(),
                    atInvokeException.suppress(),
                    atInvokeException.suppressHandler());
        }

    }
}
