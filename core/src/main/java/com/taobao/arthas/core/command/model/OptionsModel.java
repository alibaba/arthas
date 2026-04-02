package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * 选项命令的数据模型
 * 用于封装options命令的执行结果，可以包含选项列表或选项变更结果
 *
 * @author gongdewei 2020/4/15
 */
public class OptionsModel extends ResultModel{
    /**
     * 选项列表
     * 存储所有可用的Arthas配置选项，每个选项包含名称、类型、值、描述等信息
     */
    private List<OptionVO> options;

    /**
     * 选项变更结果
     * 当执行选项修改操作时，存储变更操作的结果信息
     */
    private ChangeResultVO changeResult;

    /**
     * 默认构造函数
     * 创建一个空的选项数据模型实例
     */
    public OptionsModel() {
    }

    /**
     * 带选项列表的构造函数
     * 用于创建包含选项列表的数据模型
     *
     * @param options 选项列表，包含所有可用的配置选项
     */
    public OptionsModel(List<OptionVO> options) {
        this.options = options;
    }

    /**
     * 带变更结果的构造函数
     * 用于创建包含选项变更结果的数据模型
     *
     * @param changeResult 选项变更操作的结果信息
     */
    public OptionsModel(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }

    /**
     * 获取结果模型的类型标识
     * 用于前端或客户端识别返回的数据类型
     *
     * @return 返回"options"字符串标识，表示这是选项命令的返回结果
     */
    @Override
    public String getType() {
        return "options";
    }

    /**
     * 获取选项列表
     *
     * @return 选项列表，包含所有可用的配置选项
     */
    public List<OptionVO> getOptions() {
        return options;
    }

    /**
     * 设置选项列表
     *
     * @param options 要设置的选项列表，包含所有可用的配置选项
     */
    public void setOptions(List<OptionVO> options) {
        this.options = options;
    }

    /**
     * 获取选项变更结果
     *
     * @return 选项变更操作的结果信息
     */
    public ChangeResultVO getChangeResult() {
        return changeResult;
    }

    /**
     * 设置选项变更结果
     *
     * @param changeResult 选项变更操作的结果信息
     */
    public void setChangeResult(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }
}
