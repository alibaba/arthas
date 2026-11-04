package com.taobao.arthas.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.taobao.arthas.core.env.ArthasEnvironment;
import com.taobao.arthas.core.env.MapPropertySource;

public class ConfigureTest {

    @Test
    public void shouldTransmitCrossMountNamespaceMetadataThroughAgentArgs() {
        Configure source = new Configure();
        source.setJavaPid(123L);
        source.setCrossMountNamespace(true);

        Map<String, String> attachArgs = FeatureCodec.DEFAULT_COMMANDLINE_CODEC.toMap(source.toString());
        Map<String, Object> targetArgs = new HashMap<String, Object>();
        for (Map.Entry<String, String> entry : attachArgs.entrySet()) {
            targetArgs.put("arthas." + entry.getKey(), entry.getValue());
        }
        ArthasEnvironment environment = new ArthasEnvironment();
        environment.addFirst(new MapPropertySource("agentArgs", targetArgs));
        Configure target = new Configure();
        BinderUtils.inject(environment, target);

        assertThat(target.getJavaPid()).isEqualTo(123L);
        assertThat(target.isCrossMountNamespace()).isTrue();
    }
}
