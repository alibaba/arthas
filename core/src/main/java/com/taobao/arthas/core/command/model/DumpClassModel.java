package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.List;

/**
 * @author gongdewei 2020/4/21
 */
public class DumpClassModel extends ResultModel {

    private List<DumpClassVO> dumpedClasses;

    private Collection<ClassVO> matchedClasses;
    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;

    public DumpClassModel() {
    }

    @Override
    public String getType() {
        return "dump";
    }

    public List<DumpClassVO> getDumpedClasses() {
        return dumpedClasses;
    }

    public DumpClassModel setDumpedClasses(List<DumpClassVO> dumpedClasses) {
        this.dumpedClasses = dumpedClasses;
        return this;
    }

    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    public DumpClassModel setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
        return this;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public DumpClassModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public DumpClassModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

}
