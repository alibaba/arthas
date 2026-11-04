package com.taobao.arthas.core.command.model;

/**
 * Command execute result
 *
 * @author gongdewei 2020-03-26
 */
public abstract class ResultModel {

    private int jobId;

    /**
     * Command type (name)
     *
     * @return
     */
    public abstract String getType();


    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
