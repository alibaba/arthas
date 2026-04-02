package com.taobao.arthas.core.command.model;

/**
 * Base64编码结果模型
 *
 * 该模型用于封装需要以Base64编码格式输出的命令执行结果。
 * 继承自ResultModel基类，用于在Arthas命令系统中统一处理不同类型的输出结果。
 * 主要用于那些需要Base64编码后才能正确显示或传输的数据内容。
 *
 * @author hengyunabc 2021-01-05
 *
 */
public class Base64Model extends ResultModel {

    /**
     * Base64编码的内容
     * 存储经过Base64编码的实际数据内容
     */
    private String content;

    /**
     * 默认构造函数
     * 创建一个空的Base64Model对象，content字段为null
     */
    public Base64Model() {
    }

    /**
     * 带参数的构造函数
     *
     * @param content Base64编码的内容字符串
     */
    public Base64Model(String content) {
        this.content = content;
    }

    /**
     * 获取结果类型标识
     *
     * @return 返回"base64"字符串，标识这是一个Base64类型的结果模型
     */
    @Override
    public String getType() {
        return "base64";
    }

    /**
     * 获取Base64编码的内容
     *
     * @return Base64编码的内容字符串
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置Base64编码的内容
     *
     * @param content 要设置的Base64编码内容字符串
     */
    public void setContent(String content) {
        this.content = content;
    }
}
