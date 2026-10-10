package com.taobao.arthas.core.command.express;

import org.junit.Assert;
import org.junit.Test;

public class OgnlExpressTest {

    @Test
    public void testValidOgnlExpr1() throws ExpressException {
        Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
        Assert.assertEquals(unpooledExpress.get("\"test\".length() % 2 == 0 ? \"even length\" : \"odd length\""),
                "even length");
    }

    @Test
    public void testValidOgnlExpr2() throws ExpressException {
        System.setProperty("ognl.chain.short-circuit", String.valueOf(false));
        Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
        Assert.assertEquals(unpooledExpress.get("4 in {1, 2, 3, 4}"), true);
        Assert.assertEquals(unpooledExpress.get("{1, 2, 3, 4}.{^ #this % 2 == 0}[$]"), 2);
        Assert.assertEquals(unpooledExpress.get("{1, 2, 3, 4}.{? #this % 2 == 0}[$]"), 4);
    }

    @Test
    public void testValidOgnlExpr3() throws ExpressException {
        Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
        Assert.assertEquals(unpooledExpress.get("#factorial = :[#this <= 1 ? 1 : #this * #factorial(#this - 1)], #factorial(5)"),
                120);
    }

    @Test
    public void testValidOgnlExpr4() throws ExpressException {
        Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
        System.setProperty("arthas.test1", "arthas");
        System.setProperty("arthas.ognl.test2", "test");
        Assert.assertEquals(unpooledExpress.get("#value1=@System@getProperty(\"arthas.test1\")," +
                        "#value2=@System@getProperty(\"arthas.ognl.test2\"), {#value1, #value2}").toString(),
                "[arthas, test]");
        System.clearProperty("arthas.test1");
        System.clearProperty("arthas.ognl.test2");
    }

    @Test
    public void testInvalidOgnlExpr() {
        try {
            Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
            System.out.println(unpooledExpress.get("#value1=@System.getProperty(\"java.home\")," +
                            "#value2=@System@getProperty(\"java.runtime.name\"), {#value1, #value2}").toString());
        } catch (Exception e){
            Assert.assertTrue(e.getCause() instanceof ognl.ExpressionSyntaxException);
        }
    }

    @Test
    public void testNullSourcePropertyAccess() throws ExpressException {
        // Test accessing property on null object - should return null instead of throwing exception
        Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
        
        // Create a test object with null field
        TestObject testObj = new TestObject();
        testObj.nullField = null;
        
        // This should not throw "source is null for getProperty" exception
        Object result = unpooledExpress.bind(testObj).get("nullField.someProperty");
        Assert.assertNull(result);
    }

    @Test
    public void testNullArrayAccess() throws ExpressException {
        // Test accessing index on null array - should return null instead of throwing exception
        Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
        
        // Create a test object with null array
        TestObject testObj = new TestObject();
        testObj.nullArray = null;
        
        // This should not throw "source is null for getProperty" exception
        Object result = unpooledExpress.bind(testObj).get("nullArray[2]");
        Assert.assertNull(result);
    }

    @Test
    public void testAdviceWithNullParams() throws ExpressException {
        // Simulate the actual scenario from the issue: accessing params[2] when params is null or short
        Express unpooledExpress = ExpressFactory.unpooledExpress(OgnlExpressTest.class.getClassLoader());
        
        // Simulate Advice object with null params
        TestAdvice advice1 = new TestAdvice();
        advice1.params = null;
        
        // Accessing params[2] should return null instead of throwing exception
        Object result1 = unpooledExpress.bind(advice1).get("params[2]");
        Assert.assertNull(result1);
        
        // Simulate Advice object with params array where element is null
        TestAdvice advice2 = new TestAdvice();
        advice2.params = new Object[]{null, null, null};
        
        // Accessing params[2].someField should return null instead of throwing exception
        Object result2 = unpooledExpress.bind(advice2).get("params[2].someField");
        Assert.assertNull(result2);
        
        // Test with short array
        TestAdvice advice3 = new TestAdvice();
        advice3.params = new Object[]{1};
        
        // Accessing params[2] on short array should work normally (OGNL handles array bounds)
        try {
            unpooledExpress.bind(advice3).get("params[2]");
        } catch (ExpressException e) {
            // ArrayIndexOutOfBoundsException is expected and should not be suppressed
            Assert.assertTrue(e.getCause() instanceof ArrayIndexOutOfBoundsException ||
                    e.getMessage().contains("ArrayIndexOutOfBoundsException"));
        }
    }

    // Helper class for testing
    public static class TestObject {
        public Object nullField;
        public Object[] nullArray;
    }

    // Helper class to simulate Advice object
    public static class TestAdvice {
        public Object[] params;
    }
}
