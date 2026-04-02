package com.taobao.arthas.core.command.model;

/**
 * 'dashboard'命令的内存信息值对象
 *
 * 用于展示JVM内存区域的详细信息，包括堆内存、非堆内存和缓冲池内存的使用情况
 *
 * @author gongdewei 2020/4/22
 */
public class MemoryEntryVO {

    /**
     * 堆内存类型常量
     */
    public static final String TYPE_HEAP = "heap";

    /**
     * 非堆内存类型常量
     */
    public static final String TYPE_NON_HEAP = "nonheap";

    /**
     * 缓冲池内存类型常量
     */
    public static final String TYPE_BUFFER_POOL = "buffer_pool";

    /**
     * 内存区域类型
     * 可选值：heap（堆内存）、nonheap（非堆内存）、buffer_pool（缓冲池）
     */
    private String type;

    /**
     * 内存区域名称
     * 例如：Eden Space、Survivor Space、Old Gen、Code Cache、Metaspace等
     */
    private String name;

    /**
     * 已使用的内存大小（字节）
     */
    private long used;

    /**
     * 已提交的内存大小（字节）
     * 表示Java虚拟机保证可用的内存大小
     */
    private long total;

    /**
     * 最大可用内存大小（字节）
     * 表示该内存区域可以使用的最大内存
     */
    private long max;

    /**
     * 默认构造函数
     * 用于创建空的内存信息对象，通过setter方法设置属性值
     */
    public MemoryEntryVO() {
    }

    /**
     * 全参数构造函数
     *
     * @param type 内存区域类型
     * @param name 内存区域名称
     * @param used 已使用的内存大小（字节）
     * @param total 已提交的内存大小（字节）
     * @param max 最大可用内存大小（字节）
     */
    public MemoryEntryVO(String type, String name, long used, long total, long max) {
        this.type = type;
        this.name = name;
        this.used = used;
        this.total = total;
        this.max = max;
    }

    /**
     * 获取内存区域类型
     *
     * @return 内存区域类型字符串
     */
    public String getType() {
        return type;
    }

    /**
     * 设置内存区域类型
     *
     * @param type 内存区域类型字符串
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取内存区域名称
     *
     * @return 内存区域名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置内存区域名称
     *
     * @param name 内存区域名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取已使用的内存大小
     *
     * @return 已使用的内存大小（字节）
     */
    public long getUsed() {
        return used;
    }

    /**
     * 设置已使用的内存大小
     *
     * @param used 已使用的内存大小（字节）
     */
    public void setUsed(long used) {
        this.used = used;
    }

    /**
     * 获取已提交的内存大小
     *
     * @return 已提交的内存大小（字节）
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置已提交的内存大小
     *
     * @param total 已提交的内存大小（字节）
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 获取最大可用内存大小
     *
     * @return 最大可用内存大小（字节）
     */
    public long getMax() {
        return max;
    }

    /**
     * 设置最大可用内存大小
     *
     * @param max 最大可用内存大小（字节）
     */
    public void setMax(long max) {
        this.max = max;
    }
}
