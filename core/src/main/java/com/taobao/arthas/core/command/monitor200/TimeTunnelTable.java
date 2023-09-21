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

    public static TableElement createDefaultTable() {
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

    // 填充表格行
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

    public static void drawThrowException(TableElement table, TimeFragmentVO tf) {
        if (tf.isThrow()) {
            //noinspection ThrowableResultOfMethodCallIgnored
            ObjectVO throwableVO = tf.getThrowExp();
            if (throwableVO.needExpand()) {
                table.row("THROW-EXCEPTION", new ObjectView(throwableVO).draw());
            } else {
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

    public static void drawReturnObj(TableElement table, TimeFragmentVO tf, Integer sizeLimit) {
        if (tf.isReturn()) {
            if (tf.getReturnObj().needExpand()) {
                table.row("RETURN-OBJ", new ObjectView(sizeLimit, tf.getReturnObj()).draw());
            } else {
                table.row("RETURN-OBJ", "" + StringUtils.objectToString(tf.getReturnObj()));
            }
        }
    }

    public static void drawParameters(TableElement table, ObjectVO[] params) {
        if (params != null) {
            int paramIndex = 0;
            for (ObjectVO param : params) {
                if (param.needExpand()) {
                    table.row("PARAMETERS[" + paramIndex++ + "]", new ObjectView(param).draw());
                } else {
                    table.row("PARAMETERS[" + paramIndex++ + "]", "" + StringUtils.objectToString(param));
                }
            }
        }
    }

    public static void drawWatchTableHeader(TableElement table) {
        table.row(true, label("INDEX").style(Decoration.bold.bold()), label("SEARCH-RESULT")
                .style(Decoration.bold.bold()));
    }

    public static void drawWatchResults(TableElement table, Map<Integer, ObjectVO> watchResults, Integer sizeLimit) {
        for (Map.Entry<Integer, ObjectVO> entry : watchResults.entrySet()) {
            ObjectVO objectVO = entry.getValue();
            table.row("" + entry.getKey(), "" +
                    (objectVO.needExpand() ? new ObjectView(sizeLimit, objectVO).draw() : StringUtils.objectToString(objectVO.getObject())));
        }
    }

    public static TableElement drawPlayHeader(String className, String methodName, String objectAddress, int index,
                                       TableElement table) {
        return table.row("RE-INDEX", "" + index)
                .row("GMT-REPLAY", DateUtils.formatDateTime(LocalDateTime.now()))
                .row("OBJECT", objectAddress)
                .row("CLASS", className)
                .row("METHOD", methodName);
    }

    public static void drawPlayResult(TableElement table, ObjectVO returnObjVO,
                               int sizeLimit, double cost) {
        // 执行成功:输出成功状态
        table.row("IS-RETURN", "" + true);
        table.row("IS-EXCEPTION", "" + false);
        table.row("COST(ms)", "" + cost);

        // 执行成功:输出成功结果
        if (returnObjVO.needExpand()) {
            table.row("RETURN-OBJ", new ObjectView(sizeLimit, returnObjVO).draw());
        } else {
            table.row("RETURN-OBJ", "" + StringUtils.objectToString(returnObjVO.getObject()));
        }
    }

    public static void drawPlayException(TableElement table, ObjectVO throwableVO) {
        // 执行失败:输出失败状态
        table.row("IS-RETURN", "" + false);
        table.row("IS-EXCEPTION", "" + true);

        // 执行失败:输出失败异常信息
        Throwable cause;
        Throwable t = (Throwable) throwableVO.getObject();
        if (t instanceof InvocationTargetException) {
            cause = t.getCause();
        } else {
            cause = t;
        }

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
