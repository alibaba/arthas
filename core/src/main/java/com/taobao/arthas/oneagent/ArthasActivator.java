package com.taobao.arthas.oneagent;

import java.lang.instrument.Instrumentation;

import com.alibaba.oneagent.plugin.PluginActivator;
import com.alibaba.oneagent.plugin.PluginContext;
import com.taobao.arthas.core.server.ArthasBootstrap;

/**
 * 
 * @author hengyunabc 2020-09-18
 *
 */
public class ArthasActivator implements PluginActivator {
    private ArthasBootstrap arthasBootstrap;

    @Override
    public boolean enabled(PluginContext context) {
        return true;
    }

    @Override
    public void init(PluginContext context) throws Exception {
        System.out.println("init ArthasActivator");
    }

    @Override
    public void start(PluginContext context) throws Exception {
        Instrumentation instrumentation = context.getInstrumentation();
        try {
            arthasBootstrap = ArthasBootstrap.getInstance(instrumentation, "");
        } catch (Throwable e) {
            throw new Exception("ArthasActivator start error", e);
        }
    }

    @Override
    public void stop(PluginContext context) throws Exception {
        System.out.println("stop ArthasActivator");
        if (arthasBootstrap != null) {
            arthasBootstrap.destroy();
        }
    }

}
