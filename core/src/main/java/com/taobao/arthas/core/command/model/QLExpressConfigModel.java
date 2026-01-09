package com.taobao.arthas.core.command.model;

/**
 * @Author TaoKan
 * @Date 2024/9/22 7:51 AM
 */
public class QLExpressConfigModel {
    //QL_OPTIONS
    private boolean precise = false;
    private boolean polluteUserContext = false;
    private long timeoutMillis = -1L;

    private boolean cache = false;
    private boolean avoidNullPointer = false;
    private int maxArrLength = -1;


    //INIT_OPTIONS
    private boolean allowPrivateAccess = true;
    private boolean debug;
    private boolean useCacheClear;

    public boolean isAllowPrivateAccess() {
        return allowPrivateAccess;
    }

    public void setAllowPrivateAccess(boolean allowPrivateAccess) {
        this.allowPrivateAccess = allowPrivateAccess;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isUseCacheClear() {
        return useCacheClear;
    }

    public void setUseCacheClear(boolean useCacheClear) {
        this.useCacheClear = useCacheClear;
    }



    public boolean isPrecise() {
        return precise;
    }

    public void setPrecise(boolean precise) {
        this.precise = precise;
    }

    public boolean isPolluteUserContext() {
        return polluteUserContext;
    }

    public void setPolluteUserContext(boolean polluteUserContext) {
        this.polluteUserContext = polluteUserContext;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isAvoidNullPointer() {
        return avoidNullPointer;
    }

    public void setAvoidNullPointer(boolean avoidNullPointer) {
        this.avoidNullPointer = avoidNullPointer;
    }

    public int getMaxArrLength() {
        return maxArrLength;
    }

    public void setMaxArrLength(int maxArrLength) {
        this.maxArrLength = maxArrLength;
    }


    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }
}
