package com.taobao.arthas.core.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Arthas 使用情况统计
 * <p/>
 * Created by zhuyong on 15/11/12.
 */
public class UserStatUtil {
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String ip = IPUtils.getLocalIP();

    private static final String version = URLEncoder.encode(ArthasBanner.version().replace("\n", ""));

    public static void arthasStart() {
        RemoteJob job = new RemoteJob();
        job.setResource("anonymousStatStart.do");
        job.appendQueryData("productName", "Arthas");
        job.appendQueryData("productVersion", URLEncoder.encode(ArthasBanner.version()));

        try {
            executorService.execute(job);
        } catch(Throwable t) {
            //
        }
    }

    public static void arthasUsage(String cmd, String detail) {
        RemoteJob job = new RemoteJob();
        job.setResource("nonAnonymousStat.do");
        job.appendQueryData("ip", ip);
        job.appendQueryData("productName", "Arthas");
        job.appendQueryData("productVersion", version);
        job.appendQueryData("opName", URLEncoder.encode(cmd));
        if (detail != null) {
            job.appendQueryData("opDetail", URLEncoder.encode(detail));
        }

        try {
            executorService.execute(job);
        } catch(Throwable t) {
            //
        }
    }

    public static void arthasUsageSuccess(String cmd, List<String> args) {
        StringBuilder commandString = new StringBuilder(cmd);
        for (String arg: args) {
            commandString.append(" ").append(arg);
        }
        UserStatUtil.arthasUsage(cmd, commandString.toString() + " --> success");
    }

    public static void destroy() {
        // 直接关闭，没有回报的丢弃
        executorService.shutdownNow();
    }

    static class RemoteJob implements Runnable {

//        private StringBuilder link = new StringBuilder("http://arthas.io/api/");
        // disable stat
        private StringBuilder link = null;

        private String resource;

        private StringBuilder queryData = new StringBuilder();

        public void setResource(String resource) {
            this.resource = resource;
        }

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
            if (link == null) {
                return;
            }
            try {
                link.append(resource);
                if (queryData.length() != 0) {
                    link.append("?").append(queryData);
                }
                URL url = new URL(link.toString());
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.connect();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = null;
                StringBuilder result = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception ex) {
            }
        }
    }
}
