package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Java系统属性的终端视图类
 * 负责将Java系统属性（System Properties）以表格形式展示
 *
 * @author gongdewei 2020/4/2
 */
public class SystemPropertyView extends ResultView<SystemPropertyModel> {

    /**
     * 绘制系统属性视图
     * 将系统属性键值对渲染为表格格式并输出
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 系统属性模型对象，包含系统属性映射表
     */
    @Override
    public void draw(CommandProcess process, SystemPropertyModel result) {
        // 使用工具类将系统属性映射表渲染为对齐的表格格式
        // process.width() 获取终端宽度，用于自动调整表格显示
        process.write(ViewRenderUtil.renderKeyValueTable(result.getProps(), process.width()));
    }

}
