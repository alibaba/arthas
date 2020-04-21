package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.ClassLoaderStat;

import java.util.List;
import java.util.Map;

/**
 * @author gongdewei 2020/4/21
 */
public class ClassLoaderModel extends ResultModel {

    private List<ClassSetVO> allClasses;
    private List<String> resources;
    private ClassVO loadClass;
    private List<String> urls;
    //classloader -l -t
    private List<ClassLoaderVO> classLoaders;
    private Boolean tree;

    private Map<String, ClassLoaderStat> classLoaderStats;

    public ClassLoaderModel() {
    }

    @Override
    public String getType() {
        return "classloader";
    }

    public List<ClassSetVO> getAllClasses() {
        return allClasses;
    }

    public ClassLoaderModel setAllClasses(List<ClassSetVO> allClasses) {
        this.allClasses = allClasses;
        return this;
    }

    public List<String> getResources() {
        return resources;
    }

    public ClassLoaderModel setResources(List<String> resources) {
        this.resources = resources;
        return this;
    }

    public ClassVO getLoadClass() {
        return loadClass;
    }

    public ClassLoaderModel setLoadClass(ClassVO loadClass) {
        this.loadClass = loadClass;
        return this;
    }

    public List<String> getUrls() {
        return urls;
    }

    public ClassLoaderModel setUrls(List<String> urls) {
        this.urls = urls;
        return this;
    }

    public List<ClassLoaderVO> getClassLoaders() {
        return classLoaders;
    }

    public ClassLoaderModel setClassLoaders(List<ClassLoaderVO> classLoaders) {
        this.classLoaders = classLoaders;
        return this;
    }

    public Boolean getTree() {
        return tree;
    }

    public ClassLoaderModel setTree(Boolean tree) {
        this.tree = tree;
        return this;
    }

    public Map<String, ClassLoaderStat> getClassLoaderStats() {
        return classLoaderStats;
    }

    public ClassLoaderModel setClassLoaderStats(Map<String, ClassLoaderStat> classLoaderStats) {
        this.classLoaderStats = classLoaderStats;
        return this;
    }
}
