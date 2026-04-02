package com.taobao.arthas.core.command.model;

/**
 * 命令执行结果抽象模型
 * 用于表示所有Arthas命令执行后的返回结果基类
 *
 * @author gongdewei 2020-03-26
 */
public abstract class ResultModel {

    /**
     * 任务ID
     * 用于标识和跟踪具体的命令执行任务
     */
    private int jobId;

    /**
     * 获取命令类型（名称）
     * 每个具体的命令结果类需要实现此方法来返回其对应的命令类型
     *
     * @return 命令类型字符串，例如"session"、"status"等
     */
    public abstract String getType();


    /**
     * 获取任务ID
     *
     * @return 当前任务ID
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * 设置任务ID
     *
     * @param jobId 要设置的任务ID
     */
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
