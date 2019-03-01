package com.taobao.arthas.demo.plugin;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;

public class TestPrintSuppressHandler {

    @ExceptionHandler(inline = true)
    public static void onSuppress(@Binding.Throwable Throwable e, @Binding.Class Object clazz) {
        System.out.println("exception handler: " + clazz);
        e.printStackTrace();
    }
}