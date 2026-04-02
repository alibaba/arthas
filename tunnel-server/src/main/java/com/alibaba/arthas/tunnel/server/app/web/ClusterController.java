package com.alibaba.arthas.tunnel.server.app.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

/**
 * 集群控制器类
 * <p>
 * 该类是一个Spring MVC控制器，提供了与Tunnel Server集群相关的HTTP API接口。
 * 主要用于在集群环境中查询Agent连接的信息，帮助客户端找到应该连接的Tunnel Server节点。
 * </p>
 * <p>
 * 在分布式部署场景中，可能有多个Tunnel Server实例。客户端需要知道目标Agent连接到了哪个Tunnel Server，
 * 以便客户端能够连接到正确的Tunnel Server节点来访问该Agent。
 * </p>
 *
 * @author hengyunabc 2020-10-27
 *
 */
@Controller
public class ClusterController {

    /**
     * 日志记录器
     * <p>
     * 使用SLF4J记录系统运行日志，便于问题追踪和系统监控。
     * </p>
     */
    private final static Logger logger = LoggerFactory.getLogger(ClusterController.class);

    /**
     * Tunnel服务器实例
     * <p>
     * 通过Spring的依赖注入机制自动注入，用于访问Tunnel Server的核心功能和集群存储。
     * </p>
     */
    @Autowired
    TunnelServer tunnelServer;

    /**
     * 查找Agent连接的Tunnel Server主机地址
     * <p>
     * 该接口用于在集群环境中查询指定Agent连接到了哪个Tunnel Server节点。
     * 客户端可以使用这个接口来确定应该连接到哪个Tunnel Server来访问目标Agent。
     * </p>
     * <p>
     * 处理流程：
     * <ol>
     * <li>从请求参数中获取agentId（Agent的唯一标识）</li>
     * <li>从Tunnel Server获取集群存储对象</li>
     * <li>在集群存储中查找指定Agent的集群信息</li>
     * <li>从集群信息中提取客户端应该连接的主机地址</li>
     * <li>返回主机地址，如果未找到则返回空字符串</li>
     * <li>记录查询日志，包含agentId和查询结果</li>
     * </ol>
     * </p>
     * <p>
     * 请求示例：GET /api/cluster/findHost?agentId=xxx
     * </p>
     * <p>
     * 响应示例：
     * <ul>
     * <li>如果找到Agent：返回Tunnel Server的IP地址，如 "192.168.1.100"</li>
     * <li>如果未找到Agent：返回空字符串 ""</li>
     * </ul>
     * </p>
     *
     * @param agentId Agent的唯一标识符，必填参数
     *                通过@RequestParam注解指定该参数必须提供
     * @return 客户端应该连接的Tunnel Server的主机地址（IP地址或域名），
     *         如果未找到对应的Agent则返回空字符串
     */
    @RequestMapping(value = "/api/cluster/findHost")
    @ResponseBody
    public String execute(@RequestParam(value = "agentId", required = true) String agentId) {
        // 获取Tunnel Server的集群存储对象
        // 该对象用于在集群环境中存储和查询Agent的连接信息
        TunnelClusterStore tunnelClusterStore = tunnelServer.getTunnelClusterStore();

        String host = null;

        // 检查集群存储对象是否存在（只有在集群模式下才会有该对象）
        if (tunnelClusterStore != null) {
            // 在集群存储中查找指定Agent的集群信息
            // AgentClusterInfo包含了Agent连接信息以及客户端应该连接的Tunnel Server信息
            AgentClusterInfo info = tunnelClusterStore.findAgent(agentId);

            // 从集群信息中提取客户端应该连接的主机地址
            if (info != null) {
                host = info.getClientConnectHost();
            }
        }

        // 如果未找到主机地址，设置为空字符串
        if (host == null) {
            host = "";
        }

        // 记录查询日志，包含agentId和查询结果
        // 日志级别为info，用于追踪集群查询操作
        logger.info("arthas cluster findHost, agentId: {}, host: {}", agentId, host);

        // 返回查询结果
        return host;
    }
}
