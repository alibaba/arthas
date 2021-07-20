package com.taobao.arthas.core.shell.term.impl.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.common.IOUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 
 * @author hengyunabc 2019-11-06
 *
 */
public class DirectoryBrowser {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final long MIN_NETTY_DIRECT_SEND_SIZE = ArthasConstants.MAX_HTTP_CONTENT_LENGTH;
    private static final Logger logger = LoggerFactory.getLogger(DirectoryBrowser.class);
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

    static String renderDir(File dir) {
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

    /**
     *  write data here,still return not null just to know succeeded.
     * @param dir
     * @param path
     * @param request
     * @param ctx
     * @return
     * @throws IOException
     */
    public static DefaultFullHttpResponse directView(File dir, String path, FullHttpRequest request, ChannelHandlerContext ctx) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }

        // path maybe: arthas-output/20201225-203454.svg 
        // 需要取 dir的parent来去掉前缀
        File file = new File(dir.getParent(), path);
        HttpVersion version = request.protocolVersion();
        if (isSubFile(dir, file)) {
            DefaultFullHttpResponse fullResp = new DefaultFullHttpResponse(version, HttpResponseStatus.OK);

            if (file.isDirectory()) {
                if (!path.endsWith("/")) {
                    fullResp.setStatus(HttpResponseStatus.FOUND).headers().set(HttpHeaderNames.LOCATION, "/" + path + "/");
                }
                
                String renderResult = renderDir(file);
                fullResp.content().writeBytes(renderResult.getBytes("utf-8"));
                fullResp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
                ctx.write(fullResp);
                ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                future.addListener(ChannelFutureListener.CLOSE);
                return fullResp;
            } else {
                logger.info("get file now. file:" + file.getPath());
                if (file.isHidden() || !file.exists() || file.isDirectory() || !file.isFile()) {
                    return null;
                }

                RandomAccessFile raf;
                try {
                    raf = new RandomAccessFile(file, "r");
                } catch (Exception ignore) {
                    return null;
                }
                long fileLength = raf.length();
                if (fileLength < MIN_NETTY_DIRECT_SEND_SIZE){
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        byte[] content = IOUtils.getBytes(fileInputStream);
                        fullResp.content().writeBytes(content);
                        HttpUtil.setContentLength(fullResp, fullResp.content().readableBytes());
                    } finally {
                        IOUtils.close(fileInputStream);
                    }
                    ctx.write(fullResp);
                    ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                    future.addListener(ChannelFutureListener.CLOSE);
                    return fullResp;
                }
                logger.info("file {} size bigger than {}, send by future.",file.getName(), MIN_NETTY_DIRECT_SEND_SIZE);
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
                HttpUtil.setContentLength(response, fileLength);
                setContentTypeHeader(response, file);
                setDateAndCacheHeaders(response, file);
                if (HttpUtil.isKeepAlive(request)) {
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }

                // Write the initial line and the header.
                ctx.write(response);
                // Write the content.
                ChannelFuture sendFileFuture;
                ChannelFuture lastContentFuture;
                if (ctx.pipeline().get(SslHandler.class) == null) {
                    sendFileFuture =
                            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
                    // Write the end marker.
                    lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                } else {
                    sendFileFuture =
                            ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                                    ctx.newProgressivePromise());
                    // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                    lastContentFuture = sendFileFuture;
                }

                sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
                    @Override
                    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                        if (total < 0) { // total unknown
                            logger.info(future.channel() + " Transfer progress: " + progress);
                        } else {
                            logger.info(future.channel() + " Transfer progress: " + progress + " / " + total);
                        }
                    }

                    @Override
                    public void operationComplete(ChannelProgressiveFuture future) {
                        logger.info(future.channel() + " Transfer complete.");
                    }
                });

                // Decide whether to close the connection or not.
                if (!HttpUtil.isKeepAlive(request)) {
                    // Close the connection when the whole content is written out.
                    lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                }
                return fullResp;
            }
        }

        return null;
    }
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, 3600);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + 3600);
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    private static void setContentTypeHeader(HttpResponse response, File file) {
        String contentType = "application/octet-stream";
        // 暂时hardcode 大文件的content-type
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
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
