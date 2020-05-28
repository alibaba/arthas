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

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtInvoke.InvokeInterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.taobao.arthas.bytekit.asm.location.InvokeLocationMatcher;
import com.taobao.arthas.bytekit.asm.location.LocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = InvokeInterceptorProcessorParser.class)
public @interface AtInvoke {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    Class<?> owner() default Void.class;

    String name();

    String desc() default "";

    int count() default -1;

    boolean whenComplete() default false;

    /**
     * method name excludes
     * @return
     */
    String[] excludes() default {};

    class InvokeInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());
            InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
            interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);

            interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
            interceptorMethodConfig.setMethodName(method.getName());
            interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

            AtInvoke atInvoke = (AtInvoke) annotationOnMethod;

            String owner = null;
            String desc = null;
            if (!atInvoke.owner().equals(Void.class)) {
                owner = Type.getType(atInvoke.owner()).getInternalName();
            }
            if (atInvoke.desc().isEmpty()) {
                desc = null;
            }

            List<String> excludes = new ArrayList<String>();
            for (String exclude : atInvoke.excludes()) {
                excludes.add(exclude);
            }

            LocationMatcher locationMatcher = new InvokeLocationMatcher(owner, atInvoke.name(), desc, atInvoke.count(),
                    atInvoke.whenComplete(), excludes);
            interceptorProcessor.setLocationMatcher(locationMatcher);

            interceptorMethodConfig.setInline(atInvoke.inline());

            List<Binding> bindings = BindingParserUtils.parseBindings(method);

            interceptorMethodConfig.setBindings(bindings);

            InterceptorMethodConfig errorHandlerMethodConfig = ExceptionHandlerUtils
                    .errorHandlerMethodConfig(atInvoke.suppress(), atInvoke.suppressHandler());
            if (errorHandlerMethodConfig != null) {
                interceptorProcessor.setExceptionHandlerConfig(errorHandlerMethodConfig);
            }

            return interceptorProcessor;
        }

    }
}
