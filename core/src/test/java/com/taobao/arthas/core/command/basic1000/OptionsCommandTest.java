package com.taobao.arthas.core.command.basic1000;

import org.junit.Assert;
import org.junit.Test;

public class OptionsCommandTest {

    @Test
    public void testValidateObjectSizeLimitOptionValue() {
        Assert.assertEquals("options[object-size-limit] must be greater than 0.",
                OptionsCommand.validateOptionValue("object-size-limit", Integer.valueOf(0)));
        Assert.assertEquals("options[object-size-limit] must be greater than 0.",
                OptionsCommand.validateOptionValue("object-size-limit", Integer.valueOf(-1)));
        Assert.assertNull(OptionsCommand.validateOptionValue("object-size-limit", Integer.valueOf(1)));
        Assert.assertNull(OptionsCommand.validateOptionValue("json-format", Boolean.TRUE));
    }
}
