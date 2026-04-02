package com.taobao.arthas.core.command.model;


/**
 * 命令选项视图对象（Value Object）
 * <p>
 * 用于表示命令行选项的详细信息，包括选项的长名称、短名称、描述和是否接受参数值。
 * 主要用于在命令帮助信息中展示各个选项的用法和说明。
 * </p>
 *
 * @author gongdewei 2020/4/3
 */
public class CommandOptionVO {
    /**
     * 选项的长名称
     * 使用双横线开头，例如：--help、--class-pattern
     */
    private String longName;

    /**
     * 选项的短名称
     * 使用单横线开头，通常为单个字符，例如：-h、-c
     */
    private String shortName;

    /**
     * 选项的描述信息
     * 用于说明该选项的作用和用法
     */
    private String description;

    /**
     * 是否接受参数值
     * true表示该选项需要或可以接受一个或多个值
     * false表示该选项是一个开关选项，不接受值
     */
    private boolean acceptValue;

    /**
     * 默认构造函数
     * 创建一个空的命令选项视图对象
     */
    public CommandOptionVO() {
    }

    /**
     * 获取选项的长名称
     *
     * @return 选项的长名称，例如 "--help"
     */
    public String getLongName() {
        return longName;
    }

    /**
     * 设置选项的长名称
     *
     * @param longName 要设置的长名称
     */
    public void setLongName(String longName) {
        this.longName = longName;
    }

    /**
     * 获取选项的短名称
     *
     * @return 选项的短名称，例如 "-h"
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * 设置选项的短名称
     *
     * @param shortName 要设置的短名称
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * 获取选项的描述信息
     *
     * @return 选项的描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置选项的描述信息
     *
     * @param description 要设置的描述信息
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 判断选项是否接受参数值
     *
     * @return true表示接受参数值，false表示不接受
     */
    public boolean isAcceptValue() {
        return acceptValue;
    }

    /**
     * 设置选项是否接受参数值
     *
     * @param acceptValue true表示接受参数值，false表示不接受
     */
    public void setAcceptValue(boolean acceptValue) {
        this.acceptValue = acceptValue;
    }
}
