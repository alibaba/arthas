package com.taobao.arthas.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.rule.OutputCapture;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtEnter;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.utils.Decompiler;

public class AtEnterTest {

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
            e.printStackTrace();
        }
    }

    public static class EnterInterceptor {

        @AtEnter(inline = true
                , suppress = RuntimeException.class, suppressHandler = TestPrintSuppressHandler.class
                )
        public static long onEnter(
                @Binding.This Object object, @Binding.Class Object clazz,
               @Binding.Field(name = "longField") long longField,
               @Binding.Field(name = "longField") Object longFieldObject,
               @Binding.Field(name = "intField") int intField,
               @Binding.Field(name = "strField") String strField,
               @Binding.Field(name = "intField") Object intFielObject,
               @Binding.MethodName String methodName,
               @Binding.MethodDesc String methodDesc
               ) {
            System.err.println("onEnter, object:" + object);
            System.err.println("onEnter, methodName:" + methodName);
            System.err.println("onEnter, methodDesc:" + methodDesc);
            return 123L;
        }

    }



    @Test
    public void testEnter() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(EnterInterceptor.class).methodMatcher("hello")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().hello("abc", false);

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("onEnter, object:");
    }

}
