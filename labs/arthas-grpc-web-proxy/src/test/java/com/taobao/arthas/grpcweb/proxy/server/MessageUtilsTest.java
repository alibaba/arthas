package com.taobao.arthas.grpcweb.proxy.server;

import com.taobao.arthas.grpcweb.proxy.MessageUtils;
import org.junit.Assert;
import org.junit.Test;

public class MessageUtilsTest {

    @Test
    public void testValidateContentType(){
        String contentType1 = "application/grpc-web";
        MessageUtils.ContentType result1 = MessageUtils.validateContentType(contentType1);
        String contentType2 = "application/grpc-web+proto";
        MessageUtils.ContentType result2 = MessageUtils.validateContentType(contentType2);
        String contentType3 = "application/grpc-web-text";
        MessageUtils.ContentType result3 = MessageUtils.validateContentType(contentType3);
        String contentType4 = "application/grpc-web-text+proto";
        MessageUtils.ContentType result4 = MessageUtils.validateContentType(contentType4);
        MessageUtils.ContentType result5 = MessageUtils.ContentType.GRPC_WEB_BINARY;
        try {
            String contentType5 = null;
            result5 = MessageUtils.validateContentType(contentType5);
        }catch (IllegalArgumentException e){
            result5 = null;
        }

        Assert.assertEquals(result1,MessageUtils.ContentType.GRPC_WEB_BINARY);
        Assert.assertEquals(result2,MessageUtils.ContentType.GRPC_WEB_BINARY);
        Assert.assertEquals(result3,MessageUtils.ContentType.GRPC_WEB_TEXT);
        Assert.assertEquals(result4,MessageUtils.ContentType.GRPC_WEB_TEXT);
        Assert.assertNull(result5);
    }
}
