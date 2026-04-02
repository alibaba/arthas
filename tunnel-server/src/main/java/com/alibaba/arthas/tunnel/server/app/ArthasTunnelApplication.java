package com.alibaba.arthas.tunnel.server.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Arthas隧道服务器应用程序主类
 * <p>
 * 这是Arthas Tunnel Server的Spring Boot应用程序入口类。
 * Tunnel Server作为中间服务器，用于在客户端（如浏览器、命令行工具）和目标Java应用的Agent之间建立通信隧道。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 * <li>接收并管理Agent的连接</li>
 * <li>接收客户端的连接请求</li>
 * <li>在Agent和客户端之间转发通信数据</li>
 * <li>支持集群部署，通过TunnelClusterStore实现多节点协同</li>
 * </ul>
 * </p>
 * <p>
 * 使用@EnableCaching注解启用Spring缓存支持，用于提高Agent查询等操作的响应速度。
 * </p>
 *
 * @author hengyunabc 2019-08-27
 */
@SpringBootApplication(scanBasePackages = { "com.alibaba.arthas.tunnel.server.app",
        "com.alibaba.arthas.tunnel.server.endpoint" })
/**
 * 启用Spring缓存支持
 * <p>
 * 通过该注解，Spring会自动配置缓存管理器，允许在方法上使用@Cacheable、@CacheEvict等注解。
 * 在Tunnel Server中，缓存主要用于加速Agent信息的查询，减少对集群存储的频繁访问。
 * </p>
 */
@EnableCaching
public class ArthasTunnelApplication {

    /**
     * 应用程序主入口方法
     * <p>
     * 该方法启动Spring Boot应用程序，初始化Spring容器并启动内嵌的Web服务器。
     * SpringApplication.run()方法会执行以下操作：
     * <ol>
     * <li>创建SpringApplication实例</li>
     * <li>加载application.properties/application.yml配置文件</li>
     * <li>启动Spring容器，扫描并注册Bean</li>
     * <li>启动内嵌的Tomcat服务器（默认）</li>
     * <li>应用程序开始接收HTTP请求</li>
     * </ol>
     * </p>
     *
     * @param args 命令行参数，可以通过--option=value格式传递配置项
     */
    public static void main(String[] args) {
        SpringApplication.run(ArthasTunnelApplication.class, args);
    }

}
