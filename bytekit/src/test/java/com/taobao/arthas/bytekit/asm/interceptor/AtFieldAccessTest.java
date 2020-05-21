package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtFieldAccess;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class AtFieldAccessTest {
    @Rule
    public OutputCapture capture = new OutputCapture();
    
    class Sample {
        long longField;
        int intField;
        String strField;
        
        public int testReadField(int ii) {
            longField = 999;
            return 123;
        }
    }
    
    public static class TestPrintSuppressHandler {

        @ExceptionHandler(inline = false)
        public static void onSuppress(@Binding.Throwable Throwable e, @Binding.Class Object clazz) {
            System.err.println("exception handler: " + clazz);
            e.printStackTrace();
        }
    }
    
    public static class FieldAccessInterceptor {
        @AtFieldAccess(name = "longField" , inline =false)
        public static void onFieldAccess(@Binding.This Object object,
                @Binding.Class Object clazz) {
            System.err.println("AtFieldAccess: this" + object);
        }
    }
    
    @Test
    public void testEnter() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(FieldAccessInterceptor.class).methodMatcher("testReadField")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().testReadField(100);

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("AtFieldAccess: this");
    }


}
