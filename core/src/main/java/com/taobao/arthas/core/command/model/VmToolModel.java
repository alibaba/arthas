package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * 
 * @author hengyunabc 2022-04-24
 *
 */
public class VmToolModel extends ResultModel {
    private ObjectVO value;

    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;


    @Override
    public String getType() {
        return "vmtool";
    }

    public ObjectVO getValue() {
        return value;
    }

    public VmToolModel setValue(ObjectVO value) {
        this.value = value;
        return this;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public VmToolModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public VmToolModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
