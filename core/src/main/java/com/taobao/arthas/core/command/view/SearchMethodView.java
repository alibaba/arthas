package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SearchMethodModel;
import com.taobao.arthas.core.command.model.MethodVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.text.util.RenderUtil;


/**
 * SearchMethodCommand 的渲染视图
 * 用于显示搜索到的类方法信息，支持简单列表和详细信息两种显示模式
 *
 * @author gongdewei 2020/4/9
 */
public class SearchMethodView extends ResultView<SearchMethodModel> {

    /**
     * 渲染方法搜索结果
     * 根据搜索结果显示匹配的类加载器或方法信息
     *
     * @param process 命令处理进程，用于写入输出
     * @param result 方法搜索结果模型
     */
    @Override
    public void draw(CommandProcess process, SearchMethodModel result) {
        // 情况1：搜索到多个匹配的类加载器
        // 当方法搜索匹配到多个类加载器时，显示所有匹配的类加载器信息
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            ClassLoaderView.drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }

        // 获取是否显示详细信息的标志
        boolean detail = result.isDetail();
        // 获取方法信息对象
        MethodVO methodInfo = result.getMethodInfo();

        // 情况2：详细信息模式
        if (detail) {
            // 判断是否是构造方法
            if (methodInfo.isConstructor()) {
                // 渲染构造方法的详细信息
                process.write(RenderUtil.render(ClassUtils.renderConstructor(methodInfo), process.width()) + "\n");
            } else {
                // 渲染普通方法的详细信息
                process.write(RenderUtil.render(ClassUtils.renderMethod(methodInfo), process.width()) + "\n");
            }
        }
        // 情况3：简单列表模式（默认）
        // 格式：类名 方法名+方法描述符
        // 例如：java.util.List indexOf(Ljava/lang/Object;)I
        else {
            // 输出声明类
            process.write(methodInfo.getDeclaringClass())
                    .write(" ")
                    // 输出方法名
                    .write(methodInfo.getMethodName())
                    // 输出方法描述符（包含参数类型和返回类型）
                    .write(methodInfo.getDescriptor())
                    .write("\n");
        }

    }
}
