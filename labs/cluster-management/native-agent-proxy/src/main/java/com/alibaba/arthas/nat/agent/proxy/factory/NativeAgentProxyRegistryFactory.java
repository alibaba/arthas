package com.alibaba.arthas.nat.agent.proxy.factory;

import com.alibaba.arthas.nat.agent.proxy.registry.NativeAgentProxyRegistry;

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
public class NativeAgentProxyRegistryFactory {

    private static final String FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.proxy.NativeAgentProxyRegistryFactory";
    private static Map<String, NativeAgentProxyRegistry> nativeAgentProxyRegistryMap = new ConcurrentHashMap<>();

    private static volatile NativeAgentProxyRegistryFactory nativeAgentProxyRegistryFactory;

    private NativeAgentProxyRegistryFactory() {
        Map<String, String> registrationConfigMap = readConfigInfo(FILE_PATH);
        loadNativeAgentProxyRegistry2Map(registrationConfigMap);
    }

    public static NativeAgentProxyRegistryFactory getNativeAgentProxyRegistryFactory() {
        if (nativeAgentProxyRegistryFactory == null) {
            synchronized (NativeAgentProxyRegistryFactory.class) {
                if (nativeAgentProxyRegistryFactory == null) {
                    nativeAgentProxyRegistryFactory = new NativeAgentProxyRegistryFactory();
                }
            }
        }
        return nativeAgentProxyRegistryFactory;
    }

    private void loadNativeAgentProxyRegistry2Map(Map<String, String> registrationConfigMap) {
        for (Map.Entry<String, String> entry : registrationConfigMap.entrySet()) {
            String name = entry.getKey();
            String classPath = entry.getValue();

            try {
                Class<?> clazz = Class.forName(classPath);
                Constructor<?> constructor = clazz.getConstructor();
                NativeAgentProxyRegistry instance = (NativeAgentProxyRegistry) constructor.newInstance();
                nativeAgentProxyRegistryMap.put(name, instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public Map<String, String> readConfigInfo (String filePath) {
        Map<String, String> nativeAgentDiscoveryConfigMap = new ConcurrentHashMap<>();
        ClassLoader classLoader = NativeAgentProxyRegistryFactory.class.getClassLoader();

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

    public NativeAgentProxyRegistry getNativeAgentProxyRegistry(String name) {
        return nativeAgentProxyRegistryMap.get(name);
    }
}
