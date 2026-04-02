package com.alibaba.arthas.tunnel.server.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;

/**
 * Arthas 监控端点
 *
 * <p>这是一个 Spring Boot Actuator 端点，用于暴露 Arthas Tunnel Server 的运行时信息。</p>
 * <p>通过该端点可以获取：</p>
 * <ul>
 *   <li>版本信息（version）</li>
 *   <li>配置属性（properties）</li>
 *   <li>已连接的 Agent 列表（agents）</li>
 *   <li>客户端连接信息（clientConnections）</li>
 * </ul>
 *
 * <p>访问方式：通过 HTTP GET 请求访问 /actuator/arthas 端点</p>
 *
 * @author hengyunabc 2019-08-29
 */
@Endpoint(id = "arthas")
public class ArthasEndpoint {

    /**
     * Arthas 配置属性
     *
     * <p>自动注入的配置对象，包含服务器的各项配置信息</p>
     */
    @Autowired
    ArthasProperties arthasProperties;

    /**
     * 隧道服务器实例
     *
     * <p>自动注入的服务器对象，用于管理 Agent 连接和客户端连接</p>
     */
    @Autowired
    TunnelServer tunnelServer;

    /**
     * 读取端点数据
     *
     * <p>该方法返回 Arthas Tunnel Server 的运行时状态信息，包括：</p>
     * <ol>
     *   <li>version：从包的清单文件中读取的实现版本</li>
     *   <li>properties：服务器配置属性对象</li>
     *   <li>agents：所有已连接的 Agent 信息映射（Map 结构）</li>
     *   <li>clientConnections：所有客户端连接信息映射（Map 结构）</li>
     * </ol>
     *
     * @return 包含服务器运行时信息的 Map，key 为信息类型，value 为对应的数据
     */
    @ReadOperation
    public Map<String, Object> invoke() {
        // 创建容量为 4 的 HashMap，预知会有 4 个键值对
        Map<String, Object> result = new HashMap<>(4);

        // 获取并添加版本信息（从 MANIFEST.MF 读取 Implementation-Version）
        result.put("version", this.getClass().getPackage().getImplementationVersion());

        // 添加配置属性对象
        result.put("properties", arthasProperties);

        // 添加所有已连接的 Agent 信息
        // Map 的 key 通常是 Agent 的唯一标识，value 是 Agent 的详细信息
        result.put("agents", tunnelServer.getAgentInfoMap());

        // 添加所有客户端连接信息
        // Map 的 key 通常是连接 ID，value 是连接的详细信息
        result.put("clientConnections", tunnelServer.getClientConnectionInfoMap());

        return result;
    }

}
