package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/4/16
 */
public class RedefineModel extends ResultModel {

    private int redefinitionCount;

    private List<String> redefinedClasses;

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

    @Override
    public String getType() {
        return "redefine";
    }

}
