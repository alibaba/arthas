package com.alibaba.arthas.tunnel.proxy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.net.InetAddress;

/**
 * 启动入口
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@Slf4j
@EnableWebFlux
@EnableDiscoveryClient
@SpringBootApplication
public class ArthasProxyApplication {

    public static void main(String[] args) {
        run(ArthasProxyApplication.class, args, WebApplicationType.REACTIVE);
    }

    private static void run(Class<?> mainClass, String[] args, WebApplicationType webApplicationType) {
        try {
            SpringApplication app = new SpringApplicationBuilder(mainClass).web(webApplicationType).build();
            app.setBannerMode(Banner.Mode.OFF);

            ConfigurableApplicationContext context = app.run(args);
            Environment env = context.getEnvironment();
            logApplicationServerAfterRunning(env);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void logApplicationServerAfterRunning(Environment env) {
        String appName = StringUtils.trimToEmpty(env.getProperty("spring.application.name"));
        String contextPath = StringUtils.trimToEmpty(env.getProperty("server.servlet.context-path"));
        int serverPort = NumberUtils.toInt(env.getProperty("server.port"));
        String protocol = env.containsProperty("server.ssl.key-store") ? "https" : "http";
        String localhostAddress = "localhost";
        String hostAddress;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            hostAddress = localhostAddress;
            log.warn("The host name could not be determined, using ‘localhost‘ as fallback");
        }

        log.info("\n----------------------------------------------------------\n\t"
                + "Application '{}' is running! \n\t"
                + "Local Access URL: \t{}://{}:{}{}\n\t"
                + "External Access URL: \t{}://{}:{}{}"
                + "\n----------------------------------------------------------",
        appName,
        protocol, localhostAddress, serverPort, contextPath,
        protocol, hostAddress, serverPort, contextPath);
    }
}
