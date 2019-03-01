package com.taobao.arthas.plugin;

import java.lang.instrument.Instrumentation;
import java.util.Properties;

/**
 *
 * @author hengyunabc 2019-03-01
 *
 */
public class PluginContextImpl implements PluginContext {

    private Plugin plugin;

    private Properties properties;

    private Instrumentation instrumentation;

    public PluginContextImpl(Plugin plugin, Instrumentation instrumentation, Properties properties) {
        this.plugin = plugin;
        this.instrumentation = instrumentation;
        this.properties = properties;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

}
