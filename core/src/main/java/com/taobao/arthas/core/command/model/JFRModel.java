package com.taobao.arthas.core.command.model;

/**
 * JFR(Java Flight Recorder)命令结果模型
 * 用于封装JFR命令执行后返回的数据结果
 *
 * @author xulong 2022/7/25
 */
public class JFRModel extends ResultModel {

    /**
     * JFR输出内容
     * 用于存储JFR命令执行过程中的输出信息
     */
    private String jfrOutput = "";

    /**
     * 获取结果类型
     * 用于标识该模型对应的命令类型
     *
     * @return 返回"jfr"字符串标识
     */
    @Override
    public String getType() {
        return "jfr";
    }

    /**
     * 获取JFR输出内容
     *
     * @return 返回JFR命令的输出字符串
     */
    public String getJfrOutput() {
        return jfrOutput;
    }

    /**
     * 设置JFR输出内容
     * 该方法采用追加模式，不会覆盖已有的输出内容
     *
     * @param jfrOutput 要追加的JFR输出内容
     */
    public void setJfrOutput(String jfrOutput) {
        // 使用+=操作符，支持追加内容而非覆盖
        this.jfrOutput += jfrOutput;
    }
}
