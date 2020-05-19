package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class AtExitTest {
    @Rule
    public OutputCapture capture = new OutputCapture();
    
    static class Sample {
        long longField;
        int intField;
        String strField;
        
        public void voidExit() {
            
        }
        
        public long longExit() {
            return 100L;
        }
        
        public static long staticExit() {
            return 999L;
        }
    }
    
    public static class TestPrintSuppressHandler {

        @ExceptionHandler(inline = false)
        public static void onSuppress(@Binding.Throwable Throwable e, @Binding.Class Object clazz) {
            System.err.println("exception handler: " + clazz);
            e.printStackTrace();
        }
    }
    
    public static class TestAccessInterceptor {
        @AtExit(inline = false)
        public static void atExit(@Binding.This Object object,
                @Binding.Class Object clazz
                ,
                @Binding.Return Object re
                ) {
            System.err.println("AtFieldAccess: this" + object);
        }
    }
    
    public static class ChangeReturnInterceptor {
        
        @AtExit(inline = false, suppress = RuntimeException.class, suppressHandler = TestPrintSuppressHandler.class)
        public static Object onExit(@Binding.This Object object, @Binding.Class Object clazz) {
            System.err.println("onExit, object:" + object);
            return 123L;
        }
    }
    
    @Test
    public void testExit() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(TestAccessInterceptor.class).methodMatcher("voidExit")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().voidExit();

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("AtFieldAccess: this");
    }


    @Test
    public void testExitAndChangeReturn() throws Exception {

        TestHelper helper = TestHelper.builder().interceptorClass(ChangeReturnInterceptor.class).methodMatcher("longExit")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        System.err.println(Decompiler.decompile(bytes));

        long re = new Sample().longExit();
        
        assertThat(re).isEqualTo(123);
        assertThat(capture.toString()).contains("onExit, object:");
    }
}
