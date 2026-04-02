package com.taobao.arthas.common;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ServerSocketFactory;

/**
 * Socket工具类
 * 提供Socket端口相关的工具方法，包括查找监听指定端口的进程、检测端口可用性、查找可用端口等功能
 *
 * @author hengyunabc 2018-11-07
 *
 */
public class SocketUtils {

    /**
     * 端口范围的最小值
     * 用于查找可用Socket端口时的默认最小端口号
     */
    public static final int PORT_RANGE_MIN = 1024;

    /**
     * 端口范围的最大值
     * 用于查找可用Socket端口时的默认最大端口号
     */
    public static final int PORT_RANGE_MAX = 65535;

    /**
     * 随机数生成器
     * 用于随机选择端口号
     */
    private static final Random random = new Random(System.currentTimeMillis());

    /**
     * 私有构造函数，防止实例化
     */
    private SocketUtils() {
    }

    /**
     * 查找监听指定TCP端口的进程ID
     * 该方法会添加5秒超时限制，防止操作阻塞
     *
     * @param port 要查询的端口号
     * @return 监听该端口的进程ID，如果未找到或发生错误则返回-1
     */
    public static long findTcpListenProcess(int port) {
        // 添加5秒超时，防止阻塞
        final int TIMEOUT_SECONDS = 5;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // 提交查找任务到线程池
            Future<Long> future = executor.submit(new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    return doFindTcpListenProcess(port);
                }
            });

            try {
                // 等待任务完成，最多等待5秒
                return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                // 超时则取消任务并返回-1
                future.cancel(true);
                return -1;
            } catch (Exception e) {
                // 发生异常返回-1
                return -1;
            }
        } finally {
            // 关闭线程池
            executor.shutdownNow();
        }
    }

    /**
     * 实际执行查找监听指定TCP端口的进程ID
     * 根据操作系统类型调用不同的实现方法
     *
     * @param port 要查询的端口号
     * @return 监听该端口的进程ID，如果未找到或发生错误则返回-1
     */
    private static long doFindTcpListenProcess(int port) {
        try {
            // Windows系统使用netstat命令
            if (OSUtils.isWindows()) {
                return findTcpListenProcessOnWindows(port);
            }

            // Linux或Mac系统使用lsof命令
            if (OSUtils.isLinux() || OSUtils.isMac()) {
                return findTcpListenProcessOnUnix(port);
            }
        } catch (Throwable e) {
            // 忽略所有异常
        }
        return -1;
    }

    /**
     * 在Windows系统上查找监听指定TCP端口的进程ID
     * 通过执行netstat -ano -p TCP命令并解析输出来获取进程ID
     *
     * @param port 要查询的端口号
     * @return 监听该端口的进程ID，如果未找到则返回-1
     */
    private static long findTcpListenProcessOnWindows(int port) {
        // 构造Windows的netstat命令
        String[] command = { "netstat", "-ano", "-p", "TCP" };
        // 执行命令并获取输出
        List<String> lines = ExecutingCommand.runNative(command);
        for (String line : lines) {
            // 查找包含LISTENING状态的行
            if (line.contains("LISTENING")) {
                // 示例行: TCP 0.0.0.0:49168 0.0.0.0:0 LISTENING 476
                // 按空格分割行
                String[] strings = line.trim().split("\\s+");
                if (strings.length == 5) {
                    // 检查第二列（本地地址）是否以:port结尾
                    if (strings[1].endsWith(":" + port)) {
                        // 返回第五列（进程ID）
                        return Long.parseLong(strings[4]);
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 在Unix/Linux/Mac系统上查找监听指定TCP端口的进程ID
     * 通过执行lsof -t -s TCP:LISTEN -i TCP:port命令来获取进程ID
     *
     * @param port 要查询的端口号
     * @return 监听该端口的进程ID，如果未找到则返回-1
     */
    private static long findTcpListenProcessOnUnix(int port) {
        // 执行lsof命令获取监听指定端口的进程ID
        // -t: 只输出进程ID
        // -s TCP:LISTEN: 只显示TCP LISTEN状态的连接
        // -i TCP:port: 指定端口号
        String pid = ExecutingCommand.getFirstAnswer("lsof -t -s TCP:LISTEN -i TCP:" + port);
        if (pid != null && !pid.trim().isEmpty()) {
            try {
                return Long.parseLong(pid.trim());
            } catch (NumberFormatException e) {
                // 忽略解析异常
            }
        }
        return -1;
    }

    /**
     * 检查指定的TCP端口是否可用
     * 通过尝试创建ServerSocket来判断端口是否被占用
     *
     * @param port 要检查的端口号
     * @return 如果端口可用返回true，否则返回false
     */
    public static boolean isTcpPortAvailable(int port) {
        try {
            // 尝试创建绑定到指定端口的ServerSocket
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1,
                    InetAddress.getByName("localhost"));
            serverSocket.close();
            return true;
        } catch (Exception ex) {
            // 端口被占用或其他异常
            return false;
        }
    }

    /**
     * 查找一个可用的TCP端口
     * 从默认范围[1024, 65535]中随机选择一个可用端口
     *
     * @return 可用的TCP端口号
     * @throws IllegalStateException 如果找不到可用端口
     */
    public static int findAvailableTcpPort() {
        return findAvailableTcpPort(PORT_RANGE_MIN);
    }

    /**
     * 查找一个可用的TCP端口
     * 从指定最小端口到默认最大端口(65535)的范围内随机选择一个可用端口
     *
     * @param minPort 最小端口号
     * @return 可用的TCP端口号
     * @throws IllegalStateException 如果找不到可用端口
     */
    public static int findAvailableTcpPort(int minPort) {
        return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
    }

    /**
     * 查找一个可用的TCP端口
     * 从指定范围内随机选择一个可用端口
     *
     * @param minPort 最小端口号
     * @param maxPort 最大端口号
     * @return 可用的TCP端口号
     * @throws IllegalStateException 如果找不到可用端口
     */
    public static int findAvailableTcpPort(int minPort, int maxPort) {
        return findAvailablePort(minPort, maxPort);
    }

    /**
     * 查找一个可用端口
     * 在指定范围内随机选择端口，直到找到一个可用的端口
     *
     * @param minPort 最小端口号
     * @param maxPort 最大端口号
     * @return 可用的端口号
     * @throws IllegalStateException 如果找不到可用端口
     */
    private static int findAvailablePort(int minPort, int maxPort) {

        // 计算端口范围
        int portRange = maxPort - minPort;
        int candidatePort;
        int searchCounter = 0;
        do {
            // 检查是否超出最大尝试次数
            if (searchCounter > portRange) {
                throw new IllegalStateException(
                        String.format("Could not find an available tcp port in the range [%d, %d] after %d attempts",
                                minPort, maxPort, searchCounter));
            }
            // 随机选择一个端口
            candidatePort = findRandomPort(minPort, maxPort);
            searchCounter++;
        } while (!isTcpPortAvailable(candidatePort));

        return candidatePort;
    }

    /**
     * 在指定范围内找到一个伪随机端口号
     *
     * @param minPort 最小端口号
     * @param maxPort 最大端口号
     * @return 指定范围内的随机端口号
     */
    private static int findRandomPort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        // 返回minPort到maxPort之间的随机数（包含maxPort）
        return minPort + random.nextInt(portRange + 1);
    }
}
