package com.alibaba.arthas.channel.server.message.topic;

import java.util.Objects;

/**
 * @author gongdewei 2020/8/12
 */
public class ActionResponseTopic implements Topic {

    private String agentId;

    private String requestId;

    public ActionResponseTopic(String agentId, String requestId) {
        this.agentId = agentId;
        this.requestId = requestId;
    }

    @Override
    public String getAgentId() {
        return agentId;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String getTopic() {
        return "arthas:channel:topics:agent:" + agentId + ":response:" + requestId;
    }

    @Override
    public String toString() {
        return "ActionResponseTopic{" +
                "agentId='" + agentId + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionResponseTopic that = (ActionResponseTopic) o;

        if (!Objects.equals(agentId, that.agentId)) return false;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        int result = agentId != null ? agentId.hashCode() : 0;
        result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
        return result;
    }
}
