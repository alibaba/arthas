package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class AtExceptionExitTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    public static class Sample {
        
        long longField;
        String strField;
        static int intField;

        public int hello(String str, boolean exception) {
            if (exception) {
                throw new RuntimeException("test exception");
            }
            return str.length();
        }
        
        public long toBeInvoke(int i , long l, String s, long ll) {
            return l + ll;
        }
        
        public void testInvokeArgs() {
            toBeInvoke(1, 123L, "abc", 100L);
        }
        
    }

    public static class TestPrintSuppressHandler {

        @ExceptionHandler(inline = true)
        public static void onSuppress(@Binding.Throwable Throwable e, @Binding.Class Object clazz) {
            System.err.println("exception handler: " + clazz);
            System.err.println(e.getMessage());
            assertThat(e).hasMessage("exception for ExceptionHandler");
        }
    }

    public static class ExceptionExitInterceptor {
        @AtExceptionExit(inline = false, onException = RuntimeException.class ,suppress = Throwable.class, suppressHandler = TestPrintSuppressHandler.class)
        public static void onExceptionExit(@Binding.Throwable RuntimeException ex, @Binding.This Object object,
                @Binding.Class Object clazz) {
            System.err.println("AtExceptionExit, ex:" + ex);
            throw new RuntimeException("exception for ExceptionHandler");
        }
    }
    
    
    @Test
    public void testExecptionExitException() throws Exception {

        TestHelper helper = TestHelper.builder().interceptorClass(ExceptionExitInterceptor.class).methodMatcher("hello")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        System.err.println(Decompiler.decompile(bytes));
        try {
            new Sample().hello("abc", true);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasMessageContaining("test exception");
        }

        assertThat(capture.toString()).contains("AtExceptionExit, ex:");

    }

}
