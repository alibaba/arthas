package org.example.jfranalyzerbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "arthas")
public class ArthasConfig {
    private String jfrStoragePath;

    public String getJfrStoragePath() {
        return jfrStoragePath;
    }

    public void setJfrStoragePath(String jfrStoragePath) {
        this.jfrStoragePath = jfrStoragePath;
    }
}
