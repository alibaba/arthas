package com.alibaba.arthas.nat.agent.client.cluster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: NativeAgentClientRegistryFactory create all the realization of the registry
 * @author：flzjkl
 * @date: 2024-09-15 16:22
 */
public class NativeAgentClientRegistryFactory {

    private static final String FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.client.NativeAgentClientRegistryFactory";
    private static Map<String, NativeAgentClientRegistry> registrationMap = new ConcurrentHashMap<>();
    private static volatile NativeAgentClientRegistryFactory nativeAgentClientRegistryFactory;

    private NativeAgentClientRegistryFactory() {
        Map<String, String> registrationConfigMap = readConfigInfo(FILE_PATH);
        loadRegister2Map(registrationConfigMap);
    }

    public static NativeAgentClientRegistryFactory getNativeAgentClientRegisterFactory() {
        if (nativeAgentClientRegistryFactory == null) {
            synchronized (NativeAgentClientRegistryFactory.class) {
                if (nativeAgentClientRegistryFactory == null) {
                    nativeAgentClientRegistryFactory = new NativeAgentClientRegistryFactory();
                }
            }
        }
        return nativeAgentClientRegistryFactory;
    }

    private void loadRegister2Map(Map<String, String> registrationConfigMap) {
        for (Map.Entry<String, String> entry : registrationConfigMap.entrySet()) {
            String name = entry.getKey();
            String classPath = entry.getValue();

            try {
                Class<?> clazz = Class.forName(classPath);
                Constructor<?> constructor = clazz.getConstructor();
                NativeAgentClientRegistry instance = (NativeAgentClientRegistry) constructor.newInstance();
                registrationMap.put(name, instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Map<String, String> readConfigInfo (String filePath) {
        Map<String, String> registrationConfigMap = new ConcurrentHashMap<>();
        ClassLoader classLoader = NativeAgentClientRegistryFactory.class.getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(filePath); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + filePath);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        registrationConfigMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return registrationConfigMap;
    }

    public NativeAgentClientRegistry getServiceRegistration(String name) {
        return registrationMap.get(name);
    }
}
