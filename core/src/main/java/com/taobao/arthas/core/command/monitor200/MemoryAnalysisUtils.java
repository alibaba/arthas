package com.taobao.arthas.core.command.monitor200;

/**
 * @Author zyzhangyang01
 */
public class MemoryAnalysisUtils {


    private String[] args;

    private String operationName;


    public void MemoryAnalysisUtils(){

    }

    public MemoryAnalysisUtils(String[] args, String operationName) {
        this.args = args;
        this.operationName = operationName;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
