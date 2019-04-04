package com.alibaba.arthas.plugin;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 *
 * @author hengyunabc 2019-02-27
 *
 */
public class PluginManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    private ClassLoader parentClassLoader = PluginManager.class.getClassLoader();
    private List<Plugin> plugins = new ArrayList<Plugin>();

    private Instrumentation instrumentation;

    private Properties properties;

    private List<URL> scanPluginlLoacations = new ArrayList<URL>();

    public PluginManager(Instrumentation instrumentation, Properties properties, URL scanPluginLocation) {
        this.instrumentation = instrumentation;
        this.properties = properties;
        this.scanPluginlLoacations.add(scanPluginLocation);
    }

    // 可能会执行多次
    synchronized public void scanPlugins() throws PluginException {
        for (URL scanLocation : scanPluginlLoacations) {
            File dir = new File(scanLocation.getFile());
            try {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isHidden() && file.isDirectory()) {
                            ArthasPlugin plugin = new ArthasPlugin(file.toURI().toURL(), instrumentation,
                                            parentClassLoader, properties);
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

        Collections.sort(plugins, new PluginComparator());
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
        if (plugin != null) {
            if (plugin.state() == PluginState.NONE || plugin.state() == PluginState.STOPED) {
                plugin.enabled();
            }
            if (plugin.state() == PluginState.ENABLED) {
                updateState(plugin, PluginState.INITING);
                logger.info("Init plugin, name: {}", plugin.name());
                plugin.init();
                logger.info("Init plugin success, name: {}", plugin.name());
                updateState(plugin, PluginState.INITED);
            }

            if (plugin.state() == PluginState.INITED) {
                updateState(plugin, PluginState.STARTING);
                logger.info("Start plugin, name: {}", plugin.name());
                plugin.start();
                logger.info("Start plugin success, name: {}", plugin.name());
                updateState(plugin, PluginState.STARTED);
            }
        }
    }

    public void uninstallPlugin(String name) {
        Plugin plugin = findPlugin(name);
        if (plugin != null && plugin.state() == PluginState.STOPED) {
            if (plugin instanceof ArthasPlugin) {
                ((ArthasPlugin) plugin).uninstall();
                this.plugins.remove(plugin);
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
        if (plugin != null && (plugin.state() == PluginState.DISABLED || plugin.state() == PluginState.NONE
                        || plugin.state() == PluginState.STOPED)) {
            updateState(plugin, PluginState.ENABLED);
        }
    }

    private void updateState(Plugin plugin, PluginState state) {
        if (plugin instanceof ArthasPlugin) {
            ((ArthasPlugin) plugin).setState(state);
        }
    }

    synchronized public List<Plugin> allPlugins() {
        ArrayList<Plugin> result = new ArrayList<Plugin>(plugins.size());
        result.addAll(plugins);
        return result;
    }

    synchronized public void enablePlugins() {
        for (Plugin plugin : plugins) {
            try {
                plugin.enabled();
            } catch (PluginException e) {
                logger.error("enabled plugin {} error.", plugin.name(), e);
            }
        }
    }

    synchronized public void initPlugins() throws PluginException {
        logger.info("Init available plugins");
        for (Plugin plugin : plugins) {
            if (plugin.state() == PluginState.ENABLED) {
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

    public static class PluginComparator implements Comparator<Plugin> {

        @Override
        public int compare(Plugin p1, Plugin p2) {
            return p1.order() - p2.order();
        }

    }
}