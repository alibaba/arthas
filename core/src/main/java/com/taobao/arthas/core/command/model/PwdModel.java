package com.taobao.arthas.core.command.model;

/**
 * Pwd命令的模型类
 * <p>
 * 用于封装pwd（print working directory）命令的执行结果。
 * 该命令用于显示Java进程当前的工作目录路径。
 * </p>
 * <p>
 * 工作目录是指Java进程启动时所在的目录，或者通过System.setProperty("user.dir")设置的目录。
 * 这个目录会影响相对路径的解析，例如File类和FileInputStream等API使用的相对路径。
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>排查文件读写问题时，确认当前工作目录</li>
 *   <li>分析日志文件输出位置</li>
 *   <li>理解相对路径解析的基准目录</li>
 * </ul>
 * </p>
 *
 * @author gongdewei 2020/5/11
 */
public class PwdModel extends ResultModel {

    /**
     * 当前工作目录的绝对路径
     * <p>
     * 存储Java进程当前的工作目录完整路径。
     * 这个路径是操作系统特定的绝对路径格式。
     * </p>
     */
    private String workingDir;

    /**
     * 默认构造函数
     * <p>
     * 创建一个空的PwdModel实例，工作目录字段使用默认值（null）。
     * </p>
     */
    public PwdModel() {
    }

    /**
     * 全参数构造函数
     * <p>
     * 创建一个包含工作目录信息的PwdModel实例。
     * </p>
     *
     * @param workingDir 当前工作目录的绝对路径
     */
    public PwdModel(String workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * 获取模型类型
     * <p>
     * 返回此模型的类型标识符，用于前端识别如何渲染此模型数据。
     * </p>
     *
     * @return 模型类型字符串，固定返回"pwd"
     */
    @Override
    public String getType() {
        return "pwd";
    }

    /**
     * 获取当前工作目录路径
     *
     * @return 当前工作目录的绝对路径
     */
    public String getWorkingDir() {
        return workingDir;
    }

    /**
     * 设置当前工作目录路径
     *
     * @param workingDir 要设置的工作目录路径
     */
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }
}
