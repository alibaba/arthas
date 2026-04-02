package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.klass100.RetransformCommand.RetransformEntry;
import com.taobao.arthas.core.command.model.RetransformModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

/**
 * Retransform 命令的结果视图
 * 用于显示类转换操作的结果，包括显示匹配的类加载器、删除的条目、转换列表等
 *
 * @author hengyunabc 2021-01-06
 */
public class RetransformView extends ResultView<RetransformModel> {

    /**
     * 渲染 Retransform 命令的结果
     * 根据不同的操作类型显示不同的输出格式
     *
     * @param process 命令处理进程，用于写入输出
     * @param result Retransform 命令的结果模型
     */
    @Override
    public void draw(CommandProcess process, RetransformModel result) {
        // 情况1：匹配到多个 classloader
        // 当类搜索匹配到多个类加载器时，显示所有匹配的类加载器信息
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        // 情况2：执行 retransform -d 命令删除转换条目
        // 显示成功删除的转换条目 ID
        if (result.getDeletedRetransformEntry() != null) {
            process.write("Delete RetransformEntry by id success. id: " + result.getDeletedRetransformEntry().getId());
            process.write("\n");
            return;
        }

        // 情况3：执行 retransform -l 命令列出所有转换条目
        // 以表格形式显示所有已转换的类信息
        if (result.getRetransformEntries() != null) {
            // 创建表格，包含5列：Id、ClassName、TransformCount、LoaderHash、LoaderClassName
            TableElement table = new TableElement(1, 1, 1, 1, 1).rightCellPadding(1);
            // 添加表头，使用粗体样式
            table.add(new RowElement().style(Decoration.bold.bold()).add("Id", "ClassName", "TransformCount", "LoaderHash",
                    "LoaderClassName"));

            // 遍历所有转换条目，添加到表格中
            for (RetransformEntry entry : result.getRetransformEntries()) {
                table.row("" + entry.getId(),                       // 转换条目 ID
                          "" + entry.getClassName(),                // 类名
                          "" + entry.getTransformCount(),           // 转换次数
                          "" + entry.getHashCode(),                 // 类加载器哈希码
                          "" + entry.getClassLoaderClass());        // 类加载器类名
            }

            // 渲染表格并输出
            process.write(RenderUtil.render(table));
            return;
        }

        // 情况4：执行 retransform /tmp/Demo.class 命令进行类转换
        // 显示转换成功的类列表
        if (result.getRetransformClasses() != null) {
            StringBuilder sb = new StringBuilder();
            // 将所有转换成功的类名添加到字符串构建器
            for (String aClass : result.getRetransformClasses()) {
                sb.append(aClass).append("\n");
            }
            // 输出转换成功信息，包括转换的类数量和类名列表
            process.write("retransform success, size: " + result.getRetransformCount()).write(", classes:\n")
                    .write(sb.toString());
        }

    }

}
