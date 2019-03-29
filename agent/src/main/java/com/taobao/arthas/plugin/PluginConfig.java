package com.taobao.arthas.plugin;

/**
 *
 * @author hengyunabc 2019-02-27
 *
 */
public class PluginConfig {

    private String version;
    private String name;
    private String pluginActivator;

    private int order;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getPluginActivator() {
        return pluginActivator;
    }

    public void setPluginActivator(String pluginActivator) {
        this.pluginActivator = pluginActivator;
    }

}
