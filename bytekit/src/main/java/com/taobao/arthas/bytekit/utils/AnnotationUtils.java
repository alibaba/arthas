package com.taobao.arthas.bytekit.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationUtils {

    public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        return method.getAnnotation(annotationType);
    }

}
