package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtThrow;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class AtThrowTest {
    @Rule
    public OutputCapture capture = new OutputCapture();
    
    static class Sample {
        
        public static long testThrow(int i , long l, String s) {
            try {
                if(i < 0) {
                    throw new RuntimeException("eeeee");
                }
            } catch (Exception e) {
                
                System.err.println(e.getMessage());
            }
            return l + i;
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
        
        @AtThrow(inline = false)
        public static void atThrow(
                @Binding.This Object object,
                @Binding.Class Object clazz
                ,  
                @Binding.LocalVars Object[] vars,
                @Binding.Throwable Throwable t
                ) {
            System.err.println("atThrow: this" + object);
            System.err.println("vars: " + Arrays.toString(vars));
            System.err.println("t: " + t);
            
            assertThat(t).hasMessage("eeeee");
        }
    }
    
    @Test
    public void testThrow() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(TestAccessInterceptor.class).methodMatcher("testThrow")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        Sample.testThrow(-1, 0, null);

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("atThrow: this");
    }


}
