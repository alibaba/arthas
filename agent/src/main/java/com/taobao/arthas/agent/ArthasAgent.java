package com.taobao.arthas.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.util.Map;
import java.util.Properties;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.arthas.plugin.PluginManager;
import com.taobao.arthas.common.FeatureCodec;

/**
 *
 * @author hengyunabc 2019-03-01
 *
 */
public class ArthasAgent {
    private static final String defaultLoggerConfigurationFileProperty = "arthas.logback.configurationFile";
    private static final String defaultLoggerConfigurationFile = "logback-arthas.xml";

    private static volatile ArthasAgent instance;
    private final static FeatureCodec codec = new FeatureCodec(';', '=');

    private PluginManager pluginManager;

    private Logger logger;

    public static void premain(String args, Instrumentation inst) {
        main(true, args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    private static synchronized void main(final String args, final Instrumentation inst) {
        main(false, args, inst);
    }

    private static synchronized void main(boolean premain, final String args, final Instrumentation inst) {
        if (instance == null) {
            synchronized (ArthasAgent.class) {
                if (instance == null) {
                    ArthasAgent temp = new ArthasAgent();
                    temp.init(premain, args, inst);
                    instance = temp;
                }
            }
        }
    }

    public static ArthasAgent getInstance() {
        return instance;
    }

    public PluginManager pluginMaanger() {
        return pluginManager;
    }

    private void initLogger() {
        String arthasLoggerConfiguration = System.getProperty(defaultLoggerConfigurationFileProperty);
        if (arthasLoggerConfiguration == null || arthasLoggerConfiguration.trim().isEmpty()) {
            System.setProperty(defaultLoggerConfigurationFileProperty, defaultLoggerConfigurationFile);
        }
        if (logger == null) {
            logger = LoggerFactory.getLogger("arthas");
        }

    }

    private void init(boolean premain, final String args, final Instrumentation inst) {
        initLogger();

        Map<String, String> map = codec.toMap(args);

        String arthasHome = map.get("arthas.home");

        if (arthasHome == null) {
            CodeSource codeSource = ArthasAgent.class.getProtectionDomain().getCodeSource();
            URL agentJarLocation = codeSource.getLocation();
            arthasHome = new File(agentJarLocation.getFile()).getParent();
            map.put("arthas.home", arthasHome);
        }

        logger.info("arthas home: " + map.get("arthas.home"));

        Properties properties = new Properties();
        properties.putAll(map);

        logger.debug("PluginManager properties: {}", properties);
        try {
            pluginManager = new PluginManager(inst, properties, new File(arthasHome, "plugins").toURI().toURL());

            pluginManager.scanPlugins();

            pluginManager.enablePlugins();

            pluginManager.initPlugins();

            pluginManager.startPlugins();

        } catch (Exception e) {
            logger.error("PluginManager error", e);
            e.printStackTrace();
        }
    }

}
