package com.taobao.arthas.core.security;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;

import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.server.ArthasBootstrap;

import io.netty.channel.ChannelHandlerContext;

/**
 * 认证工具类
 * 用于处理Arthas服务器的认证相关逻辑，包括本地连接的身份验证
 *
 * @author hengyunabc 2021-09-01
 */
public class AuthUtils {
    /**
     * Arthas配置对象，从ArthasBootstrap单例中获取
     * 用于获取认证相关的配置信息
     */
    private static Configure configure = ArthasBootstrap.getInstance().getConfigure();

    /**
     * 获取本地连接的Principal（身份主体）
     * 如果配置允许本地连接无需认证，且当前连接确实是本地连接，则返回LocalConnectionPrincipal
     *
     * @param ctx Netty通道处理上下文，包含连接的详细信息
     * @return 如果是本地连接且配置允许免认证，返回LocalConnectionPrincipal；否则返回null
     */
    public static Principal localPrincipal(ChannelHandlerContext ctx) {
        // 检查配置是否允许本地连接免认证
        if (configure.isLocalConnectionNonAuth() && isLocalConnection(ctx)) {
            // 返回本地连接的身份主体，表示已通过本地连接认证
            return new LocalConnectionPrincipal();
        }
        // 不满足本地免认证条件，返回null表示需要其他认证方式
        return null;
    }

    /**
     * 判断当前连接是否为本地连接
     * 通过检查远程地址是否为127.0.0.1来判断
     *
     * @param ctx Netty通道处理上下文，包含连接的详细信息
     * @return 如果是本地连接（127.0.0.1）返回true，否则返回false
     */
    public static boolean isLocalConnection(ChannelHandlerContext ctx) {
        // 获取通道的远程地址
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        // 检查是否为InetSocketAddress类型（IP套接字地址）
        if (remoteAddress instanceof InetSocketAddress) {
            // 获取远程地址的IP地址字符串形式
            String hostAddress = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
            // 判断是否为本地回环地址127.0.0.1
            if ("127.0.0.1".equals(hostAddress)) {
                return true;
            }
        }
        // 不是本地连接
        return false;
    }
}
