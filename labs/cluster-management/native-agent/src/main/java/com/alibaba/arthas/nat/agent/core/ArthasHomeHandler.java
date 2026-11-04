package com.alibaba.arthas.nat.agent.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @description: find arthas home
 * @author：flzjkl
 * @date: 2024-07-27 9:12
 */
public class ArthasHomeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ArthasHomeHandler.class);
    public static File ARTHAS_HOME_DIR;

    public static void findArthasHome() {
        // find arthas home
        File arthasHomeDir = null;
        try {
            if (arthasHomeDir == null) {
                // try to find from ~/.arthas/lib
                File arthasDir = new File(System.getProperty("user.home"), ".arthas" + File.separator + "lib"
                        + File.separator + "arthas");
                verifyArthasHome(arthasDir.getAbsolutePath());
                arthasHomeDir = arthasDir;
            }
        } catch (Exception e) {
            // ignore
        }

        // Try set the directory where arthas-boot.jar is located to arhtas home
        try {
            if (arthasHomeDir == null) {
                URL jarUrl = ArthasHomeHandler.class.getProtectionDomain().getCodeSource().getLocation();
                if (jarUrl != null) {
                    File arthasDir = new File(jarUrl.toURI());
                    // If the path is a JAR file, use it directly
                    String jarDir = arthasDir.getParent();
                    verifyArthasHome(jarDir);
                    if (arthasDir != null) {
                        arthasHomeDir = new File(jarDir);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if (arthasHomeDir == null) {
            logger.error("Please ensure that arthas-native agent-client is in the same directory as arthas-core.jar, arthas-agent.jar, and arthas-spy.jar");
            throw new RuntimeException("arthas home not found");
        }

        ARTHAS_HOME_DIR = arthasHomeDir;
    }

    private static void verifyArthasHome(String arthasHome) {
        File home = new File(arthasHome);
        if (home.isDirectory()) {
            String[] fileList = {"arthas-core.jar", "arthas-agent.jar", "arthas-spy.jar"};

            for (String fileName : fileList) {
                if (!new File(home, fileName).exists()) {
                    logger.error("Please ensure that arthas-native agent-client is in the same directory as arthas-core.jar, arthas-agent.jar, and arthas-spy.jar");
                    throw new IllegalArgumentException(
                            fileName + " do not exist, arthas home: " + home.getAbsolutePath());
                }
            }
            return;
        }

        throw new IllegalArgumentException("illegal arthas home: " + home.getAbsolutePath());
    }
}
