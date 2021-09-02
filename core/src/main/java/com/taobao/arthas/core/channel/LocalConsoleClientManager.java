package com.taobao.arthas.core.channel;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage console clients of local term server
 * @author gongdewei 2020/9/2
 */
public class LocalConsoleClientManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalConsoleClientManager.class);

    // consoleId => client channel
    private Map<String, Channel> clientMap = new ConcurrentHashMap<String, Channel>();

//    public String addConsoleClient(Channel clientChannel) {
//        final String consoleId = generateConsoleId();
//        clientMap.put(consoleId, clientChannel);
//        return consoleId;
//    }

    public String addConsoleClient(String consoleId, Channel clientChannel) {
        clientMap.put(consoleId, clientChannel);
        return consoleId;
    }

    public Channel getConsoleClient(String consoleId) {
        return clientMap.get(consoleId);
    }

    public void closeConsoleClient(String consoleId) {
        logger.info("close console: {}", consoleId);
        Channel clientChannel = clientMap.remove(consoleId);
        if (clientChannel != null) {
            try {
                writeData(clientChannel, getReadActionJson("q\\r"));
                writeData(clientChannel, getReadActionJson("quit\\r"));
            } catch (Exception e) {
                //ignore
            }
            try {
                clientChannel.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    public void writeToConsole(String consoleId, String data) {
        Channel clientChannel = clientMap.get(consoleId);
        try {
            if (clientChannel != null) {
                writeData(clientChannel, data);
            }
        } catch (Throwable e) {
            logger.error("write to console error, consoleId: {}", consoleId, e);
            closeConsoleClient(consoleId);
        }
    }

    public static void writeData(Channel clientChannel, String data) {
        clientChannel.writeAndFlush(new TextWebSocketFrame(data));
    }

    public static String getReadActionJson(String inputChars) {
        return "{\"action\":\"read\",\"data\":\""+inputChars+"\"}";
    }

    public String generateConsoleId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
