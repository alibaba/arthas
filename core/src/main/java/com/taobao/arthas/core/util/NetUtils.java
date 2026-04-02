package com.taobao.arthas.core.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.common.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

/**
 * 网络工具类
 * 提供HTTP请求和端口检测等网络相关功能
 *
 * @author ralf0131 on 2015-11-11 15:39.
 */
public class NetUtils {

    /**
     * QOS服务主机名，默认为localhost
     */
    private static final String QOS_HOST = "localhost";

    /**
     * QOS服务端口号
     */
    private static final int QOS_PORT = 12201;

    /**
     * QOS响应起始行标识
     */
    private static final String QOS_RESPONSE_START_LINE = "pandora>[QOS Response]";

    /**
     * HTTP内部服务器错误状态码
     */
    private static final int INTERNAL_SERVER_ERROR = 500;

    /**
     * HTTP连接超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 1000;

    /**
     * HTTP读取超时时间（毫秒）
     */
    private static final int READ_TIMEOUT = 3000;

    /**
     * 发送HTTP请求并获取响应
     * 基于HttpURLConnection实现
     *
     * @param urlString 请求的URL地址
     * @return 响应对象，包含响应内容和状态
     */
    public static Response request(String urlString) {
        HttpURLConnection urlConnection = null;
        InputStream in = null;
        try {
            // 创建URL对象
            URL url = new URL(urlString);
            // 打开HTTP连接
            urlConnection = (HttpURLConnection)url.openConnection();
            // 设置连接超时时间
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            // 设置读取超时时间
            urlConnection.setReadTimeout(READ_TIMEOUT);;
            // 设置Accept头，优先接受JSON格式，其次接受纯文本
            urlConnection.setRequestProperty("Accept", "application/json,text/plain;q=0.2");
            // 获取输入流
            in = urlConnection.getInputStream();
            // 使用BufferedReader读取响应内容
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = null;
            StringBuilder sb = new StringBuilder();
            // 逐行读取响应内容
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            // 获取HTTP状态码
            int statusCode = urlConnection.getResponseCode();
            String result = sb.toString().trim();
            // 处理500内部服务器错误
            if (statusCode == INTERNAL_SERVER_ERROR) {
                JSONObject errorObj = JSON.parseObject(result);
                // 如果响应包含errorMsg字段，返回错误信息
                if (errorObj.containsKey("errorMsg")) {
                    return new Response(errorObj.getString("errorMsg"), false);
                }
                return new Response(result, false);
            }
            // 返回成功响应
            return new Response(result);
        } catch (IOException e) {
            // 发生IO异常，返回失败响应
            return new Response(e.getMessage(), false);
        } finally {
            // 关闭输入流
            IOUtils.close(in);
            // 断开连接
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * 发送简单的HTTP请求
     *
     * @deprecated 此方法基于HttpURLConnection实现，无法很好地处理非200状态码，建议使用request()方法
     * @param url 请求的URL地址
     * @return 响应字符串，如果发生错误则返回null
     */
    public static String simpleRequest(String url) {
        BufferedReader br = null;
        try {
            // 创建URL对象
            URL obj = new URL(url);
            // 打开HTTP连接
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置Accept头，接受JSON格式
            con.setRequestProperty("Accept", "application/json");
            // 获取响应状态码
            int responseCode = con.getResponseCode();

            // 使用BufferedReader读取响应内容
            br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            // 逐行读取响应
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            String result = sb.toString().trim();
            // 处理500内部服务器错误
            if (responseCode == 500) {
                JSONObject errorObj = JSON.parseObject(result);
                // 如果包含errorMsg字段，返回错误信息
                if (errorObj.containsKey("errorMsg")) {
                    return errorObj.getString("errorMsg");
                }
                return result;
            } else {
                return result;
            }

        } catch (Exception e) {
            // 发生异常返回null
            return null;
        } finally {
            // 关闭BufferedReader
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * 通过Socket发送HTTP请求到Pandora QOS服务
     * 仅在tomcat monitor版本 <= 1.0.1 时使用此方法
     * 此方法会向pandora qos端口12201发送HTTP请求并显示响应
     * 注意：在2.1.0版本之前，pandora qos响应不完全兼容HTTP标准，
     * 因此我们过滤了一些内容，只显示有用的内容
     *
     * @param path 相对于http://localhost:12201的路径
     *             例如：/pandora/ls
     *             对于需要参数的命令，使用以下格式
     *             例如：/pandora/find?arg0=RPCProtocolService
     *             注意：参数名在pandora qos中不会被使用，所以名称（如arg0）无关紧要
     * @return QOS响应字符串
     */
    public static Response requestViaSocket(String path) {
        BufferedReader br = null;
        try {
            // 创建Socket连接到QOS服务
            Socket s = new Socket(QOS_HOST, QOS_PORT);
            // 获取输出流，用于发送HTTP请求
            PrintWriter pw = new PrintWriter(s.getOutputStream());
            // 构造HTTP GET请求
            pw.println("GET " + path + " HTTP/1.1");
            pw.println("Host: " + QOS_HOST + ":" + QOS_PORT);
            pw.println("");
            // 刷新输出流，发送请求
            pw.flush();

            // 获取输入流，读取响应
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            boolean start = false;
            // 逐行读取响应
            while ((line = br.readLine()) != null) {
                // 在QOS响应起始行之后才开始收集内容
                if (start) {
                    sb.append(line).append("\n");
                }
                // 检测到QOS响应起始行
                if (line.equals(QOS_RESPONSE_START_LINE)) {
                    start = true;
                }
            }
            String result = sb.toString().trim();
            return new Response(result);
        } catch (Exception e) {
            // 发生异常，返回失败响应
            return new Response(e.getMessage(), false);
        } finally {
            // 关闭BufferedReader
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * HTTP响应封装类
     * 用于封装HTTP请求的响应内容和状态
     */
    public static class Response {

        /**
         * 请求是否成功
         */
        private boolean success;

        /**
         * 响应内容
         */
        private String content;

        /**
         * 构造一个响应对象
         *
         * @param content 响应内容
         * @param success 请求是否成功
         */
        public Response(String content, boolean success) {
            this.success = success;
            this.content = content;
        }

        /**
         * 构造一个成功的响应对象
         *
         * @param content 响应内容
         */
        public Response(String content) {
            this.content = content;
            this.success = true;
        }

        /**
         * 获取请求是否成功
         *
         * @return 如果请求成功返回true，否则返回false
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * 获取响应内容
         *
         * @return 响应内容字符串
         */
        public String getContent() {
            return content;
        }
    }


    /**
     * 测试指定主机的端口是否开放
     *
     * @param host 主机地址
     * @param port 端口号
     * @return 如果端口开放返回true，否则返回false
     */
    public static boolean serverListening(String host, int port) {
        Socket s = null;
        try {
            // 尝试创建Socket连接
            s = new Socket(host, port);
            // 连接成功，端口开放
            return true;
        } catch (Exception e) {
            // 连接失败，端口未开放
            return false;
        } finally {
            // 关闭Socket连接
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
        }
    }


}
