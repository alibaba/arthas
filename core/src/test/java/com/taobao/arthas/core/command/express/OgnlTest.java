package com.taobao.arthas.core.command.express;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.taobao.arthas.core.advisor.Advice;
import ognl.OgnlException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * https://github.com/alibaba/arthas/issues/2954
 */
public class OgnlTest {

    private Express express;

    @BeforeEach
    public void setUp() throws OgnlException, ExpressException {
        FlowContext context = new FlowContext();
        Object[] params = new Object[4];
        params[0] = context;
        Advice advice = Advice.newForAfterReturning(null, getClass(), null, null, params, null);
        express = ExpressFactory.unpooledExpress(null).bind(advice).bind("cost", 123);
    }

    @Test
    public void testStringEquals() throws OgnlException, ExpressException {
        String conditionExpress = "\"aaa\".equals(params[0].flowAttribute.getBxApp())";
        boolean result = express.is(conditionExpress);
        assertTrue(result);
    }

    @Test
    public void testObjectEquals() throws OgnlException, ExpressException {
        String conditionExpress = "params[0].flowAttribute.getBxApp().equals(\"aaa\")";
        boolean result = express.is(conditionExpress);
        assertTrue(result);
    }

    @Test
    public void testEqualSign() throws OgnlException, ExpressException {
        String conditionExpress = "\"aaa\" == params[0].flowAttribute.getBxApp()";
        boolean result = express.is(conditionExpress);
        assertTrue(result);
    }
}
