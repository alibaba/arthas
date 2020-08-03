package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.JvmModel;
import com.taobao.arthas.core.command.model.JvmItemVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.taobao.text.ui.Element.label;

/**
 * View of 'jvm' command
 *
 * @author gongdewei 2020/4/24
 */
public class JvmView extends ResultView<JvmModel> {

    @Override
    public void draw(CommandProcess process, JvmModel result) {
        TableElement table = new TableElement(2, 5).leftCellPadding(1).rightCellPadding(1);

        for (Map.Entry<String, List<JvmItemVO>> entry : result.getJvmInfo().entrySet()) {
            String group = entry.getKey();
            List<JvmItemVO> items = entry.getValue();

            table.row(true, label(group).style(Decoration.bold.bold()));
            for (JvmItemVO item : items) {
                String valueStr;
                if (item.getValue() instanceof Map && item.getName().endsWith("MEMORY-USAGE")) {
                    valueStr = renderMemoryUsage((Map<String, Object>) item.getValue());
                } else {
                    valueStr = renderItemValue(item.getValue());
                }
                if (item.getDesc() != null) {
                    table.row(item.getName() + "\n[" + item.getDesc() + "]", valueStr);
                } else {
                    table.row(item.getName(), valueStr);
                }
            }
            table.row("", "");
        }

        process.write(RenderUtil.render(table, process.width()));
    }

    private String renderCountTime(long[] value) {
        //count/time
        return value[0] + "/" + value[1];
    }

    private String renderItemValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Collection) {
            return renderCollectionValue((Collection) value);
        } else if (value instanceof String[]) {
            return renderArrayValue((String[]) value);
        } else if (value instanceof Map) {
            return renderMapValue((Map) value);
        }
        return String.valueOf(value);
    }

    private String renderCollectionValue(Collection<String> strings) {
        final StringBuilder colSB = new StringBuilder();
        if (strings.isEmpty()) {
            colSB.append("[]");
        } else {
            for (String str : strings) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    private String renderArrayValue(String... stringArray) {
        final StringBuilder colSB = new StringBuilder();
        if (null == stringArray
                || stringArray.length == 0) {
            colSB.append("[]");
        } else {
            for (String str : stringArray) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    private String renderMapValue(Map<String, Object> valueMap) {
        final StringBuilder colSB = new StringBuilder();
        if (valueMap != null) {
            for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                colSB.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
            }
        }
        return colSB.toString();
    }

    private String renderMemoryUsage(Map<String, Object> valueMap) {
        final StringBuilder colSB = new StringBuilder();
        String[] keys = new String[]{"init", "used", "committed", "max"};
        for (String key : keys) {
            Object value = valueMap.get(key);
            String valueStr = value != null ? formatMemoryByte((Long) value) : "";
            colSB.append(key).append(" : ").append(valueStr).append("\n");
        }
        return colSB.toString();
    }

    private String formatMemoryByte(long bytes) {
        return String.format("%s(%s)", bytes, StringUtils.humanReadableByteCount(bytes));
    }
}
