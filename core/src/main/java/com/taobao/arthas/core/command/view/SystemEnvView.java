package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SystemEnvModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 系统环境变量的终端视图类
 * 负责将系统环境变量以表格形式展示
 *
 * @author gongdewei 2020/4/2
 */
public class SystemEnvView extends ResultView<SystemEnvModel> {

    /**
     * 绘制系统环境变量视图
     * 将环境变量键值对渲染为表格格式并输出
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 系统环境变量模型对象，包含环境变量映射表
     */
    @Override
    public void draw(CommandProcess process, SystemEnvModel result) {
        // 使用工具类将环境变量映射表渲染为对齐的表格格式
        // process.width() 获取终端宽度，用于自动调整表格显示
        process.write(ViewRenderUtil.renderKeyValueTable(result.getEnv(), process.width()));
    }

}
