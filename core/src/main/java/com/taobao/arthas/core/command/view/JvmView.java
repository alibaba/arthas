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
 * JVM 信息命令视图类
 *
 * <p>负责渲染和显示 JVM 的各种运行时信息，包括：</p>
 * <ul>
 *   <li>内存使用情况（堆内存、非堆内存等）</li>
 *   <li>线程信息</li>
 *   <li>类加载信息</li>
 *   <li>GC（垃圾回收）统计信息</li>
 *   <li>运行时参数等</li>
 * </ul>
 *
 * @author gongdewei 2020/4/24
 */
public class JvmView extends ResultView<JvmModel> {

    /**
     * 绘制 JVM 信息视图
     *
     * <p>将 JVM 信息渲染为表格形式，按分组展示各项指标。</p>
     * <p>表格包含两列：项目名称和对应的值。</p>
     *
     * @param process 命令处理进程，用于输出渲染结果
     * @param result JVM 模型数据，包含所有要显示的 JVM 信息
     */
    @Override
    public void draw(CommandProcess process, JvmModel result) {
        // 创建表格元素，2列5行的布局，设置左右内边距为1个字符
        TableElement table = new TableElement(2, 5).leftCellPadding(1).rightCellPadding(1);

        // 遍历 JVM 信息，按分组组织数据
        for (Map.Entry<String, List<JvmItemVO>> entry : result.getJvmInfo().entrySet()) {
            // 获取分组名称（如：MEMORY、THREAD、GC 等）
            String group = entry.getKey();
            // 获取该分组下的所有 JVM 信息项
            List<JvmItemVO> items = entry.getValue();

            // 添加分组标题行（加粗显示）
            table.row(true, label(group).style(Decoration.bold.bold()));

            // 遍历该分组下的每一项
            for (JvmItemVO item : items) {
                String valueStr;
                // 特殊处理：内存使用信息需要特殊渲染格式
                if (item.getValue() instanceof Map && item.getName().endsWith("MEMORY-USAGE")) {
                    valueStr = renderMemoryUsage((Map<String, Object>) item.getValue());
                } else {
                    // 普通值的渲染
                    valueStr = renderItemValue(item.getValue());
                }
                // 如果有描述信息，将其附加到名称后面
                if (item.getDesc() != null) {
                    table.row(item.getName() + "\n[" + item.getDesc() + "]", valueStr);
                } else {
                    table.row(item.getName(), valueStr);
                }
            }
            // 在每个分组后添加空行，增强可读性
            table.row("", "");
        }

        // 将渲染后的表格输出到进程
        process.write(RenderUtil.render(table, process.width()));
    }

    /**
     * 渲染计数/时间格式的值
     *
     * <p>将包含两个元素的数组格式化为 "计数/时间" 的形式。</p>
     * <p>例如：[100, 200] 会渲染为 "100/200"</p>
     *
     * @param value 长整型数组，第一个元素为计数，第二个元素为时间
     * @return 格式化后的字符串，格式为 "count/time"
     */
    private String renderCountTime(long[] value) {
        //count/time
        return value[0] + "/" + value[1];
    }

    /**
     * 渲染 JVM 信息项的值
     *
     * <p>根据值的类型使用不同的渲染策略：</p>
     * <ul>
     *   <li>null 值：返回 "null" 字符串</li>
     *   <li>Collection 集合：每个元素占一行</li>
     *   <li>String[] 数组：每个元素占一行</li>
     *   <li>Map：每个键值对占一行，格式为 "key : value"</li>
     *   <li>其他类型：直接转换为字符串</li>
     * </ul>
     *
     * @param value 要渲染的值对象
     * @return 格式化后的字符串
     */
    private String renderItemValue(Object value) {
        // 处理 null 值
        if (value == null) {
            return "null";
        }
        // 处理集合类型
        if (value instanceof Collection) {
            return renderCollectionValue((Collection) value);
        // 处理字符串数组
        } else if (value instanceof String[]) {
            return renderArrayValue((String[]) value);
        // 处理 Map 类型
        } else if (value instanceof Map) {
            return renderMapValue((Map) value);
        }
        // 其他类型直接转换为字符串
        return String.valueOf(value);
    }

    /**
     * 渲染集合类型的值
     *
     * <p>将集合中的每个字符串元素渲染为单独的一行。</p>
     * <p>如果集合为空，返回 "[]"。</p>
     *
     * @param strings 字符串集合
     * @return 每个元素占一行的字符串
     */
    private String renderCollectionValue(Collection<String> strings) {
        final StringBuilder colSB = new StringBuilder();
        // 处理空集合的情况
        if (strings.isEmpty()) {
            colSB.append("[]");
        } else {
            // 遍历集合，每个元素占一行
            for (String str : strings) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    /**
     * 渲染字符串数组的值
     *
     * <p>将字符串数组中的每个元素渲染为单独的一行。</p>
     * <p>如果数组为 null 或空数组，返回 "[]"。</p>
     *
     * @param stringArray 字符串数组（可变参数）
     * @return 每个元素占一行的字符串
     */
    private String renderArrayValue(String... stringArray) {
        final StringBuilder colSB = new StringBuilder();
        // 处理 null 或空数组的情况
        if (null == stringArray
                || stringArray.length == 0) {
            colSB.append("[]");
        } else {
            // 遍历数组，每个元素占一行
            for (String str : stringArray) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    /**
     * 渲染 Map 类型的值
     *
     * <p>将 Map 中的每个键值对渲染为单独的一行，格式为 "key : value"。</p>
     * <p>如果 Map 为 null，返回空字符串。</p>
     *
     * @param valueMap 键为字符串、值为对象的 Map
     * @return 每个键值对占一行的字符串
     */
    private String renderMapValue(Map<String, Object> valueMap) {
        final StringBuilder colSB = new StringBuilder();
        // 处理 null Map 的情况
        if (valueMap != null) {
            // 遍历 Map，每个键值对占一行
            for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                colSB.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
            }
        }
        return colSB.toString();
    }

    /**
     * 渲染内存使用信息
     *
     * <p>将内存使用情况渲染为易读的格式，包括：</p>
     * <ul>
     *   <li>init：初始内存大小</li>
     *   <li>used：已使用内存大小</li>
     *   <li>committed：已提交内存大小</li>
     *   <li>max：最大内存大小</li>
     * </ul>
     * <p>每个值都会显示字节数和人类可读的格式（如 KB、MB、GB）。</p>
     *
     * @param valueMap 包含内存使用信息的 Map，键为属性名，值为字节数
     * @return 格式化后的内存使用信息字符串
     */
    private String renderMemoryUsage(Map<String, Object> valueMap) {
        final StringBuilder colSB = new StringBuilder();
        // 定义要显示的内存属性，按特定顺序排列
        String[] keys = new String[]{"init", "used", "committed", "max"};
        // 遍历每个内存属性
        for (String key : keys) {
            // 从 Map 中获取对应的值
            Object value = valueMap.get(key);
            // 格式化内存字节数（转换为人类可读格式）
            String valueStr = value != null ? formatMemoryByte((Long) value) : "";
            // 将属性名和格式化后的值拼接输出
            colSB.append(key).append(" : ").append(valueStr).append("\n");
        }
        return colSB.toString();
    }

    /**
     * 格式化内存字节数
     *
     * <p>将字节数转换为两种格式的组合：</p>
     * <ul>
     *   <li>原始字节数（如：1048576）</li>
     *   <li>人类可读格式（如：1MB）</li>
     * </ul>
     * <p>最终格式为：字节数(人类可读格式)，例如：1048576(1MB)</p>
     *
     * @param bytes 内存字节数
     * @return 格式化后的字符串，包含原始字节数和人类可读格式
     */
    private String formatMemoryByte(long bytes) {
        // 使用 String.format 将字节数和人类可读格式组合输出
        return String.format("%s(%s)", bytes, StringUtils.humanReadableByteCount(bytes));
    }
}
