package com.taobao.arthas.agent;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author beiwei30 on 09/12/2016.
 * arthas-agent会使用自定义的classloader(ArthasClassLoader)加载arthas-core.jar里面的com.taobao.arthas.core.config.Configure类以及com.taobao.arthas.core.server.ArthasBootstrap。
 *
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
        } catch (Exception e) {
            // ignore
        }
        return super.loadClass(name, resolve);
    }
}
