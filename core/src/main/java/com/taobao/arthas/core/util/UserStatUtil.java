package com.taobao.arthas.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Arthas 使用情况统计工具类
 * <p>
 * 用于收集和上报Arthas的使用统计数据，包括：
 * <ul>
 * <li>Arthas启动事件</li>
 * <li>用户执行的命令及参数</li>
 * <li>用户ID（可选）</li>
 * <li>IP地址和版本信息</li>
 * </ul>
 * <p>
 * 统计数据通过异步方式上报到远程服务器，不影响主线程性能。
 * </p>
 * <p/>
 * Created by zhuyong on 15/11/12.
 */
public class UserStatUtil {

    /**
     * 默认缓冲区大小，用于读取HTTP响应
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 用于跳过HTTP响应内容的字节缓冲区
     * 只读取响应但不处理，用于触发HTTP请求
     */
    private static final byte[] SKIP_BYTE_BUFFER = new byte[DEFAULT_BUFFER_SIZE];

    /**
     * 单线程执行器，用于异步执行统计上报任务
     * 使用守护线程，不会阻止JVM退出
     */
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, "arthas-UserStat");
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * 本机IP地址
     */
    private static final String ip = IPUtils.getLocalIP();

    /**
     * Arthas版本号，已进行URL编码
     */
    private static final String version = URLEncoder.encode(ArthasBanner.version().replace("\n", ""));

    /**
     * 统计数据上报的URL地址
     * 使用volatile确保多线程可见性
     */
    private static volatile String statUrl = null;

    /**
     * Agent的唯一标识ID
     * 使用volatile确保多线程可见性
     */
    private static volatile String agentId = null;

    /**
     * 获取统计上报的URL地址
     *
     * @return 统计URL
     */
    public static String getStatUrl() {
        return statUrl;
    }

    /**
     * 设置统计上报的URL地址
     *
     * @param url 统计URL
     */
    public static void setStatUrl(String url) {
        statUrl = url;
    }

    /**
     * 获取Agent ID
     *
     * @return Agent ID
     */
    public static String getAgentId() {
        return agentId;
    }

    /**
     * 设置Agent ID
     *
     * @param id Agent ID
     */
    public static void setAgentId(String id) {
        agentId = id;
    }

    /**
     * 上报Arthas启动事件
     * <p>
     * 当Arthas启动时调用，将启动信息（IP、版本、AgentID等）上报到统计服务器。
     * 上报是异步执行的，不会阻塞启动流程。
     * </p>
     */
    public static void arthasStart() {
        // 如果未设置统计URL，直接返回
        if (statUrl == null) {
            return;
        }

        // 创建远程上报任务
        RemoteJob job = new RemoteJob();
        job.appendQueryData("ip", ip);
        job.appendQueryData("version", version);
        if (agentId != null) {
            job.appendQueryData("agentId", agentId);
        }
        job.appendQueryData("command", "start");

        // 异步执行上报任务
        try {
            executorService.execute(job);
        } catch (Throwable t) {
            // 忽略异常，不影响主流程
        }
    }

    /**
     * 上报Arthas命令使用情况
     * <p>
     * 将用户执行的命令及其参数上报到统计服务器。
     * </p>
     *
     * @param cmd 命令名称
     * @param detail 命令参数详情
     * @param userId 用户ID（可选）
     */
    private static void arthasUsage(String cmd, String detail, String userId) {
        // 创建远程上报任务
        RemoteJob job = new RemoteJob();
        job.appendQueryData("ip", ip);
        job.appendQueryData("version", version);
        if (agentId != null) {
            job.appendQueryData("agentId", agentId);
        }
        if (userId != null) {
            job.appendQueryData("userId", URLEncoder.encode(userId));
        }
        // 对命令名称进行URL编码
        job.appendQueryData("command", URLEncoder.encode(cmd));
        if (detail != null) {
            // 对命令参数进行URL编码
            job.appendQueryData("arguments", URLEncoder.encode(detail));
        }

        // 异步执行上报任务
        try {
            executorService.execute(job);
        } catch (Throwable t) {
            // 忽略异常，不影响主流程
        }
    }

    /**
     * 上报命令执行成功事件（带用户ID）
     * <p>
     * 当命令执行成功后调用，记录用户使用的命令及参数。
     * </p>
     *
     * @param cmd 命令名称
     * @param args 命令参数列表
     * @param userId 用户ID
     */
    public static void arthasUsageSuccess(String cmd, List<String> args, String userId) {
        if (statUrl == null) {
            return;
        }
        // 将命令和参数拼接成一个字符串
        StringBuilder commandString = new StringBuilder(cmd);
        for (String arg : args) {
            commandString.append(" ").append(arg);
        }
        UserStatUtil.arthasUsage(cmd, commandString.toString(), userId);
    }

    /**
     * 上报命令执行成功事件（不带用户ID）
     * <p>
     * 当命令执行成功后调用，记录用户使用的命令及参数。
     * </p>
     *
     * @param cmd 命令名称
     * @param args 命令参数列表
     */
    public static void arthasUsageSuccess(String cmd, List<String> args) {
        arthasUsageSuccess(cmd, args, null);
    }

    /**
     * 销毁统计工具
     * <p>
     * 立即关闭执行器，丢弃所有待执行和正在执行的任务。
     * 通常在Arthas关闭时调用。
     * </p>
     */
    public static void destroy() {
        // 直接关闭，未完成的上报任务将被丢弃
        executorService.shutdownNow();
    }

    /**
     * 远程上报任务
     * <p>
     * 异步执行的HTTP请求任务，将统计数据上报到远程服务器。
     * 使用GET请求，超时时间为1秒。
     * </p>
     */
    static class RemoteJob implements Runnable {
        /**
         * 查询参数字符串构建器
         * 用于构建URL的查询参数部分
         */
        private StringBuilder queryData = new StringBuilder();

        /**
         * 添加查询参数
         * <p>
         * 将键值对添加到查询字符串中，多个参数用&amp;连接。
         * </p>
         *
         * @param key 参数键
         * @param value 参数值（应该已经过URL编码）
         */
        public void appendQueryData(String key, String value) {
            if (key != null && value != null) {
                if (queryData.length() == 0) {
                    // 第一个参数，直接添加
                    queryData.append(key).append("=").append(value);
                } else {
                    // 后续参数，用&连接
                    queryData.append("&").append(key).append("=").append(value);
                }
            }
        }

        /**
         * 执行HTTP上报请求
         * <p>
         * 将统计数据发送到远程服务器。设置1秒的连接和读取超时，
         * 避免阻塞过长时间影响主程序。
         * </p>
         */
        @Override
        public void run() {
            String link = statUrl;
            if (link == null) {
                return;
            }

            InputStream inputStream = null;
            try {
                // 构建完整的URL（包含查询参数）
                if (queryData.length() != 0) {
                    link = link + "?" + queryData;
                }

                // 创建HTTP连接
                URL url = new URL(link);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(1000);  // 连接超时1秒
                connection.setReadTimeout(1000);     // 读取超时1秒
                connection.connect();

                // 读取响应内容（但不处理，只为了触发请求）
                inputStream = connection.getInputStream();
                //noinspection StatementWithEmptyBody
                while (inputStream.read(SKIP_BYTE_BUFFER) != -1) {
                    // 循环读取响应内容，但不做任何处理
                    // 这是为了确保HTTP请求完整发送
                }
            } catch (Throwable t) {
                // 忽略所有异常，上报失败不影响主程序
            } finally {
                // 关闭输入流
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // 忽略关闭异常
                    }
                }
            }
        }
    }
}
