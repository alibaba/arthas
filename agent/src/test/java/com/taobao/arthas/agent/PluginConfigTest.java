package com.taobao.arthas.agent;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.arthas.plugin.PluginConfig;
import com.taobao.arthas.common.properties.PropertiesInjectUtil;

public class PluginConfigTest {


    @Test
    public void test() {
        Properties p = new Properties();
        p.put("order", "100");
        p.put("pluginActivator", "com.test.Demo");

        PluginConfig config = new PluginConfig();

        PropertiesInjectUtil.inject(p, config);

        Assert.assertEquals(config.getOrder(), 100);
        Assert.assertEquals(config.getPluginActivator(), "com.test.Demo");

    }

}
