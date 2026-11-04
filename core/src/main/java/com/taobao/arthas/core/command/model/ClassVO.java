package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/8
 */
public class ClassVO {

    private String name;
    private String[] classloader;
    private String classLoaderHash;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getClassloader() {
        return classloader;
    }

    public void setClassloader(String[] classloader) {
        this.classloader = classloader;
    }

    public String getClassLoaderHash() {
        return classLoaderHash;
    }

    public void setClassLoaderHash(String classLoaderHash) {
        this.classLoaderHash = classLoaderHash;
    }
}
