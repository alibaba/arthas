package com.taobao.arthas.core.channel;

import com.alibaba.arthas.channel.client.AgentInfoService;
import com.alibaba.arthas.channel.proto.AgentInfo;
import com.alibaba.arthas.channel.proto.AgentStatus;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.arthas.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author gongdewei 2020/8/15
 */
public class AgentInfoServiceImpl implements AgentInfoService {

    private static final Logger logger = LoggerFactory.getLogger(AgentInfoServiceImpl.class);

    private AgentInfo agentInfo;

    private Configure configure;

    public AgentInfoServiceImpl(Configure configure) {
        this.configure = configure;
    }

    @Override
    public AgentInfo getAgentInfo() {
        if (agentInfo == null) {
            agentInfo = AgentInfo.newBuilder()
                    .setAgentId(configure.getAgentId())
                    .setAgentVersion(ArthasBanner.version())
                    .setAgentStatus(AgentStatus.UP)
                    .setChannelServer(configure.getChannelServer())
                    .setClassPath(getClassPath())
                    .setHostname(getHostname())
                    .setIp(getIpAddress())
                    .setOsVersion(getOsVersion())
                    .build();
        }
        return agentInfo;
    }

    @Override
    public void updateAgentStatus(AgentStatus agentStatus) {
        AgentInfo.Builder agentInfoBuilder = this.getAgentInfo().toBuilder();
        agentInfoBuilder.setAgentStatus(agentStatus);
        this.agentInfo = agentInfoBuilder.build();
    }

    private String getClassPath() {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null) {
            return "";
        }
        // get first part of classpath: /xxx/arthas-demo.jar:/xxx/arthas-agent.jar
        String[] strs = classpath.split(":");
        return strs[0];
    }

    private String getOsVersion() {
        return System.getProperty("os.name");
    }

    private String getHostname() {
        // from https://stackoverflow.com/a/20793241
        // try InetAddress.LocalHost first;
        // NOTE -- InetAddress.getLocalHost().getHostName() will not work in certain environments.
        try {
            String result = InetAddress.getLocalHost().getHostName();
            if (!StringUtils.isEmpty(result))
                return result;
        } catch (UnknownHostException e) {
            // failed;  try alternate means.
        }

        // try environment properties.
        String host = System.getenv("COMPUTERNAME");
        if (host != null)
            return host;
        host = System.getenv("HOSTNAME");
        if (host != null)
            return host;

        // undetermined.
        return null;
    }

    private String getIpAddress() {
        String channelServer = configure.getChannelServer();
        String host = null;
        int port;
        try {
            String[] strs = channelServer.split(":");
            if (strs.length != 2) {
                throw new IllegalArgumentException("server address format must be 'host:port' or 'ip:port'.");
            }

            host = strs[0].trim();
            if (host.length() == 0) {
                throw new IllegalArgumentException("server host is invalid");
            }

            try {
                port = Integer.parseInt(strs[1].trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("server port is invalid");
            }
        } catch (Exception e) {
            logger.error("parse channel server address failure", e);
            return "";
        }

        Socket socket  = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            logger.error("connect to channel server failure", e);
            // ignore
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }
        return "";
    }

}
