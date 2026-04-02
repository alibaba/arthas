package com.taobao.arthas.core.command.model;

/**
 * 欢迎信息模型类
 *
 * 该类用于封装Arthas启动时显示的欢迎信息，包含了进程ID、时间戳、版本信息、
 * 文档链接、教程链接以及主类信息等关键数据。
 * 继承自ResultModel，表示这是一个可返回给客户端的命令执行结果。
 *
 * @author gongdewei 2020/4/20
 */
public class WelcomeModel extends ResultModel {

    /**
     * 进程ID
     * 表示当前被Arthas监控的Java进程的进程标识符
     */
    private String pid;

    /**
     * 时间戳
     * 表示Arthas启动或欢迎信息生成的时间
     */
    private String time;

    /**
     * 版本信息
     * 表示当前Arthas的版本号
     */
    private String version;

    /**
     * Wiki文档链接
     * 指向Arthas官方Wiki文档的URL，用户可以查阅详细的使用文档
     */
    private String wiki;

    /**
     * 教程链接
     * 指向Arthas教程的URL，提供快速上手指南和示例
     */
    private String tutorials;

    /**
     * 主类信息
     * 表示被监控Java进程的main方法的入口类全限定名
     */
    private String mainClass;

    /**
     * 默认构造函数
     *
     * 创建一个空的WelcomeModel对象，所有字段初始化为null
     * 可以通过后续的setter方法设置各个字段的值
     */
    public WelcomeModel() {
    }

    /**
     * 获取结果类型
     *
     * 返回该模型的类型标识符，用于在客户端区分不同类型的命令结果
     *
     * @return 类型标识符，固定返回"welcome"字符串
     */
    @Override
    public String getType() {
        // 返回固定的类型标识符"welcome"，客户端可以根据此类型识别欢迎信息
        return "welcome";
    }

    /**
     * 获取进程ID
     *
     * 返回当前被监控Java进程的进程标识符
     *
     * @return 进程ID字符串
     */
    public String getPid() {
        return pid;
    }

    /**
     * 设置进程ID
     *
     * 设置当前被监控Java进程的进程标识符
     *
     * @param pid 进程ID字符串
     */
    public void setPid(String pid) {
        this.pid = pid;
    }

    /**
     * 获取时间戳
     *
     * 返回欢迎信息生成的时间戳
     *
     * @return 时间戳字符串
     */
    public String getTime() {
        return time;
    }

    /**
     * 设置时间戳
     *
     * 设置欢迎信息生成的时间戳
     *
     * @param time 时间戳字符串
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * 获取版本信息
     *
     * 返回当前Arthas的版本号
     *
     * @return 版本号字符串
     */
    public String getVersion() {
        return version;
    }

    /**
     * 设置版本信息
     *
     * 设置当前Arthas的版本号
     *
     * @param version 版本号字符串
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 获取Wiki文档链接
     *
     * 返回Arthas官方Wiki文档的URL地址
     *
     * @return Wiki文档链接字符串
     */
    public String getWiki() {
        return wiki;
    }

    /**
     * 设置Wiki文档链接
     *
     * 设置Arthas官方Wiki文档的URL地址
     *
     * @param wiki Wiki文档链接字符串
     */
    public void setWiki(String wiki) {
        this.wiki = wiki;
    }

    /**
     * 获取教程链接
     *
     * 返回Arthas教程的URL地址
     *
     * @return 教程链接字符串
     */
    public String getTutorials() {
        return tutorials;
    }

    /**
     * 设置教程链接
     *
     * 设置Arthas教程的URL地址
     *
     * @param tutorials 教程链接字符串
     */
    public void setTutorials(String tutorials) {
        this.tutorials = tutorials;
    }

    /**
     * 获取主类信息
     *
     * 返回被监控Java进程的main方法入口类的全限定名
     *
     * @return 主类全限定名字符串
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * 设置主类信息
     *
     * 设置被监控Java进程的main方法入口类的全限定名
     *
     * @param mainClass 主类全限定名字符串
     */
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
}
