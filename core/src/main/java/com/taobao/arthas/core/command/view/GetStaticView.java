package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.GetStaticModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.text.ui.Element;
import com.taobao.text.util.RenderUtil;

/**
 * 获取静态字段视图
 * 用于渲染getstatic命令的执行结果，展示类的静态字段值
 * 支持显示字段名称、字段值以及匹配的类加载器信息
 *
 * @author gongdewei 2020/4/20
 */
public class GetStaticView extends ResultView<GetStaticModel> {

    /**
     * 绘制命令执行结果
     * 根据不同的结果类型展示不同的信息：
     * 1. 匹配的类加载器列表
     * 2. 静态字段的值（字段名和字段内容）
     * 3. 匹配的类列表
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 命令执行结果模型
     */
    @Override
    public void draw(CommandProcess process, GetStaticModel result) {
        // 如果存在匹配的类加载器，则显示类加载器信息
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        // 如果存在字段信息，则显示字段名称和字段值
        if (result.getField() != null) {
            ObjectVO field = result.getField();
            // 判断字段是否需要展开显示（复杂对象需要展开，简单对象直接显示）
            String valueStr = StringUtils.objectToString(field.needExpand() ? new ObjectView(field).draw() : field.getObject());
            // 输出字段名和字段值
            process.write("field: " + result.getFieldName() + "\n" + valueStr + "\n");
        // 否则如果存在匹配的类，则显示匹配的类列表
        } else if (result.getMatchedClasses() != null) {
            Element table = ClassUtils.renderMatchedClasses(result.getMatchedClasses());
            process.write(RenderUtil.render(table)).write("\n");
        }
    }
}
