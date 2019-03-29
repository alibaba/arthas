package com.taobao.arthas.plugin;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 *
 * @author hengyunabc 2019-02-27
 *
 */
public class PluginManager {
    private ClassLoader parentClassLoader = PluginManager.class.getClassLoader();
    private List<Plugin> plugins = new CopyOnWriteArrayList<Plugin>();

    private Instrumentation instrumentation;

    private Properties properties;

    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    public PluginManager(Instrumentation instrumentation, Properties properties) {
        this.instrumentation = instrumentation;
        this.properties = properties;
    }

    // 可能会执行多次
    synchronized public void scanPlugins(File dir) throws PluginException {
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isHidden() && file.isDirectory()) {
                        ArthasPlugin plugin = new ArthasPlugin(file.toURI().toURL(), instrumentation, parentClassLoader, properties);
                        if (!containsPlugin(plugin.name())) {
                            plugins.add(plugin);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new PluginException("scan plugins error.", e);
        }

    }

    synchronized public boolean containsPlugin(String name) {
        for (Plugin plugin : plugins) {
            if (plugin.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Plugin findPlugin(String name) {
        for (Plugin plugin : plugins) {
            if (plugin.name().equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    public void startPlugin(String name) throws PluginException {
        Plugin plugin = findPlugin(name);
        if (plugin != null && (plugin.state() == PluginState.NONE || plugin.state() == PluginState.STOPED)) {
            updateState(plugin, PluginState.INITING);
            logger.info("Init plugin, name: {}", plugin.name());
            plugin.init();
            logger.info("Init plugin success, name: {}", plugin.name());
            updateState(plugin, PluginState.INITED);
        }
        if (plugin != null && plugin.state() == PluginState.INITED) {
            updateState(plugin, PluginState.STARTING);
            logger.info("Start plugin, name: {}", plugin.name());
            plugin.start();
            logger.info("Start plugin success, name: {}", plugin.name());
            updateState(plugin, PluginState.STARTED);
        }
    }

    public void uninstallPlugin(String name) {
        Plugin plugin = findPlugin(name);
        if (plugin != null && plugin.state() == PluginState.STOPED) {
            if (plugin instanceof ArthasPlugin) {
                ((ArthasPlugin) plugin).uninstall();
            }
        }
    }

    public void stopPlugin(String name) throws PluginException {
        Plugin plugin = findPlugin(name);
        if (plugin != null && plugin.state() == PluginState.STARTED) {
            updateState(plugin, PluginState.STOPPING);
            logger.info("Stop plugin, name: {}", plugin.name());
            plugin.stop();
            logger.info("Stop plugin success, name: {}", plugin.name());
            updateState(plugin, PluginState.STOPED);
        }
    }

    public void enablePlugin(String name) {
        Plugin plugin = findPlugin(name);
        if (plugin != null && plugin.state() == PluginState.DISABLED) {
            updateState(plugin, PluginState.NONE);
        }
    }

    private void updateState(Plugin plugin, PluginState state) {
        if (plugin instanceof ArthasPlugin) {
            ((ArthasPlugin) plugin).setState(state);
        }
    }

    synchronized public List<Plugin> allPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    synchronized public void initPlugins() throws PluginException {
        logger.info("Init available plugins");
        for (Plugin plugin : plugins) {
            if (plugin.state() == PluginState.NONE) {
                updateState(plugin, PluginState.INITING);
                logger.info("Init plugin, name: {}", plugin.name());
                plugin.init();
                logger.info("Init plugin success, name: {}", plugin.name());
                updateState(plugin, PluginState.INITED);
            } else {
                logger.debug("skip init plugin, name: {}, state: {}", plugin.name(), plugin.state());
            }
        }
    }

    synchronized public void startPlugins() throws PluginException {
        logger.info("Starting available plugins");
        for (Plugin plugin : plugins) {
            if (plugin.state() == PluginState.INITED) {
                updateState(plugin, PluginState.STARTING);
                logger.info("Start plugin, name: {}", plugin.name());
                plugin.start();
                logger.info("Start plugin success, name: {}", plugin.name());
                updateState(plugin, PluginState.STARTED);
            } else {
                logger.debug("skip start plugin, name: {}, state: {}", plugin.name(), plugin.state());
            }
        }
    }

    synchronized public void stopPlugins() throws PluginException {
        logger.info("Stopping available plugins");
        for (Plugin plugin : plugins) {
            if (plugin.state() == PluginState.STARTED) {
                updateState(plugin, PluginState.STOPPING);
                logger.info("Stop plugin, name: {}", plugin.name());
                plugin.stop();
                logger.info("Stop plugin success, name: {}", plugin.name());
                updateState(plugin, PluginState.STOPED);
            } else {
                logger.debug("skip stop plugin, name: {}, state: {}", plugin.name(), plugin.state());
            }

        }
    }

    public Properties properties() {
        return this.properties;
    }
}