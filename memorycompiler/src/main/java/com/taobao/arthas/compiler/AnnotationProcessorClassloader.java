package com.taobao.arthas.compiler;

import java.net.URL;
import java.net.URLClassLoader;

public class AnnotationProcessorClassloader extends URLClassLoader {

    public AnnotationProcessorClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}
