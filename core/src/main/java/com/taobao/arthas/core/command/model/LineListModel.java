package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * Available line numbers for line command.
 */
public class LineListModel extends ResultModel {
    private String className;
    private String sourceFile;
    private String methodName;
    private String methodDesc;
    private List<Integer> lines;

    @Override
    public String getType() {
        return "line-list";
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public List<Integer> getLines() {
        return lines;
    }

    public void setLines(List<Integer> lines) {
        this.lines = lines;
    }
}
