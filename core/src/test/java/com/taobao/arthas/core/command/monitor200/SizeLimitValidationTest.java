package com.taobao.arthas.core.command.monitor200;

import org.junit.Assert;
import org.junit.Test;

public class SizeLimitValidationTest {

    @Test
    public void testWatchSizeLimitValidation() {
        Assert.assertEquals("sizeLimit must be greater than 0.", WatchCommand.validateSizeLimit(Integer.valueOf(0)));
        Assert.assertEquals("sizeLimit must be greater than 0.", WatchCommand.validateSizeLimit(Integer.valueOf(-1)));
        Assert.assertNull(WatchCommand.validateSizeLimit(Integer.valueOf(1)));
        Assert.assertNull(WatchCommand.validateSizeLimit(null));
    }

    @Test
    public void testTimeTunnelSizeLimitValidation() {
        Assert.assertEquals("sizeLimit must be greater than 0.", TimeTunnelCommand.validateSizeLimit(Integer.valueOf(0)));
        Assert.assertEquals("sizeLimit must be greater than 0.", TimeTunnelCommand.validateSizeLimit(Integer.valueOf(-1)));
        Assert.assertNull(TimeTunnelCommand.validateSizeLimit(Integer.valueOf(1)));
        Assert.assertNull(TimeTunnelCommand.validateSizeLimit(null));
    }
}
