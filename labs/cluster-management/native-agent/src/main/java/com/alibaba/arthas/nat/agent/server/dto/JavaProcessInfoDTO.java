package com.alibaba.arthas.nat.agent.server.dto;

/**
 * @description: Java Process DTO
 * @author：flzjkl
 * @date: 2024-09-06 21:31
 */
public class JavaProcessInfoDTO {
    private String processName;
    private Integer pid;


    public JavaProcessInfoDTO() {

    }

    public JavaProcessInfoDTO(String applicationName, Integer pid) {
        this.processName = applicationName;
        this.pid = pid;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public Integer getPid() {
        return pid;
    }

    public String getProcessName() {
        return processName;
    }

}
