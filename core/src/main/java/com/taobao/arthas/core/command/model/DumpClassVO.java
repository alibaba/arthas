package com.taobao.arthas.core.command.model;

/**
 * Dump类值对象
 * <p>
 * 该类用于表示已被dump（导出）到文件系统的类的详细信息。
 * 继承自ClassVO，除了包含类的基本信息（如类名、类加载器等）外，
 * 还额外存储了类字节码被dump到文件系统后的存储位置信息。
 * 该类主要用于dump命令的执行结果展示，让用户能够了解类的dump状态和文件位置。
 * </p>
 *
 * @author gongdewei 2020/7/9
 */
public class DumpClassVO extends ClassVO {

    /**
     * Dump文件位置
     * <p>
     * 存储类字节码被dump到文件系统后的绝对路径。
     * 当dump命令成功执行后，类的字节码会被写入到指定的目录中，
     * 此属性记录了dump文件的完整路径，方便用户定位和使用dump的类文件。
     * </p>
     */
    private String location;

    /**
     * 获取Dump文件位置
     * <p>
     * 返回类字节码被dump到文件系统后的绝对路径。
     * 用户可以通过此路径找到dump的类文件，用于后续的类文件分析、反编译或其他用途。
     * </p>
     *
     * @return dump文件的绝对路径，如果dump未成功则可能返回null
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置Dump文件位置
     * <p>
     * 设置类字节码被dump到文件系统后的绝对路径。
     * 在dump命令成功执行后，会将写入的文件路径设置到此属性，
     * 以便在结果中展示给用户。
     * </p>
     *
     * @param location dump文件的绝对路径，通常是包含类名的完整文件路径
     */
    public void setLocation(String location) {
        this.location = location;
    }
}
