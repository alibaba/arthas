package com.taobao.arthas.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.binding.annotation.BindingParser;
import com.taobao.arthas.bytekit.asm.binding.annotation.BindingParserHandler;
import com.taobao.arthas.bytekit.utils.InstanceUtils;

public class BindingParserUtils {

    public static List<Binding> parseBindings(Method method) {
        // 从 parameter 里解析出来 binding
        List<Binding> bindings = new ArrayList<Binding>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int parameterIndex = 0; parameterIndex < parameterAnnotations.length; ++parameterIndex) {
            Annotation[] annotationsOnParameter = parameterAnnotations[parameterIndex];
            for (int j = 0; j < annotationsOnParameter.length; ++j) {

                Annotation[] annotationsOnBinding = annotationsOnParameter[j].annotationType().getAnnotations();
                for (Annotation annotationOnBinding : annotationsOnBinding) {
                    if (BindingParserHandler.class.isAssignableFrom(annotationOnBinding.annotationType())) {
                        BindingParserHandler bindingParserHandler = (BindingParserHandler) annotationOnBinding;
                        BindingParser bindingParser = InstanceUtils.newInstance(bindingParserHandler.parser());
                        Binding binding = bindingParser.parse(annotationsOnParameter[j]);
                        bindings.add(binding);
                    }
                }
            }
        }
        return bindings;
    }
}
