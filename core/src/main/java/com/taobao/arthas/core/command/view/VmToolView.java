package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.VmToolModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * VM工具视图类
 * 用于展示 vmtool 命令的执行结果
 *
 * @author hengyunabc 2022-04-24
 *
 */
public class VmToolView extends ResultView<VmToolModel> {
    /**
     * 绘制 vmtool 命令的执行结果到命令行
     * 根据结果类型展示匹配的类加载器或对象值
     *
     * @param process 命令进程对象，用于输出信息
     * @param model vmtool 命令的模型对象，包含执行结果
     */
    @Override
    public void draw(CommandProcess process, VmToolModel model) {
        // 如果有匹配的类加载器，则展示类加载器列表
        if (model.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            // 使用 ClassLoaderView 绘制类加载器信息，false 表示不显示索引
            ClassLoaderView.drawClassLoaders(process, model.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        // 获取对象值
        ObjectVO objectVO = model.getValue();
        // 判断是否需要展开对象，如果需要则使用 ObjectView 进行格式化，否则直接获取对象
        String resultStr = StringUtils.objectToString(objectVO.needExpand() ? new ObjectView(objectVO).draw() : objectVO.getObject());
        // 将结果字符串写入命令行
        process.write(resultStr).write("\n");
    }
}
