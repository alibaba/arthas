package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * @Author TaoKan
 * @Date 2025/3/23 9:12 PM
 */
public class QLExpressModel extends ResultModel {
    private ObjectVO value;

    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;


    @Override
    public String getType() {
        return "ql";
    }

    public ObjectVO getValue() {
        return value;
    }

    public QLExpressModel setValue(ObjectVO value) {
        this.value = value;
        return this;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public QLExpressModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public QLExpressModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
