package com.taobao.arthas.compiler;

import java.net.URI;

public class ClassUrlWrapper {
    private final URI uri;

    private final String className;

    public ClassUrlWrapper(String className, URI uri) {
        this.className = className;
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public String getClassName() {
        return className;
    }
}
