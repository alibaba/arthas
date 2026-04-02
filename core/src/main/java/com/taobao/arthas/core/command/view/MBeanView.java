package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MBeanAttributeVO;
import com.taobao.arthas.core.command.model.MBeanModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import javax.management.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.taobao.text.ui.Element.label;
import static javax.management.MBeanOperationInfo.*;

/**
 * mbean命令的视图类
 * 负责将JMX MBean的信息渲染为表格形式展示给用户
 *
 * @author gongdewei 2020/4/26
 */
public class MBeanView extends ResultView<MBeanModel> {
    /**
     * 绘制mbean命令的执行结果
     * 根据结果类型分发到不同的渲染方法
     *
     * @param process 命令处理进程，用于输出结果
     * @param result mbean命令的执行结果模型
     */
    @Override
    public void draw(CommandProcess process, MBeanModel result) {
        // 如果结果包含MBean名称列表，则绘制名称列表
        if (result.getMbeanNames() != null) {
            drawMBeanNames(process, result.getMbeanNames());
        } else if (result.getMbeanMetadata() != null) {
            // 如果结果包含MBean元数据，则绘制元数据信息
            drawMBeanMetadata(process, result.getMbeanMetadata());
        } else if (result.getMbeanAttribute() != null) {
            // 如果结果包含MBean属性值，则绘制属性信息
            drawMBeanAttributes(process, result.getMbeanAttribute());
        }
    }

    /**
     * 绘制MBean属性信息
     * 将每个MBean的属性名和值以表格形式展示
     *
     * @param process 命令处理进程，用于输出结果
     * @param mbeanAttributeMap MBean属性映射表，key为MBean对象名称，value为属性列表
     */
    private void drawMBeanAttributes(CommandProcess process, Map<String, List<MBeanAttributeVO>> mbeanAttributeMap) {
        // 遍历每个MBean
        for (Map.Entry<String, List<MBeanAttributeVO>> entry : mbeanAttributeMap.entrySet()) {
            String objectName = entry.getKey();
            List<MBeanAttributeVO> attributeVOList = entry.getValue();

            // 创建表格，设置左右内边距
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            // 添加MBean对象名称行，跨两列显示
            table.row(true, "OBJECT_NAME", objectName);
            // 添加表头：名称和值
            table.row(true, label("NAME").style(Decoration.bold.bold()),
                    label("VALUE").style(Decoration.bold.bold()));

            // 遍历并添加每个属性
            for (MBeanAttributeVO attributeVO : attributeVOList) {
                String attributeName = attributeVO.getName();
                String valueStr;
                // 如果属性值获取时出错，则显示红色错误信息
                if (attributeVO.getError() != null) {
                    valueStr = RenderUtil.render(new LabelElement(attributeVO.getError()).style(Decoration.bold_off.fg(Color.red)));
                } else {
                    // 将数组类型转换为列表形式，便于显示
                    // TODO 支持所有数组类型
                    Object value = attributeVO.getValue();
                    if (value instanceof String[]) {
                        value = Arrays.asList((String[]) value);
                    } else if (value instanceof Integer[]) {
                        value = Arrays.asList((Integer[]) value);
                    } else if (value instanceof Long[]) {
                        value = Arrays.asList((Long[]) value);
                    } else if (value instanceof int[]) {
                        value = convertArrayToList((int[]) value);
                    } else if (value instanceof long[]) {
                        value = convertArrayToList((long[]) value);
                    }
                    // 转换为字符串
                    valueStr = String.valueOf(value);
                }
                // 添加属性行
                table.row(attributeName, valueStr);
            }
            // 渲染并输出表格
            process.write(RenderUtil.render(table, process.width()));
            process.write("\n");
        }
    }

    /**
     * 将long数组转换为Long列表
     *
     * @param longs long类型的数组
     * @return 转换后的Long列表
     */
    private List<Long> convertArrayToList(long[] longs) {
        List<Long> list = new ArrayList<Long>();
        for (long aLong : longs) {
            list.add(aLong);
        }
        return list;
    }

    /**
     * 将int数组转换为Integer列表
     *
     * @param ints int类型的数组
     * @return 转换后的Integer列表
     */
    private List<Integer> convertArrayToList(int[] ints) {
        List<Integer> list = new ArrayList<Integer>();
        for (int anInt : ints) {
            list.add(anInt);
        }
        return list;
    }

    /**
     * 绘制MBean元数据信息
     * 包含MBean的基本信息、属性、操作和通知等
     *
     * @param process 命令处理进程，用于输出结果
     * @param mbeanMetadata MBean元数据映射表，key为MBean对象名称，value为MBeanInfo对象
     */
    private void drawMBeanMetadata(CommandProcess process, Map<String, MBeanInfo> mbeanMetadata) {
        // 创建表格
        TableElement table = createTable();
        // 遍历每个MBean的元数据
        for (Map.Entry<String, MBeanInfo> entry : mbeanMetadata.entrySet()) {
            String objectName = entry.getKey();
            MBeanInfo mBeanInfo = entry.getValue();
            // 绘制基本信息
            drawMetaInfo(mBeanInfo, objectName, table);
            // 绘制属性信息
            drawAttributeInfo(mBeanInfo.getAttributes(), table);
            // 绘制操作信息
            drawOperationInfo(mBeanInfo.getOperations(), table);
            // 绘制通知信息
            drawNotificationInfo(mBeanInfo.getNotifications(), table);
        }
        // 渲染并输出表格
        process.write(RenderUtil.render(table, process.width()));

    }

    /**
     * 绘制MBean名称列表
     * 将所有MBean名称逐行输出
     *
     * @param process 命令处理进程，用于输出结果
     * @param mbeanNames MBean名称列表
     */
    private void drawMBeanNames(CommandProcess process, List<String> mbeanNames) {
        for (String mbeanName : mbeanNames) {
            process.write(mbeanName).write("\n");
        }
    }

    /**
     * 创建元数据表格
     * 表格包含两列：名称和值
     *
     * @return 创建的表格元素
     */
    private static TableElement createTable() {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表头行
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()));
        return table;
    }

    /**
     * 绘制MBean的基本元数据信息
     * 包括类名、描述、构造函数等
     *
     * @param mBeanInfo MBean信息对象
     * @param objectName MBean对象名称
     * @param table 表格元素，用于添加数据行
     */
    private void drawMetaInfo(MBeanInfo mBeanInfo, String objectName, TableElement table) {
        // 添加MBeanInfo标题，红色加粗显示
        table.row(new LabelElement("MBeanInfo").style(Decoration.bold.fg(Color.red)));
        // 添加Info标签，黄色加粗显示
        table.row(new LabelElement("Info:").style(Decoration.bold.fg(Color.yellow)));
        // 添加对象名称、类名和描述信息
        table.row("ObjectName", objectName);
        table.row("ClassName", mBeanInfo.getClassName());
        table.row("Description", mBeanInfo.getDescription());
        // 绘制描述符信息
        drawDescriptorInfo("Info Descriptor:", mBeanInfo, table);
        // 获取构造函数数组
        MBeanConstructorInfo[] constructors = mBeanInfo.getConstructors();
        // 如果存在构造函数，则逐个添加
        if (constructors.length > 0) {
            for (int i = 0; i < constructors.length; i++) {
                table.row(new LabelElement("Constructor-" + i).style(Decoration.bold.fg(Color.yellow)));
                table.row("Name", constructors[i].getName());
                table.row("Description", constructors[i].getDescription());
            }
        }
    }

    /**
     * 绘制MBean属性信息
     * 包括属性名、描述、可读性、可写性、类型等
     *
     * @param attributes MBean属性信息数组
     * @param table 表格元素，用于添加数据行
     */
    private void drawAttributeInfo(MBeanAttributeInfo[] attributes, TableElement table) {
        // 遍历每个属性
        for (MBeanAttributeInfo attribute : attributes) {
            // 添加属性信息标题，红色加粗显示
            table.row(new LabelElement("MBeanAttributeInfo").style(Decoration.bold.fg(Color.red)));
            // 添加Attribute标签，黄色加粗显示
            table.row(new LabelElement("Attribute:").style(Decoration.bold.fg(Color.yellow)));
            // 添加属性名称、描述、可读性、可写性、Is标识和类型
            table.row("Name", attribute.getName());
            table.row("Description", attribute.getDescription());
            table.row("Readable", String.valueOf(attribute.isReadable()));
            table.row("Writable", String.valueOf(attribute.isWritable()));
            table.row("Is", String.valueOf(attribute.isIs()));
            table.row("Type", attribute.getType());
            // 绘制属性的描述符信息
            drawDescriptorInfo("Attribute Descriptor:", attribute, table);
        }
    }

    /**
     * 绘制MBean操作信息
     * 包括操作名、描述、影响类型、返回类型和参数等
     *
     * @param operations MBean操作信息数组
     * @param table 表格元素，用于添加数据行
     */
    private void drawOperationInfo(MBeanOperationInfo[] operations, TableElement table) {
        // 遍历每个操作
        for (MBeanOperationInfo operation : operations) {
            // 添加操作信息标题，红色加粗显示
            table.row(new LabelElement("MBeanOperationInfo").style(Decoration.bold.fg(Color.red)));
            // 添加Operation标签，黄色加粗显示
            table.row(new LabelElement("Operation:").style(Decoration.bold.fg(Color.yellow)));
            // 添加操作名称和描述
            table.row("Name", operation.getName());
            table.row("Description", operation.getDescription());
            // 根据影响类型转换为字符串表示
            String impact = "";
            switch (operation.getImpact()) {
                case ACTION:
                    impact = "action";
                    break;
                case ACTION_INFO:
                    impact = "action/info";
                    break;
                case INFO:
                    impact = "info";
                    break;
                case UNKNOWN:
                    impact = "unknown";
                    break;
            }
            // 添加影响类型和返回类型
            table.row("Impact", impact);
            table.row("ReturnType", operation.getReturnType());
            // 获取操作签名（参数列表）
            MBeanParameterInfo[] signature = operation.getSignature();
            // 如果存在参数，则逐个添加参数信息
            if (signature.length > 0) {
                for (int i = 0; i < signature.length; i++) {
                    table.row(new LabelElement("Parameter-" + i).style(Decoration.bold.fg(Color.yellow)));
                    table.row("Name", signature[i].getName());
                    table.row("Type", signature[i].getType());
                    table.row("Description", signature[i].getDescription());
                }
            }
            // 绘制操作的描述符信息
            drawDescriptorInfo("Operation Descriptor:", operation, table);
        }
    }

    /**
     * 绘制MBean通知信息
     * 包括通知名称、描述和通知类型等
     *
     * @param notificationInfos MBean通知信息数组
     * @param table 表格元素，用于添加数据行
     */
    private void drawNotificationInfo(MBeanNotificationInfo[] notificationInfos, TableElement table) {
        // 遍历每个通知
        for (MBeanNotificationInfo notificationInfo : notificationInfos) {
            // 添加通知信息标题，红色加粗显示
            table.row(new LabelElement("MBeanNotificationInfo").style(Decoration.bold.fg(Color.red)));
            // 添加Notification标签，黄色加粗显示
            table.row(new LabelElement("Notification:").style(Decoration.bold.fg(Color.yellow)));
            // 添加通知名称、描述和通知类型
            table.row("Name", notificationInfo.getName());
            table.row("Description", notificationInfo.getDescription());
            table.row("NotifTypes", Arrays.toString(notificationInfo.getNotifTypes()));
            // 绘制通知的描述符信息
            drawDescriptorInfo("Notification Descriptor:", notificationInfo, table);
        }
    }

    /**
     * 绘制描述符信息
     * 描述符包含了额外的元数据信息
     *
     * @param title 描述符标题
     * @param descriptorRead 描述符读取对象
     * @param table 表格元素，用于添加数据行
     */
    private void drawDescriptorInfo(String title, DescriptorRead descriptorRead, TableElement table) {
        // 获取描述符对象
        Descriptor descriptor = descriptorRead.getDescriptor();
        // 获取所有字段名
        String[] fieldNames = descriptor.getFieldNames();
        // 如果存在字段，则逐个添加
        if (fieldNames.length > 0) {
            // 添加描述符标题，黄色加粗显示
            table.row(new LabelElement(title).style(Decoration.bold.fg(Color.yellow)));
            for (String fieldName : fieldNames) {
                // 获取字段值
                Object fieldValue = descriptor.getFieldValue(fieldName);
                // 添加字段名和值，如果值为null则显示空字符串
                table.row(fieldName, fieldValue == null ? "" : fieldValue.toString());
            }
        }
    }

}
