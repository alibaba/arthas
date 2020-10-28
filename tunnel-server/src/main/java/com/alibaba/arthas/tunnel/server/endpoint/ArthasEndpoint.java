package com.alibaba.arthas.tunnel.server.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;

@Endpoint(id = "arthas")
public class ArthasEndpoint {

    @Autowired
    ArthasProperties arthasProperties;
    @Autowired
    TunnelServer tunnelServer;

    @ReadOperation
    public Map<String, Object> invoke() {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("version", this.getClass().getPackage().getImplementationVersion());
        result.put("properties", arthasProperties);

        result.put("agents", tunnelServer.getAgentInfoMap());
        result.put("clientConnections", tunnelServer.getClientConnectionInfoMap());

        return result;
    }

}
