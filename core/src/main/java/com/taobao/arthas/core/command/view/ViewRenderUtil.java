package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ChangeResultVO;
import com.taobao.arthas.core.command.model.EnhancerAffectVO;
import com.taobao.arthas.core.command.model.ThreadVO;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.Overflow;
import com.taobao.text.ui.RowElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.taobao.text.ui.Element.label;
import static java.lang.String.format;

/**
 * 视图渲染工具类
 * 用于在终端/TTY环境下渲染各种视图和表格
 *
 * @author gongdewei 2020/6/22
 */
public class ViewRenderUtil {

    /**
     * 线程状态颜色映射表
     * 定义了不同线程状态对应的显示颜色
     */
    public static final EnumMap<Thread.State, Color> colorMapping = new EnumMap<Thread.State, Color>(Thread.State.class);
    static {
        colorMapping.put(Thread.State.NEW, Color.cyan);
        colorMapping.put(Thread.State.RUNNABLE, Color.green);
        colorMapping.put(Thread.State.BLOCKED, Color.red);
        colorMapping.put(Thread.State.WAITING, Color.yellow);
        colorMapping.put(Thread.State.TIMED_WAITING, Color.magenta);
        colorMapping.put(Thread.State.TERMINATED, Color.blue);
    }

    /**
     * 渲染键值对表格
     * 将 Map 数据渲染为格式化的表格字符串
     *
     * @param map 包含键值对的 Map 对象
     * @param width 表格的宽度
     * @return 渲染后的表格字符串
     */
    public static String renderKeyValueTable(Map<String, String> map, int width) {
        // 创建表格，设置列宽和内边距
        TableElement table = new TableElement(1, 4).leftCellPadding(1).rightCellPadding(1);
        // 添加表头，使用粗体样式
        table.row(true, label("KEY").style(Decoration.bold.bold()), label("VALUE").style(Decoration.bold.bold()));

        // 遍历 Map，为每个键值对添加一行
        for (Map.Entry<String, String> entry : map.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        // 渲染表格为字符串
        return RenderUtil.render(table, width);
    }

    /**
     * 渲染变更结果对象
     * 将字段变更前后的值渲染为表格
     *
     * @param result 变更结果值对象，包含字段名、变更前值和变更后值
     * @return 渲染后的表格元素
     */
    public static TableElement renderChangeResult(ChangeResultVO result) {
        // 创建表格，设置左右内边距
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表头：字段名、变更前值、变更后值，使用粗体样式
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("BEFORE-VALUE").style(Decoration.bold.bold()),
                label("AFTER-VALUE").style(Decoration.bold.bold()));
        // 添加数据行
        table.row(result.getName(), StringUtils.objectToString(result.getBeforeValue()),
                StringUtils.objectToString(result.getAfterValue()));
        return table;
    }

    /**
     * 渲染增强器影响结果
     * 将类增强的影响信息（如类dump文件、增强的方法、耗时等）格式化为字符串
     *
     * @param affectVO 增强器影响值对象，包含增强操作的影响信息
     * @return 格式化后的影响信息字符串
     */
    public static String renderEnhancerAffect(EnhancerAffectVO affectVO) {
        final StringBuilder infoSB = new StringBuilder();
        // 获取并处理类dump文件列表
        List<String> classDumpFiles = affectVO.getClassDumpFiles();
        if (classDumpFiles != null) {
            for (String classDumpFile : classDumpFiles) {
                infoSB.append("[dump: ").append(classDumpFile).append("]\n");
            }
        }

        // 获取并处理受影响的方法列表
        List<String> methods = affectVO.getMethods();
        if (methods != null) {
            for (String method : methods) {
                infoSB.append("[Affect method: ").append(method).append("]\n");
            }
        }

        // 添加汇总信息：影响的类数量、方法数量、耗时、监听器ID
        infoSB.append(format("Affect(class count: %d , method count: %d) cost in %s ms, listenerId: %d",
                affectVO.getClassCount(),
                affectVO.getMethodCount(),
                affectVO.getCost(),
                affectVO.getListenerId()));
        // 如果超过限制，添加提示信息
        if (!StringUtils.isEmpty(affectVO.getOverLimitMsg())) {
            infoSB.append("\n" + affectVO.getOverLimitMsg());
        }
        // 如果有异常，添加异常信息
        if (affectVO.getThrowable() != null) {
            infoSB.append("\nEnhance error! exception: ").append(affectVO.getThrowable());
        }
        infoSB.append("\n");

        return infoSB.toString();
    }

    /**
     * 绘制线程信息列表
     * 将线程列表渲染为格式化的表格，包含线程的详细信息
     *
     * @param threads 线程信息对象列表
     * @param width 表格宽度
     * @param height 表格高度（行数限制）
     * @return 渲染后的线程信息表格字符串
     */
    public static String drawThreadInfo(List<ThreadVO> threads, int width, int height) {
        // 创建表格，设置列宽和溢出处理
        TableElement table = new TableElement(1, 6, 3, 2, 2, 2, 2, 2, 2, 2).overflow(Overflow.HIDDEN).rightCellPadding(1);

        // 添加表头，使用粗体样式，黑色前景色和白色背景色
        table.add(
                new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add(
                        "ID",          // 线程ID
                        "NAME",        // 线程名称
                        "GROUP",       // 线程组
                        "PRIORITY",    // 优先级
                        "STATE",       // 状态
                        "%CPU",        // CPU使用率
                        "DELTA_TIME",  // 增量时间
                        "TIME",        // 运行时间
                        "INTERRUPTED", // 是否中断
                        "DAEMON"       // 是否守护线程
                )
        );

        int count = 0;
        // 遍历线程列表，为每个线程添加一行
        for (ThreadVO thread : threads) {
            // 根据线程状态获取对应颜色
            Color color = colorMapping.get(thread.getState());
            // 格式化时间和增量时间
            String time = formatTimeMills(thread.getTime());
            String deltaTime = formatTimeMillsToSeconds(thread.getDeltaTime());
            double cpu = thread.getCpu();

            // 创建守护线程标签，非守护线程使用洋红色标记
            LabelElement daemonLabel = new LabelElement(thread.isDaemon());
            if (!thread.isDaemon()) {
                daemonLabel.setStyle(Style.style(Color.magenta));
            }
            // 创建线程状态元素，使用对应颜色
            LabelElement stateElement;
            if (thread.getState() != null) {
                stateElement = new LabelElement(thread.getState()).style(color.fg());
            } else {
                stateElement = new LabelElement("-");
            }
            // 添加线程信息行
            table.row(
                    new LabelElement(thread.getId()),
                    new LabelElement(thread.getName()),
                    new LabelElement(thread.getGroup() != null ? thread.getGroup() : "-"),
                    new LabelElement(thread.getPriority()),
                    stateElement,
                    new LabelElement(cpu),
                    new LabelElement(deltaTime),
                    new LabelElement(time),
                    new LabelElement(thread.isInterrupted()),
                    daemonLabel
            );
            // 限制显示的行数，避免超出高度限制
            if (++count >= height) {
                break;
            }
        }
        return RenderUtil.render(table, width, height);
    }

    /**
     * 格式化时间（毫秒）为 分钟:秒.毫秒 格式
     * 例如: 1:23.456 表示 1分23秒456毫秒
     *
     * @param timeMills 时间毫秒数
     * @return 格式化后的时间字符串
     */
    private static String formatTimeMills(long timeMills) {
        long seconds = timeMills / 1000;
        long mills = timeMills % 1000;
        long min = seconds / 60;
        seconds = seconds % 60;

        // 拼接格式化的时间字符串，保持毫秒部分为3位数字
        String str;
        if (mills >= 100) {
            str = min + ":" + seconds + "." + mills;
        } else if (mills >= 10) {
            str = min + ":" + seconds + ".0" + mills;
        } else {
            str = min + ":" + seconds + ".00" + mills;
        }
        return str;
    }

    /**
     * 格式化时间（毫秒）为 秒.毫秒 格式
     * 例如: 12.345 表示 12秒345毫秒
     *
     * @param timeMills 时间毫秒数
     * @return 格式化后的时间字符串
     */
    private static String formatTimeMillsToSeconds(long timeMills) {
        long seconds = timeMills / 1000;
        long mills = timeMills % 1000;

        // 拼接格式化的时间字符串，保持毫秒部分为3位数字
        String str;
        if (mills >= 100) {
            str = seconds + "." + mills;
        } else if (mills >= 10) {
            str = seconds + ".0" + mills;
        } else {
            str = seconds + ".00" + mills;
        }
        return str;
    }
}
