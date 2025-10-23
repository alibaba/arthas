package com.taobao.arthas.core.command.express;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.model.ExpressTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @Author TaoKan
 * @Date 2025/1/12 5:31 PM
 */
public class QLExpressTest {

    private Express express;

    @BeforeEach
    public void setUp() {
        FlowContext context = new FlowContext();
        Object[] params = new Object[4];
        params[0] = context;
        Advice advice = Advice.newForAfterReturning(null, getClass(), null, null, params, null);
        GlobalOptions.ExpressType = ExpressTypeEnum.QLEXPRESS.getExpressType();
        express = ExpressFactory.unpooledExpress(null).bind(advice).bind("cost", 123);
    }

    @Test
    public void testStringEquals() throws ExpressException {
        String conditionExpress = "\"aaa\".equals(params[0].flowAttribute.getBxApp())";
        boolean result = express.is(conditionExpress);
        assertTrue(result);
    }

    @Test
    public void testObjectEquals() throws ExpressException {
        String conditionExpress = "params[0].flowAttribute.getBxApp().equals(\"aaa\")";
        boolean result = express.is(conditionExpress);
        assertTrue(result);
    }

    @Test
    public void testEqualSign() throws ExpressException {
        String conditionExpress = "\"aaa\" == params[0].flowAttribute.getBxApp()";
        boolean result = express.is(conditionExpress);
        assertTrue(result);
    }
}
