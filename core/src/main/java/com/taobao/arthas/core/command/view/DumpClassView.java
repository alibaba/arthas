package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.DumpClassModel;
import com.taobao.arthas.core.command.model.DumpClassVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * 类转储视图
 * 用于渲染dump-class命令的执行结果，展示已加载类的详细信息
 *
 * @author gongdewei 2020/4/21
 */
public class DumpClassView extends ResultView<DumpClassModel> {

    /**
     * 绘制命令执行结果
     * 根据不同的结果类型展示不同的信息：
     * 1. 匹配的类加载器列表
     * 2. 已转储的类信息（包含哈希码、类加载器、文件路径）
     * 3. 匹配的类列表
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 命令执行结果模型
     */
    @Override
    public void draw(CommandProcess process, DumpClassModel result) {
        // 如果存在匹配的类加载器，则显示类加载器信息
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        // 如果存在已转储的类，则显示类转储结果表格
        if (result.getDumpedClasses() != null) {
            drawDumpedClasses(process, result.getDumpedClasses());

        // 否则如果存在匹配的类，则显示匹配的类列表
        } else if (result.getMatchedClasses() != null) {
            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
            process.write(RenderUtil.render(table)).write("\n");
        }
    }

    /**
     * 绘制已转储的类信息表格
     * 表格包含三列：类加载器哈希码、类加载器信息、转储文件路径
     *
     * @param process 命令处理进程，用于输出结果
     * @param classVOs 已转储的类信息列表
     */
    private void drawDumpedClasses(CommandProcess process, List<DumpClassVO> classVOs) {
        // 创建表格，设置左右内边距为1
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表格表头：哈希码、类加载器、文件路径
        table.row(new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                new LabelElement("CLASSLOADER").style(Decoration.bold.bold()),
                new LabelElement("LOCATION").style(Decoration.bold.bold()));

        // 遍历所有已转储的类，为每个类添加一行数据
        for (DumpClassVO clazz : classVOs) {
            table.row(label(clazz.getClassLoaderHash()).style(Decoration.bold.fg(Color.red)),
                    TypeRenderUtils.drawClassLoader(clazz),
                    label(clazz.getLocation()).style(Decoration.bold.fg(Color.red)));
        }

        // 渲染表格并输出，使用终端宽度自动适配
        process.write(RenderUtil.render(table, process.width()))
                .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
    }

}
