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
 * Arthas 使用情况统计
 * <p/>
 * Created by zhuyong on 15/11/12.
 */
public class UserStatUtil {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final byte[] SKIP_BYTE_BUFFER = new byte[DEFAULT_BUFFER_SIZE];

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, "arthas-UserStat");
            t.setDaemon(true);
            return t;
        }
    });
    private static final String ip = IPUtils.getLocalIP();

    private static final String version = URLEncoder.encode(ArthasBanner.version().replace("\n", ""));

    private static volatile String statUrl = null;

    private static volatile String agentId = null;

    public static String getStatUrl() {
        return statUrl;
    }

    public static void setStatUrl(String url) {
        statUrl = url;
    }

    public static String getAgentId() {
        return agentId;
    }

    public static void setAgentId(String id) {
        agentId = id;
    }

    public static void arthasStart() {
        if (statUrl == null) {
            return;
        }
        RemoteJob job = new RemoteJob();
        job.appendQueryData("ip", ip);
        job.appendQueryData("version", version);
        if (agentId != null) {
            job.appendQueryData("agentId", agentId);
        }
        job.appendQueryData("command", "start");

        try {
            executorService.execute(job);
        } catch (Throwable t) {
            //
        }
    }

    private static void arthasUsage(String cmd, String detail) {
        RemoteJob job = new RemoteJob();
        job.appendQueryData("ip", ip);
        job.appendQueryData("version", version);
        if (agentId != null) {
            job.appendQueryData("agentId", agentId);
        }
        job.appendQueryData("command", URLEncoder.encode(cmd));
        if (detail != null) {
            job.appendQueryData("arguments", URLEncoder.encode(detail));
        }

        try {
            executorService.execute(job);
        } catch (Throwable t) {
            //
        }
    }

    public static void arthasUsageSuccess(String cmd, List<String> args) {
        if (statUrl == null) {
            return;
        }
        StringBuilder commandString = new StringBuilder(cmd);
        for (String arg : args) {
            commandString.append(" ").append(arg);
        }
        UserStatUtil.arthasUsage(cmd, commandString.toString());
    }

    public static void destroy() {
        // 直接关闭，没有回报的丢弃
        executorService.shutdownNow();
    }

    static class RemoteJob implements Runnable {
        private StringBuilder queryData = new StringBuilder();

        public void appendQueryData(String key, String value) {
            if (key != null && value != null) {
                if (queryData.length() == 0) {
                    queryData.append(key).append("=").append(value);
                } else {
                    queryData.append("&").append(key).append("=").append(value);
                }
            }
        }

        @Override
        public void run() {
            String link = statUrl;
            if (link == null) {
                return;
            }
            InputStream inputStream = null;
            try {
                if (queryData.length() != 0) {
                    link = link + "?" + queryData;
                }
                URL url = new URL(link);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                inputStream = connection.getInputStream();
                //noinspection StatementWithEmptyBody
                while (inputStream.read(SKIP_BYTE_BUFFER) != -1) {
                    // do nothing
                }
            } catch (Throwable t) {
                // ignore
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }
}
