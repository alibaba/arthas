package com.taobao.arthas.core.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.taobao.arthas.common.JavaVersionUtils;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeRenderUtilsTest {


    public class TestClass implements Serializable {
        private int testField;
        public char anotherTestField;

        public int testMethod(int i, boolean b) {
            return 0;
        }

        public void anotherTestMethod() throws NullPointerException {

        }
    }

    @Test
    public void testDrawInterface() {
        if (JavaVersionUtils.isGreaterThanJava11()) {
            Assertions.assertThat(TypeRenderUtils.drawInterface(String.class)).isEqualTo(
                    "java.io.Serializable,java.lang.Comparable,java.lang.CharSequence,java.lang.constant.Constable,java.lang.constant.ConstantDesc");
        } else {
            Assertions.assertThat(TypeRenderUtils.drawInterface(String.class))
                    .isEqualTo("java.io.Serializable,java.lang.Comparable,java.lang.CharSequence");
        }
        
        assertThat(TypeRenderUtils.drawInterface(TestClass.class), is(equalTo("java.io.Serializable")));
        assertThat(TypeRenderUtils.drawInterface(Serializable.class), is(equalTo("")));
    }

    @Test
    public void testDrawParametersForMethod() throws NoSuchMethodException {
        Class[] classesOfParameters = new Class[2];
        classesOfParameters[0] = int.class;
        classesOfParameters[1] = boolean.class;

        assertThat(TypeRenderUtils.drawParameters(TestClass.class.getMethod("testMethod", classesOfParameters)), is(equalTo("int\nboolean")));
        assertThat(TypeRenderUtils.drawParameters(TestClass.class.getMethod("anotherTestMethod")), is(equalTo("")));

        assertThat(TypeRenderUtils.drawParameters(String.class.getMethod("charAt", int.class)), is(equalTo("int")));
        assertThat(TypeRenderUtils.drawParameters(String.class.getMethod("isEmpty")), is(equalTo("")));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testDrawParametersForMethodThrowsException() throws NoSuchMethodException {
        assertThat(TypeRenderUtils.drawParameters(TestClass.class.getMethod("method")), is(equalTo("")));
    }

    @Test
    public void testDrawParametersForConstructor() throws NoSuchMethodException {
        Class[] classesOfParameters = new Class[3];
        classesOfParameters[0] = char[].class;
        classesOfParameters[1] = int.class;
        classesOfParameters[2] = int.class;

        assertThat(TypeRenderUtils.drawParameters(String.class.getConstructor(classesOfParameters)), is(equalTo("[]\nint\nint")));
        assertThat(TypeRenderUtils.drawParameters(String.class.getConstructor()), is(equalTo("")));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testDrawParametersForConstructorThrowsException() throws NoSuchMethodException {
        assertThat(TypeRenderUtils.drawParameters(TestClass.class.getConstructor()), is(equalTo("")));
    }

    @Test
    public void testDrawReturn() throws NoSuchMethodException {
        Class[] classesOfParameters = new Class[2];
        classesOfParameters[0] = int.class;
        classesOfParameters[1] = boolean.class;

        assertThat(TypeRenderUtils.drawReturn(TestClass.class.getMethod("testMethod", classesOfParameters)), is(equalTo("int")));
        assertThat(TypeRenderUtils.drawReturn(TestClass.class.getMethod("anotherTestMethod")), is(equalTo("void")));

        assertThat(TypeRenderUtils.drawReturn(String.class.getMethod("isEmpty")), is(equalTo("boolean")));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testDrawReturnThrowsException() throws NoSuchMethodException {
        assertThat(TypeRenderUtils.drawReturn(TestClass.class.getMethod("method")), is(equalTo("")));
    }

    @Test
    public void testDrawExceptionsForMethod() throws NoSuchMethodException {
        Class[] classesOfParameters = new Class[2];
        classesOfParameters[0] = int.class;
        classesOfParameters[1] = boolean.class;

        assertThat(TypeRenderUtils.drawExceptions(TestClass.class.getMethod("testMethod", classesOfParameters)), is(equalTo("")));
        assertThat(TypeRenderUtils.drawExceptions(TestClass.class.getMethod("anotherTestMethod")), is(equalTo("java.lang.NullPointerException")));

        assertThat(TypeRenderUtils.drawExceptions(String.class.getMethod("getBytes", String.class)), is(equalTo("java.io.UnsupportedEncodingException")));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testDrawExceptionsForMethodThrowsException() throws NoSuchMethodException {
        assertThat(TypeRenderUtils.drawExceptions(TestClass.class.getMethod("method")), is(equalTo("")));
    }

    @Test
    public void testDrawExceptionsForConstructor() throws NoSuchMethodException {
        Class[] classesOfConstructorParameters = new Class[2];
        classesOfConstructorParameters[0] = byte[].class;
        classesOfConstructorParameters[1] = String.class;

        assertThat(TypeRenderUtils.drawExceptions(String.class.getConstructor()), is(equalTo("")));
        assertThat(TypeRenderUtils.drawExceptions(String.class.getConstructor(classesOfConstructorParameters)), is(equalTo("java.io.UnsupportedEncodingException")));
    }

    @Test(expected = NoSuchMethodException.class)
    public void testDrawExceptionsForConstructorThrowsException() throws NoSuchMethodException {
        assertThat(TypeRenderUtils.drawExceptions(TestClass.class.getConstructor()), is(equalTo("")));
    }

}