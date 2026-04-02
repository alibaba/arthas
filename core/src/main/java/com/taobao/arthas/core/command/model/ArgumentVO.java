package com.taobao.arthas.core.command.model;

/**
 * 命令参数值对象（VO）
 *
 * 用于封装命令参数的元数据信息，包括参数名称、是否必填、是否多值等属性。
 * 主要用于在Arthas命令系统中描述和传递命令参数的定义信息。
 *
 * @author gongdewei 2020/4/3
 */
public class ArgumentVO {
    /**
     * 参数名称
     * 表示命令参数的标识符
     */
    private String argName;

    /**
     * 是否为必填参数
     * true表示该参数必须提供，false表示可选
     */
    private boolean required;

    /**
     * 是否支持多值
     * true表示该参数可以接受多个值，false表示只能接受单个值
     */
    private boolean multiValued;

    /**
     * 默认构造函数
     * 创建一个空的ArgumentVO对象，所有字段使用默认值
     */
    public ArgumentVO() {
    }

    /**
     * 全参数构造函数
     *
     * @param argName 参数名称
     * @param required 是否必填
     * @param multiValued 是否支持多值
     */
    public ArgumentVO(String argName, boolean required, boolean multiValued) {
        this.argName = argName;
        this.required = required;
        this.multiValued = multiValued;
    }

    /**
     * 获取参数名称
     *
     * @return 参数名称字符串
     */
    public String getArgName() {
        return argName;
    }

    /**
     * 设置参数名称
     *
     * @param argName 要设置的参数名称
     */
    public void setArgName(String argName) {
        this.argName = argName;
    }

    /**
     * 判断参数是否必填
     *
     * @return true表示必填，false表示可选
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 设置参数是否必填
     *
     * @param required true表示必填，false表示可选
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * 判断参数是否支持多值
     *
     * @return true表示支持多值，false表示只支持单值
     */
    public boolean isMultiValued() {
        return multiValued;
    }

    /**
     * 设置参数是否支持多值
     *
     * @param multiValued true表示支持多值，false表示只支持单值
     */
    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }
}
