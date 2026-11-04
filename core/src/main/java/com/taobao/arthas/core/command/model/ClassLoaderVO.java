package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/4/21
 */
public class ClassLoaderVO {
    private String name;
    private String hash;
    private String parent;
    private Integer loadedCount;
    private Integer numberOfInstances;
    private List<ClassLoaderVO> children;

    public ClassLoaderVO() {
    }

    public void addChild(ClassLoaderVO child){
        if (this.children == null){
            this.children = new ArrayList<ClassLoaderVO>();
        }
        this.children.add(child);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Integer getLoadedCount() {
        return loadedCount;
    }

    public void setLoadedCount(Integer loadedCount) {
        this.loadedCount = loadedCount;
    }

    public Integer getNumberOfInstances() {
        return numberOfInstances;
    }

    public void setNumberOfInstances(Integer numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    public List<ClassLoaderVO> getChildren() {
        return children;
    }

    public void setChildren(List<ClassLoaderVO> children) {
        this.children = children;
    }
}
