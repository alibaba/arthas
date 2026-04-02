package com.taobao.arthas.core.command.model;

/**
 * WebUI 的输入状态模型
 * <p>
 * 用于向 Web 用户界面传递当前命令输入的状态信息，
 * 指导前端展示对应的交互状态（如：是否允许输入、是否允许中断等）
 * </p>
 *
 * @author gongdewei 2020/4/14
 */
public class InputStatusModel extends ResultModel {

    /**
     * 输入状态枚举值
     * 表示当前命令输入框应该呈现的状态
     */
    private InputStatus inputStatus;

    /**
     * 构造函数
     *
     * @param inputStatus 输入状态枚举值
     */
    public InputStatusModel(InputStatus inputStatus) {
        this.inputStatus = inputStatus;
    }

    /**
     * 获取输入状态
     *
     * @return 输入状态枚举值
     */
    public InputStatus getInputStatus() {
        return inputStatus;
    }

    /**
     * 设置输入状态
     *
     * @param inputStatus 输入状态枚举值
     */
    public void setInputStatus(InputStatus inputStatus) {
        this.inputStatus = inputStatus;
    }

    /**
     * 获取模型类型标识
     * 用于序列化时区分不同的模型类型
     *
     * @return 类型标识字符串 "input_status"
     */
    @Override
    public String getType() {
        return "input_status";
    }

}
