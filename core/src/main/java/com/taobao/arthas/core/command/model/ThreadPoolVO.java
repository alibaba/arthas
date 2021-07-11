package com.taobao.arthas.core.command.model;

/**
 * Thread VO of 'threadpool' command
 *
 * @author HJ
 * @date 2021-07-09
 **/
public class ThreadPoolVO implements Comparable<ThreadPoolVO> {

    private String stackInfo;

    private Integer corePoolSize;

    private Integer maximumPoolSize;

    private Integer currentSizeOfWorkQueue;

    private Integer activeThreadCount;

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Integer getCurrentSizeOfWorkQueue() {
        return currentSizeOfWorkQueue;
    }

    public void setCurrentSizeOfWorkQueue(Integer currentSizeOfWorkQueue) {
        this.currentSizeOfWorkQueue = currentSizeOfWorkQueue;
    }

    public Integer getActiveThreadCount() {
        return activeThreadCount;
    }

    public void setActiveThreadCount(Integer activeThreadCount) {
        this.activeThreadCount = activeThreadCount;
    }

    public String getStackInfo() {
        return stackInfo;
    }

    public void setStackInfo(String stackInfo) {
        this.stackInfo = stackInfo;
    }


    @Override
    public int compareTo(ThreadPoolVO o1) {
        if (this == o1) {
            return 0;
        }
        int compareActiveThreadCount = compareIntegerWithNull(getActiveThreadCount(),o1.getActiveThreadCount());
        // 优先按繁忙线程数排序
        if(compareActiveThreadCount != 0 ){
            return compareActiveThreadCount;
        }
        // 其次按队列堆积数排序
        int compareCurrentSizeOfWorkQueue = compareIntegerWithNull(getCurrentSizeOfWorkQueue(),o1.getCurrentSizeOfWorkQueue());
        if(compareCurrentSizeOfWorkQueue != 0){
            return compareCurrentSizeOfWorkQueue;
        }
        // 最后按最大线程数排序
        return compareIntegerWithNull(getMaximumPoolSize(),o1.getMaximumPoolSize());
    }

    private int compareIntegerWithNull(Integer i1,Integer i2){
        if (i1 == null || i2 == null) {
            if (i1 != null) {
                return -1;
            } else if (i2 != null) {
                return 1;
            }
            return 0;
        }

        return i2 - i1;
    }
}
