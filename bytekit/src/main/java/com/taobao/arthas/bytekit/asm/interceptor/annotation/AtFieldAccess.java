package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtFieldAccess.FieldAccessInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.FieldAccessLocationMatcher;
import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = FieldAccessInterceptorProcessorParser.class)
public @interface AtFieldAccess {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    java.lang.Class<?> owner() default Void.class;

    java.lang.Class<?> type() default Void.class;

    String name();

    int count() default -1;

    int flags() default Location.ACCESS_READ | Location.ACCESS_WRITE;

    boolean whenComplete() default false;

    class FieldAccessInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());
            InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
            interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);

            interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
            interceptorMethodConfig.setMethodName(method.getName());
            interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

            AtFieldAccess atFieldAccess = (AtFieldAccess) annotationOnMethod;

            String ownerClass = null;
            String fieldDesc = null;
            if(! atFieldAccess.owner().equals(Void.class)) {
                ownerClass = Type.getType(atFieldAccess.owner()).getInternalName();
            }
            if(!atFieldAccess.type().equals(Void.class)) {
                fieldDesc = Type.getType(atFieldAccess.type()).getDescriptor();
            }

            LocationMatcher locationMatcher = new FieldAccessLocationMatcher(
                    ownerClass,
                    fieldDesc, atFieldAccess.name(), atFieldAccess.count(),
                    atFieldAccess.flags(), atFieldAccess.whenComplete());
            interceptorProcessor.setLocationMatcher(locationMatcher);

            interceptorMethodConfig.setInline(atFieldAccess.inline());

            List<Binding> bindings = BindingParserUtils.parseBindings(method);

            interceptorMethodConfig.setBindings(bindings);

            InterceptorMethodConfig errorHandlerMethodConfig = ExceptionHandlerUtils
                    .errorHandlerMethodConfig(atFieldAccess.suppress(), atFieldAccess.suppressHandler());
            if (errorHandlerMethodConfig != null) {
                interceptorProcessor.setExceptionHandlerConfig(errorHandlerMethodConfig);
            }

            return interceptorProcessor;
        }

    }
}
