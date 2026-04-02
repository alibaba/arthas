package com.taobao.arthas.core.command.model;

/**
 * heapdump命令的数据模型
 * 用于封装heapdump命令的执行结果，包括堆转储文件路径和是否只转储存活对象等信息
 *
 * @author gongdewei 2020/4/24
 */
public class HeapDumpModel extends ResultModel {

    /**
     * 堆转储文件路径
     * 指定生成的堆转储文件的存储路径
     */
    private String dumpFile;

    /**
     * 是否只转储存活对象
     * true表示只转储堆中的存活对象，false表示转储堆中的所有对象
     */
    private boolean live;

    /**
     * 默认构造函数
     * 创建一个空的HeapDumpModel实例
     */
    public HeapDumpModel() {
    }

    /**
     * 构造函数 - 用于创建包含堆转储信息的模型
     *
     * @param dumpFile 堆转储文件路径
     * @param live 是否只转储存活对象
     */
    public HeapDumpModel(String dumpFile, boolean live) {
        this.dumpFile = dumpFile;
        this.live = live;
    }

    /**
     * 获取堆转储文件路径
     *
     * @return 堆转储文件的完整路径
     */
    public String getDumpFile() {
        return dumpFile;
    }

    /**
     * 设置堆转储文件路径
     *
     * @param dumpFile 堆转储文件的完整路径
     */
    public void setDumpFile(String dumpFile) {
        this.dumpFile = dumpFile;
    }

    /**
     * 判断是否只转储存活对象
     *
     * @return true表示只转储存活对象，false表示转储所有对象
     */
    public boolean isLive() {
        return live;
    }

    /**
     * 设置是否只转储存活对象
     *
     * @param live true表示只转储存活对象，false表示转储所有对象
     */
    public void setLive(boolean live) {
        this.live = live;
    }

    /**
     * 获取模型类型
     * 用于标识这是一个heapdump命令的结果模型
     *
     * @return 模型类型标识符 "heapdump"
     */
    @Override
    public String getType() {
        return "heapdump";
    }

}
