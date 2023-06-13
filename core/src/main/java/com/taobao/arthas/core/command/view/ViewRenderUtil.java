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
 * view render util for term/tty
 * @author gongdewei 2020/6/22
 */
public class ViewRenderUtil {

    /** Thread State Colors */
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
     * Render key-value table
     * @param map
     * @param width
     * @return
     */
    public static String renderKeyValueTable(Map<String, String> map, int width) {
        TableElement table = new TableElement(1, 4).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("KEY").style(Decoration.bold.bold()), label("VALUE").style(Decoration.bold.bold()));

        for (Map.Entry<String, String> entry : map.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }

        return RenderUtil.render(table, width);
    }

    /**
     * Render change result vo
     * @param result
     * @return
     */
    public static TableElement renderChangeResult(ChangeResultVO result) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("BEFORE-VALUE").style(Decoration.bold.bold()),
                label("AFTER-VALUE").style(Decoration.bold.bold()));
        table.row(result.getName(), StringUtils.objectToString(result.getBeforeValue()),
                StringUtils.objectToString(result.getAfterValue()));
        return table;
    }

    /**
     * Render EnhancerAffectVO
     * @param affectVO
     * @return
     */
    public static String renderEnhancerAffect(EnhancerAffectVO affectVO) {
        final StringBuilder infoSB = new StringBuilder();
        List<String> classDumpFiles = affectVO.getClassDumpFiles();
        if (classDumpFiles != null) {
            for (String classDumpFile : classDumpFiles) {
                infoSB.append("[dump: ").append(classDumpFile).append("]\n");
            }
        }

        List<String> methods = affectVO.getMethods();
        if (methods != null) {
            for (String method : methods) {
                infoSB.append("[Affect method: ").append(method).append("]\n");
            }
        }

        infoSB.append(format("Affect(class count: %d , method count: %d) cost in %s ms, listenerId: %d",
                affectVO.getClassCount(),
                affectVO.getMethodCount(),
                affectVO.getCost(),
                affectVO.getListenerId()));
        if (!StringUtils.isEmpty(affectVO.getOverLimitMsg())) {
            infoSB.append("\n" + affectVO.getOverLimitMsg());
        }
        if (affectVO.getThrowable() != null) {
            infoSB.append("\nEnhance error! exception: ").append(affectVO.getThrowable());
        }
        infoSB.append("\n");

        return infoSB.toString();
    }

    public static String drawThreadInfo(List<ThreadVO> threads, int width, int height) {
        TableElement table = new TableElement(1, 6, 3, 2, 2, 2, 2, 2, 2, 2).overflow(Overflow.HIDDEN).rightCellPadding(1);

        // Header
        table.add(
                new RowElement().style(Decoration.bold.fg(Color.black).bg(Color.white)).add(
                        "ID",
                        "NAME",
                        "GROUP",
                        "PRIORITY",
                        "STATE",
                        "%CPU",
                        "DELTA_TIME",
                        "TIME",
                        "INTERRUPTED",
                        "DAEMON"
                )
        );

        int count = 0;
        for (ThreadVO thread : threads) {
            Color color = colorMapping.get(thread.getState());
            String time = formatTimeMills(thread.getTime());
            String deltaTime = formatTimeMillsToSeconds(thread.getDeltaTime());
            double cpu = thread.getCpu();

            LabelElement daemonLabel = new LabelElement(thread.isDaemon());
            if (!thread.isDaemon()) {
                daemonLabel.setStyle(Style.style(Color.magenta));
            }
            LabelElement stateElement;
            if (thread.getState() != null) {
                stateElement = new LabelElement(thread.getState()).style(color.fg());
            } else {
                stateElement = new LabelElement("-");
            }
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
            if (++count >= height) {
                break;
            }
        }
        return RenderUtil.render(table, width, height);
    }

    private static String formatTimeMills(long timeMills) {
        long seconds = timeMills / 1000;
        long mills = timeMills % 1000;
        long min = seconds / 60;
        seconds = seconds % 60;

        //return String.format("%d:%d.%03d", min, seconds, mills);
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

    private static String formatTimeMillsToSeconds(long timeMills) {
        long seconds = timeMills / 1000;
        long mills = timeMills % 1000;

        //return String.format("%d.%03d", seconds, mills);
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
