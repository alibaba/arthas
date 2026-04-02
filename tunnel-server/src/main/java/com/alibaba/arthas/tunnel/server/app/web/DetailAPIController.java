package com.alibaba.arthas.tunnel.server.app.web;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

/**
 * 详情页面 API 控制器
 * <p>
 * 提供 Tunnel Server 的详情查询接口，包括应用列表、Agent 信息查询等功能。
 * 主要用于前端展示 Tunnel 集群中的 Agent 连接状态和应用分布情况。
 * </p>
 *
 * @author hengyunabc 2020-11-03
 *
 */
@Controller
public class DetailAPIController {

    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(DetailAPIController.class);

    /**
     * Arthas 配置属性
     * 用于控制是否启用详情页面等功能
     */
    @Autowired
    ArthasProperties arthasProperties;

    /**
     * Tunnel 集群存储
     * 用于存储和查询集群中的 Agent 信息
     * 设置为可选依赖（required = false），支持非集群模式运行
     */
    @Autowired(required = false)
    private TunnelClusterStore tunnelClusterStore;

    /**
     * 获取所有连接到 Tunnel 的应用列表
     * <p>
     * 该接口会遍历所有已连接的 Agent，从 Agent ID 中提取应用名称，
     * 返回不重复的应用名称集合。
     * </p>
     *
     * @param request HTTP 请求对象
     * @param model Spring MVC 模型对象
     * @return 所有应用的名称集合
     * @throws IllegalAccessError 当未启用详情页面时抛出异常
     */
    @RequestMapping("/api/tunnelApps")
    @ResponseBody
    public Set<String> tunnelApps(HttpServletRequest request, Model model) {
        // 检查是否启用详情页面，如果未启用则抛出异常
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }

        // 创建结果集，用于存储所有应用名称
        Set<String> result = new HashSet<String>();

        // 如果集群存储可用，则查询所有 Agent
        if (tunnelClusterStore != null) {
            // 获取所有 Agent ID
            Collection<String> agentIds = tunnelClusterStore.allAgentIds();

            // 遍历所有 Agent ID，提取应用名称
            for (String id : agentIds) {
                // 从 Agent ID 中查找应用名称
                // Agent ID 格式通常为：appName_agentUniqueId
                String appName = findAppNameFromAgentId(id);
                if (appName != null) {
                    // 如果找到有效的应用名称，添加到结果集
                    result.add(appName);
                } else {
                    // 如果 Agent ID 格式不合法，记录警告日志
                    logger.warn("illegal agentId: " + id);
                }
            }

        }

        // 返回应用名称集合
        return result;
    }

    /**
     * 根据应用名称获取该应用下所有 Agent 的详细信息
     * <p>
     * 该接口用于查询某个特定应用的所有 Agent 连接信息，
     * 包括 Agent 的网络地址、连接状态等详细信息。
     * </p>
     *
     * @param appName 应用名称（必填参数）
     * @param request HTTP 请求对象
     * @param model Spring MVC 模型对象
     * @return Agent ID 到 Agent 信息的映射表
     * @throws IllegalAccessError 当未启用详情页面时抛出异常
     */
    @RequestMapping("/api/tunnelAgentInfo")
    @ResponseBody
    public Map<String, AgentClusterInfo> tunnelAgentIds(@RequestParam(value = "app", required = true) String appName,
            HttpServletRequest request, Model model) {
        // 检查是否启用详情页面，如果未启用则抛出异常
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }

        // 如果集群存储可用，则查询指定应用的 Agent 信息
        if (tunnelClusterStore != null) {
            // 根据应用名称获取所有 Agent 信息
            Map<String, AgentClusterInfo> agentInfos = tunnelClusterStore.agentInfo(appName);

            // 返回查询结果
            return agentInfos;
        }

        // 如果集群存储不可用，返回空映射
        return Collections.emptyMap();
    }

    /**
     * 检查指定的 Agent ID 是否存在
     * <p>
     * 该接口用于验证某个 Agent 是否已连接到 Tunnel 集群。
     * 返回结果中包含 success 字段，表示 Agent 是否存在。
     * </p>
     *
     * @param agentId Agent ID（必填参数）
     * @return 包含查询结果的映射，success 字段表示 Agent 是否存在
     */
    @RequestMapping("/api/tunnelAgents")
    @ResponseBody
    public Map<String, Object> tunnelAgentIds(@RequestParam(value = "agentId", required = true) String agentId) {
        // 创建结果对象
        Map<String, Object> result = new HashMap<String, Object>();
        // 初始化为未找到
        boolean success = false;

        try {
            // 尝试从集群存储中查找 Agent
            AgentClusterInfo info = tunnelClusterStore.findAgent(agentId);
            // 如果找到 Agent 信息，则标记为成功
            if (info != null) {
                success = true;
            }
        } catch (Throwable e) {
            // 如果查找过程中出现异常，记录错误日志
            logger.error("try to find agentId error, id: {}", agentId, e);
        }

        // 将查询结果放入返回对象
        result.put("success", success);
        return result;
    }

    /**
     * 从 Agent ID 中提取应用名称
     * <p>
     * Agent ID 的格式约定为：appName_agentUniqueId
     * 该方法通过提取第一个下划线之前的部分作为应用名称。
     * </p>
     *
     * @param id Agent ID
     * @return 应用名称，如果格式不合法则返回 null
     */
    private static String findAppNameFromAgentId(String id) {
        // 查找第一个下划线的位置
        int index = id.indexOf('_');
        // 如果没有找到下划线或下划线在末尾，则格式不合法
        if (index < 0 || index >= id.length()) {
            return null;
        }

        // 返回下划线之前的子串作为应用名称
        return id.substring(0, index);
    }
}
