package com.taobao.arthas.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author beiwei30 on 09/12/2016.
 */
public class ArthasClassloader extends URLClassLoader {
    public ArthasClassloader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        // 优先从parent（SystemClassLoader）里加载系统类，避免抛出ClassNotFoundException
        if (name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
            return super.loadClass(name, resolve);
        }
        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (ClassNotFoundException e) {
            // ignore
        }

        try {
            Class<?> aClass = loadClassFromClassPath(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            // ignore
        }

        return super.loadClass(name, resolve);
    }

    private Class<?> loadClassFromClassPath(String name) throws ClassNotFoundException {
        String classpath = System.getProperty("java.class.path");
        String[] paths = classpath.split(File.pathSeparator);

        byte[] classData = null;
        for (String path : paths) {
            if (path.endsWith(".jar")) {
                classData = loadClassDataFromJar(name, path);
            } else {
                classData = loadClassDataFromDirectory(name, path);
            }

            if (classData != null) {
                break;
            }
        }

        if (classData == null) {
            throw new ClassNotFoundException("Class " + name + " not found in CLASSPATH.");
        }
        return defineClass(name, classData, 0, classData.length);
    }

    private byte[] loadClassDataFromDirectory(String className, String classpath) {
        String path = classpath + File.separator
            + className.replace('.', File.separatorChar) + ".class";
        try (InputStream input = new FileInputStream(path)) {
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] loadClassDataFromJar(String className, String jarPath) {
        String entryName = className.replace('.', '/') + ".class";
        try (JarFile jarFile = new JarFile(jarPath)) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                return null;
            }

            try (InputStream input = jarFile.getInputStream(entry)) {
                byte[] buffer = new byte[(int) entry.getSize()];
                input.read(buffer);
                return buffer;
            }
        } catch (IOException e) {
            return null;
        }
    }
}
