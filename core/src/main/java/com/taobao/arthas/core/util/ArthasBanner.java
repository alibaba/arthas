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
 * @author beiwei30 on 16/11/2016.
 */
public class ArthasBanner {
    private static final String LOGO_LOCATION = "/com/taobao/arthas/core/res/logo.txt";
    private static final String CREDIT_LOCATION = "/com/taobao/arthas/core/res/thanks.txt";
    private static final String VERSION_LOCATION = "/com/taobao/arthas/core/res/version";
    private static final String WIKI = "https://arthas.aliyun.com/doc";
    private static final String TUTORIALS = "https://arthas.aliyun.com/doc/arthas-tutorials.html";
    private static final String ARTHAS_LATEST_VERSIONS_URL = "https://arthas.aliyun.com/api/latest_version";

    private static final int CONNECTION_TIMEOUT = 1000;

    private static final int READ_TIMEOUT = 1000;

    private static String LOGO = "Welcome to Arthas";
    private static String VERSION = "unknown";
    private static String THANKS = "";

    private static final Logger logger = LoggerFactory.getLogger(ArthasBanner.class);

    static {
        try {
            String logoText = IOUtils.toString(ShellServerOptions.class.getResourceAsStream(LOGO_LOCATION));
            THANKS = IOUtils.toString(ShellServerOptions.class.getResourceAsStream(CREDIT_LOCATION));
            InputStream versionInputStream = ShellServerOptions.class.getResourceAsStream(VERSION_LOCATION);
            if (versionInputStream != null) {
                VERSION = IOUtils.toString(versionInputStream).trim();
            } else {
                String implementationVersion = ArthasBanner.class.getPackage().getImplementationVersion();
                if (implementationVersion != null) {
                    VERSION = implementationVersion;
                }
            }

            StringBuilder sb = new StringBuilder();
            String[] LOGOS = new String[6];
            int i = 0, j = 0;
            for (String line : logoText.split("\n")) {
                sb.append(line);
                sb.append("\n");
                if (i++ == 4) {
                    LOGOS[j++] = sb.toString();
                    i = 0;
                    sb.setLength(0);
                }
            }

            TableElement logoTable = new TableElement();
            logoTable.row(label(LOGOS[0]).style(Decoration.bold.fg(Color.red)),
                    label(LOGOS[1]).style(Decoration.bold.fg(Color.yellow)),
                    label(LOGOS[2]).style(Decoration.bold.fg(Color.cyan)),
                    label(LOGOS[3]).style(Decoration.bold.fg(Color.magenta)),
                    label(LOGOS[4]).style(Decoration.bold.fg(Color.green)),
                    label(LOGOS[5]).style(Decoration.bold.fg(Color.blue)));
            LOGO = RenderUtil.render(logoTable);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static String wiki() {
        return WIKI;
    }

    public static String tutorials() {
        return TUTORIALS;
    }

    public static String credit() {
        return THANKS;
    }

    public static String version() {
        return VERSION;
    }

    public static String logo() {
        return LOGO;
    }

    public static String plainTextLogo() {
        return RenderUtil.ansiToPlainText(LOGO);
    }

    public static String welcome() {
        return welcome(Collections.<String, String>emptyMap());
    }

    public static String welcome(Map<String, String> infos) {
        logger.info("Current arthas version: {}, recommend latest version: {}", version(), latestVersion());
        String appName = System.getProperty("project.name");
        if (appName == null) {
            appName = System.getProperty("app.name");
        }
        if (appName == null) {
            appName = System.getProperty("spring.application.name");
        }
        TableElement table = new TableElement().rightCellPadding(1)
                        .row("wiki", wiki())
                        .row("tutorials", tutorials())
                        .row("version", version())
                        .row("main_class", PidUtils.mainClass());

        if (appName != null) {
            table.row("app_name", appName);
        }
        table.row("pid", PidUtils.currentPid())
             .row("start_time", DateUtils.getStartDateTime())
             .row("current_time", DateUtils.getCurrentDateTime());
        for (Entry<String, String> entry : infos.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        return logo() + "\n" + RenderUtil.render(table);
    }

    static String latestVersion() {
        final String[] version = { "" };
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URLConnection urlConnection = openURLConnection(ARTHAS_LATEST_VERSIONS_URL);
                    InputStream inputStream = urlConnection.getInputStream();
                    version[0] = com.taobao.arthas.common.IOUtils.toString(inputStream).trim();
                } catch (Throwable e) {
                    logger.debug("get latest version error", e);
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(2000); // Wait up to 2 seconds for the version check
        } catch (Throwable e) {
            // Ignore
        }

        return version[0];
    }

    /**
     * support redirect
     *
     * @param url
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private static URLConnection openURLConnection(String url) throws MalformedURLException, IOException {
        URLConnection connection = new URL(url).openConnection();
        if (connection instanceof HttpURLConnection) {
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            // normally, 3xx is redirect
            int status = ((HttpURLConnection) connection).getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = connection.getHeaderField("Location");
                    logger.debug("Try to open url: {}, redirect to: {}", url, newUrl);
                    return openURLConnection(newUrl);
                }
            }
        }
        return connection;
    }
}
