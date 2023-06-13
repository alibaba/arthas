package com.taobao.arthas.core.security;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;

import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.server.ArthasBootstrap;

import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author hengyunabc 2021-09-01
 *
 */
public class AuthUtils {
    private static Configure configure = ArthasBootstrap.getInstance().getConfigure();

    public static Principal localPrincipal(ChannelHandlerContext ctx) {
        if (configure.isLocalConnectionNonAuth() && isLocalConnection(ctx)) {
            return new LocalConnectionPrincipal();
        }
        return null;
    }

    public static boolean isLocalConnection(ChannelHandlerContext ctx) {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            String hostAddress = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
            if ("127.0.0.1".equals(hostAddress)) {
                return true;
            }
        }
        return false;
    }
}
