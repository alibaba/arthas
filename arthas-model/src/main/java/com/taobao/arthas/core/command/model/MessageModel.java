package com.taobao.arthas.core.command.model;

/**
 * 消息模型
 * <p>
 * 用于封装简单的文本消息，通常用于向客户端返回
 * 通知、提示或错误信息等纯文本内容。
 * </p>
 *
 * @author gongdewei 2020/4/2
 */
public class MessageModel extends ResultModel {

    /**
     * 消息文本内容
     * 存储要传递给客户端的消息字符串
     */
    private String message;

    /**
     * 默认构造函数
     * 创建一个空的消息对象
     */
    public MessageModel() {
    }

    /**
     * 构造函数
     *
     * @param message 消息文本内容
     */
    public MessageModel(String message) {
        this.message = message;
    }

    /**
     * 获取消息文本
     *
     * @return 消息文本内容
     */
    public String getMessage() {
        return message;
    }

    /**
     * 获取模型类型标识
     * 用于序列化时区分不同的模型类型
     *
     * @return 类型标识字符串 "message"
     */
    @Override
    public String getType() {
        return "message";
    }
}
