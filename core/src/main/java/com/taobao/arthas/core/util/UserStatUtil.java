package com.taobao.arthas.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public static String getStatUrl() {
        return statUrl;
    }

    public static void setStatUrl(String url) {
        statUrl = url;
    }

    public static void arthasStart() {
        RemoteJob job = new RemoteJob();
        job.appendQueryData("ip", ip);
        job.appendQueryData("version", version);
        job.appendQueryData("command", "start");

        try {
            executorService.execute(job);
        } catch (Throwable t) {
            //
        }
    }

    public static void arthasUsage(String cmd, String detail) {
        RemoteJob job = new RemoteJob();
        job.appendQueryData("ip", ip);
        job.appendQueryData("version", version);
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
                    queryData.append(key + "=" + value);
                } else {
                    queryData.append("&" + key + "=" + value);
                }
            }
        }

        @Override
        public void run() {
            String link = statUrl;
            if (link == null) {
                return;
            }
            BufferedReader br = null;
            try {
                if (queryData.length() != 0) {
                    link = link + "?" + queryData;
                }
                URL url = new URL(link.toString());
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                StringBuilder result = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
            } catch (Throwable t) {
                // ignore
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }
}
