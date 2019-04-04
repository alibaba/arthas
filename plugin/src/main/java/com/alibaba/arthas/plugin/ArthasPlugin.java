package com.alibaba.arthas.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.properties.PropertiesInjectUtil;

/**
 *
 * @author hengyunabc 2019-02-28
 *
 */
public class ArthasPlugin implements Plugin {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    public static final int DEFAULT_ORDER = 1000;

    private URL location;

    private ClassLoader parentClassLoader;
    private PlguinClassLoader classLoader;

    private PluginConfig pluginConfig;

    private volatile PluginState state;

    private PluginActivator pluginActivator;

    private PluginContext pluginContext;

    public ArthasPlugin(URL location, Instrumentation instrumentation, ClassLoader parentClassLoader,
                    Properties gobalProperties) throws PluginException {
        this(location, Collections.<URL>emptySet(), instrumentation, parentClassLoader, gobalProperties);
    }

    public ArthasPlugin(URL location, Set<URL> extraURLs, Instrumentation instrumentation,
                    ClassLoader parentClassLoader, Properties gobalProperties) throws PluginException {

        this.location = location;
        this.parentClassLoader = parentClassLoader;
        this.state = PluginState.NONE;

        List<URL> urls = new ArrayList<URL>();
        urls.addAll(extraURLs);
        urls.addAll(scanPluginUrls());

        classLoader = new PlguinClassLoader(urls.toArray(new URL[0]), parentClassLoader);

        URL pluginPropertiesURL = classLoader.getResource("arthas-plugin.properties");

        Properties properties = new Properties();
        properties.putAll(gobalProperties);
        if (pluginPropertiesURL != null) {
            try {
                properties.load(pluginPropertiesURL.openStream());
            } catch (IOException e) {
                throw new PluginException("load plugin properties error, url: " + pluginPropertiesURL, e);
            }
        }

        pluginConfig = new PluginConfig();
        PropertiesInjectUtil.inject(properties, pluginConfig);

        this.pluginContext = new PluginContextImpl(this, instrumentation, properties);
    }

    @Override
    public boolean enabled() throws PluginException {
        boolean enabled = false;
        try {
            Class<?> activatorClass = classLoader.loadClass(pluginConfig.getPluginActivator());
            pluginActivator = (PluginActivator) activatorClass.newInstance();
            enabled = pluginActivator.enabled(pluginContext);
            if (enabled) {
                this.state = PluginState.ENABLED;
            } else {
                this.state = PluginState.DISABLED;
                logger.info("plugin {} disabled.", this.pluginConfig.getName());
            }

        } catch (Throwable e) {
            this.state = PluginState.ERROR;
            throw new PluginException("check enabled plugin error, plugin name: " + pluginConfig.getName(), e);
        }
        return enabled;
    }

    @Override
    public void init() throws PluginException {
        try {
            pluginActivator.init(pluginContext);
        } catch (Throwable e) {
            this.state = PluginState.ERROR;
            throw new PluginException("init plugin error, plugin name: " + pluginConfig.getName(), e);
        }
    }

    @Override
    public void start() throws PluginException {
        try {
            pluginActivator.start(pluginContext);
        } catch (Throwable e) {
            this.state = PluginState.ERROR;
            throw new PluginException("start plugin error, plugin name: " + pluginConfig.getName(), e);
        }
    }

    @Override
    public void stop() throws PluginException {
        try {
            pluginActivator.stop(pluginContext);
        } catch (Throwable e) {
            this.state = PluginState.ERROR;
            throw new PluginException("stop plugin error, plugin name: " + pluginConfig.getName(), e);
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

    @Override
    public int order() {
        return pluginConfig.getOrder();
    }

    private List<URL> scanPluginUrls() throws PluginException {
        File libDir = new File(location.getFile(), "lib");
        File[] listFiles = libDir.listFiles();
        List<URL> urls = new ArrayList<URL>();
        try {
            if (listFiles != null) {
                for (File file : listFiles) {
                    if (file.getName().endsWith(".jar")) {

                        urls.add(file.toURI().toURL());

                    }
                }
            }

            File confDir = new File(location.getFile(), "conf");
            if (confDir.isDirectory()) {
                urls.add(confDir.toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new PluginException("", e);
        }

        return urls;
    }

}
