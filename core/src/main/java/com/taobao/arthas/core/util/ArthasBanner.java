package com.taobao.arthas.core.util;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.PidUtils;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.io.InputStream;
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
        logger.info("arthas version: " + version());
        TableElement table = new TableElement().rightCellPadding(1)
                        .row("wiki", wiki())
                        .row("tutorials", tutorials())
                        .row("version", version())
                        .row("pid", PidUtils.currentPid())
                        .row("time", DateUtils.getCurrentDate());
        for (Entry<String, String> entry : infos.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        return logo() + "\n" + RenderUtil.render(table);
    }
}
