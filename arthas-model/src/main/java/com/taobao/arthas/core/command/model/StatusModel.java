package com.taobao.arthas.core.command.model;

/**
 * 状态结果模型
 * 用于表示命令执行的状态信息，包含状态码和消息
 * 通常用于返回操作是否成功、失败或进行中的状态
 */
public class StatusModel extends ResultModel {

    /**
     * 状态码
     * 用于标识命令执行的状态，如200表示成功，其他值表示各种错误或中间状态
     */
    private int statusCode;

    /**
     * 状态消息
     * 对状态码的详细描述信息，用于说明具体的执行结果或错误原因
     */
    private String message;

    /**
     * 构造函数：仅指定状态码
     *
     * @param statusCode 状态码值
     */
    public StatusModel(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * 构造函数：指定状态码和消息
     *
     * @param statusCode 状态码值
     * @param message    状态消息描述
     */
    public StatusModel(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    /**
     * 获取状态码
     *
     * @return 当前状态码
     */
    public int getStatusCode() {
        return statusCode;
    }


    /**
     * 获取状态消息
     *
     * @return 状态消息字符串
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取结果类型
     *
     * @return 返回"status"字符串，标识这是一个状态类型的结果
     */
    @Override
    public String getType() {
        return "status";
    }

}
