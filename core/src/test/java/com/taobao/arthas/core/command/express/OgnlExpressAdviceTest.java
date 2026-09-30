package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.util.Constants;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration test for OGNL expression evaluation with Advice objects
 * Tests the fix for issue "source is null for getProperty(null, "2")"
 */
public class OgnlExpressAdviceTest {

    @Test
    public void testConditionExpressionWithNullParams() throws ExpressException {
        // Simulate the scenario from the issue where params might be null or contain null elements
        Express express = ExpressFactory.unpooledExpress(OgnlExpressAdviceTest.class.getClassLoader());
        
        // Create Advice with null params
        Advice advice = createTestAdvice(null, null, null);
        
        // Bind advice and test various condition expressions that would previously throw exceptions
        express.bind(advice).bind(Constants.COST_VARIABLE, 100.0);
        
        // Test 1: Access params[2] when params is null
        Object result1 = express.get("params[2]");
        Assert.assertNull("Accessing params[2] when params is null should return null", result1);
        
        // Test 2: Access params[2].someField when params is null
        Object result2 = express.get("params[2].field");
        Assert.assertNull("Accessing params[2].field when params is null should return null", result2);
        
        // Test 3: Condition expression that checks if params[2] is not null
        // When params is null, params[2] returns null, so we check that directly
        Object paramsElement = express.get("params[2]");
        Assert.assertNull("params[2] should be null when params is null", paramsElement);
    }

    @Test
    public void testConditionExpressionWithNullParamElement() throws ExpressException {
        // Test when params array exists but contains null element
        Express express = ExpressFactory.unpooledExpress(OgnlExpressAdviceTest.class.getClassLoader());
        
        Object[] params = new Object[]{new TestObject("arg0"), null, new TestObject("arg2")};
        Advice advice = createTestAdvice(null, params, null);
        
        express.bind(advice).bind(Constants.COST_VARIABLE, 100.0);
        
        // Test: Access params[1].field when params[1] is null
        Object result = express.get("params[1].field");
        Assert.assertNull("Accessing params[1].field when params[1] is null should return null", result);
        
        // Test: Access params[0].field when params[0] is not null
        Object result2 = express.get("params[0].field");
        Assert.assertEquals("Should get field value from non-null param", "arg0", result2);
    }

    @Test
    public void testConditionExpressionWithNullReturnObj() throws ExpressException {
        // Test when returnObj is null
        Express express = ExpressFactory.unpooledExpress(OgnlExpressAdviceTest.class.getClassLoader());
        
        Advice advice = createTestAdvice(null, new Object[]{}, null);
        
        express.bind(advice).bind(Constants.COST_VARIABLE, 100.0);
        
        // Test: Access returnObj.field when returnObj is null
        Object result = express.get("returnObj.someField");
        Assert.assertNull("Accessing returnObj.someField when returnObj is null should return null", result);
    }

    @Test
    public void testComplexConditionExpressionWithNullHandling() throws ExpressException {
        // Test complex condition expressions that involve null checks
        Express express = ExpressFactory.unpooledExpress(OgnlExpressAdviceTest.class.getClassLoader());
        
        Object[] params = new Object[]{null, new TestObject("value")};
        Advice advice = createTestAdvice(null, params, null);
        
        express.bind(advice).bind(Constants.COST_VARIABLE, 150.0);
        
        // Test: Complex expression with null-safe navigation
        // This previously would throw "source is null for getProperty" exception
        boolean result1 = express.is("params[0] == null || params[0].field == null");
        Assert.assertTrue("Expression with null check should work", result1);
        
        boolean result2 = express.is("params[1] != null && params[1].field != null");
        Assert.assertTrue("Expression checking non-null param should work", result2);
        
        boolean result3 = express.is("#cost > 100");
        Assert.assertTrue("Cost comparison should work", result3);
    }

    // Helper method to create test Advice
    private Advice createTestAdvice(Object target, Object[] params, Object returnObj) {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> clazz = getClass();
        ArthasMethod method = new ArthasMethod(clazz, "testMethod", "()V");
        return Advice.newForAfterReturning(loader, clazz, method, target, params, returnObj);
    }

    // Helper class for testing
    public static class TestObject {
        public String field;
        
        public TestObject(String field) {
            this.field = field;
        }
    }
}
