package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.affect.RowAffect;

/**
 * 行影响统计模型
 * 用于封装和展示命令执行后影响的行数统计信息
 *
 * @author gongdewei 2020/4/8
 */
public class RowAffectModel extends ResultModel {

    /**
     * 行影响统计对象，包含影响行数的具体统计信息
     */
    private RowAffect affect;

    /**
     * 默认构造函数
     * 创建一个空的行影响统计模型
     */
    public RowAffectModel() {
    }

    /**
     * 构造函数
     *
     * @param affect 行影响统计对象，包含影响行数的具体统计信息
     */
    public RowAffectModel(RowAffect affect) {
        this.affect = affect;
    }

    /**
     * 获取结果模型的类型标识
     * 用于前端识别结果类型并进行相应的展示处理
     *
     * @return 类型标识字符串，固定返回 "row_affect"
     */
    @Override
    public String getType() {
        return "row_affect";
    }

    /**
     * 获取影响的行数
     *
     * @return 影响的行数
     */
    public int getRowCount() {
        return affect.rCnt();
    }

    /**
     * 获取行影响统计对象
     *
     * @return 行影响统计对象
     */
    public RowAffect affect() {
        return affect;
    }
}
