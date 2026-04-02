package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * WatchModel 的终端视图类
 * 用于展示 watch 命令的执行结果，包括方法调用信息和观察到的对象值
 *
 * @author gongdewei 2020/3/27
 */
public class WatchView extends ResultView<WatchModel> {

    /**
     * 绘制 watch 命令的执行结果到命令行
     * 展示方法调用信息、时间戳、耗时和观察到的对象值
     *
     * @param process 命令进程对象，用于输出信息
     * @param model watch 命令的模型对象，包含观察结果
     */
    @Override
    public void draw(CommandProcess process, WatchModel model) {
        // 获取观察到的对象值
        ObjectVO objectVO = model.getValue();
        // 标化对象长度限制
        int sizeLimit = ObjectView.normalizeMaxObjectLength(model.getSizeLimit());
        // 判断是否需要展开对象，如果需要则使用 ObjectView 进行格式化，否则直接获取对象
        String result = StringUtils.objectToString(
                objectVO.needExpand() ? new ObjectView(sizeLimit, objectVO).draw() : objectVO.getObject());

        // 构建结果字符串
        StringBuilder sb = new StringBuilder();
        // 添加方法名和访问点信息（例如：method=ClassA.methodName location=AtExit()）
        sb.append("method=").append(model.getClassName()).append(".").append(model.getMethodName())
                .append(" location=").append(model.getAccessPoint()).append("\n");
        // 添加时间戳、耗时和观察结果
        sb.append("ts=").append(DateUtils.formatDateTime(model.getTs()))
                .append("; [cost=").append(model.getCost()).append("ms] result=").append(result).append("\n");

        // 将结果字符串写入命令行
        process.write(sb.toString());
    }
}
