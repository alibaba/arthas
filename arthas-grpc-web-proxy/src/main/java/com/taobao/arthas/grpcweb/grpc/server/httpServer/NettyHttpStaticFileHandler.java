package com.taobao.arthas.grpcweb.grpc.server.httpServer;
 
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
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
import java.io.RandomAccessFile;
 
import javax.activation.MimetypesFileTypeMap;

public class NettyHttpStaticFileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 资源所在路径
    private static final String STATIC_LOCATION = "F:\\ASummer\\arthas\\arthas-grpc-web-proxy\\src\\main\\java\\com\\taobao\\arthas\\grpcweb\\grpc\\server\\httpServer\\dist";
 
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 获取URI
        String uri = request.uri();
        // 设置不支持favicon.ico文件
        if ("/favicon.ico".equals(uri)) {
            return;
        }
        // 根据路径地址构建文件
        String path = STATIC_LOCATION + uri;
        File html = new File(path);
 
        // 状态为1xx的话，继续请求
        
        if (HttpUtil.is100ContinueExpected(request)) {
            send100Continue(ctx);
        }
 
        // 当文件不存在的时候，将资源指向NOT_FOUND
        if (!html.exists()) {
            sendNotFound(ctx);
            return;
        }
 
        final RandomAccessFile randomAccessFile = new RandomAccessFile(html, "r");
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
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.out.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }
 
            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.out.println(future.channel() + " Transfer complete.");
            }
        });
  
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