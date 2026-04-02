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
 * 目录浏览器
 * <p>
 * 该类提供了 HTTP 文件浏览功能，用于：
 * <ul>
 *   <li>展示目录内容，生成 HTML 格式的目录列表页面</li>
 *   <li>支持文件下载，包括小文件和大文件的传输</li>
 *   <li>支持目录导航，包括返回上级目录</li>
 *   <li>根据文件大小自动选择最优的传输方式</li>
 * </ul>
 * </p>
 * <p>
 * 对于小文件（小于 MIN_NETTY_DIRECT_SEND_SIZE），直接将文件内容加载到内存并一次性发送；
 * 对于大文件，使用零拷贝技术或分块传输以提高性能
 * </p>
 *
 * @author hengyunabc 2019-11-06
 */
public class DirectoryBrowser {

    /**
     * HTTP 日期格式
     * <p>
     * 用于设置 HTTP 响应头中的日期格式，符合 RFC 1123 标准
     * </p>
     */
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * HTTP 日期时区
     * <p>
     * HTTP 协议规定使用 GMT 时区
     * </p>
     */
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";

    /**
     * Netty 直接发送的最小文件大小阈值
     * <p>
     * 当文件大小大于此值时，使用零拷贝或分块传输方式；
     * 小于此值时，直接将文件内容加载到内存发送
     * </p>
     */
    public static final long MIN_NETTY_DIRECT_SEND_SIZE = ArthasConstants.MAX_HTTP_CONTENT_LENGTH;

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(DirectoryBrowser.class);
    //@formatter:off
    /**
     * HTML 页面头部模板
     * <p>
     * 包含 HTML 文档的声明、头部信息和样式设置。
     * 使用两个 %s 占位符，分别用于填充标题
     * </p>
     */
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

    /**
     * HTML 页面尾部模板
     * <p>
     * 关闭 HTML 标签
     * </p>
     */
    private static String pageFooter = "       </pre>\n" +
                    "    </main>\n" +
                    "    <hr/>\n" +
                    "</body>\n" +
                    "\n" +
                    "</html>";
    //@formatter:on

    /**
     * 文件/目录链接的第一部分模板
     * <p>
     * 格式化为 HTML 链接标签，包含 href 和 title 属性
     * </p>
     */
    private static String linePart1Str = "<a href=\"%s\" title=\"%s\">";

    /**
     * 文件/目录名称格式化模板
     * <p>
     * 固定宽度为 60 个字符，左对齐
     * </p>
     */
    private static String linePart2Str = "%-60s";

    /**
     * 渲染目录为 HTML 格式
     * <p>
     * 生成目录列表的 HTML 页面，包含：
     * <ul>
     *   <li>上级目录链接（如果需要）</li>
     *   <li>子目录列表（按名称排序，显示修改时间）</li>
     *   <li>文件列表（按名称排序，显示修改时间和大小）</li>
     * </ul>
     * </p>
     *
     * @param dir            要渲染的目录
     * @param printParentLink 是否打印上级目录链接
     * @return 渲染后的 HTML 字符串
     */
    static String renderDir(File dir, boolean printParentLink) {
        // 获取目录下的所有文件和子目录
        File[] listFiles = dir.listFiles();

        // 使用 StringBuilder 构建 HTML 内容
        StringBuilder sb = new StringBuilder(8192);
        String dirName = dir.getName() + "/";
        // 添加页面头部
        sb.append(String.format(pageHeader, dirName, dirName));

        // 如果需要，添加上级目录链接
        if (printParentLink) {
            sb.append("<a href=\"../\" title=\"../\">../</a>\n");
        }

        if (listFiles != null) {
            // 按名称排序
            Arrays.sort(listFiles);

            // 先处理所有子目录
            for (File f : listFiles) {
                if (f.isDirectory()) {
                    String name = f.getName() + "/";
                    // 格式化链接部分
                    String part1Format = String.format(linePart1Str, name, name, name);
                    sb.append(part1Format);

                    // 添加名称和结束标签
                    String linePart2 = name + "</a>";
                    String part2Format = String.format(linePart2Str, linePart2);
                    sb.append(part2Format);

                    // 添加修改时间
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String modifyStr = simpleDateFormat.format(new Date(f.lastModified()));
                    sb.append(modifyStr);
                    // 目录不显示大小，显示 "-"
                    sb.append("         -      ").append("\r\n");
                }
            }

            // 再处理所有文件
            for (File f : listFiles) {
                if (f.isFile()) {
                    String name = f.getName();
                    // 格式化链接部分
                    String part1Format = String.format(linePart1Str, name, name, name);
                    sb.append(part1Format);

                    // 添加名称和结束标签
                    String linePart2 = name + "</a>";
                    String part2Format = String.format(linePart2Str, linePart2);
                    sb.append(part2Format);

                    // 添加修改时间
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String modifyStr = simpleDateFormat.format(new Date(f.lastModified()));
                    sb.append(modifyStr);

                    // 添加文件大小
                    String sizeStr = String.format("%10d      ", f.length());
                    sb.append(sizeStr).append("\r\n");
                }
            }
        }

        // 添加页面尾部
        sb.append(pageFooter);
        return sb.toString();
    }

    /**
     * 直接查看或下载文件/目录
     * <p>
     * 根据请求的路径处理文件或目录：
     * <ul>
     *   <li>如果是目录，渲染目录列表页面</li>
     *   <li>如果是小文件（小于阈值），直接读取到内存并发送</li>
     *   <li>如果是大文件，使用零拷贝或分块传输</li>
     * </ul>
     * </p>
     *
     * @param dir     基础目录，用于安全检查
     * @param path    请求的相对路径
     * @param request HTTP 请求对象
     * @param ctx     通道处理器上下文
     * @return HTTP 响应对象，如果文件不在允许范围内返回 null
     * @throws IOException 读写文件时可能抛出的异常
     */
    public static DefaultFullHttpResponse directView(File dir, String path, FullHttpRequest request, ChannelHandlerContext ctx) throws IOException {
        // 移除路径开头的斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // path maybe: arthas-output/20201225-203454.svg
        // 需要取 dir的parent来去掉前缀
        File file = new File(dir.getParent(), path);
        HttpVersion version = request.protocolVersion();

        // 安全检查：确保请求的文件在允许的目录范围内
        if (isSubFile(dir, file)) {
            DefaultFullHttpResponse fullResp = new DefaultFullHttpResponse(version, HttpResponseStatus.OK);

            if (file.isDirectory()) {
                // 处理目录请求
                if (!path.endsWith("/")) {
                    // 如果路径不以 / 结尾，重定向到带 / 的路径
                    fullResp.setStatus(HttpResponseStatus.FOUND).headers().set(HttpHeaderNames.LOCATION, "/" + path + "/");
                }
                // 渲染目录列表
                String renderResult = renderDir(file, !isSameFile(dir, file));
                fullResp.content().writeBytes(renderResult.getBytes("utf-8"));
                fullResp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
                ctx.write(fullResp);
                ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                future.addListener(ChannelFutureListener.CLOSE);
                return fullResp;
            } else {
                // 处理文件请求
                logger.info("get file now. file:" + file.getPath());
                // 检查文件是否可访问
                if (file.isHidden() || !file.exists() || file.isDirectory() || !file.isFile()) {
                    return null;
                }

                long fileLength = file.length();
                // 小文件处理：直接读取到内存
                if (fileLength < MIN_NETTY_DIRECT_SEND_SIZE){
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        byte[] content = IOUtils.getBytes(fileInputStream);
                        fullResp.content().writeBytes(content);
                        HttpUtil.setContentLength(fullResp, fullResp.content().readableBytes());
                    } finally {
                        IOUtils.close(fileInputStream);
                    }
                    // 异步发送响应
                    ChannelFuture channelFuture = ctx.writeAndFlush(fullResp);
                    channelFuture.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                        } else {
                            future.channel().close();
                        }
                    });
                    return fullResp;
                }

                // 大文件处理：使用零拷贝或分块传输
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
                RandomAccessFile raf = new RandomAccessFile(file, "r"); // will closed by netty
                if (ctx.pipeline().get(SslHandler.class) == null) {
                    // 非 SSL 连接：使用零拷贝传输
                    sendFileFuture =
                            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
                    // Write the end marker.
                    lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                } else {
                    // SSL 连接：使用分块传输
                    sendFileFuture =
                            ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                                    ctx.newProgressivePromise());
                    // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                    lastContentFuture = sendFileFuture;
                }

                // 添加传输进度监听器
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

        // 文件不在允许的范围内
        return null;
    }
    /**
     * 设置 HTTP 响应的日期和缓存头
     * <p>
     * 设置以下 HTTP 头：
     * <ul>
     *   <li>Date: 当前日期</li>
     *   <li>Expires: 过期时间（当前时间 + 1 小时）</li>
     *   <li>Cache-Control: 缓存控制策略</li>
     *   <li>Last-Modified: 文件最后修改时间</li>
     * </ul>
     * </p>
     *
     * @param response    HTTP 响应对象
     * @param fileToCache 要缓存的文件对象
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        // 创建日期格式化器，使用美国英语环境和 GMT 时区
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header: 设置当前日期
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers: 设置缓存策略，缓存 1 小时
        time.add(Calendar.SECOND, 3600);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + 3600);
        // Last-Modified: 设置文件的最后修改时间
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * 设置 HTTP 响应的内容类型头
     * <p>
     * 目前硬编码为 application/octet-stream，表示二进制文件
     * </p>
     *
     * @param response HTTP 响应对象
     * @param file     文件对象
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        String contentType = "application/octet-stream";
        // 暂时hardcode 大文件的content-type
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    /**
     * 检查一个文件是否是另一个文件的子文件或子目录
     * <p>
     * 通过比较规范路径来判断 child 是否在 parent 目录下
     * </p>
     *
     * @param parent 父目录
     * @param child  子文件或目录
     * @return 如果 child 是 parent 的子文件或目录返回 true，否则返回 false
     * @throws IOException 获取规范路径时可能抛出的异常
     */
    public static boolean isSubFile(File parent, File child) throws IOException {
        String parentPath = parent.getCanonicalPath();
        String childPath = child.getCanonicalPath();
        // 检查 child 是否是 parent 或 parent 的子文件
        if (parentPath.equals(childPath) || childPath.startsWith(parent.getCanonicalPath() + File.separator)) {
            return true;
        }
        return false;
    }

    /**
     * 检查两个文件是否是同一个文件
     * <p>
     * 通过比较规范路径来判断
     * </p>
     *
     * @param a 第一个文件
     * @param b 第二个文件
     * @return 如果是同一个文件返回 true，否则返回 false
     */
    public static boolean isSameFile(File a, File b) {
        try {
            return a.getCanonicalPath().equals(b.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }
}
