package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.ClassLoaderStat;
import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.ClassLoaderUrlStat;
import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.UrlClassStat;
import com.taobao.arthas.core.command.model.ClassDetailVO;
import com.taobao.arthas.core.command.model.ClassLoaderModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ClassSetVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.*;
import com.taobao.text.util.RenderUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * ClassLoader命令的结果视图类
 *
 * 负责将ClassLoader相关命令的结果渲染并输出到命令行界面。
 * 支持多种ClassLoader相关的展示功能，包括：
 * 1. 展示匹配的ClassLoader列表
 * 2. 展示ClassLoader加载的所有类
 * 3. 展示ClassLoader的资源信息
 * 4. 展示已加载的类详情
 * 5. 展示ClassLoader的URL列表
 * 6. 展示ClassLoader树状结构或表格形式
 * 7. 展示ClassLoader统计信息
 * 8. 展示URL统计信息
 * 9. 展示URL类统计信息
 *
 * @author gongdewei 2020/4/21
 */
public class ClassLoaderView extends ResultView<ClassLoaderModel> {

    /**
     * 绘制ClassLoader命令的执行结果
     *
     * 根据结果对象中包含的不同类型数据，调用相应的绘制方法进行渲染。
     * 支持多种展示模式，按优先级依次判断并渲染：
     * 1. 匹配的ClassLoader列表
     * 2. 类集合信息
     * 3. 资源信息
     * 4. 加载的类详情
     * 5. ClassLoader的URL列表
     * 6. ClassLoader列表（表格或树状）
     * 7. ClassLoader统计信息
     * 8. URL统计信息
     * 9. URL类统计信息
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param result ClassLoader命令的执行结果模型，包含各种ClassLoader相关信息
     */
    @Override
    public void draw(CommandProcess process, ClassLoaderModel result) {
        // 如果有匹配的ClassLoader，则显示匹配的ClassLoader列表
        if (result.getMatchedClassLoaders() != null) {
            process.write("Matched classloaders: \n");
            drawClassLoaders(process, result.getMatchedClassLoaders(), false);
            process.write("\n");
            return;
        }
        // 如果有类集合信息，则显示所有类
        if (result.getClassSet() != null) {
            drawAllClasses(process, result.getClassSet());
        }
        // 如果有资源信息，则显示资源
        if (result.getResources() != null) {
            drawResources(process, result.getResources());
        }
        // 如果有加载的类详情，则显示类详情
        if (result.getLoadClass() != null) {
            drawLoadClass(process, result.getLoadClass());
        }
        // 如果有URL列表，则显示ClassLoader的URL
        if (result.getUrls() != null) {
            drawClassLoaderUrls(process, result.getUrls());
        }
        // 如果有ClassLoader列表，则显示ClassLoader信息（树状或表格）
        if (result.getClassLoaders() != null){
            drawClassLoaders(process, result.getClassLoaders(), result.getTree());
        }
        // 如果有ClassLoader统计信息，则显示统计数据
        if (result.getClassLoaderStats() != null){
            drawClassLoaderStats(process, result.getClassLoaderStats());
        }
        // 如果有URL统计信息，则显示URL统计
        if (result.getUrlStats() != null) {
            drawUrlStats(process, result.getUrlStats());
        }
        // 如果有URL类统计信息，则显示URL类统计
        if (result.getUrlClassStats() != null) {
            drawUrlClassStats(process, result.getClassLoader(), result.getUrlClassStats(),
                    Boolean.TRUE.equals(result.getUrlClassStatsDetail()));
        }
    }

    /**
     * 绘制URL类统计信息
     *
     * 显示每个URL加载的类的统计信息，包括已加载的类数量和匹配的类数量。
     * 支持两种显示模式：
     * 1. 简略模式（detail=false）：只显示URL和类数量统计
     * 2. 详细模式（detail=true）：显示URL、统计信息以及每个类的详细列表
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param classLoader ClassLoader对象，用于显示ClassLoader的名称和hash
     * @param urlClassStats URL类统计信息列表
     * @param detail 是否显示详细信息，true表示显示类列表，false表示只显示统计
     */
    private void drawUrlClassStats(CommandProcess process, ClassLoaderVO classLoader, List<UrlClassStat> urlClassStats,
            boolean detail) {
        // 如果ClassLoader不为空，则显示ClassLoader的名称和hash
        if (classLoader != null) {
            process.write(classLoader.getName() + ", hash:" + classLoader.getHash() + "\n");
        }

        // 检查是否有匹配的类数量信息
        boolean hasMatched = false;
        for (UrlClassStat stat : urlClassStats) {
            if (stat.getMatchedClassCount() != null) {
                hasMatched = true;
                break;
            }
        }

        // 如果不是详细模式，则只显示统计表格
        if (!detail) {
            // 创建表格，设置左右单元格内边距
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            // 创建表头，使用粗体样式
            RowElement header = new RowElement().style(Decoration.bold.bold());
            if (hasMatched) {
                // 如果有匹配的类，则显示三列：URL、已加载类数量、匹配类数量
                header.add("url", "loadedClassCount", "matchedClassCount");
            } else {
                // 如果没有匹配的类，则只显示两列：URL、已加载类数量
                header.add("url", "loadedClassCount");
            }
            table.add(header);

            // 填充数据行
            for (UrlClassStat stat : urlClassStats) {
                if (hasMatched) {
                    table.row(stat.getUrl(), "" + stat.getLoadedClassCount(), "" + stat.getMatchedClassCount());
                } else {
                    table.row(stat.getUrl(), "" + stat.getLoadedClassCount());
                }
            }
            // 渲染表格并输出
            process.write(RenderUtil.render(table, process.width()))
                    .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
            return;
        }

        // 详细模式：为每个URL创建一个表格，显示类列表
        for (UrlClassStat stat : urlClassStats) {
            // 创建表格，设置左右单元格内边距
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);

            // 构建标题，包含URL、已加载类数量和匹配类数量
            StringBuilder title = new StringBuilder();
            title.append(stat.getUrl())
                    .append(" (loaded: ").append(stat.getLoadedClassCount());
            if (hasMatched) {
                title.append(", matched: ").append(stat.getMatchedClassCount());
            }
            title.append(")");
            // 添加标题行，使用粗体样式
            table.row(new LabelElement(title.toString()).style(Decoration.bold.bold()));

            // 获取类列表并逐行添加
            List<String> classes = stat.getClasses();
            if (classes != null) {
                for (String className : classes) {
                    table.row(className);
                }
            }

            // 如果结果被截断且有类列表，则显示提示信息
            if (stat.isTruncated() && classes != null) {
                int total = hasMatched ? stat.getMatchedClassCount() : stat.getLoadedClassCount();
                table.row(new LabelElement("... (showing first " + classes.size() + " of " + total
                        + ", use -n/--limit to change limit)"));
            }

            // 渲染表格并输出
            process.write(RenderUtil.render(table, process.width()))
                    .write("\n");
        }
    }

    /**
     * 绘制URL统计信息
     *
     * 显示每个ClassLoader的URL使用情况，包括已使用的URL和未使用的URL。
     * 忽略那些没有任何URL使用的动态ClassLoader（如sun.reflect.DelegatingClassLoader）。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param urlStats URL统计信息的映射表，键为ClassLoader，值为URL统计信息
     */
    private void drawUrlStats(CommandProcess process, Map<ClassLoaderVO, ClassLoaderUrlStat> urlStats) {
        // 遍历每个ClassLoader的URL统计信息
        for (Entry<ClassLoaderVO, ClassLoaderUrlStat> entry : urlStats.entrySet()) {
            ClassLoaderVO classLoaderVO = entry.getKey();
            ClassLoaderUrlStat urlStat = entry.getValue();

            // 忽略 sun.reflect.DelegatingClassLoader 等动态ClassLoader
            // 如果既没有已使用的URL，也没有未使用的URL，则跳过
            if (urlStat.getUsedUrls().isEmpty() && urlStat.getUnUsedUrls().isEmpty()) {
                continue;
            }

            // 创建表格，设置左右单元格内边距
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            // 添加ClassLoader名称和hash作为标题行
            table.row(new LabelElement(classLoaderVO.getName() + ", hash:" + classLoaderVO.getHash())
                    .style(Decoration.bold.bold()));
            // 获取已使用的URL集合
            Collection<String> usedUrls = urlStat.getUsedUrls();
            // 添加已使用URL的标题
            table.row(new LabelElement("Used URLs:").style(Decoration.bold.bold()));
            // 逐行添加已使用的URL
            for (String url : usedUrls) {
                table.row(url);
            }
            // 获取未使用的URL集合
            Collection<String> UnnsedUrls = urlStat.getUnUsedUrls();
            // 添加未使用URL的标题
            table.row(new LabelElement("Unused URLs:").style(Decoration.bold.bold()));
            // 逐行添加未使用的URL
            for (String url : UnnsedUrls) {
                table.row(url);
            }
            // 渲染表格并输出
            process.write(RenderUtil.render(table, process.width()))
                    .write("\n");
        }
    }

    /**
     * 绘制ClassLoader统计信息
     *
     * 显示每种类型的ClassLoader的统计信息，包括实例数量和加载的类总数。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param classLoaderStats ClassLoader统计信息的映射表，键为ClassLoader名称，值为统计信息
     */
    private void drawClassLoaderStats(CommandProcess process, Map<String, ClassLoaderStat> classLoaderStats) {
        // 渲染统计信息为表格元素
        Element element = renderStat(classLoaderStats);
        // 渲染表格并输出到命令行
        process.write(RenderUtil.render(element, process.width()))
                .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);

    }

    /**
     * 渲染ClassLoader统计信息为表格元素
     *
     * 创建一个表格，包含ClassLoader的名称、实例数量和加载的类总数。
     *
     * @param classLoaderStats ClassLoader统计信息的映射表
     * @return 表格元素，包含ClassLoader统计信息
     */
    private static TableElement renderStat(Map<String, ClassLoaderStat> classLoaderStats) {
        // 创建表格，设置左右单元格内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表头，包含三列：名称、实例数量、加载类总数
        table.add(new RowElement().style(Decoration.bold.bold()).add("name", "numberOfInstances", "loadedCountTotal"));
        // 遍历统计信息，填充数据行
        for (Map.Entry<String, ClassLoaderStat> entry : classLoaderStats.entrySet()) {
            table.row(entry.getKey(), "" + entry.getValue().getNumberOfInstance(), "" + entry.getValue().getLoadedCount());
        }
        return table;
    }

    /**
     * 绘制ClassLoader列表
     *
     * 根据isTree参数决定使用树状结构还是表格形式显示ClassLoader信息。
     * 树状结构显示ClassLoader的继承关系，表格形式显示ClassLoader的详细信息。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param classLoaders ClassLoader集合
     * @param isTree 是否使用树状结构显示，true为树状，false为表格
     */
    public static void drawClassLoaders(CommandProcess process, Collection<ClassLoaderVO> classLoaders, boolean isTree) {
        // 根据isTree参数选择渲染方式
        Element element = isTree ? renderTree(classLoaders) : renderTable(classLoaders);
        // 渲染元素并输出到命令行
        process.write(RenderUtil.render(element, process.width()))
                .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
    }

    /**
     * 绘制ClassLoader的URL列表
     *
     * 显示ClassLoader所包含的所有URL路径。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param urls URL列表
     */
    private void drawClassLoaderUrls(CommandProcess process, List<String> urls) {
        // 渲染URL列表并输出
        process.write(RenderUtil.render(renderClassLoaderUrls(urls), process.width()));
        process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
    }

    /**
     * 绘制已加载的类详情
     *
     * 显示类的详细信息，包括类名、类加载器、超类、实现的接口等。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param loadClass 类详情对象
     */
    private void drawLoadClass(CommandProcess process, ClassDetailVO loadClass) {
        // 使用ClassUtils渲染类信息并输出
        process.write(RenderUtil.render(ClassUtils.renderClassInfo(loadClass), process.width()) + "\n");
    }

    /**
     * 绘制所有类
     *
     * 显示ClassLoader加载的所有类，按段（segment）组织。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param classSetVO 类集合对象，包含ClassLoader信息和类列表
     */
    private void drawAllClasses(CommandProcess process, ClassSetVO classSetVO) {
        // 渲染类集合并输出
        process.write(RenderUtil.render(renderClasses(classSetVO), process.width()));
        process.write("\n");
    }

    /**
     * 绘制资源列表
     *
     * 显示ClassLoader中的资源文件列表。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param resources 资源文件列表
     */
    private void drawResources(CommandProcess process, List<String> resources) {
        // 创建表格，设置左右单元格内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 逐行添加资源路径
        for (String resource : resources) {
            table.row(resource);
        }
        // 渲染表格并输出
        process.write(RenderUtil.render(table, process.width()) + "\n");
        process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
    }

    /**
     * 渲染类集合为表格元素
     *
     * 创建一个表格，显示ClassLoader加载的所有类。
     * 如果是第一段（segment为0），则在顶部显示ClassLoader的hash和名称。
     *
     * @param classSetVO 类集合对象，包含ClassLoader信息和类列表
     * @return 表格元素，包含类信息
     */
    private Element renderClasses(ClassSetVO classSetVO) {
        // 创建表格，设置左右单元格内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 如果是第一段，则添加ClassLoader信息作为标题
        if (classSetVO.getSegment() == 0) {
            table.row(new LabelElement("hash:" + classSetVO.getClassloader().getHash() + ", " + classSetVO.getClassloader().getName())
                    .style(Decoration.bold.bold()));
        }
        // 逐行添加类名
        for (String className : classSetVO.getClasses()) {
            table.row(new LabelElement(className));
        }
        return table;
    }

    /**
     * 渲染ClassLoader的URL列表为标签元素
     *
     * 将URL列表格式化为多行文本，每个URL一行。
     *
     * @param urls URL列表
     * @return 标签元素，包含格式化后的URL列表
     */
    private static Element renderClassLoaderUrls(List<String> urls) {
        // 使用StringBuilder构建多行文本
        StringBuilder sb = new StringBuilder();
        for (String url : urls) {
            sb.append(url).append("\n");
        }
        return new LabelElement(sb.toString());
    }

    /**
     * 渲染ClassLoader信息为表格形式
     *
     * 创建一个表格，显示ClassLoader的详细信息，包括名称、加载的类数量、hash和父ClassLoader。
     * 用于统计和展示所有的ClassLoader的信息。
     *
     * @param classLoaderInfos ClassLoader信息集合
     * @return 表格元素，包含ClassLoader的详细信息
     */
    // 统计所有的ClassLoader的信息
    private static TableElement renderTable(Collection<ClassLoaderVO> classLoaderInfos) {
        // 创建表格，设置左右单元格内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表头，包含四列：名称、加载类数量、hash、父ClassLoader
        table.add(new RowElement().style(Decoration.bold.bold()).add("name", "loadedCount", "hash", "parent"));
        // 遍历ClassLoader信息，填充数据行
        for (ClassLoaderVO classLoaderVO : classLoaderInfos) {
            table.row(classLoaderVO.getName(), "" + classLoaderVO.getLoadedCount(), classLoaderVO.getHash(), classLoaderVO.getParent());
        }
        return table;
    }

    /**
     * 渲染ClassLoader信息为树状结构
     *
     * 创建一个树状结构，显示ClassLoader的继承关系。
     * 每个ClassLoader作为树的节点，子ClassLoader作为子节点。
     *
     * @param classLoaderInfos ClassLoader信息集合
     * @return 树元素，包含ClassLoader的继承结构
     */
    // 以树状列出ClassLoader的继承结构
    private static Element renderTree(Collection<ClassLoaderVO> classLoaderInfos) {
        // 创建树的根节点
        TreeElement root = new TreeElement();
        // 遍历ClassLoader信息，为每个ClassLoader创建子节点
        for (ClassLoaderVO classLoader : classLoaderInfos) {
            // 创建子节点，节点名称为ClassLoader的名称
            TreeElement child = new TreeElement(classLoader.getName());
            // 将子节点添加到根节点
            root.addChild(child);
            // 递归渲染子树，包含所有子ClassLoader
            renderSubtree(child, classLoader);
        }
        return root;
    }

    /**
     * 递归渲染ClassLoader的子树
     *
     * 为当前ClassLoader的所有子ClassLoader创建树节点，并递归处理子ClassLoader的子节点。
     *
     * @param parent 父树节点
     * @param parentClassLoader 父ClassLoader对象
     */
    private static void renderSubtree(TreeElement parent, ClassLoaderVO parentClassLoader) {
        // 如果没有子ClassLoader，则直接返回
        if (parentClassLoader.getChildren() == null){
            return;
        }
        // 遍历所有子ClassLoader
        for (ClassLoaderVO childClassLoader : parentClassLoader.getChildren()) {
            // 为子ClassLoader创建树节点
            TreeElement child = new TreeElement(childClassLoader.getName());
            // 将子节点添加到父节点
            parent.addChild(child);
            // 递归处理子ClassLoader的子节点
            renderSubtree(child, childClassLoader);
        }
    }
}
