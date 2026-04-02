package com.alibaba.arthas.tunnel.server.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络地址工具类
 * <p>
 * 提供获取本机 IP 地址的工具方法。
 * 主要用于 Tunnel Server 启动时获取自身的 IP 地址，
 * 以便在集群中进行通信。
 * </p>
 *
 * @author hengyunabc 2020-10-27
 *
 */
public class InetAddressUtil {
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(InetAddressUtil.class);

    /**
     * 获得本机 IP 地址
     * <p>
     * 该方法遍历所有网络接口，查找第一个有效的非回环、非本地链路的 IP 地址。
     * </p>
     * <p>
     * 注意事项：
     * - 在超过一块网卡时可能会有问题，因为这里只是取了第一个有效网卡绑定的 IP 地址
     * - 会过滤掉回环地址（127.x.x.x）、通配地址（0.0.0.0）、链路本地地址（169.254.x.x）和 IPv6 地址
     * </p>
     *
     * @return 本机 IP 地址，如果未找到则返回 null
     */
    public static String getInetAddress() {
        try {
            // 获取本机所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress address = null;

            // 遍历所有网络接口
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                // 获取该网络接口绑定的所有 IP 地址
                Enumeration<InetAddress> addresses = ni.getInetAddresses();

                // 遍历所有 IP 地址
                while (addresses.hasMoreElements()) {
                    address = addresses.nextElement();

                    // 检查地址是否有效
                    if (isValidAddress(address)) {
                        // 返回第一个有效地址的字符串表示
                        return address.getHostAddress();
                    }
                }
            }

            // 如果遍历完所有网络接口仍未找到有效地址，记录警告日志
            logger.warn("Can not get the server IP address");
            return null;
        } catch (Throwable t) {
            // 如果获取过程中出现异常，记录错误日志并返回 null
            logger.error("Can not get the server IP address", t);
            return null;
        }
    }

    /**
     * 判断 IP 地址是否有效
     * <p>
     * 有效的 IP 地址需要满足以下条件：
     * - 不为 null
     * - 不是回环地址（127.x.x.x）
     * - 不是通配地址（0.0.0.0）
     * - 不是链路本地地址（169.254.x.x）
     * - 不是 IPv6 地址（不包含冒号）
     * </p>
     *
     * @param address 待判断的 IP 地址
     * @return 如果地址有效返回 true，否则返回 false
     */
    public static boolean isValidAddress(InetAddress address) {
        return address != null
                && !address.isLoopbackAddress() // 过滤掉回环地址 127.x.x.x
                && !address.isAnyLocalAddress() // 过滤掉通配地址 0.0.0.0
                && !address.isLinkLocalAddress() // 过滤掉链路本地地址 169.254.x.x
                && !address.getHostAddress().contains(":"); // 过滤掉 IPv6 地址
    }
}
