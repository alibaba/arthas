package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static com.taobao.text.ui.Element.label;
import static java.lang.Integer.toHexString;

/**
 * @author beiwei30 on 30/11/2016.
 */
public class TimeTunnelTable {
    // 各列宽度
    private static final int[] TABLE_COL_WIDTH = new int[]{
            8, // index
            20, // timestamp
            10, // cost(ms)
            8, // isRet
            8, // isExp
            15, // object address
            30, // class
            30, // method
    };

    // 各列名称
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

    static TableElement createTable() {
        return new TableElement(TABLE_COL_WIDTH).leftCellPadding(1).rightCellPadding(1);
    }

    static TableElement createDefaultTable() {
        return new TableElement().leftCellPadding(1).rightCellPadding(1);
    }

    static TableElement fillTableHeader(TableElement table) {
        LabelElement[] headers = new LabelElement[TABLE_COL_TITLE.length];
        for (int i = 0; i < TABLE_COL_TITLE.length; ++i) {
            headers[i] = label(TABLE_COL_TITLE[i]).style(Decoration.bold.bold());
        }
        table.row(true, headers);
        return table;
    }

    // 绘制TimeTunnel表格
    static Element drawTimeTunnelTable(Map<Integer, TimeFragment> timeTunnelMap) {
        TableElement table = fillTableHeader(createTable());
        for (Map.Entry<Integer, TimeFragment> entry : timeTunnelMap.entrySet()) {
            final int index = entry.getKey();
            final TimeFragment tf = entry.getValue();
            fillTableRow(table, index, tf);
        }
        return table;
    }

    // 填充表格行
    static TableElement fillTableRow(TableElement table, int index, TimeFragment tf) {
        Advice advice = tf.getAdvice();
        return table.row(
                "" + index,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(tf.getGmtCreate()),
                "" + tf.getCost(),
                "" + advice.isAfterReturning(),
                "" + advice.isAfterThrowing(),
                advice.getTarget() == null
                        ? "NULL"
                        : "0x" + toHexString(advice.getTarget().hashCode()),
                StringUtils.substringAfterLast("." + advice.getClazz().getName(), "."),
                advice.getMethod().getName()
        );
    }

    static void drawTimeTunnel(TimeFragment tf, Integer index, TableElement table) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        table.row("INDEX", "" + index)
                .row("GMT-CREATE", sdf.format(tf.getGmtCreate()))
                .row("COST(ms)", "" + tf.getCost());
    }

    static void drawMethod(Advice advice, String className, String methodName, String objectAddress, TableElement table) {
        table.row("OBJECT", objectAddress)
                .row("CLASS", className)
                .row("METHOD", methodName)
                .row("IS-RETURN", "" + advice.isAfterReturning())
                .row("IS-EXCEPTION", "" + advice.isAfterThrowing());
    }

    static void drawThrowException(Advice advice, TableElement table, boolean isNeedExpand, int expandLevel) {
        if (advice.isAfterThrowing()) {
            //noinspection ThrowableResultOfMethodCallIgnored
            Throwable throwable = advice.getThrowExp();
            if (isNeedExpand) {
                table.row("THROW-EXCEPTION", new ObjectView(advice.getThrowExp(), expandLevel).draw());
            } else {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                try {
                    throwable.printStackTrace(printWriter);
                    table.row("THROW-EXCEPTION", stringWriter.toString());
                } finally {
                    printWriter.close();
                }
            }
        }
    }

    static void drawReturnObj(Advice advice, TableElement table, boolean isNeedExpand, int expandLevel, int sizeLimit) {
        // fill the returnObj
        if (advice.isAfterReturning()) {
            if (isNeedExpand) {
                table.row("RETURN-OBJ", new ObjectView(advice.getReturnObj(), expandLevel, sizeLimit).draw());
            } else {
                table.row("RETURN-OBJ", "" + StringUtils.objectToString(advice.getReturnObj()));
            }
        }
    }

    static void drawParameters(Advice advice, TableElement table, boolean isNeedExpand, int expandLevel) {
        // fill the parameters
        if (null != advice.getParams()) {
            int paramIndex = 0;
            for (Object param : advice.getParams()) {
                if (isNeedExpand) {
                    table.row("PARAMETERS[" + paramIndex++ + "]", new ObjectView(param, expandLevel).draw());
                } else {
                    table.row("PARAMETERS[" + paramIndex++ + "]", "" + StringUtils.objectToString(param));
                }
            }
        }
    }

    static void drawWatchTableHeader(TableElement table) {
        table.row(true, label("INDEX").style(Decoration.bold.bold()), label("SEARCH-RESULT")
                .style(Decoration.bold.bold()));
    }

    static void drawWatchExpress(Map<Integer, TimeFragment> matchingTimeSegmentMap, TableElement table,
                                 String watchExpress, boolean isNeedExpand, int expandLevel, int sizeLimit)
            throws ExpressException {
        for (Map.Entry<Integer, TimeFragment> entry : matchingTimeSegmentMap.entrySet()) {
            Object value = ExpressFactory.threadLocalExpress(entry.getValue().getAdvice()).get(watchExpress);
            table.row("" + entry.getKey(), "" +
                    (isNeedExpand ? new ObjectView(value, expandLevel, sizeLimit).draw() : StringUtils.objectToString(value)));
        }
    }

    static TableElement drawPlayHeader(String className, String methodName, String objectAddress, int index,
                                       TableElement table) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return table.row("RE-INDEX", "" + index)
                .row("GMT-REPLAY", sdf.format(new Date()))
                .row("OBJECT", objectAddress)
                .row("CLASS", className)
                .row("METHOD", methodName);
    }

    static void drawPlayResult(TableElement table, Object returnObj, boolean isNeedExpand, int expandLevel,
                               int sizeLimit, double cost) {
        // 执行成功:输出成功状态
        table.row("IS-RETURN", "" + true);
        table.row("IS-EXCEPTION", "" + false);
        table.row("COST(ms)", "" + cost);

        // 执行成功:输出成功结果
        if (isNeedExpand) {
            table.row("RETURN-OBJ", new ObjectView(returnObj, expandLevel, sizeLimit).draw());
        } else {
            table.row("RETURN-OBJ", "" + StringUtils.objectToString(returnObj));
        }
    }

    static void drawPlayException(TableElement table, Throwable t, boolean isNeedExpand, int expandLevel) {
        // 执行失败:输出失败状态
        table.row("IS-RETURN", "" + false);
        table.row("IS-EXCEPTION", "" + true);

        // 执行失败:输出失败异常信息
        Throwable cause;
        if (t instanceof InvocationTargetException) {
            cause = t.getCause();
        } else {
            cause = t;
        }

        if (isNeedExpand) {
            table.row("THROW-EXCEPTION", new ObjectView(cause, expandLevel).draw());
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
