package com.alibaba.arthas.channel.server.message.topic;

/**
 * @author gongdewei 2020/8/12
 */
public class ActionRequestTopic implements Topic {

    String agentId;

    public ActionRequestTopic(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }

    @Override
    public String getTopic() {
        return "arthas:channel:topics:agent:" + agentId + ":requests";
    }

    @Override
    public String toString() {
        return "ActionRequestTopic{" +
                "agentId='" + agentId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionRequestTopic that = (ActionRequestTopic) o;

        return agentId != null ? agentId.equals(that.agentId) : that.agentId == null;
    }

    @Override
    public int hashCode() {
        return agentId != null ? agentId.hashCode() : 0;
    }
}
