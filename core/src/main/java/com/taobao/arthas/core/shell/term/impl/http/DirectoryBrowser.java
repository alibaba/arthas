package com.taobao.arthas.core.shell.term.impl.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.taobao.arthas.common.IOUtils;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

/**
 * 
 * @author hengyunabc 2019-11-06
 *
 */
public class DirectoryBrowser {

    //@formatter:off
    private static String pageHeader = "<!DOCTYPE html>\n" + 
                    "<html>\n" + 
                    "\n" + 
                    "<head>\n" + 
                    "    <title>Arthas Resouces: %s</title>\n" + 
                    "    <meta charset=\"utf-8\" name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" + 
                    "    <style>\n" + 
                    "body {\n" + 
                    "    background: #fff;\n" + 
                    "}\n" + 
                    "    </style>\n" + 
                    "</head>\n" + 
                    "\n" + 
                    "<body>\n" + 
                    "    <header>\n" + 
                    "        <h1>%s</h1>\n" + 
                    "    </header>\n" + 
                    "    <hr/>\n" + 
                    "    <main>\n" + 
                    "        <pre id=\"contents\">\n";

    private static String pageFooter = "       </pre>\n" + 
                    "    </main>\n" + 
                    "    <hr/>\n" + 
                    "</body>\n" + 
                    "\n" + 
                    "</html>";
    //@formatter:on

    private static String linePart1Str = "<a href=\"%s\" title=\"%s\">";
    private static String linePart2Str = "%-60s";

    private static String renderDir(File dir) {
        File[] listFiles = dir.listFiles();

        StringBuilder sb = new StringBuilder(8192);
        String dirName = dir.getName() + "/";
        sb.append(String.format(pageHeader, dirName, dirName));

        sb.append("<a href=\"../\" title=\"../\">../</a>\n");

        if (listFiles != null) {
            Arrays.sort(listFiles);
            for (File f : listFiles) {
                if (f.isDirectory()) {
                    String name = f.getName() + "/";
                    String part1Format = String.format(linePart1Str, name, name, name);
                    sb.append(part1Format);

                    String linePart2 = name + "</a>";
                    String part2Format = String.format(linePart2Str, linePart2);
                    sb.append(part2Format);

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String modifyStr = simpleDateFormat.format(new Date(f.lastModified()));

                    sb.append(modifyStr);
                    sb.append("         -      ").append("\r\n");
                }
            }

            for (File f : listFiles) {
                if (f.isFile()) {
                    String name = f.getName();
                    String part1Format = String.format(linePart1Str, name, name, name);
                    sb.append(part1Format);

                    String linePart2 = name + "</a>";
                    String part2Format = String.format(linePart2Str, linePart2);
                    sb.append(part2Format);

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String modifyStr = simpleDateFormat.format(new Date(f.lastModified()));
                    sb.append(modifyStr);

                    String sizeStr = String.format("%10d      ", f.length());
                    sb.append(sizeStr).append("\r\n");
                }
            }
        }

        sb.append(pageFooter);
        return sb.toString();
    }

    public static DefaultFullHttpResponse view(File dir, String path, HttpVersion version) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        File file = new File(path);

        if (isSubFile(dir, file)) {
            DefaultFullHttpResponse fullResp = new DefaultFullHttpResponse(version, HttpResponseStatus.OK);

            if (file.isDirectory()) {
                if (!path.endsWith("/")) {
                    fullResp.setStatus(HttpResponseStatus.FOUND).headers().set(HttpHeaderNames.LOCATION, "/" + path + "/");
                }
                
                String renderResult = renderDir(file);
                fullResp.content().writeBytes(renderResult.getBytes("utf-8"));
                fullResp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
            } else {
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    byte[] content = IOUtils.getBytes(fileInputStream);
                    fullResp.content().writeBytes(content);
                    HttpUtil.setContentLength(fullResp, fullResp.content().readableBytes());
                } finally {
                    IOUtils.close(fileInputStream);
                }
            }
            return fullResp;
        }

        return null;
    }

    public static boolean isSubFile(File parent, File child) throws IOException {
        String parentPath = parent.getCanonicalPath();
        String childPath = child.getCanonicalPath();
        if (parentPath.equals(childPath) || childPath.startsWith(parent.getCanonicalPath() + File.separator)) {
            return true;
        }
        return false;
    }

}
