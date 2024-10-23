package com.alibaba.arthas.nat.agent.management.web.factory;

import com.alibaba.arthas.nat.agent.management.web.discovery.NativeAgentProxyDiscovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: NativeAgentProxyDiscoveryFactory
 * @author：flzjkl
 * @date: 2024-10-20 20:37
 */
public class NativeAgentProxyDiscoveryFactory {

    private static final String FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.management.web.NativeAgentProxyDiscoveryFactory";
    private static Map<String, NativeAgentProxyDiscovery> nativeAgentProxyDiscoveryMap = new ConcurrentHashMap<>();

    private static volatile NativeAgentProxyDiscoveryFactory nativeAgentProxyDiscoveryFactory;

    private NativeAgentProxyDiscoveryFactory() {
        Map<String, String> registrationConfigMap = readConfigInfo(FILE_PATH);
        loadNativeAgentDiscovery2Map(registrationConfigMap);
    }

    public static NativeAgentProxyDiscoveryFactory getNativeAgentProxyDiscoveryFactory() {
        if (nativeAgentProxyDiscoveryFactory == null) {
            synchronized (NativeAgentProxyDiscoveryFactory.class) {
                if (nativeAgentProxyDiscoveryFactory == null) {
                    nativeAgentProxyDiscoveryFactory = new NativeAgentProxyDiscoveryFactory();
                }
            }
        }
        return nativeAgentProxyDiscoveryFactory;
    }

    private void loadNativeAgentDiscovery2Map(Map<String, String> registrationConfigMap) {
        for (Map.Entry<String, String> entry : registrationConfigMap.entrySet()) {
            String name = entry.getKey();
            String classPath = entry.getValue();

            try {
                Class<?> clazz = Class.forName(classPath);
                Constructor<?> constructor = clazz.getConstructor();
                NativeAgentProxyDiscovery instance = (NativeAgentProxyDiscovery) constructor.newInstance();
                nativeAgentProxyDiscoveryMap.put(name, instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public Map<String, String> readConfigInfo (String filePath) {
        Map<String, String> nativeAgentDiscoveryConfigMap = new ConcurrentHashMap<>();
        ClassLoader classLoader = NativeAgentProxyDiscoveryFactory.class.getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + filePath);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        nativeAgentDiscoveryConfigMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nativeAgentDiscoveryConfigMap;
    }

    public NativeAgentProxyDiscovery getNativeAgentProxyDiscovery(String name) {
        return nativeAgentProxyDiscoveryMap.get(name);
    }
}
