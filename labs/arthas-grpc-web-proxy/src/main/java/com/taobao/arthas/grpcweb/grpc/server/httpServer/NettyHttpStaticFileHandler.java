package com.taobao.arthas.grpcweb.grpc.server.httpServer;
 
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
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
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import javax.activation.MimetypesFileTypeMap;

public class NettyHttpStaticFileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());
    // 资源所在路径
    private final String STATIC_LOCATION;

    public NettyHttpStaticFileHandler(String staticLocation){
        this.STATIC_LOCATION = staticLocation;
    }
 
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws URISyntaxException, IOException {
        // 获取URI
        String uri = new URI(request.uri()).getPath();
        // 设置不支持favicon.ico文件
        if ("/favicon.ico".equals(uri)) {
            return;
        }
        if ("/".equals(uri)) {
            uri = "/index.html";
        }
        // 根据路径地址构建文件
        String path = Paths.get(this.STATIC_LOCATION, uri).toString();
        File file = new File(path);
        // 状态为1xx的话，继续请求
        if (HttpUtil.is100ContinueExpected(request)) {
            send100Continue(ctx);
        }
        // 当文件隐藏/不存在/是目录/非文件的时候，将资源指向NOT_FOUND
        if (file.isHidden() || !file.exists() || file.isDirectory() || !file.isFile()) {
            sendNotFound(ctx);
            return;
        }
        final RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            sendNotFound(ctx);
            throw new RuntimeException(e);
        }
        HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
 
        // 设置文件格式内容
        if (path.endsWith(".html")){
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        }else if(path.endsWith(".js")){
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-javascript");
        }else if(path.endsWith(".css")){
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/css; charset=UTF-8");
        }else{
        	MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
        	response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimetypesFileTypeMap.getContentType(path));
        }

        boolean keepAlive =  HttpUtil.isKeepAlive(request);
 
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, randomAccessFile.length());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.write(response);
 
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture =
                    ctx.write(new DefaultFileRegion(randomAccessFile.getChannel(), 0, randomAccessFile.length()), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            sendFileFuture =
                    ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(randomAccessFile, 0, randomAccessFile.length(), 10 * 1024 * 1024)),
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
    }
 
    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }
    
    private static void sendNotFound(ChannelHandlerContext ctx){
    	FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
    	response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
    	ctx.writeAndFlush(response);
    }
}