package com.alibaba.arthas.nat.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import static com.alibaba.arthas.nat.agent.core.ArthasHomeHandler.ARTHAS_HOME_DIR;

/**
 * @description: list java process via invoke com.taobao.arthas.boot.ProcessUtils#listProcessByJps
 * @author：flzjkl
 * @date: 2024-07-18 8:25
 */
public class ListJvmProcessHandler {

    private static final Logger logger = LoggerFactory.getLogger(ListJvmProcessHandler.class);
    private static final String PROCESS_UTILS_PATH = "com.taobao.arthas.boot.ProcessUtils";
    private static final String LIST_PROCESS_BY_JPS_METHOD = "listProcessByJps";

    public static Map<Long, String> listJvmProcessByInvoke()  {
        if (ARTHAS_HOME_DIR == null) {
            ArthasHomeHandler.findArthasHome();
        }

        if (ARTHAS_HOME_DIR == null) {
            return null;
        }

        String arthasBootPath = ARTHAS_HOME_DIR + File.separator + "arthas-boot.jar";
        Method method = null;
        Object instance = null;
        Map<Long, String> result = null;

        try {
            URLClassLoader classLoader = new URLClassLoader(new URL[]{new File(arthasBootPath).toURI().toURL()});

            Class<?> clazz = classLoader.loadClass(PROCESS_UTILS_PATH);

            method = clazz.getDeclaredMethod(LIST_PROCESS_BY_JPS_METHOD, boolean.class);
            method.setAccessible(true);

            instance = clazz.getDeclaredConstructor().newInstance();

            result = (Map<Long, String>) method.invoke(instance, false);
        } catch (Exception e) {
            logger.error("invoke list java  process failed:" + e.getMessage());
            throw new RuntimeException(e);
        }

        return result;
    }
}
