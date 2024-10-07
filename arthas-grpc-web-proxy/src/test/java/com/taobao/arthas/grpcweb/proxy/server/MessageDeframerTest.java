package com.taobao.arthas.grpcweb.proxy.server;

import com.taobao.arthas.grpcweb.proxy.MessageDeframer;
import com.taobao.arthas.grpcweb.proxy.MessageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;

public class MessageDeframerTest {

    @Test
    public void testProcessInput(){
        String str = "AAAAAAcKBWhlbGxv";
        ByteBuf content = Unpooled.copiedBuffer(str, CharsetUtil.UTF_8);
        InputStream in = new ByteBufInputStream(content);
        String contentTypeStr = "application/grpc-web-text";
        MessageUtils.ContentType contentType = MessageUtils.validateContentType(contentTypeStr);
        MessageDeframer deframer = new MessageDeframer();

        boolean result = deframer.processInput(in, contentType);

        Assert.assertTrue(result);
    }
}
