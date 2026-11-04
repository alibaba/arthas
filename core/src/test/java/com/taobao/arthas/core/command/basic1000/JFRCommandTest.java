package com.taobao.arthas.core.command.basic1000;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class JFRCommandTest {

    @Test
    public void testParseTimespan() throws Exception {
        JFRCommand command = new JFRCommand();

        Assert.assertEquals(TimeUnit.SECONDS.toNanos(60), command.parseTimespan("60"));
        Assert.assertEquals(TimeUnit.SECONDS.toNanos(60), command.parseTimespan("60s"));
        Assert.assertEquals(TimeUnit.SECONDS.toNanos(120), command.parseTimespan("2m"));
        Assert.assertEquals(TimeUnit.SECONDS.toNanos(3600), command.parseTimespan("1h"));
        Assert.assertEquals(TimeUnit.SECONDS.toNanos(86400), command.parseTimespan("1d"));
    }
}
