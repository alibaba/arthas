package com.alibaba.arthas.nat.agent.factory;

import com.alibaba.arthas.nat.agent.registry.NativeAgentRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: NativeAgentRegistryFactory create all the realization of the registry
 * @author：flzjkl
 * @date: 2024-09-15 16:22
 */
public class NativeAgentRegistryFactory {

    private static final String FILE_PATH = "META-INF/arthas/com.alibaba.arthas.native.agent.NativeAgentRegistryFactory";
    private static Map<String, NativeAgentRegistry> registrationMap = new ConcurrentHashMap<>();
    private static volatile NativeAgentRegistryFactory nativeAgentRegistryFactory;

    private NativeAgentRegistryFactory() {
        Map<String, String> registrationConfigMap = readConfigInfo(FILE_PATH);
        loadRegister2Map(registrationConfigMap);
    }

    public static NativeAgentRegistryFactory getNativeAgentClientRegisterFactory() {
        if (nativeAgentRegistryFactory == null) {
            synchronized (NativeAgentRegistryFactory.class) {
                if (nativeAgentRegistryFactory == null) {
                    nativeAgentRegistryFactory = new NativeAgentRegistryFactory();
                }
            }
        }
        return nativeAgentRegistryFactory;
    }

    private void loadRegister2Map(Map<String, String> registrationConfigMap) {
        for (Map.Entry<String, String> entry : registrationConfigMap.entrySet()) {
            String name = entry.getKey();
            String classPath = entry.getValue();

            try {
                Class<?> clazz = Class.forName(classPath);
                Constructor<?> constructor = clazz.getConstructor();
                NativeAgentRegistry instance = (NativeAgentRegistry) constructor.newInstance();
                registrationMap.put(name, instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Map<String, String> readConfigInfo (String filePath) {
        Map<String, String> registrationConfigMap = new ConcurrentHashMap<>();
        ClassLoader classLoader = NativeAgentRegistryFactory.class.getClassLoader();

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

    public NativeAgentRegistry getServiceRegistration(String name) {
        return registrationMap.get(name);
    }
}
