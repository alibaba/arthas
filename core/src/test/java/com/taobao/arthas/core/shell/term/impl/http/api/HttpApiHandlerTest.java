package com.taobao.arthas.core.shell.term.impl.http.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.core.bytecode.TestHelper;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.history.impl.HistoryManagerImpl;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import net.bytebuddy.agent.ByteBuddyAgent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.instrument.Instrumentation;

class HttpApiHandlerTest {

    @Test
    @DisplayName("http api exec retransform enhance class should return ids in response")
    void testExecRetransform_ReturnsIdsSuccess() throws Throwable {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        TestHelper.appendSpyJar(instrumentation);

        ArthasBootstrap instance = ArthasBootstrap.getInstance(instrumentation, "ip=127.0.0.1");

        HttpApiHandler handler = new HttpApiHandler(new HistoryManagerImpl(), instance.getSessionManager());

        // prepare HTTP request body
        String body = JSON.toJSONString(new Object() {
            public final String action = "exec";
            public final String command = "retransform target/classes/com/taobao/arthas/core/GlobalOptions.class";
            public final Integer execTimeout = 5000;
        });

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/api",
                Unpooled.copiedBuffer(body, CharsetUtil.UTF_8));
        request.headers().set("Content-Type", "application/json");

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        @SuppressWarnings("unchecked")
        Attribute<Object> attribute = mock(Attribute.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.attr(any())).thenReturn((Attribute) attribute);
        when(attribute.get()).thenReturn(null);

        // invoke
        io.netty.handler.codec.http.HttpResponse httpResponse = handler.handle(ctx, request);

        // verify and parse response
        assertThat(httpResponse).isNotNull();
        byte[] bytes = new byte[((io.netty.handler.codec.http.FullHttpResponse) httpResponse).content().readableBytes()];
        ((io.netty.handler.codec.http.FullHttpResponse) httpResponse).content().readBytes(bytes);
        String respJson = new String(bytes, CharsetUtil.UTF_8);

        // expecting succeeded state and jobStatus STOPPED in returned JSON
        assertThat(respJson).contains("\"state\":\"SUCCEEDED\"");
        JSONObject jsonObject = JSON.parseObject(respJson).getJSONObject("body").getJSONArray("results").getJSONObject(0);
        assertThat(jsonObject.containsKey("ids")).isTrue();
        assertThat(jsonObject.getJSONArray("ids").get(0)).isEqualTo(1);
    }
}
