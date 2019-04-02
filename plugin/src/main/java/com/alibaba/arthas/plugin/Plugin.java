package com.alibaba.arthas.plugin;

import java.net.URL;

/**
 *
 * @author hengyunabc 2019-02-27
 *
 */
public interface Plugin {

    boolean enabled() throws PluginException;

    void init() throws PluginException;

    void start() throws PluginException;

    void stop() throws PluginException;

    int order();

    PluginState state();

    String name();

    URL location();
}
