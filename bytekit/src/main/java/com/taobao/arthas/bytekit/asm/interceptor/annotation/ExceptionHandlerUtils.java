package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.binding.ThrowableBinding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.taobao.arthas.bytekit.utils.AnnotationUtils;
import com.taobao.arthas.bytekit.utils.ReflectionUtils;
import com.taobao.arthas.bytekit.utils.ReflectionUtils.MethodCallback;
import com.taobao.arthas.bytekit.utils.ReflectionUtils.MethodFilter;

public class ExceptionHandlerUtils {

    public static InterceptorMethodConfig errorHandlerMethodConfig(Class<?> suppress, Class<?> handlerClass) {

        // TODO 要解析 errorHander Class里的内容
        final InterceptorMethodConfig errorHandlerMethodConfig = new InterceptorMethodConfig();

        if(suppress.equals(None.class)) {
            suppress = Throwable.class;
        }
        errorHandlerMethodConfig.setSuppress(Type.getType(suppress).getInternalName());

        if (!handlerClass.equals(Void.class)) {
            // find method with @ExceptionHandler
            ReflectionUtils.doWithMethods(handlerClass, new MethodCallback() {

                @Override
                public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                    for (Annotation onMethodAnnotation : method.getAnnotations()) {
                        if (ExceptionHandler.class.isAssignableFrom(onMethodAnnotation.annotationType())) {

                            if (!Modifier.isStatic(method.getModifiers())) {
                                throw new IllegalArgumentException("method must be static. method: " + method);
                            }

                            ExceptionHandler handler = (ExceptionHandler) onMethodAnnotation;

                            errorHandlerMethodConfig.setInline(handler.inline());

                            List<Binding> errorHandlerBindings = BindingParserUtils.parseBindings(method);
                            // 检查第一个 bidning要是 Throwable Binding
                            if (errorHandlerBindings.size() == 0) {
                                throw new IllegalArgumentException(
                                        "error handler bingins must have at least a binding");
                            }
                            if (!(errorHandlerBindings.get(0) instanceof ThrowableBinding)) {
                                throw new IllegalArgumentException(
                                        "error handler bingins first binding must be ThrowableBinding.");
                            }
                            // 去掉第一个 ThrowableBinding
                            // TODO 可能要copy一下，保证可以修改成功
                            errorHandlerBindings.remove(0);
                            errorHandlerMethodConfig.setBindings(errorHandlerBindings);
                            errorHandlerMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
                            errorHandlerMethodConfig.setMethodName(method.getName());
                            errorHandlerMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));
                        }
                    }

                }

            }, new MethodFilter() {

                @Override
                public boolean matches(Method method) {
                    return AnnotationUtils.findAnnotation(method, ExceptionHandler.class) != null;
                }

            });
        }

        if (errorHandlerMethodConfig.getMethodDesc() == null) {
            return null;
        }

        return errorHandlerMethodConfig;
    }
}
