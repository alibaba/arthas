package com.taobao.arthas.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.Test;

import com.taobao.arthas.core.env.ArthasEnvironment;
import com.taobao.arthas.core.env.PropertiesPropertySource;

public class ConfigureStartupScriptTest {
    @Test
    public void shouldBindStartupScript() {
        Properties properties = new Properties();
        properties.put("arthas.startupScript", "/opt/arthas/startup.as");
        ArthasEnvironment environment = new ArthasEnvironment();
        environment.addLast(new PropertiesPropertySource("test", properties));

        Configure configure = new Configure();
        BinderUtils.inject(environment, configure);

        assertThat(configure.getStartupScript()).isEqualTo("/opt/arthas/startup.as");
    }
}
