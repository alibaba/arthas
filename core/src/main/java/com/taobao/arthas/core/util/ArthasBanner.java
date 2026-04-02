package com.taobao.arthas.core.util;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import static com.taobao.text.ui.Element.label;

/**
 * Arthas启动横幅工具类
 * 负责生成和显示Arthas的启动欢迎信息，包括Logo、版本号、文档链接等
 *
 * @author beiwei30 on 16/11/2016.
 */
public class ArthasBanner {
    /**
     * Logo文件位置
     * 存储在classpath资源路径下的logo文本文件
     */
    private static final String LOGO_LOCATION = "/com/taobao/arthas/core/res/logo.txt";

    /**
     * 致谢信息文件位置
     * 存储在classpath资源路径下的感谢文本文件
     */
    private static final String CREDIT_LOCATION = "/com/taobao/arthas/core/res/thanks.txt";

    /**
     * 版本号文件位置
     * 存储在classpath资源路径下的版本信息文件
     */
    private static final String VERSION_LOCATION = "/com/taobao/arthas/core/res/version";

    /**
     * Arthas官方文档地址
     */
    private static final String WIKI = "https://arthas.aliyun.com/doc";

    /**
     * Arthas教程地址
     */
    private static final String TUTORIALS = "https://arthas.aliyun.com/doc/arthas-tutorials.html";

    /**
     * Arthas最新版本查询接口
     */
    private static final String ARTHAS_LATEST_VERSIONS_URL = "https://arthas.aliyun.com/api/latest_version";

    /**
     * HTTP连接超时时间（毫秒）
     */
    private static final int CONNECTION_TIMEOUT = 1000;

    /**
     * HTTP读取超时时间（毫秒）
     */
    private static final int READ_TIMEOUT = 1000;

    /**
     * Logo文本内容
     * 默认值为"Welcome to Arthas"，在静态初始化块中会被实际Logo内容替换
     */
    private static String LOGO = "Welcome to Arthas";

    /**
     * Arthas版本号
     * 默认值为"unknown"，在静态初始化块中会被实际版本号替换
     */
    private static String VERSION = "unknown";

    /**
     * 致谢信息文本
     * 默认为空字符串，在静态初始化块中会被实际内容替换
     */
    private static String THANKS = "";

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(ArthasBanner.class);

    /**
     * 静态初始化块
     * 在类加载时执行，负责：
     * 1. 读取Logo文本文件
     * 2. 读取致谢信息文件
     * 3. 读取版本号信息
     * 4. 将Logo文本分割并渲染成彩色ASCII艺术字
     */
    static {
        try {
            // 从资源文件读取Logo文本内容
            String logoText = IOUtils.toString(ShellServerOptions.class.getResourceAsStream(LOGO_LOCATION));

            // 从资源文件读取致谢信息
            THANKS = IOUtils.toString(ShellServerOptions.class.getResourceAsStream(CREDIT_LOCATION));

            // 尝试读取版本号文件
            InputStream versionInputStream = ShellServerOptions.class.getResourceAsStream(VERSION_LOCATION);
            if (versionInputStream != null) {
                // 如果版本号文件存在，直接读取并去除首尾空格
                VERSION = IOUtils.toString(versionInputStream).trim();
            } else {
                // 如果版本号文件不存在，尝试从包的实现版本中获取
                String implementationVersion = ArthasBanner.class.getPackage().getImplementationVersion();
                if (implementationVersion != null) {
                    VERSION = implementationVersion;
                }
            }

            // 将Logo文本分割成6部分，每5行为一部分
            StringBuilder sb = new StringBuilder();
            String[] LOGOS = new String[6];
            int i = 0, j = 0;
            for (String line : logoText.split("\n")) {
                sb.append(line);
                sb.append("\n");
                // 每5行分割一次
                if (i++ == 4) {
                    LOGOS[j++] = sb.toString();
                    i = 0;
                    sb.setLength(0);
                }
            }

            // 创建彩色Logo表格
            // 使用6种不同颜色（红、黄、青、洋红、绿、蓝）渲染Logo的不同部分
            TableElement logoTable = new TableElement();
            logoTable.row(label(LOGOS[0]).style(Decoration.bold.fg(Color.red)),
                    label(LOGOS[1]).style(Decoration.bold.fg(Color.yellow)),
                    label(LOGOS[2]).style(Decoration.bold.fg(Color.cyan)),
                    label(LOGOS[3]).style(Decoration.bold.fg(Color.magenta)),
                    label(LOGOS[4]).style(Decoration.bold.fg(Color.green)),
                    label(LOGOS[5]).style(Decoration.bold.fg(Color.blue)));
            // 渲染Logo表格为最终字符串
            LOGO = RenderUtil.render(logoTable);
        } catch (Throwable e) {
            // 如果读取失败，打印堆栈信息并使用默认值
            e.printStackTrace();
        }
    }

    /**
     * 获取Arthas Wiki文档地址
     *
     * @return Wiki文档URL字符串
     */
    public static String wiki() {
        return WIKI;
    }

    /**
     * 获取Arthas教程地址
     *
     * @return 教程URL字符串
     */
    public static String tutorials() {
        return TUTORIALS;
    }

    /**
     * 获取致谢信息
     *
     * @return 致谢信息文本
     */
    public static String credit() {
        return THANKS;
    }

    /**
     * 获取Arthas版本号
     *
     * @return 版本号字符串
     */
    public static String version() {
        return VERSION;
    }

    /**
     * 获取彩色Logo文本
     *
     * @return 包含ANSI颜色代码的Logo字符串
     */
    public static String logo() {
        return LOGO;
    }

    /**
     * 获取纯文本Logo（去除ANSI颜色代码）
     *
     * @return 纯文本Logo字符串
     */
    public static String plainTextLogo() {
        return RenderUtil.ansiToPlainText(LOGO);
    }

    /**
     * 生成默认欢迎信息（无额外信息）
     *
     * @return 欢迎信息字符串
     */
    public static String welcome() {
        return welcome(Collections.<String, String>emptyMap());
    }

    /**
     * 生成欢迎信息
     * 包含Logo、版本信息、文档链接、进程信息等
     *
     * @param infos 额外的信息键值对，会添加到欢迎信息中
     * @return 完整的欢迎信息字符串
     */
    public static String welcome(Map<String, String> infos) {
        // 记录当前版本和最新版本信息
        logger.info("Current arthas version: {}, recommend latest version: {}", version(), latestVersion());

        // 尝试获取应用名称，按优先级依次检查：project.name、app.name、spring.application.name
        String appName = System.getProperty("project.name");
        if (appName == null) {
            appName = System.getProperty("app.name");
        }
        if (appName == null) {
            appName = System.getProperty("spring.application.name");
        }

        // 创建信息表格
        TableElement table = new TableElement().rightCellPadding(1)
                        .row("wiki", wiki())                            // Wiki文档地址
                        .row("tutorials", tutorials())                   // 教程地址
                        .row("version", version())                       // Arthas版本
                        .row("main_class", PidUtils.mainClass());       // 主类信息

        // 如果有应用名称，添加到表格中
        if (appName != null) {
            table.row("app_name", appName);
        }

        // 添加进程信息
        table.row("pid", PidUtils.currentPid())                     // 进程ID
             .row("start_time", DateUtils.getStartDateTime())       // 进程启动时间
             .row("current_time", DateUtils.getCurrentDateTime());  // 当前时间

        // 添加额外的自定义信息
        for (Entry<String, String> entry : infos.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        // 返回Logo和信息表格的组合
        return logo() + "\n" + RenderUtil.render(table);
    }

    /**
     * 获取Arthas最新版本号
     * 通过HTTP请求远程API获取最新版本，最多等待2秒
     * 使用守护线程在后台执行，避免阻塞主流程
     *
     * @return 最新版本号字符串，如果获取失败则返回空字符串
     */
    static String latestVersion() {
        // 使用数组以便在匿名Runnable中修改值
        final String[] version = { "" };

        // 创建守护线程来获取最新版本信息
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 打开HTTP连接获取最新版本信息
                    URLConnection urlConnection = openURLConnection(ARTHAS_LATEST_VERSIONS_URL);
                    InputStream inputStream = urlConnection.getInputStream();
                    version[0] = com.taobao.arthas.common.IOUtils.toString(inputStream).trim();
                } catch (Throwable e) {
                    // 获取失败时记录调试日志
                    logger.debug("get latest version error", e);
                }
            }
        });

        // 设置为守护线程，不阻止JVM退出
        thread.setDaemon(true);
        thread.start();

        // 等待最多2秒获取版本信息
        try {
            thread.join(2000); // Wait up to 2 seconds for the version check
        } catch (Throwable e) {
            // Ignore - 忽略异常，使用默认空字符串
        }

        return version[0];
    }

    /**
     * 打开URL连接，支持HTTP重定向
     * 如果服务器返回3xx重定向状态码，会自动跟随重定向
     *
     * @param url 要打开的URL地址
     * @return URLConnection对象
     * @throws MalformedURLException URL格式错误
     * @throws IOException IO异常
     */
    private static URLConnection openURLConnection(String url) throws MalformedURLException, IOException {
        // 打开URL连接
        URLConnection connection = new URL(url).openConnection();

        if (connection instanceof HttpURLConnection) {
            // 设置连接超时时间
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            // 设置读取超时时间
            connection.setReadTimeout(READ_TIMEOUT);

            // 获取HTTP响应状态码
            // normally, 3xx is redirect
            int status = ((HttpURLConnection) connection).getResponseCode();

            // 如果响应状态不是200 OK，检查是否为重定向
            if (status != HttpURLConnection.HTTP_OK) {
                // 检查是否为临时重定向(302)、永久重定向(301)或临时重定向(303)
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    // 获取重定向的目标URL
                    String newUrl = connection.getHeaderField("Location");
                    logger.debug("Try to open url: {}, redirect to: {}", url, newUrl);
                    // 递归调用自己，跟随重定向
                    return openURLConnection(newUrl);
                }
            }
        }
        return connection;
    }
}
