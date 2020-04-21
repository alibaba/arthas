package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author gongdewei 2020/4/21
 */
public class ClassSetVO {
    private ClassLoaderVO classloader;
    private List<String> classes;

    public ClassSetVO(ClassLoaderVO classloader, List<String> classes) {
        this.classloader = classloader;
        this.classes = classes;
    }

    public ClassLoaderVO getClassloader() {
        return classloader;
    }

    public void setClassloader(ClassLoaderVO classloader) {
        this.classloader = classloader;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }
}
