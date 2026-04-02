package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.affect.EnhancerAffect;

/**
 * Reset命令结果模型类
 * 用于封装reset命令的执行结果，reset命令用于重置之前被增强的类，恢复到原始状态
 *
 * @author gongdewei 2020/6/22
 */
public class ResetModel extends ResultModel {

    /**
     * 增强器影响对象视图
     * 封装了reset操作影响的类、方法等统计信息
     */
    private EnhancerAffectVO affect;

    /**
     * 构造函数（使用增强器影响视图对象）
     *
     * @param affect 增强器影响视图对象，包含reset操作的影响信息
     */
    public ResetModel(EnhancerAffectVO affect) {
        this.affect = affect;
    }

    /**
     * 构造函数（使用增强器影响对象）
     * 会自动将EnhancerAffect对象转换为EnhancerAffectVO视图对象
     *
     * @param affect 增强器影响对象，包含reset操作的影响信息
     */
    public ResetModel(EnhancerAffect affect) {
        this.affect = EnhancerModelFactory.createEnhancerAffectVO(affect);
    }

    /**
     * 获取结果模型的类型标识
     *
     * @return 类型标识字符串 "reset"
     */
    @Override
    public String getType() {
        return "reset";
    }

    /**
     * 设置增强器影响信息
     * 使用链式调用风格，方便连续设置多个属性
     * 会自动将EnhancerAffect对象转换为EnhancerAffectVO视图对象
     *
     * @param affect 增强器影响对象
     * @return 当前ResetModel对象实例，支持链式调用
     */
    public ResetModel affect(EnhancerAffect affect) {
        this.affect = EnhancerModelFactory.createEnhancerAffectVO(affect);
        return this;
    }

    /**
     * 获取增强器影响信息
     *
     * @return 增强器影响视图对象
     */
    public EnhancerAffectVO getAffect() {
        return affect;
    }
}
