package com.taobao.arthas.core.command.model;

/**
 * Echo命令结果模型类
 *
 * <p>该类用于封装echo命令的执行结果，继承自ResultModel基类。
 * Echo命令用于将用户指定的内容原样输出，常用于测试、调试或在命令行中显示信息。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>封装echo命令的输出内容</li>
 *   <li>提供内容的获取和设置方法</li>
 *   <li>返回固定的模型类型标识"echo"</li>
 * </ul>
 *
 * @author gongdewei 2020/5/11
 */
public class EchoModel extends ResultModel {

    /**
     * echo命令的输出内容
     * 该字段存储用户指定要显示的文本内容
     */
    private String content;

    /**
     * 默认构造函数
     * 创建一个空的EchoModel实例，content字段默认为null
     */
    public EchoModel() {
    }

    /**
     * 带参数的构造函数
     *
     * @param content echo命令要输出的内容
     */
    public EchoModel(String content) {
        this.content = content;
    }

    /**
     * 获取模型类型标识
     *
     * @return 返回固定的类型字符串"echo"，用于标识这是一个Echo模型
     */
    @Override
    public String getType() {
        return "echo";
    }

    /**
     * 获取echo命令的输出内容
     *
     * @return 存储的文本内容，可能为null
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置echo命令的输出内容
     *
     * @param content 要设置的文本内容
     */
    public void setContent(String content) {
        this.content = content;
    }
}
