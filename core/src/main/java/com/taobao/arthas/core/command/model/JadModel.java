package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.NavigableMap;

/**
 * @author gongdewei 2020/4/22
 * @author hengyunabc 2021-02-23
 */
public class JadModel extends ResultModel {
    private ClassVO classInfo;
    private String location;
    private String source;
    private NavigableMap<Integer,Integer> mappings;
    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;

    //match multiple classes
    private Collection<ClassVO> matchedClasses;

    @Override
    public String getType() {
        return "jad";
    }

    public JadModel() {
    }

    public ClassVO getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassVO classInfo) {
        this.classInfo = classInfo;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public NavigableMap<Integer, Integer> getMappings() {
        return mappings;
    }

    public void setMappings(NavigableMap<Integer, Integer> mappings) {
        this.mappings = mappings;
    }

    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    public void setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public JadModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public JadModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }
}
