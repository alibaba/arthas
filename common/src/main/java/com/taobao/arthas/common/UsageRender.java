package com.taobao.arthas.common;

/**
 *
 * @author hengyunabc 2018-11-22
 *
 */
public class UsageRender {

    public static String render(String usage) {
        if (AnsiLog.enableColor()) {
            StringBuilder sb = new StringBuilder(1024);
            String lines[] = usage.split("\\r?\\n");
            for (String line : lines) {
                if (line.startsWith("Usage: ")) {
                    sb.append(AnsiLog.green("Usage: "));
                    sb.append(line.substring("Usage: ".length()));
                } else if (!line.startsWith(" ") && line.endsWith(":")) {
                    sb.append(AnsiLog.green(line));
                } else {
                    sb.append(line);
                }
                sb.append('\n');
            }
            return sb.toString();
        } else {
            return usage;
        }
    }
}
