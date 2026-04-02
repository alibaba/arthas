package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * Tomcat信息值对象（VO）
 * 用于在dashboard命令中展示Tomcat服务器的状态信息
 * 包含连接器统计信息和线程池信息
 *
 * @author gongdewei 2020/4/23
 */
public class TomcatInfoVO {

    /** 连接器统计信息列表，记录各个Connector的运行状态 */
    private List<ConnectorStats> connectorStats;

    /** 线程池信息列表，记录各个ThreadPool的运行状态 */
    private List<ThreadPool> threadPools;

    /**
     * 默认构造函数
     * 创建一个空的TomcatInfoVO实例
     */
    public TomcatInfoVO() {
    }

    /**
     * 获取连接器统计信息列表
     *
     * @return 连接器统计信息列表
     */
    public List<ConnectorStats> getConnectorStats() {
        return connectorStats;
    }

    /**
     * 设置连接器统计信息列表
     *
     * @param connectorStats 连接器统计信息列表
     */
    public void setConnectorStats(List<ConnectorStats> connectorStats) {
        this.connectorStats = connectorStats;
    }

    /**
     * 获取线程池信息列表
     *
     * @return 线程池信息列表
     */
    public List<ThreadPool> getThreadPools() {
        return threadPools;
    }

    /**
     * 设置线程池信息列表
     *
     * @param threadPools 线程池信息列表
     */
    public void setThreadPools(List<ThreadPool> threadPools) {
        this.threadPools = threadPools;
    }

    /**
     * 连接器统计信息
     * 用于记录Tomcat Connector的运行统计信息，包括QPS、响应时间、错误率等
     */
    public static class ConnectorStats {
        /** 连接器名称 */
        private String name;

        /** 每秒查询率（Queries Per Second），表示每秒处理的请求数 */
        private double qps;

        /** 响应时间（Response Time），表示平均响应耗时 */
        private double rt;

        /** 错误率，表示请求失败的比率 */
        private double error;

        /** 接收的字节数，统计从客户端接收到的总字节数 */
        private long received;

        /** 发送的字节数，统计向客户端发送的总字节数 */
        private long sent;

        /**
         * 获取连接器名称
         *
         * @return 连接器名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置连接器名称
         *
         * @param name 连接器名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取每秒查询率
         *
         * @return QPS值
         */
        public double getQps() {
            return qps;
        }

        /**
         * 设置每秒查询率
         *
         * @param qps QPS值
         */
        public void setQps(double qps) {
            this.qps = qps;
        }

        /**
         * 获取响应时间
         *
         * @return 响应时间
         */
        public double getRt() {
            return rt;
        }

        /**
         * 设置响应时间
         *
         * @param rt 响应时间
         */
        public void setRt(double rt) {
            this.rt = rt;
        }

        /**
         * 获取错误率
         *
         * @return 错误率
         */
        public double getError() {
            return error;
        }

        /**
         * 设置错误率
         *
         * @param error 错误率
         */
        public void setError(double error) {
            this.error = error;
        }

        /**
         * 获取接收的字节数
         *
         * @return 接收的字节数
         */
        public long getReceived() {
            return received;
        }

        /**
         * 设置接收的字节数
         *
         * @param received 接收的字节数
         */
        public void setReceived(long received) {
            this.received = received;
        }

        /**
         * 获取发送的字节数
         *
         * @return 发送的字节数
         */
        public long getSent() {
            return sent;
        }

        /**
         * 设置发送的字节数
         *
         * @param sent 发送的字节数
         */
        public void setSent(long sent) {
            this.sent = sent;
        }
    }

    /**
     * 线程池信息
     * 用于记录Tomcat线程池的运行状态，包括线程名称、忙碌线程数、总线程数等
     */
    public static class ThreadPool {
        /** 线程池名称 */
        private String name;

        /** 忙碌线程数，当前正在执行任务的线程数量 */
        private long busy;

        /** 总线程数，线程池中线程的总数量 */
        private long total;

        /**
         * 默认构造函数
         * 创建一个空的ThreadPool实例
         */
        public ThreadPool() {
        }

        /**
         * 带参数的构造函数
         *
         * @param name  线程池名称
         * @param busy  忙碌线程数
         * @param total 总线程数
         */
        public ThreadPool(String name, long busy, long total) {
            this.name = name;
            this.busy = busy;
            this.total = total;
        }

        /**
         * 获取线程池名称
         *
         * @return 线程池名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置线程池名称
         *
         * @param name 线程池名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取忙碌线程数
         *
         * @return 忙碌线程数
         */
        public long getBusy() {
            return busy;
        }

        /**
         * 设置忙碌线程数
         *
         * @param busy 忙碌线程数
         */
        public void setBusy(long busy) {
            this.busy = busy;
        }

        /**
         * 获取总线程数
         *
         * @return 总线程数
         */
        public long getTotal() {
            return total;
        }

        /**
         * 设置总线程数
         *
         * @param total 总线程数
         */
        public void setTotal(long total) {
            this.total = total;
        }
    }
}
