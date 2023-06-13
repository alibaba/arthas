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
 * @author ralf0131 on 2015-11-11 15:39.
 */
public class NetUtils {

    private static final String QOS_HOST = "localhost";
    private static final int QOS_PORT = 12201;
    private static final String QOS_RESPONSE_START_LINE = "pandora>[QOS Response]";
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 3000;

    /**
     * This implementation is based on Apache HttpClient.
     * @param urlString the requested url
     * @return the response string of given url
     */
    public static Response request(String urlString) {
        HttpURLConnection urlConnection = null;
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);;
            // prefer json to text
            urlConnection.setRequestProperty("Accept", "application/json,text/plain;q=0.2");
            in = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            int statusCode = urlConnection.getResponseCode();
            String result = sb.toString().trim();
            if (statusCode == INTERNAL_SERVER_ERROR) {
                JSONObject errorObj = JSON.parseObject(result);
                if (errorObj.containsKey("errorMsg")) {
                    return new Response(errorObj.getString("errorMsg"), false);
                }
                return new Response(result, false);
            }
            return new Response(result);
        } catch (IOException e) {
            return new Response(e.getMessage(), false);
        } finally {
            IOUtils.close(in);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * @deprecated
     * This implementation is based on HttpURLConnection,
     * which can not detail with status code other than 200.
     * @param url the requested url
     * @return the response string of given url
     */
    public static String simpleRequest(String url) {
        BufferedReader br = null;
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();

            br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            String result = sb.toString().trim();
            if (responseCode == 500) {
                JSONObject errorObj = JSON.parseObject(result);
                if (errorObj.containsKey("errorMsg")) {
                    return errorObj.getString("errorMsg");
                }
                return result;
            } else {
                return result;
            }

        } catch (Exception e) {
            return null;
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

    /**
     * Only use this method when tomcat monitor version <= 1.0.1
     * This will send http request to pandora qos port 12201,
     * and display the response.
     * Note that pandora qos response is not fully HTTP compatible under version 2.1.0,
     * so we filtered some of the content and only display useful content.
     * @param path the path relative to http://localhost:12201
     *             e.g. /pandora/ls
     *             For commands that requires arguments, use the following format
     *             e.g. /pandora/find?arg0=RPCProtocolService
     *             Note that the parameter name is never used in pandora qos,
     *             so the name(e.g. arg0) is irrelevant.
     * @return the qos response in string format
     */
    public static Response requestViaSocket(String path) {
        BufferedReader br = null;
        try {
            Socket s = new Socket(QOS_HOST, QOS_PORT);
            PrintWriter pw = new PrintWriter(s.getOutputStream());
            pw.println("GET " + path + " HTTP/1.1");
            pw.println("Host: " + QOS_HOST + ":" + QOS_PORT);
            pw.println("");
            pw.flush();

            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            boolean start = false;
            while ((line = br.readLine()) != null) {
                if (start) {
                    sb.append(line).append("\n");
                }
                if (line.equals(QOS_RESPONSE_START_LINE)) {
                    start = true;
                }
            }
            String result = sb.toString().trim();
            return new Response(result);
        } catch (Exception e) {
            return new Response(e.getMessage(), false);
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

    public static class Response {

        private boolean success;
        private String content;

        public Response(String content, boolean success) {
            this.success = success;
            this.content = content;
        }

        public Response(String content) {
            this.content = content;
            this.success = true;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContent() {
            return content;
        }
    }


    /**
     * Test if a port is open on the give host
     */
    public static boolean serverListening(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }


}
