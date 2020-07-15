package com.taobao.arthas.core.spi.impl;

import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.spi.ServerConnectorMetricsProvider;
import com.taobao.arthas.core.util.NetUtils;
import com.taobao.arthas.core.util.NetUtils.Response;

/**
 * arthas之前默认的实现, 其依赖于特定8006端口的http服务实现监控jvm
 * 
 * @author qxo
 * @date 2020/01/31
 */
public class ServerConnectorMetricsProvider4AliTomcat implements ServerConnectorMetricsProvider {

    private final String connectorStatPath = "http://localhost:8006/connector/stats";

    private final String threadPoolPath = "http://localhost:8006/connector/threadpool";

    @Override
    public boolean isMetricOn() {
        return NetUtils.request("http://localhost:8006").isSuccess();
    }

    @Override
    public List<JSONObject> getConnectorStats() {
        final Response connectorStatResponse = NetUtils.request(connectorStatPath);
        if (connectorStatResponse.isSuccess()) {
            List<JSONObject> connectorStats = JSON.parseArray(connectorStatResponse.getContent(), JSONObject.class);
            return connectorStats;
        }
        return null;
    }

    @Override
    public List<JSONObject> getThreadPoolInfos() {
        Response threadPoolResponse = NetUtils.request(threadPoolPath);
        if (threadPoolResponse.isSuccess()) {
            List<JSONObject> threadPoolInfos = JSON.parseArray(threadPoolResponse.getContent(), JSONObject.class);
            return threadPoolInfos;
        }
        return null;
    }

}
