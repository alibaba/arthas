package com.taobao.arthas.plugin;

public interface PluginActivator {

    //让插件本身判断是否要启动
    boolean enabled(PluginContext context);

    /**
     * Before calling this method, the {@link PluginState} is
     * {@link PluginState#STARTING}, after calling, the {@link PluginState} is
     * {@link PluginState#ACTIVE}
     *
     * @param context
     */
    public void start(PluginContext context) throws Exception;

    /**
     * Before calling this method, the {@link PluginState} is
     * {@link PluginState#STOPPING}, after calling, the {@link PluginState} is
     * {@link PluginState#RESOLVED}
     *
     * @param context
     */
    public void stop(PluginContext context) throws Exception;
}
