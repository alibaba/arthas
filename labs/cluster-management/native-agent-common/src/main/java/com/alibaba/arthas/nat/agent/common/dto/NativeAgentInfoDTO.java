package com.alibaba.arthas.nat.agent.common.dto;

/**
 * @description: NativeAgentInfoDTO
 * @author：flzjkl
 * @date: 2024-09-05 8:04
 */
public class NativeAgentInfoDTO {
    private String ip;
    private Integer httpPort;
    private Integer wsPort;

    public NativeAgentInfoDTO() {

    }

    public NativeAgentInfoDTO(String ip, Integer httpPort, Integer wsPort) {
        this.ip = ip;
        this.httpPort = httpPort;
        this.wsPort = wsPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }

    public Integer getWsPort() {
        return wsPort;
    }

    public void setWsPort(Integer wsPort) {
        this.wsPort = wsPort;
    }
}

