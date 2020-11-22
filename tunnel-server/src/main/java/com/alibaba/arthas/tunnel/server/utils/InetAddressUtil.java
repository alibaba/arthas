package com.alibaba.arthas.tunnel.server.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author hengyunabc 2020-10-27
 *
 */
public class InetAddressUtil {
    private final static Logger logger = LoggerFactory.getLogger(InetAddressUtil.class);

    /**
     * 获得本机IP。
     * <p>
     * 在超过一块网卡时会有问题，因为这里每次都只是取了第一块网卡绑定的IP地址
     */
    public static String getInetAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress address = null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    address = addresses.nextElement();
                    if (isValidAddress(address)) {
                        return address.getHostAddress();
                    }
                }
            }
            logger.warn("Can not get the server IP address");
            return null;
        } catch (Throwable t) {
            logger.error("Can not get the server IP address", t);
            return null;
        }
    }

    public static boolean isValidAddress(InetAddress address) {
        return address != null && !address.isLoopbackAddress() // filter 127.x.x.x
                && !address.isAnyLocalAddress() // filter 0.0.0.0
                && !address.isLinkLocalAddress() // filter 169.254.0.0/16
                && !address.getHostAddress().contains(":");// filter IPv6
    }
}
