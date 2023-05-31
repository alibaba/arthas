package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * Data model of OgnlCommand
 * @author gongdewei 2020/4/29
 */
public class OgnlModel extends ResultModel {
    private ObjectVO value;

    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;


    @Override
    public String getType() {
        return "ognl";
    }

    public ObjectVO getValue() {
        return value;
    }

    public OgnlModel setValue(ObjectVO value) {
        this.value = value;
        return this;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public OgnlModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public OgnlModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
