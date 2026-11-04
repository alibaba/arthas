package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author gongdewei 2020/4/16
 */
public class RedefineModel extends ResultModel {

    private int redefinitionCount;

    private List<String> redefinedClasses;
    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;

    public RedefineModel() {
        redefinedClasses = new ArrayList<String>();
    }

    public void addRedefineClass(String className) {
        redefinedClasses.add(className);
        redefinitionCount++;
    }

    public int getRedefinitionCount() {
        return redefinitionCount;
    }

    public void setRedefinitionCount(int redefinitionCount) {
        this.redefinitionCount = redefinitionCount;
    }

    public List<String> getRedefinedClasses() {
        return redefinedClasses;
    }

    public void setRedefinedClasses(List<String> redefinedClasses) {
        this.redefinedClasses = redefinedClasses;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public RedefineModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public RedefineModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    @Override
    public String getType() {
        return "redefine";
    }

}
