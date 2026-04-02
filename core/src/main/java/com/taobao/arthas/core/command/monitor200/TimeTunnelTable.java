package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.TimeFragmentVO;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.taobao.text.ui.Element.label;

/**
 * 时间隧道表格工具类
 * 用于构建和绘制方法调用时间线的表格展示
 *
 * @author beiwei30 on 30/11/2016.
 */
public class TimeTunnelTable {
    /**
     * 表格各列宽度配置
     * 分别对应：索引、时间戳、耗时、是否返回、是否异常、对象地址、类名、方法名
     */
    private static final int[] TABLE_COL_WIDTH = new int[]{
            8, // index - 调用索引
            20, // timestamp - 时间戳
            10, // cost(ms) - 耗时（毫秒）
            8, // isRet - 是否返回
            8, // isExp - 是否异常
            15, // object address - 对象地址
            30, // class - 类名
            30, // method - 方法名
    };

    /**
     * 表格各列标题名称
     */
    private static final String[] TABLE_COL_TITLE = new String[]{
            "INDEX",
            "TIMESTAMP",
            "COST(ms)",
            "IS-RET",
            "IS-EXP",
            "OBJECT",
            "CLASS",
            "METHOD"

    };

    /**
     * 创建固定列宽的表格元素
     * 使用预定义的列宽配置
     *
     * @return 表格元素
     */
    static TableElement createTable() {
        return new TableElement(TABLE_COL_WIDTH).leftCellPadding(1).rightCellPadding(1);
    }

    /**
     * 创建默认表格元素
     * 使用自适应列宽
     *
     * @return 表格元素
     */
    public static TableElement createDefaultTable() {
        return new TableElement().leftCellPadding(1).rightCellPadding(1);
    }

    /**
     * 填充表格标题行
     * 将列标题添加到表格中，并使用粗体样式
     *
     * @param table 表格元素
     * @return 填充了标题的表格元素
     */
    static TableElement fillTableHeader(TableElement table) {
        LabelElement[] headers = new LabelElement[TABLE_COL_TITLE.length];
        for (int i = 0; i < TABLE_COL_TITLE.length; ++i) {
            headers[i] = label(TABLE_COL_TITLE[i]).style(Decoration.bold.bold());
        }
        table.row(true, headers);
        return table;
    }

    /**
     * 绘制时间隧道表格
     * 将时间片段列表以表格形式展示
     *
     * @param timeFragmentList 时间片段列表
     * @param withHeader 是否包含表头
     * @return 表格元素
     */
    public static Element drawTimeTunnelTable(List<TimeFragmentVO> timeFragmentList, boolean withHeader){
        TableElement table = createTable();
        if (withHeader) {
            fillTableHeader(table);
        }
        for (TimeFragmentVO tf : timeFragmentList) {
            fillTableRow(table, tf);
        }
        return table;
    }

    /**
     * 填充表格数据行
     * 将单个时间片段的信息填充到表格的一行中
     *
     * @param table 表格元素
     * @param tf 时间片段值对象
     * @return 填充了数据的表格元素
     */
    static TableElement fillTableRow(TableElement table, TimeFragmentVO tf) {
        return table.row(
                "" + tf.getIndex(),
                DateUtils.formatDateTime(tf.getTimestamp()),
                "" + tf.getCost(),
                "" + tf.isReturn(),
                "" + tf.isThrow(),
                tf.getObject(),
                StringUtils.substringAfterLast("." + tf.getClassName(), "."),
                tf.getMethodName()
        );
    }

    /**
     * 绘制时间隧道详细信息
     * 以键值对形式展示时间片段的完整信息
     *
     * @param table 表格元素
     * @param tf 时间片段值对象
     */
    public static void drawTimeTunnel(TableElement table, TimeFragmentVO tf) {
        table.row("INDEX", "" + tf.getIndex())
                .row("GMT-CREATE", DateUtils.formatDateTime(tf.getTimestamp()))
                .row("COST(ms)", "" + tf.getCost())
                .row("OBJECT", tf.getObject())
                .row("CLASS", tf.getClassName())
                .row("METHOD", tf.getMethodName())
                .row("IS-RETURN", "" + tf.isReturn())
                .row("IS-EXCEPTION", "" + tf.isThrow());
    }

    /**
     * 绘制异常信息
     * 如果时间片段包含异常，则展示异常的堆栈信息
     *
     * @param table 表格元素
     * @param tf 时间片段值对象
     */
    public static void drawThrowException(TableElement table, TimeFragmentVO tf) {
        if (tf.isThrow()) {
            //noinspection ThrowableResultOfMethodCallIgnored
            ObjectVO throwableVO = tf.getThrowExp();
            // 如果对象需要展开显示，使用ObjectView进行可视化
            if (throwableVO.needExpand()) {
                table.row("THROW-EXCEPTION", new ObjectView(throwableVO).draw());
            } else {
                // 否则直接打印异常堆栈信息
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                try {
                    ((Throwable) throwableVO.getObject()).printStackTrace(printWriter);
                    table.row("THROW-EXCEPTION", stringWriter.toString());
                } finally {
                    printWriter.close();
                }
            }
        }
    }

    /**
     * 绘制返回对象
     * 如果时间片段包含返回值，则展示返回对象的信息
     *
     * @param table 表格元素
     * @param tf 时间片段值对象
     * @param sizeLimit 展示大小限制
     */
    public static void drawReturnObj(TableElement table, TimeFragmentVO tf, Integer sizeLimit) {
        if (tf.isReturn()) {
            // 如果返回对象需要展开显示，使用ObjectView进行可视化
            if (tf.getReturnObj().needExpand()) {
                table.row("RETURN-OBJ", new ObjectView(sizeLimit, tf.getReturnObj()).draw());
            } else {
                // 否则直接转换为字符串显示
                table.row("RETURN-OBJ", "" + StringUtils.objectToString(tf.getReturnObj()));
            }
        }
    }

    /**
     * 绘制方法参数
     * 展示方法调用的所有参数信息
     *
     * @param table 表格元素
     * @param params 参数对象数组
     */
    public static void drawParameters(TableElement table, ObjectVO[] params) {
        if (params != null) {
            int paramIndex = 0;
            for (ObjectVO param : params) {
                // 如果参数对象需要展开显示，使用ObjectView进行可视化
                if (param.needExpand()) {
                    table.row("PARAMETERS[" + paramIndex++ + "]", new ObjectView(param).draw());
                } else {
                    // 否则直接转换为字符串显示
                    table.row("PARAMETERS[" + paramIndex++ + "]", "" + StringUtils.objectToString(param));
                }
            }
        }
    }

    /**
     * 绘制监视表格的表头
     * 用于watch命令的结果展示
     *
     * @param table 表格元素
     */
    public static void drawWatchTableHeader(TableElement table) {
        table.row(true, label("INDEX").style(Decoration.bold.bold()), label("SEARCH-RESULT")
                .style(Decoration.bold.bold()));
    }

    /**
     * 绘制监视结果
     * 展示watch命令观察到的对象变化
     *
     * @param table 表格元素
     * @param watchResults 监视结果映射（索引 -> 对象值对象）
     * @param sizeLimit 展示大小限制
     */
    public static void drawWatchResults(TableElement table, Map<Integer, ObjectVO> watchResults, Integer sizeLimit) {
        for (Map.Entry<Integer, ObjectVO> entry : watchResults.entrySet()) {
            ObjectVO objectVO = entry.getValue();
            // 根据对象是否需要展开显示，选择不同的展示方式
            table.row("" + entry.getKey(), "" +
                    (objectVO.needExpand() ? new ObjectView(sizeLimit, objectVO).draw() : StringUtils.objectToString(objectVO.getObject())));
        }
    }

    /**
     * 绘制重放信息的表头
     * 用于tt命令的时间隧道重放功能
     *
     * @param className 类名
     * @param methodName 方法名
     * @param objectAddress 对象地址
     * @param index 索引
     * @param table 表格元素
     * @return 填充了重放表头的表格元素
     */
    public static TableElement drawPlayHeader(String className, String methodName, String objectAddress, int index,
                                       TableElement table) {
        return table.row("RE-INDEX", "" + index)
                .row("GMT-REPLAY", DateUtils.formatDateTime(LocalDateTime.now()))
                .row("OBJECT", objectAddress)
                .row("CLASS", className)
                .row("METHOD", methodName);
    }

    /**
     * 绘制重放成功的结果
     * 展示方法重放执行的返回值
     *
     * @param table 表格元素
     * @param returnObjVO 返回对象值对象
     * @param sizeLimit 展示大小限制
     * @param cost 执行耗时（毫秒）
     */
    public static void drawPlayResult(TableElement table, ObjectVO returnObjVO,
                               int sizeLimit, double cost) {
        // 输出执行成功的状态信息
        table.row("IS-RETURN", "" + true);
        table.row("IS-EXCEPTION", "" + false);
        table.row("COST(ms)", "" + cost);

        // 输出执行成功的返回结果
        if (returnObjVO.needExpand()) {
            table.row("RETURN-OBJ", new ObjectView(sizeLimit, returnObjVO).draw());
        } else {
            table.row("RETURN-OBJ", "" + StringUtils.objectToString(returnObjVO.getObject()));
        }
    }

    /**
     * 绘制重放失败的异常信息
     * 展示方法重放执行时抛出的异常
     *
     * @param table 表格元素
     * @param throwableVO 异常对象值对象
     */
    public static void drawPlayException(TableElement table, ObjectVO throwableVO) {
        // 输出执行失败的状态信息
        table.row("IS-RETURN", "" + false);
        table.row("IS-EXCEPTION", "" + true);

        // 提取异常的根本原因
        // 如果是InvocationTargetException，需要获取其cause
        Throwable cause;
        Throwable t = (Throwable) throwableVO.getObject();
        if (t instanceof InvocationTargetException) {
            cause = t.getCause();
        } else {
            cause = t;
        }

        // 输出执行失败的异常堆栈信息
        if (throwableVO.needExpand()) {
            table.row("THROW-EXCEPTION", new ObjectView(cause, throwableVO.expandOrDefault()).draw());
        } else {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            try {
                cause.printStackTrace(printWriter);
                table.row("THROW-EXCEPTION", stringWriter.toString());
            } finally {
                printWriter.close();
            }
        }
    }

}
