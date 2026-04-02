package com.taobao.arthas.core.command.model;

/**
 * 变更结果值对象（Value Object）
 *
 * 用于记录属性变更前后的值，常用于在命令执行过程中展示属性修改的效果对比。
 * 例如，在修改系统属性、配置参数等场景下，可以通过此类展示修改前后的值对比。
 *
 * @author gongdewei 2020/4/16
 */
public class ChangeResultVO {
    /**
     * 属性名称
     * 表示被修改的属性的名称或标识符
     */
    private String name;

    /**
     * 修改前的值
     * 保存属性在修改之前的原始值
     */
    private Object beforeValue;

    /**
     * 修改后的值
     * 保存属性在修改之后的新值
     */
    private Object afterValue;

    /**
     * 默认构造函数
     * 创建一个空的变更结果对象
     */
    public ChangeResultVO() {
    }

    /**
     * 全参数构造函数
     *
     * @param name 属性名称
     * @param beforeValue 修改前的值
     * @param afterValue 修改后的值
     */
    public ChangeResultVO(String name, Object beforeValue, Object afterValue) {
        // 设置属性名称
        this.name = name;
        // 设置修改前的值
        this.beforeValue = beforeValue;
        // 设置修改后的值
        this.afterValue = afterValue;
    }

    /**
     * 获取属性名称
     *
     * @return 属性名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置属性名称
     *
     * @param name 属性名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取修改前的值
     *
     * @return 修改前的值
     */
    public Object getBeforeValue() {
        return beforeValue;
    }

    /**
     * 设置修改前的值
     *
     * @param beforeValue 修改前的值
     */
    public void setBeforeValue(Object beforeValue) {
        this.beforeValue = beforeValue;
    }

    /**
     * 获取修改后的值
     *
     * @return 修改后的值
     */
    public Object getAfterValue() {
        return afterValue;
    }

    /**
     * 设置修改后的值
     *
     * @param afterValue 修改后的值
     */
    public void setAfterValue(Object afterValue) {
        this.afterValue = afterValue;
    }
}

