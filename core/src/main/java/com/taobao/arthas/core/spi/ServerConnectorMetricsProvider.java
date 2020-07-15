package com.taobao.arthas.core.spi;

import java.util.List;

import com.alibaba.fastjson.JSONObject;

/**
 * SPI for Server Connector metrics
 * 
 * @author qxo
 * @date 2020/01/31
 */
public interface ServerConnectorMetricsProvider {

    /**
     * @return true if metric enabled
     */
    public boolean isMetricOn();

    public List<JSONObject> getConnectorStats();

    public List<JSONObject> getThreadPoolInfos();
}
