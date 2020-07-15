 package com.taobao.arthas.core.spi.impl;

import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.spi.ServerConnectorMetricsProvider;

/**
 * ServerConnectorMetricsProvider 新旧版本兼容实现：既兼容现有阿里系IT资产的原模式, 又适用于非阿里系的其它TOMCAT环境
 * 还可通过环境变量ARTHAS_SERVER_CONNNECTOR_SPI来指定新SPI实现类, 以适配其它应用服务器
 * 注意环境变量要求在被监控的目标jvm上生效才有效
 * @author qxo
 * @date 2020/04/04
 */
public class ServerConnectorMetricsProvider4default implements ServerConnectorMetricsProvider {

    private static final String ARTHAS_SERVER_CONNNECTOR_SPI = "ARTHAS_SERVER_CONNNECTOR_SPI";

    private static final Logger logger = LoggerFactory.getLogger(ServerConnectorMetricsProvider4TomcatJmx.class);

    private final ServerConnectorMetricsProvider target;

    public ServerConnectorMetricsProvider4default() {
        super();
        String spi = System.getenv(ARTHAS_SERVER_CONNNECTOR_SPI);
        if (logger.isInfoEnabled()) {
            logger.info(ARTHAS_SERVER_CONNNECTOR_SPI + ":" + spi);
        }
        ServerConnectorMetricsProvider impl = null;
        if (spi == null || spi.isEmpty() || "old".equals(spi)) {
            impl = new ServerConnectorMetricsProvider4AliTomcat();
            if (!impl.isMetricOn()) {
                impl = new ServerConnectorMetricsProvider4TomcatJmx();
            }
        } else if ("new".equals(spi)) {
            impl = new ServerConnectorMetricsProvider4TomcatJmx();
        } else {
            final ClassLoader loader = ServerConnectorMetricsProvider.class.getClassLoader();
            try {
                impl = (ServerConnectorMetricsProvider)loader.loadClass(spi).newInstance();
            } catch (InstantiationException e) {
               logError(e);
            } catch (IllegalAccessException e) {
                logError(e);
            } catch (ClassNotFoundException e) {
                logError(e);
            }
        }
        target = impl == null ? new ServerConnectorMetricsProvider4TomcatJmx() : impl;
    }

    private static void logError(Exception e) {
        logger.warn("can not load ARTHAS_SERVER_CONNNECTOR_SPI: {} cause: {}"
                   , ARTHAS_SERVER_CONNNECTOR_SPI, e, e);
    }

    @Override
    public boolean isMetricOn() {
        return target.isMetricOn();
    }

    @Override
    public List<JSONObject> getConnectorStats() {
        return target.getConnectorStats();
    }

    @Override
    public List<JSONObject> getThreadPoolInfos() {
        return target.getThreadPoolInfos();
    }

}
