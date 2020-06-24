package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.alibaba.arthas.deps.org.objectweb.asm.commons.Method;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtInvoke;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.utils.Decompiler;

import java.util.Arrays;

public class AtInvokeTest {
    @Rule
    public OutputCapture capture = new OutputCapture();
    
    static class Sample {
        long longField;
        int intField;
        String strField;
        
        public Sample(int i, long l, String s) {
            staticToBeCall(i, l, s);
            aaa("aaa");
        }
        
        public int testCall(int ii) {
            //测试空构造函数
            StringBuilder sb = new StringBuilder();
            sb.append(ii);
            toBeCall(ii, 123L, "");
            System.err.println("abc");

            aaa("abc");
            return 123;
        }
        
        
        public  void aaa(String aaa) {
            return ;
        }
        
        public long toBeCall(int i , long l, String s) {
            return l + i;
        }
        
        public static long staticToBeCall(int i , long l, String s) {
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
        @AtInvoke(name = "", inline = false, whenComplete=false, excludes = {"System."})
        public static void onInvoke(
                @Binding.This Object object,
                @Binding.Class Object clazz
                , 
                @Binding.Line int line,
                @Binding.InvokeMethodDeclaration String declaration,
                @Binding.InvokeArgs Object[] args
                ) {
            System.err.println("onInvoke: line: " + line + ", method: "+ declaration +", args: " + Arrays.toString(args));
        }
        
        @AtInvoke(name = "toBeCall", inline = false, whenComplete = true)
        public static void onInvokeAfter(
                @Binding.This Object object,
                @Binding.Class Object clazz,
                @Binding.Line int line,
                @Binding.InvokeReturn Object invokeReturn,
                @Binding.InvokeMethodDeclaration String declaration
                ) {

            System.err.println("onInvokeAfter: this" + object);
            System.err.println("declaration: " + declaration);
            assertThat(declaration).isEqualTo(Method.getMethod("long toBeCall(int, long, java.lang.String)").getDescriptor());

            System.err.println("invokeReturn: " + invokeReturn);
            assertThat(invokeReturn).isEqualTo(100 + 123L);
        }
    }
    
    @Test
    // TODO fix com.taobao.arthas.bytekit.asm.location.Location.InvokeLocation satck save
    public void testInvokeBefore() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(TestAccessInterceptor.class).methodMatcher("testCall")
                .redefine(true);
        byte[] bytes = helper.process(Sample.class);

        System.err.println(Decompiler.decompile(bytes));

        new Sample(100, 100L, "").testCall(100);

        assertThat(capture.toString()).contains("onInvoke:");
    }


}
