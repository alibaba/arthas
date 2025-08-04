package com.alibaba.arthas.nat.agent.proxy.factory;

import com.alibaba.arthas.nat.agent.proxy.discovery.NativeAgentDiscovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: NativeAgentDiscoveryFactory
 * @author：flzjkl
 * @date: 2024-09-15 16:22
 */
public class NativeAgentDiscoveryFactory {

    private static final String FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.proxy.NativeAgentDiscoveryFactory";
    private static Map<String, NativeAgentDiscovery> nativeAgentDiscoveryMap = new ConcurrentHashMap<>();

    private static volatile NativeAgentDiscoveryFactory nativeAgentDiscoveryFactory;

    private NativeAgentDiscoveryFactory() {
        Map<String, String> registrationConfigMap = readConfigInfo(FILE_PATH);
        loadNativeAgentDiscovery2Map(registrationConfigMap);
    }

    public static NativeAgentDiscoveryFactory getNativeAgentDiscoveryFactory() {
        if (nativeAgentDiscoveryFactory == null) {
            synchronized (NativeAgentDiscoveryFactory.class) {
                if (nativeAgentDiscoveryFactory == null) {
                    nativeAgentDiscoveryFactory = new NativeAgentDiscoveryFactory();
                }
            }
        }
        return nativeAgentDiscoveryFactory;
    }

    private void loadNativeAgentDiscovery2Map(Map<String, String> registrationConfigMap) {
        for (Map.Entry<String, String> entry : registrationConfigMap.entrySet()) {
            String name = entry.getKey();
            String classPath = entry.getValue();

            try {
                Class<?> clazz = Class.forName(classPath);
                Constructor<?> constructor = clazz.getConstructor();
                NativeAgentDiscovery instance = (NativeAgentDiscovery) constructor.newInstance();
                nativeAgentDiscoveryMap.put(name, instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public Map<String, String> readConfigInfo (String filePath) {
        Map<String, String> nativeAgentDiscoveryConfigMap = new ConcurrentHashMap<>();
        ClassLoader classLoader = NativeAgentDiscoveryFactory.class.getClassLoader();

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

    public NativeAgentDiscovery getNativeAgentDiscovery (String name) {
        return nativeAgentDiscoveryMap.get(name);
    }
}
