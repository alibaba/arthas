package com.taobao.arthas.core.command.model;

/**
 * Thread VO of 'threadpool' command
 *
 * @author HJ
 * @date 2021-07-09
 **/
public class ThreadPoolVO implements Comparable<ThreadPoolVO> {

    private String stackInfo;

    private int corePoolSize;

    private int maximumPoolSize;

    private int currentSizeOfWorkQueue;

    private int activeThreadCount;

    public ThreadPoolVO(String stackInfo) {
        this.stackInfo = stackInfo;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getCurrentSizeOfWorkQueue() {
        return currentSizeOfWorkQueue;
    }

    public void setCurrentSizeOfWorkQueue(int currentSizeOfWorkQueue) {
        this.currentSizeOfWorkQueue = currentSizeOfWorkQueue;
    }

    public int getActiveThreadCount() {
        return activeThreadCount;
    }

    public void setActiveThreadCount(int activeThreadCount) {
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
        int compareActiveThreadCount = o1.getActiveThreadCount() - getActiveThreadCount();
        // 优先按繁忙线程数排序
        if(compareActiveThreadCount != 0 ){
            return compareActiveThreadCount;
        }
        // 其次按队列堆积数排序
        int compareCurrentSizeOfWorkQueue = o1.getCurrentSizeOfWorkQueue() - getCurrentSizeOfWorkQueue();
        if(compareCurrentSizeOfWorkQueue != 0){
            return compareCurrentSizeOfWorkQueue;
        }
        // 最后按最大线程数排序
        return o1.getMaximumPoolSize() - getMaximumPoolSize();
    }
}
