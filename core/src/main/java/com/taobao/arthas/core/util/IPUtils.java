package com.taobao.arthas.core.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author weipeng2k 2015-01-30 15:06:47
 */
public class IPUtils {

    private static final String WINDOWS = "windows";
    private static final String OS_NAME = "os.name";

    /**
     * check: whether current operating system is windows
     *
     * @return true---is windows
     */
    public static boolean isWindowsOS() {
        String osName = System.getProperty(OS_NAME);
        return osName.toLowerCase().contains(WINDOWS);
    }

    /**
     * get IP address, automatically distinguish the operating system.（windows or
     * linux）
     *
     * @return String
     */
    public static String getLocalIP() {
        InetAddress ip = null;
        try {
            if (isWindowsOS()) {
                ip = InetAddress.getLocalHost();
            } else {
                // scan all NetWorkInterfaces if it's loopback address
                if (!InetAddress.getLocalHost().isLoopbackAddress()) {
                    ip = InetAddress.getLocalHost();
                } else {
                    boolean bFindIP = false;
                    Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
                    while (netInterfaces.hasMoreElements()) {
                        if (bFindIP) {
                            break;
                        }
                        NetworkInterface ni = netInterfaces.nextElement();
                        // ----------特定情况，可以考虑用ni.getName判断
                        // iterator all IPs
                        Enumeration<InetAddress> ips = ni.getInetAddresses();
                        while (ips.hasMoreElements()) {
                            ip = ips.nextElement();
                            // IP starts with 127. is loopback address
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
        }

        return ip == null ? null : ip.getHostAddress();
    }


    public static boolean isAllZeroIP(String ipStr) {
        if (ipStr == null || ipStr.isEmpty()) {
            return false;
        }
        char[] charArray = ipStr.toCharArray();

        for (char c : charArray) {
            if (c != '0' && c != '.' && c != ':') {
                return false;
            }
        }

        return true;
    }
}
