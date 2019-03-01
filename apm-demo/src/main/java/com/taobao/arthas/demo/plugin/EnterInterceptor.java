package com.taobao.arthas.demo.plugin;

import java.util.Arrays;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtEnter;

public class EnterInterceptor {

    @AtEnter(inline = true, suppress = RuntimeException.class, suppressHandler = TestPrintSuppressHandler.class)
    public static void onEnter(@Binding.This Object object, @Binding.Class Object clazz, @Binding.Args Object[] args) {
        System.out.println("onEnter, object:" + object);
        System.out.println("onEnter, clazz:" + clazz);
        System.out.println("onEnter, args:" + Arrays.toString(args));
    }

}