package com.taobao.arthas.core.command.model;

/**
 * @author:
 * @Date: 2021/7/19 09:28
 * @Copyright: 2019 www.lenovo.com Inc. All rights reserved.
 */
public class ClassHistogramVO {

    private Long num;

    private Long instances;

    private Long bytes;

    private String className;

    public ClassHistogramVO() {
    }

    public ClassHistogramVO(long num, long instances, long bytes, String className) {
        this.num = num;
        this.instances = instances;
        this.bytes = bytes;
        this.className = className;
    }

    public long getNum() {
        return num;
    }

    public void setNum(long num) {
        this.num = num;
    }

    public long getInstances() {
        return instances;
    }

    public void setInstances(long instances) {
        this.instances = instances;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return "ClassHistogramVO{" +
                "num=" + num +
                ", instances=" + instances +
                ", bytes=" + bytes +
                ", className='" + className + '\'' +
                '}';
    }
}
