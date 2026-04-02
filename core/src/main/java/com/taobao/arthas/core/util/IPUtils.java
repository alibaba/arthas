package com.taobao.arthas.core.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * IP地址工具类
 * 提供获取本机IP地址和判断操作系统的功能
 *
 * @author weipeng2k 2015-01-30 15:06:47
 */
public class IPUtils {

    /**
     * Windows操作系统标识字符串
     */
    private static final String WINDOWS = "windows";

    /**
     * 操作系统名称系统属性键名
     */
    private static final String OS_NAME = "os.name";

    /**
     * 检查当前操作系统是否为Windows
     *
     * @return 如果是Windows系统返回true，否则返回false
     */
    public static boolean isWindowsOS() {
        // 获取操作系统名称属性
        String osName = System.getProperty(OS_NAME);
        // 判断操作系统名称是否包含"windows"（不区分大小写）
        return osName.toLowerCase().contains(WINDOWS);
    }

    /**
     * 获取本机IP地址
     * 自动区分操作系统类型（Windows或Linux）并获取对应的IP地址
     *
     * @return 本机IP地址字符串，如果获取失败则返回null
     */
    public static String getLocalIP() {
        InetAddress ip = null;
        try {
            // 如果是Windows系统，直接获取本地主机地址
            if (isWindowsOS()) {
                ip = InetAddress.getLocalHost();
            } else {
                // Linux/Unix系统处理
                // 如果本地主机地址不是回环地址，直接使用
                if (!InetAddress.getLocalHost().isLoopbackAddress()) {
                    ip = InetAddress.getLocalHost();
                } else {
                    // 如果是回环地址，则需要遍历所有网络接口找到合适的IP地址
                    boolean bFindIP = false;
                    // 获取所有网络接口
                    Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
                    while (netInterfaces.hasMoreElements()) {
                        // 如果已经找到IP，则退出循环
                        if (bFindIP) {
                            break;
                        }
                        NetworkInterface ni = netInterfaces.nextElement();
                        // ----------特定情况，可以考虑用ni.getName判断
                        // 遍历该网络接口的所有IP地址
                        Enumeration<InetAddress> ips = ni.getInetAddresses();
                        while (ips.hasMoreElements()) {
                            ip = ips.nextElement();
                            // 判断是否为本地站点地址且不是回环地址，并且不是IPv6地址（不包含冒号）
                            // 127开头的IP是回环地址
                            if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                                    && !ip.getHostAddress().contains(":")) {
                                bFindIP = true;
                                break;
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            // 捕获所有异常，不进行处理
        }

        // 如果ip为null则返回null，否则返回IP地址字符串
        return ip == null ? null : ip.getHostAddress();
    }


    /**
     * 检查给定的IP地址字符串是否全为零
     * 例如："0.0.0.0"、"::" 或 "0000:0000:0000:0000:0000:0000:0000:0000"
     *
     * @param ipStr 要检查的IP地址字符串
     * @return 如果IP地址全为零返回true，否则返回false
     */
    public static boolean isAllZeroIP(String ipStr) {
        // 如果IP字符串为null或空，返回false
        if (ipStr == null || ipStr.isEmpty()) {
            return false;
        }
        // 将IP字符串转换为字符数组
        char[] charArray = ipStr.toCharArray();

        // 遍历每个字符，检查是否只包含'0'、'.'和':'
        for (char c : charArray) {
            if (c != '0' && c != '.' && c != ':') {
                return false;
            }
        }

        // 所有字符都符合条件，返回true
        return true;
    }
}
