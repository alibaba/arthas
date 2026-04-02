package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.OgnlModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * OGNL表达式视图类
 *
 * 用于渲染OGNL（Object-Graph Navigation Language）表达式的执行结果。
 * 该视图支持两种模式：
 * 1. 显示匹配的类加载器列表
 * 2. 显示OGNL表达式的执行结果
 *
 * @author gongdewei 2020/4/29
 */
public class OgnlView extends ResultView<OgnlModel> {

    /**
     * 绘制OGNL命令执行结果到命令行界面
     *
     * 根据模型对象的内容，有两种输出模式：
     * 1. 如果存在匹配的类加载器，则显示类加载器列表
     * 2. 否则，显示OGNL表达式的执行结果
     *
     * 对于执行结果，会根据对象类型判断是否需要展开显示：
     * - 对于复杂对象（如集合、Map、自定义对象等），使用ObjectView进行展开
     * - 对于简单对象，直接转换为字符串显示
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param model OGNL模型对象，包含执行结果或匹配的类加载器信息
     */
    @Override
    public void draw(CommandProcess process, OgnlModel model) {
        // 如果存在匹配的类加载器列表，则显示类加载器信息
        if (model.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            // 使用ClassLoaderView绘制类加载器列表
            ClassLoaderView.drawClassLoaders(process, model.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        // 获取OGNL表达式的执行结果对象
        ObjectVO objectVO = model.getValue();

        // 判断对象是否需要展开显示
        // 如果需要展开（如复杂对象），使用ObjectView进行格式化；否则直接转换为字符串
        String resultStr = StringUtils.objectToString(objectVO.needExpand() ? new ObjectView(objectVO).draw() : objectVO.getObject());

        // 将结果写入命令行
        process.write(resultStr).write("\n");
    }
}
