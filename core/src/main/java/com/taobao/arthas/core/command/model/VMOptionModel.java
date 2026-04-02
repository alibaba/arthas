package com.taobao.arthas.core.command.model;

import com.sun.management.VMOption;

import java.util.List;

/**
 * JVM选项模型
 *
 * 该类用于封装VMOption命令的返回结果，支持两种模式：
 * 1. 查询模式：返回JVM虚拟机的所有选项列表
 * 2. 修改模式：返回修改JVM选项的结果信息
 *
 * 该类继承自ResultModel，是Arthas命令结果模型体系的一部分
 *
 * @author gongdewei 2020/4/15
 */
public class VMOptionModel extends ResultModel {

    /**
     * JVM选项列表
     *
     * 存储JVM虚拟机的所有选项信息，包括选项名称、当前值、默认值等。
     * 该字段用于查询模式下返回完整的VM选项列表。
     *
     * VMOption对象包含以下信息：
     * - name: 选项名称
     * - value: 当前值
     * - origin: 值的来源（如DEFAULT、CONFIG_FILE等）
     * - writeable: 是否可写
     */
    private List<VMOption> vmOptions;

    /**
     * 修改结果对象
     *
     * 存储修改JVM选项后的操作结果，包括是否成功、修改前后的值等信息。
     * 该字段用于修改模式下返回操作结果。
     *
     * ChangeResultVO对象包含以下信息：
     * - success: 是否修改成功
     * - message: 结果消息
     * - oldValue: 修改前的值
     * - newValue: 修改后的值
     */
    private ChangeResultVO changeResult;

    /**
     * 默认构造函数
     *
     * 创建一个空的VMOptionModel对象，
     * 通常用于序列化/反序列化场景
     */
    public VMOptionModel() {
    }

    /**
     * 构造函数 - 查询模式
     *
     * 创建包含VM选项列表的模型对象，
     * 用于返回查询到的所有JVM选项信息
     *
     * @param vmOptions JVM选项列表
     */
    public VMOptionModel(List<VMOption> vmOptions) {
        this.vmOptions = vmOptions;
    }

    /**
     * 构造函数 - 修改模式
     *
     * 创建包含修改结果的模型对象，
     * 用于返回修改JVM选项的操作结果
     *
     * @param changeResult 修改结果对象
     */
    public VMOptionModel(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }

    /**
     * 获取模型类型
     *
     * 返回该模型的类型标识，用于前端或客户端识别模型类型
     *
     * @return 模型类型字符串"vmoption"
     */
    @Override
    public String getType() {
        return "vmoption";
    }

    /**
     * 获取JVM选项列表
     *
     * @return JVM选项列表，如果没有设置则返回null
     */
    public List<VMOption> getVmOptions() {
        return vmOptions;
    }

    /**
     * 设置JVM选项列表
     *
     * @param vmOptions 要设置的JVM选项列表
     */
    public void setVmOptions(List<VMOption> vmOptions) {
        this.vmOptions = vmOptions;
    }

    /**
     * 获取修改结果对象
     *
     * @return 修改结果对象，如果没有设置则返回null
     */
    public ChangeResultVO getChangeResult() {
        return changeResult;
    }

    /**
     * 设置修改结果对象
     *
     * @param changeResult 要设置的修改结果对象
     */
    public void setChangeResult(ChangeResultVO changeResult) {
        this.changeResult = changeResult;
    }
}
