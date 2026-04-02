package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.FieldVO;
import com.taobao.arthas.core.command.model.SearchClassModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.text.util.RenderUtil;

/**
 * 类搜索命令的结果视图
 * 用于显示搜索到的类信息，支持简单列表和详细信息两种显示模式
 *
 * @author gongdewei 2020/4/8
 */
public class SearchClassView extends ResultView<SearchClassModel> {

    /**
     * 渲染类搜索结果
     * 根据搜索结果显示匹配的类加载器或类信息
     *
     * @param process 命令处理进程，用于写入输出
     * @param result 类搜索结果模型
     */
    @Override
    public void draw(CommandProcess process, SearchClassModel result) {
        // 情况1：搜索到多个匹配的类加载器
        // 当类搜索匹配到多个类加载器时，显示所有匹配的类加载器信息
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        // 情况2：详细信息模式
        // 如果用户请求详细信息（使用 -d 或 --detail 参数），则显示类的完整信息
        if (result.isDetailed()) {
            // 使用 ClassUtils 渲染类信息，包括类加载器、声明信息等
            // 根据 isWithField() 决定是否包含字段信息
            process.write(RenderUtil.render(ClassUtils.renderClassInfo(result.getClassInfo(),
                    result.isWithField()), process.width()));
            process.write("\n");
        }
        // 情况3：简单列表模式（默认）
        // 只显示匹配的类名列表，每行一个类名
        else if (result.getClassNames() != null) {
            // 遍历所有匹配的类名，逐行输出
            for (String className : result.getClassNames()) {
                process.write(className).write("\n");
            }
        }
    }

}
