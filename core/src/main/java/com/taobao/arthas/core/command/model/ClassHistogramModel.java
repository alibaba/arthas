package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * @author:
 * @Date: 2021/7/19 09:42
 * @Copyright: 2019 www.lenovo.com Inc. All rights reserved.
 */
public class ClassHistogramModel extends ResultModel {

    private long totalInstances;

    private long totalBytes;

    private List<ClassHistogramVO> classHistogramVOList;

    public ClassHistogramModel() {
    }

    public ClassHistogramModel(long totalInstances, long totalBytes, List<ClassHistogramVO> classHistogramVOList) {
        this.totalInstances = totalInstances;
        this.totalBytes = totalBytes;
        this.classHistogramVOList = classHistogramVOList;
    }

    public long getTotalInstances() {
        return totalInstances;
    }

    public void setTotalInstances(long totalInstances) {
        this.totalInstances = totalInstances;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public List<ClassHistogramVO> getClassHistogramVOList() {
        return classHistogramVOList;
    }

    public void setClassHistogramVOList(List<ClassHistogramVO> classHistogramVOList) {
        this.classHistogramVOList = classHistogramVOList;
    }

    @Override
    public String toString() {
        return "ClassHistogramModel{" +
                "totalInstances=" + totalInstances +
                ", totalBytes=" + totalBytes +
                ", classHistogramVOList=" + classHistogramVOList +
                '}';
    }

    @Override
    public String getType() {
        return "class histogram";
    }
}
