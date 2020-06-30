package com.taobao.arthas.core.command.model;

import java.util.Collection;
import java.util.List;

/**
 * @author gongdewei 2020/4/21
 */
public class DumpClassModel extends ResultModel {

    private List<ClassVO> dumpedClassFiles;

    private Collection<ClassVO> matchedClasses;

    public DumpClassModel() {
    }

    @Override
    public String getType() {
        return "dump";
    }

    public List<ClassVO> getDumpedClassFiles() {
        return dumpedClassFiles;
    }

    public DumpClassModel setDumpedClassFiles(List<ClassVO> dumpedClassFiles) {
        this.dumpedClassFiles = dumpedClassFiles;
        return this;
    }

    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    public DumpClassModel setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
        return this;
    }

}
