package com.taobao.arthas.plugin;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

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

    public PluginManager(Instrumentation instrumentation, Properties properties) {
        this.instrumentation = instrumentation;
        this.properties = properties;
    }

    public void scanPlugins(File dir) throws PluginException {
        File[] files = dir.listFiles();
        if(files != null) {
            for(File file : files) {
                if(!file.isHidden() && file.isDirectory()) {
                    ArthasPlugin plugin = new ArthasPlugin(file, instrumentation, parentClassLoader, properties);
                    plugins.add(plugin);
                }
            }
        }
    }


    public List<Plugin> allPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public void initPlugins() throws PluginException {
        System.out.println("Init available plugins");
        for (Plugin plugin : plugins) {
            System.out.println("Init plugin " + plugin.getName());
            plugin.init();
        }
    }

    public void startPlugins() throws PluginException {
        System.out.println("Starting available plugins");
        for (Plugin plugin : plugins) {
            System.out.println("Start plugin " + plugin.getName());
            if(plugin.state() == PluginState.NONE) {
                plugin.start();
            }
        }
    }

    public void stopPlugins() throws PluginException {
        System.out.println("Stopping available plugins");
        for (Plugin plugin : plugins) {
            System.out.println("Stop plugin " + plugin.getName());
            plugin.stop();
        }
    }

    public Properties properties() {
        return this.properties;
    }

//    private List<Plugin> scanForAvailablePlugins() {
//        System.out.println("Scanning for available plugins in the runtime");
//        List<Plugin> servers = new ArrayList<Plugin>();
//        ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class);
//        for (Plugin plugin : plugins) {
//            servers.add(plugin);
//        }
//        return servers;
//    }
}