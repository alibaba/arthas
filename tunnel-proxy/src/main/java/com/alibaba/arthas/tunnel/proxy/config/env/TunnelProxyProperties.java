package com.alibaba.arthas.tunnel.proxy.config.env;

import com.alibaba.arthas.tunnel.server.utils.InetAddressUtil;
import com.taobao.arthas.common.ArthasConstants;
import lombok.Data;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Tunnel Server 代理配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@Data
@ConfigurationProperties(prefix = TunnelProxyProperties.PREFIX)
public class TunnelProxyProperties {

    public static final String PREFIX = "arthas.tunnel";

    private Set<SecurityProperties.User> users;

    private String htmlTitle = "Arthas 控制台";

    private String agentSpilt = "@";

    private Server server;

    @Data
    public static class Server {

        private String host;

        private int port;

        private boolean ssl;

        private String path = ArthasConstants.DEFAULT_WEBSOCKET_PATH;

        private String clientConnectHost = InetAddressUtil.getInetAddress();
    }
}
