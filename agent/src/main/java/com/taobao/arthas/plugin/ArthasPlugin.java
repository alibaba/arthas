package com.taobao.arthas.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.properties.PropertiesInjectUtil;

/**
 *
 * @author hengyunabc 2019-02-28
 *
 */
public class ArthasPlugin implements Plugin {

    private URL location;

    private ClassLoader parentClassLoader;
    private PlguinClassLoader classLoader;

    private PluginConfig pluginConfig;

    private volatile PluginState state;

    private PluginActivator pluginActivator;

    private PluginContext pluginContext;

    public ArthasPlugin(URL location, Instrumentation instrumentation, ClassLoader parentClassLoader,
                    Properties gobalProperties) throws PluginException {
        this.location = location;
        this.parentClassLoader = parentClassLoader;
        this.state = PluginState.NONE;

        File propertiesFile = new File(location.getFile(), "conf/arthas-plugin.properties");
        Properties properties = new Properties();
        properties.putAll(gobalProperties);

        if (propertiesFile.exists()) {
            try {
                properties.load(new FileInputStream(propertiesFile));
            } catch (IOException e) {
                throw new PluginException("load plugin properties error, directory: " + location.getFile(), e);
            }
        }

        pluginConfig = new PluginConfig();
        PropertiesInjectUtil.inject(properties, pluginConfig);

        this.pluginContext = new PluginContextImpl(this, instrumentation, properties);
    }

    @Override
    public void init() throws PluginException {
        // TODO 支持 jar
        File libDir = new File(location.getFile(), "lib");
        File[] listFiles = libDir.listFiles();
        List<URL> urls = new ArrayList<URL>();
        if (listFiles != null) {
            for (File file : listFiles) {
                if (file.getName().endsWith(".jar")) {
                    try {
                        urls.add(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new PluginException("", e);
                    }
                }
            }
        }

        classLoader = new PlguinClassLoader(urls.toArray(new URL[0]), parentClassLoader);

        try {
            Class<?> activatorClass = classLoader.loadClass(pluginConfig.getPluginActivator());
            pluginActivator = (PluginActivator) activatorClass.newInstance();
            if (!pluginActivator.enabled(pluginContext)) {
                this.state = PluginState.DISABLED;
            }
        } catch (Exception e) {
            throw new PluginException("init plugin error, plugin name: " + pluginConfig.getName(), e);
        }
    }

    @Override
    public void start() {
        try {
            pluginActivator.start(pluginContext);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        try {
            pluginActivator.stop(pluginContext);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void uninstall() {
        // close classloader, 清理资源
        IOUtils.close(this.classLoader);
        this.classLoader = null;
    }

    @Override
    public String name() {
        return this.pluginConfig.getName();
    }

    @Override
    public PluginState state() {
        return this.state;
    }

    public void setState(PluginState state) {
        this.state = state;
    }

    @Override
    public URL location() {
        return location;
    }

}
