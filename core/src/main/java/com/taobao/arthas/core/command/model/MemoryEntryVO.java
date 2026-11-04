package com.taobao.arthas.core.command.model;

/**
 * Memory info of 'dashboard' command
 *
 * @author gongdewei 2020/4/22
 */
public class MemoryEntryVO {

    public static final String TYPE_HEAP = "heap";
    public static final String TYPE_NON_HEAP = "nonheap";
    public static final String TYPE_BUFFER_POOL = "buffer_pool";

    private String type;
    private String name;
    private long used;
    private long total;
    private long max;

    public MemoryEntryVO() {
    }

    public MemoryEntryVO(String type, String name, long used, long total, long max) {
        this.type = type;
        this.name = name;
        this.used = used;
        this.total = total;
        this.max = max;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }
}
