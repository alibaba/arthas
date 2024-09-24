package com.alibaba.arthas.nat.agent.server.server.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @description: HttpResourcesHandler
 * @author：flzjkl
 * @date: 2024-09-23 7:44
 */
public class HttpResourcesHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpResourcesHandler.class);

    private static final String RESOURCES_PATH = "native-agent";
    public FullHttpResponse handlerResources (String path) {
        FullHttpResponse resp = null;
        if ("/".equals(path)) {
            path = "/index.html";
        }
        if (path.contains(".html") || path.contains(".css") || path.contains(".js") || path.contains(".ico") || path.contains(".png")) {
            if (path.contains("?")) {
                path = path.split("\\?")[0];
            }
            InputStream is = getClass().getClassLoader().getResourceAsStream(RESOURCES_PATH + path);
            if (is != null) {
                try {
                    ByteBuf content = readInputStream(is);
                    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
                    HttpHeaders headers = response.headers();
                    headers.set(HttpHeaderNames.CONTENT_TYPE, getContentType(path));
                    headers.set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                    headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    resp = response;
                } catch (IOException e) {
                    logger.error("find resources error:" + e.getMessage());
                    resp = new DefaultFullHttpResponse(resp.getProtocolVersion(), HttpResponseStatus.NOT_FOUND);
                    resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return resp;
    }


    private String getContentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else {
            return "application/octet-stream";
        }
    }

    private ByteBuf readInputStream(InputStream is) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        byte[] tmp = new byte[1024];
        int length;
        while ((length = is.read(tmp)) != -1) {
            buffer.writeBytes(tmp, 0, length);
        }
        is.close();
        return buffer;
    }

}
