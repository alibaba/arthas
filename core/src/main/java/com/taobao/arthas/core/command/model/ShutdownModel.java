package com.taobao.arthas.core.command.model;

/**
 * 关闭命令的结果模型
 * <p>
 * 该类用于封装Arthas服务器关闭操作的执行结果，包含关闭方式和相关消息信息
 * </p>
 *
 * @author gongdewei 2020/6/22
 */
public class ShutdownModel extends ResultModel {

    /**
     * 是否优雅关闭
     * <p>
     * true表示优雅关闭（等待当前请求处理完成后再关闭）
     * false表示强制关闭（立即关闭服务器）
     * </p>
     */
    private boolean graceful;

    /**
     * 关闭消息
     * <p>
     * 描述关闭操作的相关信息或提示
     * </p>
     */
    private String message;

    /**
     * 构造函数
     *
     * @param graceful 是否优雅关闭，true表示优雅关闭，false表示强制关闭
     * @param message  关闭消息，描述关闭操作的相关信息
     */
    public ShutdownModel(boolean graceful, String message) {
        this.graceful = graceful;
        this.message = message;
    }

    /**
     * 获取结果类型
     * <p>
     * 返回"shutdown"标识这是一个关闭命令的结果
     * </p>
     *
     * @return 结果类型字符串"shutdown"
     */
    @Override
    public String getType() {
        return "shutdown";
    }

    /**
     * 判断是否为优雅关闭
     *
     * @return true表示优雅关闭，false表示强制关闭
     */
    public boolean isGraceful() {
        return graceful;
    }

    /**
     * 获取关闭消息
     *
     * @return 关闭消息字符串
     */
    public String getMessage() {
        return message;
    }
}
