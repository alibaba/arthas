package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SqlProfiler command result model
 *
 * @author yangxiaobing 2021/8/4
 */
public class SqlProfilerModel extends ResultModel {
    private Date ts;

    private TraceData traceData;

    private MonitorData monitorData;

    public SqlProfilerModel() {
    }

    @Override
    public String getType() {
        return "sqlprofiler";
    }

    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    public TraceData getTraceData() {
        return traceData;
    }

    public void setTraceData(TraceData traceData) {
        this.traceData = traceData;
    }

    public MonitorData getMonitorData() {
        return monitorData;
    }

    public void setMonitorData(MonitorData monitorData) {
        this.monitorData = monitorData;
    }

    public static class TraceData {
        private String className;
        private String methodName;
        private double cost;
        private boolean success;
        private String sql;
        private List<String> params;

        private List<String> batchSql;
        private List<List<String>> batchParams;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public List<String> getParams() {
            return params;
        }

        public void setParams(List<String> params) {
            this.params = params;
        }

        public List<String> getBatchSql() {
            return batchSql;
        }

        public void setBatchSql(List<String> batchSql) {
            this.batchSql = batchSql;
        }

        public List<List<String>> getBatchParams() {
            return batchParams;
        }

        public void setBatchParams(List<List<String>> batchParams) {
            this.batchParams = batchParams;
        }
    }

    public static class MonitorData {
        private List<SqlStat> topByTotalCost = new ArrayList<SqlStat>();
        private List<SqlStat> topByAvgCost = new ArrayList<SqlStat>();

        public List<SqlStat> getTopByTotalCost() {
            return topByTotalCost;
        }

        public void setTopByTotalCost(List<SqlStat> topByTotalCost) {
            this.topByTotalCost = topByTotalCost;
        }

        public List<SqlStat> getTopByAvgCost() {
            return topByAvgCost;
        }

        public void setTopByAvgCost(List<SqlStat> topByAvgCost) {
            this.topByAvgCost = topByAvgCost;
        }
    }

    public static class SqlStat {
        private String sql;
        private double avgCost = 0d;
        private double totalCost = 0d;
        private Integer count = 0;
        private Integer successCount = 0;
        private Integer failedCount = 0;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public double getAvgCost() {
            return avgCost;
        }

        public void setAvgCost(double avgCost) {
            this.avgCost = avgCost;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(double totalCost) {
            this.totalCost = totalCost;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Integer getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(Integer successCount) {
            this.successCount = successCount;
        }

        public Integer getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Integer failedCount) {
            this.failedCount = failedCount;
        }

        public synchronized void addSample(double cost) {
            count++;
            totalCost += cost;
        }

        public synchronized void calc() {
            if (count > 0) {
                avgCost = totalCost / count;
            }
        }
    }
}
