package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.JadModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.lang.LangRenderUtil;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.util.RenderUtil;

/**
 * Jad（Java Decompiler）反编译命令视图类
 *
 * <p>负责渲染和显示 Java 类的反编译结果。该视图会处理以下几种情况：</p>
 * <ul>
 *   <li>显示匹配到的类加载器列表（当类加载器不确定时）</li>
 *   <li>显示匹配到的类列表（当类名不确定时）</li>
 *   <li>显示单个类的反编译源代码（包括类加载器信息和类文件位置）</li>
 * </ul>
 *
 * @author gongdewei 2020/4/22
 */
public class JadView extends ResultView<JadModel> {

    /**
     * 绘制 Jad 反编译结果
     *
     * <p>根据结果类型渲染不同的信息：</p>
     * <ul>
     *   <li>如果包含匹配的类加载器，显示类加载器列表供用户选择</li>
     *   <li>如果包含匹配的类，显示类列表供用户选择</li>
     *   <li>否则显示反编译后的源代码，包括类加载器信息和类文件位置</li>
     * </ul>
     *
     * @param process 命令处理进程，用于输出渲染结果
     * @param result Jad 模型数据，包含反编译结果相关信息
     */
    @Override
    public void draw(CommandProcess process, JadModel result) {
        // 情况1：存在多个匹配的类加载器，需要用户选择
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            // 使用 ClassLoaderView 绘制类加载器列表
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        // 获取终端宽度，用于格式化输出
        int width = process.width();

        // 情况2：存在多个匹配的类，需要用户选择
        if (result.getMatchedClasses() != null) {
            // 渲染匹配的类列表
            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
            process.write(RenderUtil.render(table, width)).write("\n");
        } else {
            // 情况3：显示单个类的反编译结果
            ClassVO classInfo = result.getClassInfo();
            if (classInfo != null) {
                // 输出类加载器信息（红色加粗标签）
                process.write("\n");
                process.write(RenderUtil.render(new LabelElement("ClassLoader: ").style(Decoration.bold.fg(Color.red)), width));
                // 渲染类加载器层次结构
                process.write(RenderUtil.render(TypeRenderUtils.drawClassLoader(classInfo), width) + "\n");
            }
            // 输出类文件位置信息（如果存在）
            if (result.getLocation() != null) {
                process.write(RenderUtil.render(new LabelElement("Location: ").style(Decoration.bold.fg(Color.red)), width));
                // 类文件路径使用蓝色加粗显示
                process.write(RenderUtil.render(new LabelElement(result.getLocation()).style(Decoration.bold.fg(Color.blue)), width) + "\n");
            }
            // 输出反编译后的源代码（带语法高亮）
            process.write(LangRenderUtil.render(result.getSource()) + "\n");
            // 输出空字符串作为结束标记
            process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
        }
    }

}
