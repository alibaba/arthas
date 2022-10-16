package com.alibaba.arthas.tunnel.server;

import java.net.URI;
import java.net.URISyntaxException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.arthas.tunnel.common.MethodConstants;
import com.alibaba.arthas.tunnel.common.URIConstants;

/**
 * 
 * @author hengyunabc 2020-10-22
 *
 */
public class URITest {
    @Test
    public void test() throws URISyntaxException {
        String id = "xxx";
        URI responseUri = new URI("response", null, "/", "method=" + MethodConstants.AGENT_REGISTER + "&id=" + id,
                null);

        String string = responseUri.toString();

        String uriString = UriComponentsBuilder.newInstance().scheme("response").path("/")
                .queryParam("method", MethodConstants.AGENT_REGISTER).queryParam("id", id).build().toUriString();

        Assertions.assertThat(string).isEqualTo(uriString).isEqualTo("response:/?method=agentRegister&id=xxx");
    }

    @Test
    public void testEncode() throws URISyntaxException {
        String id = "xxx/%ff#ff";
        URI responseUri = new URI("response", null, "/", "method=" + MethodConstants.AGENT_REGISTER + "&id=" + id,
                null);

        String string = responseUri.toString();

        String uriString = UriComponentsBuilder.newInstance().scheme(URIConstants.RESPONSE).path("/")
                .queryParam(URIConstants.METHOD, MethodConstants.AGENT_REGISTER).queryParam(URIConstants.ID, id).build()
                .encode().toUriString();

        Assertions.assertThat(string).isEqualTo(uriString)
                .isEqualTo("response:/?method=agentRegister&id=xxx/%25ff%23ff");
    }

    @Test
    public void test3() throws URISyntaxException {

        String agentId = "ffff";
        String clientConnectionId = "ccccc";
        URI uri = new URI("response", null, "/", "method=" + MethodConstants.START_TUNNEL + "&id=" + agentId
                + "&clientConnectionId=" + clientConnectionId, null);

        String string = uri.toString();

        String uriString = UriComponentsBuilder.newInstance().scheme(URIConstants.RESPONSE).path("/")
                .queryParam(URIConstants.METHOD, MethodConstants.START_TUNNEL).queryParam(URIConstants.ID, agentId)
                .queryParam(URIConstants.CLIENT_CONNECTION_ID, clientConnectionId).build().toUriString();

        System.err.println(string);
        Assertions.assertThat(string).isEqualTo(uriString)
                .isEqualTo("response:/?method=startTunnel&id=ffff&clientConnectionId=ccccc");
    }
}
