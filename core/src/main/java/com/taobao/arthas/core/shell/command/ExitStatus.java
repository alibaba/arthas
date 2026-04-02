package com.taobao.arthas.core.shell.command;

/**
 * 命令执行的退出状态
 * 用于表示命令执行后的状态结果，包括成功和失败的情况
 */
public class ExitStatus {

    /**
     * 命令执行成功的常量状态
     * 状态码为0表示成功
     */
    public static final ExitStatus SUCCESS_STATUS = new ExitStatus(0);

    /**
     * 创建一个表示命令执行成功状态的ExitStatus对象
     *
     * @return 成功状态的ExitStatus对象
     */
    public static ExitStatus success() {
        return SUCCESS_STATUS;
    }

    /**
     * 创建一个表示命令执行失败状态的ExitStatus对象
     *
     * @param statusCode 失败状态码，必须不为0
     * @param message 失败的详细信息
     * @return 失败状态的ExitStatus对象
     * @throws IllegalArgumentException 如果状态码为0
     */
    public static ExitStatus failure(int statusCode, String message) {
        if (statusCode == 0) {
            throw new IllegalArgumentException("failure status code cannot be 0");
        }
        return new ExitStatus(statusCode, message);
    }

    /**
     * 判断给定的退出状态是否为失败状态
     *
     * @param exitStatus 要判断的退出状态对象
     * @return 如果状态不为null且状态码不为0，则返回true表示失败；否则返回false
     */
    public static boolean isFailed(ExitStatus exitStatus) {
        return exitStatus != null && exitStatus.getStatusCode() != 0;
    }


    /**
     * 状态码，0表示成功，非0表示失败
     */
    private int statusCode;

    /**
     * 状态消息，用于描述状态的详细信息
     */
    private String message;

    /**
     * 私有构造函数，创建一个只有状态码的ExitStatus对象
     *
     * @param statusCode 状态码
     */
    private ExitStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * 私有构造函数，创建一个包含状态码和消息的ExitStatus对象
     *
     * @param statusCode 状态码
     * @param message 状态消息
     */
    private ExitStatus(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    /**
     * 获取状态码
     *
     * @return 状态码，0表示成功，非0表示失败
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * 获取状态消息
     *
     * @return 状态消息字符串，可能为null
     */
    public String getMessage() {
        return message;
    }

}
