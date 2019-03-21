package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtSyncEnter.SyncEnterInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;
import com.taobao.arthas.bytekit.asm.location.SyncLocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = SyncEnterInterceptorProcessorParser.class)
public @interface AtSyncEnter {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    int count() default -1;
    boolean whenComplete() default false;

    class SyncEnterInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());
            InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
            interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);

            interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
            interceptorMethodConfig.setMethodName(method.getName());
            interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

            AtSyncEnter atSyncEnter = (AtSyncEnter) annotationOnMethod;

            LocationMatcher locationMatcher = new SyncLocationMatcher(Opcodes.MONITORENTER, atSyncEnter.count(), atSyncEnter.whenComplete());
            interceptorProcessor.setLocationMatcher(locationMatcher);

            interceptorMethodConfig.setInline(atSyncEnter.inline());

            List<Binding> bindings = BindingParserUtils.parseBindings(method);

            interceptorMethodConfig.setBindings(bindings);

            InterceptorMethodConfig errorHandlerMethodConfig = ExceptionHandlerUtils
                    .errorHandlerMethodConfig(atSyncEnter.suppress(), atSyncEnter.suppressHandler());
            if (errorHandlerMethodConfig != null) {
                interceptorProcessor.setExceptionHandlerConfig(errorHandlerMethodConfig);
            }

            return interceptorProcessor;
        }

    }
}
