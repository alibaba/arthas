package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.taobao.arthas.core.command.klass100.RetransformCommand.RetransformEntry;

/**
 * 
 * @author hengyunabc 2021-01-06
 *
 */
public class RetransformModel extends ResultModel {

    private int retransformCount;

    private List<String> retransformClasses;
    private Collection<ClassLoaderVO> matchedClassLoaders;
    private String classLoaderClass;

    private List<RetransformEntry> retransformEntries;

    private RetransformEntry deletedRetransformEntry;
    
//    private List<ClassVO> trigger

//    List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
    public RetransformModel() {
    }

    public void addRetransformClass(String className) {
        if (retransformClasses == null) {
            retransformClasses = new ArrayList<String>();
        }
        retransformClasses.add(className);
        retransformCount++;
    }

    public int getRetransformCount() {
        return retransformCount;
    }

    public void setRetransformCount(int retransformCount) {
        this.retransformCount = retransformCount;
    }

    public List<String> getRetransformClasses() {
        return retransformClasses;
    }

    public void setRetransformClasses(List<String> retransformClasses) {
        this.retransformClasses = retransformClasses;
    }

    public String getClassLoaderClass() {
        return classLoaderClass;
    }

    public RetransformModel setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
        return this;
    }

    public Collection<ClassLoaderVO> getMatchedClassLoaders() {
        return matchedClassLoaders;
    }

    public RetransformModel setMatchedClassLoaders(Collection<ClassLoaderVO> matchedClassLoaders) {
        this.matchedClassLoaders = matchedClassLoaders;
        return this;
    }

    public List<RetransformEntry> getRetransformEntries() {
        return retransformEntries;
    }

    public void setRetransformEntries(List<RetransformEntry> retransformEntries) {
        this.retransformEntries = retransformEntries;
    }

    public RetransformEntry getDeletedRetransformEntry() {
        return deletedRetransformEntry;
    }

    public void setDeletedRetransformEntry(RetransformEntry deletedRetransformEntry) {
        this.deletedRetransformEntry = deletedRetransformEntry;
    }

    @Override
    public String getType() {
        return "retransform";
    }

}
