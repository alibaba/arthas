package com.taobao.arthas.core.command.model;

import java.util.Date;
import java.util.List;

/**
 * SqlProfiler command result model
 *
 * @author yangxiaobing 2021/8/4
 */
public class SqlProfilerModel extends ResultModel {
    private Date ts;
    private String className;
    private String methodName;
    private double cost;
    private boolean success;
    private String sql;
    private List<String> params;

    private List<String> batchSql;
    private List<List<String>> batchParams;

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
