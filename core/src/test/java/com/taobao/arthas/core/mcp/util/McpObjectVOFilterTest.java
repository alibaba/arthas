package com.taobao.arthas.core.mcp.util;

import com.taobao.arthas.core.command.model.ObjectVO;
import org.junit.Assert;
import org.junit.Test;

public class McpObjectVOFilterTest {

    @Test
    public void shouldUseRenderedValueBeforeObjectValue() {
        ObjectVO objectVO = new ObjectVO(null, 2);
        objectVO.setRenderedValue("@String[snapshot]");

        Object result = new McpObjectVOFilter().apply(null, "value", objectVO);

        Assert.assertEquals("@String[snapshot]", result);
    }
}
