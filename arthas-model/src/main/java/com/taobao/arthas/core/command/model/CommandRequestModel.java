package com.taobao.arthas.core.command.model;

/**
 * 命令异步执行请求结果模型
 * <p>
 * 该类表示命令异步执行过程中的状态信息，不是命令的最终执行结果。
 * 主要用于跟踪命令的执行状态、命令内容和相关消息。
 * </p>
 *
 * @author gongdewei 2020/4/2
 */
public class CommandRequestModel extends ResultModel {

    /**
     * 命令执行状态
     * <p>
     * 例如：PENDING（待执行）、RUNNING（执行中）、COMPLETED（已完成）等
     * </p>
     */
    private String state;

    /**
     * 命令行字符串
     * <p>
     * 实际执行的命令内容
     * </p>
     */
    private String command;

    /**
     * 附加消息
     * <p>
     * 与命令执行相关的附加信息或错误消息
     * </p>
     */
    private String message;

    /**
     * 默认构造函数
     */
    public CommandRequestModel() {
    }

    /**
     * 构造函数 - 包含命令和状态
     *
     * @param command 命令行字符串
     * @param state 执行状态
     */
    public CommandRequestModel(String command, String state) {
        this.command = command;
        this.state = state;
    }

    /**
     * 构造函数 - 完整参数
     *
     * @param command 命令行字符串
     * @param state 执行状态
     * @param message 附加消息
     */
    public CommandRequestModel(String command, String state, String message) {
        this.state = state;
        this.command = command;
        this.message = message;
    }

    /**
     * 获取命令行字符串
     *
     * @return 命令行字符串
     */
    public String getCommand() {
        return command;
    }

    /**
     * 设置命令行字符串
     *
     * @param command 命令行字符串
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * 获取执行状态
     *
     * @return 执行状态字符串
     */
    public String getState() {
        return state;
    }

    /**
     * 设置执行状态
     *
     * @param state 执行状态字符串
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 获取附加消息
     *
     * @return 消息字符串
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置附加消息
     *
     * @param message 消息字符串
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取结果类型
     * <p>
     * 返回固定值"command"，标识这是一个命令请求模型
     * </p>
     *
     * @return 结果类型字符串
     */
    @Override
    public String getType() {
        return "command";
    }
}
