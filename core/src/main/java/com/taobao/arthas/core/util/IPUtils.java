package com.taobao.arthas.core.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author weipeng2k 2015年1月30日 下午3:06:47
 */
public class IPUtils {

    /**
     * 判断当前操作是否Windows.
     * 
     * @return true---是Windows操作系统
     */
    public static boolean isWindowsOS() {
        boolean isWindowsOS = false;
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().indexOf("windows") > -1) {
            isWindowsOS = true;
        }
        return isWindowsOS;
    }

    /**
     * 获取本机IP地址，并自动区分Windows还是Linux操作系统
     * 
     * @return String
     */
    public static String getLocalIP() {
        String sIP = null;
        InetAddress ip = null;
        try {
            if (isWindowsOS()) {
                ip = InetAddress.getLocalHost();
            } else {
                // 如果是回环地址则扫描所有的NetWorkInterface
                if (!InetAddress.getLocalHost().isLoopbackAddress()) {
                    ip = InetAddress.getLocalHost();
                } else {
                    boolean bFindIP = false;
                    Enumeration<NetworkInterface> netInterfaces = (Enumeration<NetworkInterface>) NetworkInterface.getNetworkInterfaces();
                    while (netInterfaces.hasMoreElements()) {
                        if (bFindIP) {
                            break;
                        }
                        NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
                        // ----------特定情况，可以考虑用ni.getName判断
                        // 遍历所有ip
                        Enumeration<InetAddress> ips = ni.getInetAddresses();
                        while (ips.hasMoreElements()) {
                            ip = (InetAddress) ips.nextElement();
                            // 127.开头的都是lookback地址
                            if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                                && ip.getHostAddress().indexOf(":") == -1) {
                                bFindIP = true;
                                break;
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
        }

        if (ip != null) {
            sIP = ip.getHostAddress();
        }
        return sIP;
    }

    public static void main(String[] args) {
        System.out.println(getLocalIP());
    }
}
