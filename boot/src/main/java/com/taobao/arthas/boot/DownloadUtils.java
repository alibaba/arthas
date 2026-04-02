package com.taobao.arthas.boot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.IOUtils;

/**
 * 下载工具类
 *
 * 该类提供了从远程服务器下载Arthas相关资源的功能。
 * 主要功能包括：
 * 1. 获取Arthas的最新版本信息
 * 2. 获取所有可用的Arthas版本列表
 * 3. 下载Arthas打包文件
 * 4. 支持HTTP/HTTPS协议
 * 5. 支持HTTP重定向
 * 6. 显示下载进度
 *
 * @author hengyunabc 2018-11-06
 */
public class DownloadUtils {
    /**
     * Arthas版本列表API地址
     *
     * 该URL返回所有可用的Arthas版本列表，每行一个版本号。
     */
    private static final String ARTHAS_VERSIONS_URL = "https://arthas.aliyun.com/api/versions";

    /**
     * Arthas最新版本API地址
     *
     * 该URL返回当前最新的Arthas版本号。
     */
    private static final String ARTHAS_LATEST_VERSIONS_URL = "https://arthas.aliyun.com/api/latest_version";

    /**
     * Arthas下载URL模板
     *
     * 支持的变量：
     * - ${VERSION}：要下载的Arthas版本号
     * - ${REPO}：镜像仓库标识（如aliyun、center）
     *
     * 示例：
     * - https://arthas.aliyun.com/download/3.5.1?mirror=aliyun
     */
    private static final String ARTHAS_DOWNLOAD_URL = "https://arthas.aliyun.com/download/${VERSION}?mirror=${REPO}";

    /**
     * 连接超时时间（毫秒）
     *
     * 建立HTTP连接的超时时间为3秒。
     * 超过这个时间未建立连接将抛出异常。
     */
    private static final int CONNECTION_TIMEOUT = 3000;

    /**
     * 读取最新发布的版本号
     *
     * 该方法从远程服务器获取Arthas的最新版本号。
     *
     * 执行流程：
     * 1. 打开与ARTHAS_LATEST_VERSIONS_URL的连接
     * 2. 读取返回的版本号字符串
     * 3. 去除首尾空白字符
     * 4. 返回版本号
     *
     * 错误处理：
     * - 如果发生异常，记录错误日志并返回null
     * - 异常类型包括：IOException、网络异常等
     *
     * @return 最新版本号字符串，如果读取失败则返回null
     */
    public static String readLatestReleaseVersion() {
        InputStream inputStream = null;
        try {
            // 打开URL连接
            URLConnection connection = openURLConnection(ARTHAS_LATEST_VERSIONS_URL);
            // 获取输入流
            inputStream = connection.getInputStream();
            // 读取版本号并去除首尾空白
            return IOUtils.toString(inputStream).trim();
        } catch (Throwable t) {
            // 记录错误日志
            AnsiLog.error("Can not read arthas version from: " + ARTHAS_LATEST_VERSIONS_URL);
            AnsiLog.debug(t);
        } finally {
            // 确保关闭输入流
            IOUtils.close(inputStream);
        }
        return null;
    }

    /**
     * 读取远程服务器上的所有版本列表
     *
     * 该方法从远程服务器获取所有可用的Arthas版本列表。
     *
     * 执行流程：
     * 1. 打开与ARTHAS_VERSIONS_URL的连接
     * 2. 读取返回的版本列表字符串
     * 3. 按换行符分割字符串
     * 4. 去除每个版本号的首尾空白
     * 5. 返回版本列表
     *
     * 返回格式示例：
     * - ["3.5.1", "3.5.0", "3.4.5", ...]
     *
     * @return 版本号列表，如果读取失败则返回null
     */
    public static List<String> readRemoteVersions() {
        InputStream inputStream = null;
        try {
            // 打开URL连接
            URLConnection connection = openURLConnection(ARTHAS_VERSIONS_URL);
            // 获取输入流
            inputStream = connection.getInputStream();
            // 读取版本列表字符串
            String versionsStr = IOUtils.toString(inputStream);
            // 按换行符分割（支持\r\n和\n）
            String[] versions = versionsStr.split("\r\n");

            // 构建结果列表
            ArrayList<String> result = new ArrayList<String>();
            for (String version : versions) {
                // 添加去除空白后的版本号
                result.add(version.trim());
            }
            return result;

        } catch (Throwable t) {
            // 记录错误日志
            AnsiLog.error("Can not read arthas versions from: " + ARTHAS_VERSIONS_URL);
            AnsiLog.debug(t);
        } finally {
            // 确保关闭输入流
            IOUtils.close(inputStream);
        }
        return null;
    }

    /**
     * 获取处理后的仓库URL
     *
     * 该方法根据参数处理仓库URL，确保格式正确。
     *
     * 处理逻辑：
     * 1. 移除URL末尾的斜杠（如果存在）
     * 2. 如果http参数为true，将https协议转换为http
     *
     * @param repoUrl 原始仓库URL
     * @param http 是否使用HTTP协议（true则强制使用HTTP）
     * @return 处理后的仓库URL
     */
    private static String getRepoUrl(String repoUrl, boolean http) {
        // 移除URL末尾的斜杠
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }

        // 如果需要强制使用HTTP，将https转换为http
        if (http && repoUrl.startsWith("https")) {
            repoUrl = "http" + repoUrl.substring("https".length());
        }
        return repoUrl;
    }

    /**
     * 下载Arthas打包文件
     *
     * 该方法从远程服务器下载指定版本的Arthas压缩包，并解压到指定目录。
     *
     * 执行流程：
     * 1. 构建下载URL
     * 2. 创建临时文件用于保存下载的压缩包
     * 3. 下载压缩包到临时文件
     * 4. 解压压缩包到目标目录
     *
     * 目录结构：
     * savePath/
     *   └── arthasVersion/
     *       └── arthas/
     *           ├── arthas-core.jar
     *           ├── arthas-agent.jar
     *           └── arthas-spy.jar
     *
     * @param repoMirror 仓库镜像标识（如"aliyun"、"center"）
     * @param http 是否使用HTTP协议
     * @param arthasVersion 要下载的Arthas版本号
     * @param savePath 保存路径的根目录
     * @throws IOException 如果下载或解压失败
     */
    public static void downArthasPackaging(String repoMirror, boolean http, String arthasVersion, String savePath)
            throws IOException {
        // 构建仓库URL（这里实际上应该用ARTHAS_DOWNLOAD_URL）
        String repoUrl = getRepoUrl(ARTHAS_DOWNLOAD_URL, http);

        // 构建解压目标目录
        File unzipDir = new File(savePath, arthasVersion + File.separator + "arthas");

        // 创建临时文件用于保存下载的压缩包
        File tempFile = File.createTempFile("arthas", "arthas");

        // 输出调试信息
        AnsiLog.debug("Arthas download temp file: " + tempFile.getAbsolutePath());

        // 构建实际的下载URL，替换模板变量
        String remoteDownloadUrl = repoUrl.replace("${REPO}", repoMirror).replace("${VERSION}", arthasVersion);
        AnsiLog.info("Start download arthas from remote server: " + remoteDownloadUrl);
        // 下载文件到临时位置，并显示进度
        saveUrl(tempFile.getAbsolutePath(), remoteDownloadUrl, true);
        AnsiLog.info("Download arthas success.");
        // 解压文件到目标目录
        IOUtils.unzip(tempFile.getAbsolutePath(), unzipDir.getAbsolutePath());
    }

    /**
     * 下载URL内容并保存到文件
     *
     * 该方法从指定的URL下载文件，并保存到本地文件系统。
     * 支持显示下载进度。
     *
     * 执行流程：
     * 1. 打开URL连接
     * 2. 获取文件大小（从Content-Length头）
     * 3. 创建输出文件
     * 4. 循环读取并写入数据
     * 5. 每秒输出一次下载进度
     *
     * @param filename 保存的本地文件路径
     * @param urlString 要下载的URL
     * @param printProgress 是否打印下载进度
     * @throws IOException 如果下载失败
     */
    private static void saveUrl(final String filename, final String urlString, boolean printProgress)
            throws IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            // 打开URL连接
            URLConnection connection = openURLConnection(urlString);
            // 获取缓冲输入流
            in = new BufferedInputStream(connection.getInputStream());
            // 尝试获取文件大小
            List<String> values = connection.getHeaderFields().get("Content-Length");
            int fileSize = 0;
            if (values != null && !values.isEmpty()) {
                String contentLength = values.get(0);
                if (contentLength != null) {
                    // 将Content-Length解析为整数
                    fileSize = Integer.parseInt(contentLength);
                }
            }

            // 创建文件输出流
            fout = new FileOutputStream(filename);

            // 创建1MB的缓冲区
            final byte[] data = new byte[1024 * 1024];
            int totalCount = 0; // 已下载的总字节数
            int count; // 本次读取的字节数
            long lastPrintTime = System.currentTimeMillis(); // 上次打印进度的时间

            // 循环读取数据
            while ((count = in.read(data, 0, data.length)) != -1) {
                totalCount += count; // 累加已下载字节数

                // 如果需要打印进度
                if (printProgress) {
                    long now = System.currentTimeMillis();
                    // 每秒打印一次进度
                    if (now - lastPrintTime > 1000) {
                        AnsiLog.info("File size: {}, downloaded size: {}, downloading ...", formatFileSize(fileSize),
                                formatFileSize(totalCount));
                        lastPrintTime = now;
                    }
                }
                // 写入数据到文件
                fout.write(data, 0, count);
            }
        } catch (javax.net.ssl.SSLException e) {
            // 处理SSL/TLS连接错误
            AnsiLog.error("TLS connect error, please try to add --use-http argument.");
            AnsiLog.error("URL: " + urlString);
            AnsiLog.error(e);
        } finally {
            // 确保关闭流
            IOUtils.close(in);
            IOUtils.close(fout);
        }
    }

    /**
     * 打开URL连接（支持重定向）
     *
     * 该方法打开与指定URL的连接，并自动处理HTTP重定向。
     *
     * 支持的重定向类型：
     * - 301 Moved Permanently（永久重定向）
     * - 302 Found（临时重定向）
     * - 303 See Other（查看其他）
     *
     * @param url 要连接的URL
     * @return URLConnection对象
     * @throws MalformedURLException 如果URL格式错误
     * @throws IOException 如果连接失败
     */
    private static URLConnection openURLConnection(String url) throws MalformedURLException, IOException {
        // 打开连接
        URLConnection connection = new URL(url).openConnection();
        // 如果是HTTP连接，设置超时并处理重定向
        if (connection instanceof HttpURLConnection) {
            // 设置连接超时
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            // 获取响应码
            // normally, 3xx is redirect
            int status = ((HttpURLConnection) connection).getResponseCode();
            // 检查是否需要重定向
            if (status != HttpURLConnection.HTTP_OK) {
                // 检查是否是重定向响应
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    // 获取重定向目标URL
                    String newUrl = connection.getHeaderField("Location");
                    AnsiLog.debug("Try to open url: {}, redirect to: {}", url, newUrl);
                    // 递归处理重定向
                    return openURLConnection(newUrl);
                }
            }
        }
        return connection;
    }

    /**
     * 格式化文件大小
     *
     * 该方法将字节数转换为人类可读的文件大小表示。
     *
     * 转换规则：
     * - < 1 KB：显示为Bytes
     * - < 1 MB：显示为KB
     * - < 1 GB：显示为MB
     * - < 1 TB：显示为GB
     * - ≥ 1 TB：显示为TB
     *
     * 格式示例：
     * - 1024 → "1.00 KB"
     * - 1536 → "1.50 KB"
     * - 1048576 → "1.00 MB"
     *
     * @param size 文件大小（字节数）
     * @return 格式化后的文件大小字符串
     */
    private static String formatFileSize(long size) {
        String hrSize;

        // 转换为各种单位
        double b = size; // 字节
        double k = size / 1024.0; // KB
        double m = ((size / 1024.0) / 1024.0); // MB
        double g = (((size / 1024.0) / 1024.0) / 1024.0); // GB
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0); // TB

        // 创建格式化对象，保留两位小数
        DecimalFormat dec = new DecimalFormat("0.00");

        // 根据大小选择合适的单位
        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" Bytes");
        }

        return hrSize;
    }

}
