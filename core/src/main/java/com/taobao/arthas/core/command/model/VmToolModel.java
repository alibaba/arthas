package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * 
 * @author hengyunabc 2022-04-24
 *
 */
public class VmToolModel extends ResultModel {
    private Object value;
    private int expand = 1;

    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;


    @Override
    public String getType() {
        return "vmtool";
    }

    public Object getValue() {
        return value;
    }

    public VmToolModel setValue(Object value) {
        this.value = value;
        return this;
    }

    public int getExpand() {
        return expand;
    }

    public VmToolModel setExpand(int expand) {
        this.expand = expand;
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
