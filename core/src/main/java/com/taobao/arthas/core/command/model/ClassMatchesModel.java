package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * Match multiple classes result for GetStaticCommand / DumpClassCommand
 * @author gongdewei 2020/4/20
 */
public class ClassMatchesModel extends ResultModel {

    private Collection<ClassVO> matchedClasses;

    public ClassMatchesModel() {
    }

    public ClassMatchesModel(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    public Collection<ClassVO> getMatchedClasses() {
        return matchedClasses;
    }

    public void setMatchedClasses(Collection<ClassVO> matchedClasses) {
        this.matchedClasses = matchedClasses;
    }

    @Override
    public String getType() {
        return "class_matches";
    }

}
