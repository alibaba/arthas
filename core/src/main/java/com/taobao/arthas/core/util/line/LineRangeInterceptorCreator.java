package com.taobao.arthas.core.util.line;

import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtLine;
import com.alibaba.bytekit.asm.interceptor.annotation.InterceptorParserUtils;
import com.alibaba.bytekit.asm.interceptor.annotation.None;
import com.alibaba.bytekit.asm.location.LocationMatcher;
import com.alibaba.bytekit.utils.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class LineRangeInterceptorCreator {
    public static InterceptorProcessor create(Method methodToInject, List<LineRange> lineRanges, boolean isInline,
                                              Class<? extends Throwable> suppress, Class<?> suppressHandler) {
        LocationMatcher locationMatcher = new LineRangeLocationManager(lineRanges);
        return InterceptorParserUtils.createInterceptorProcessor(methodToInject,
                locationMatcher,
                isInline,
                suppress == null ? None.class : suppress,
                suppressHandler == null ? Void.class : suppressHandler);
    }

    /**
     * will parse @AtLine annotation and overwrite the lines dynamically
     */
    public static List<InterceptorProcessor> createFromClass(Class<?> clazz, final List<LineRange> rangeOverrides) {

        final List<InterceptorProcessor> result = new ArrayList<InterceptorProcessor>();

        ReflectionUtils.MethodCallback methodCallback = new ReflectionUtils.MethodCallback() {

            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                for (Annotation onMethodAnnotation : method.getAnnotations()) {
                    if (AtLine.class.isAssignableFrom(onMethodAnnotation.annotationType())) {

                        if (!Modifier.isStatic(method.getModifiers())) {
                            throw new IllegalArgumentException("method must be static. method: " + method);
                        }

                        AtLine atLine = (AtLine) onMethodAnnotation;
                        InterceptorProcessor interceptorProcessor = create(
                                method, rangeOverrides, atLine.inline(), atLine.suppress(), atLine.suppressHandler());
                        result.add(interceptorProcessor);
                    }
                }
            }

        };
        ReflectionUtils.doWithMethods(clazz, methodCallback);
        return result;
    }

}
