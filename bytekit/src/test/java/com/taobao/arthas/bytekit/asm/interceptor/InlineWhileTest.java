package com.taobao.arthas.bytekit.asm.interceptor;

import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtEnter;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.taobao.arthas.bytekit.utils.Decompiler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * testcase for IINC
 */
public class InlineWhileTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    public static class Sample {

        public int hello(String str, boolean exception) {
            if (exception) {
                throw new RuntimeException("test exception");
            }
            return str.length();
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

        @AtEnter(inline = true,
                suppress = RuntimeException.class,
                suppressHandler = TestPrintSuppressHandler.class
                )
        public static long onEnter(
                @Binding.This Object object, @Binding.Class Object clazz,
               @Binding.MethodName String methodName,
               @Binding.MethodDesc String methodDesc
               ) {
            System.err.println("onEnter, object:" + object);
            System.err.println("onEnter, methodName:" + methodName);
            System.err.println("onEnter, methodDesc:" + methodDesc);

            int i=0;
            while (i++ < 3) {
                System.err.println("enter: "+i);
            }
            return 123L;
        }

        @AtExit(inline = true,
                suppress = RuntimeException.class,
                suppressHandler = TestPrintSuppressHandler.class
                )
        public static void onExit(
                @Binding.This Object object, @Binding.Class Object clazz,
               @Binding.MethodName String methodName,
               @Binding.MethodDesc String methodDesc
               ) {
            System.err.println("onExit, object:" + object);
            System.err.println("onExit, methodName:" + methodName);
            System.err.println("onExit, methodDesc:" + methodDesc);

            int i=0;
            while (i++ < 3) {
                System.err.println("exit: "+i);
            }
        }

        @AtExceptionExit
        public static void onException(@Binding.This Object object, @Binding.Class Object clazz,
                                       @Binding.MethodName String methodName,
                                       @Binding.MethodDesc String methodDesc,
                                       @Binding.Throwable Throwable ex) {
            System.err.println("onException: "+ex);
            int i=0;
            i+=3;
            System.err.println("exception: "+i);
        }
    }



    @Test
    public void test1() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(EnterInterceptor.class).methodMatcher("hello")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        System.err.println(Decompiler.decompile(bytes));

        new Sample().hello("abc", false);

        String actual = capture.toString();
        assertThat(actual).contains("onEnter, object:");
        assertThat(actual).contains("enter: 3");
        assertThat(actual).contains("onExit, object:");
        assertThat(actual).contains("exit: 3");


    }

    @Test
    public void test2() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(EnterInterceptor.class).methodMatcher("hello")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        System.err.println(Decompiler.decompile(bytes));

        try {
            new Sample().hello("abc", true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String actual = capture.toString();
        assertThat(actual).contains("onEnter, object:");
        assertThat(actual).contains("enter: 3");
        assertThat(actual).contains("onException: java.lang.RuntimeException: test exception");
        assertThat(actual).contains("exception: 3");

    }

}
