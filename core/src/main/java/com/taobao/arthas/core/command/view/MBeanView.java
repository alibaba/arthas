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
 * View of 'mbean' command
 *
 * @author gongdewei 2020/4/26
 */
public class MBeanView extends ResultView<MBeanModel> {
    @Override
    public void draw(CommandProcess process, MBeanModel result) {
        if (result.getMbeanNames() != null) {
            drawMBeanNames(process, result.getMbeanNames());
        } else if (result.getMbeanMetadata() != null) {
            drawMBeanMetadata(process, result.getMbeanMetadata());
        } else if (result.getMbeanAttribute() != null) {
            drawMBeanAttributes(process, result.getMbeanAttribute());
        }
    }

    private void drawMBeanAttributes(CommandProcess process, Map<String, List<MBeanAttributeVO>> mbeanAttributeMap) {
        for (Map.Entry<String, List<MBeanAttributeVO>> entry : mbeanAttributeMap.entrySet()) {
            String objectName = entry.getKey();
            List<MBeanAttributeVO> attributeVOList = entry.getValue();

            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            table.row(true, "OBJECT_NAME", objectName);
            table.row(true, label("NAME").style(Decoration.bold.bold()),
                    label("VALUE").style(Decoration.bold.bold()));

            for (MBeanAttributeVO attributeVO : attributeVOList) {
                String attributeName = attributeVO.getName();
                String valueStr;
                if (attributeVO.getError() != null) {
                    valueStr = RenderUtil.render(new LabelElement(attributeVO.getError()).style(Decoration.bold_off.fg(Color.red)));
                } else {
                    //convert array to list
                    // TODO support all array type
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
                    //to string
                    valueStr = String.valueOf(value);
                }
                table.row(attributeName, valueStr);
            }
            process.write(RenderUtil.render(table, process.width()));
            process.write("\n");
        }
    }

    private List<Long> convertArrayToList(long[] longs) {
        List<Long> list = new ArrayList<Long>();
        for (long aLong : longs) {
            list.add(aLong);
        }
        return list;
    }

    private List<Integer> convertArrayToList(int[] ints) {
        List<Integer> list = new ArrayList<Integer>();
        for (int anInt : ints) {
            list.add(anInt);
        }
        return list;
    }

    private void drawMBeanMetadata(CommandProcess process, Map<String, MBeanInfo> mbeanMetadata) {
        TableElement table = createTable();
        for (Map.Entry<String, MBeanInfo> entry : mbeanMetadata.entrySet()) {
            String objectName = entry.getKey();
            MBeanInfo mBeanInfo = entry.getValue();
            drawMetaInfo(mBeanInfo, objectName, table);
            drawAttributeInfo(mBeanInfo.getAttributes(), table);
            drawOperationInfo(mBeanInfo.getOperations(), table);
            drawNotificationInfo(mBeanInfo.getNotifications(), table);
        }
        process.write(RenderUtil.render(table, process.width()));

    }

    private void drawMBeanNames(CommandProcess process, List<String> mbeanNames) {
        for (String mbeanName : mbeanNames) {
            process.write(mbeanName).write("\n");
        }
    }

    private static TableElement createTable() {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()));
        return table;
    }

    private void drawMetaInfo(MBeanInfo mBeanInfo, String objectName, TableElement table) {
        table.row(new LabelElement("MBeanInfo").style(Decoration.bold.fg(Color.red)));
        table.row(new LabelElement("Info:").style(Decoration.bold.fg(Color.yellow)));
        table.row("ObjectName", objectName);
        table.row("ClassName", mBeanInfo.getClassName());
        table.row("Description", mBeanInfo.getDescription());
        drawDescriptorInfo("Info Descriptor:", mBeanInfo, table);
        MBeanConstructorInfo[] constructors = mBeanInfo.getConstructors();
        if (constructors.length > 0) {
            for (int i = 0; i < constructors.length; i++) {
                table.row(new LabelElement("Constructor-" + i).style(Decoration.bold.fg(Color.yellow)));
                table.row("Name", constructors[i].getName());
                table.row("Description", constructors[i].getDescription());
            }
        }
    }

    private void drawAttributeInfo(MBeanAttributeInfo[] attributes, TableElement table) {
        for (MBeanAttributeInfo attribute : attributes) {
            table.row(new LabelElement("MBeanAttributeInfo").style(Decoration.bold.fg(Color.red)));
            table.row(new LabelElement("Attribute:").style(Decoration.bold.fg(Color.yellow)));
            table.row("Name", attribute.getName());
            table.row("Description", attribute.getDescription());
            table.row("Readable", String.valueOf(attribute.isReadable()));
            table.row("Writable", String.valueOf(attribute.isWritable()));
            table.row("Is", String.valueOf(attribute.isIs()));
            table.row("Type", attribute.getType());
            drawDescriptorInfo("Attribute Descriptor:", attribute, table);
        }
    }

    private void drawOperationInfo(MBeanOperationInfo[] operations, TableElement table) {
        for (MBeanOperationInfo operation : operations) {
            table.row(new LabelElement("MBeanOperationInfo").style(Decoration.bold.fg(Color.red)));
            table.row(new LabelElement("Operation:").style(Decoration.bold.fg(Color.yellow)));
            table.row("Name", operation.getName());
            table.row("Description", operation.getDescription());
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
            table.row("Impact", impact);
            table.row("ReturnType", operation.getReturnType());
            MBeanParameterInfo[] signature = operation.getSignature();
            if (signature.length > 0) {
                for (int i = 0; i < signature.length; i++) {
                    table.row(new LabelElement("Parameter-" + i).style(Decoration.bold.fg(Color.yellow)));
                    table.row("Name", signature[i].getName());
                    table.row("Type", signature[i].getType());
                    table.row("Description", signature[i].getDescription());
                }
            }
            drawDescriptorInfo("Operation Descriptor:", operation, table);
        }
    }

    private void drawNotificationInfo(MBeanNotificationInfo[] notificationInfos, TableElement table) {
        for (MBeanNotificationInfo notificationInfo : notificationInfos) {
            table.row(new LabelElement("MBeanNotificationInfo").style(Decoration.bold.fg(Color.red)));
            table.row(new LabelElement("Notification:").style(Decoration.bold.fg(Color.yellow)));
            table.row("Name", notificationInfo.getName());
            table.row("Description", notificationInfo.getDescription());
            table.row("NotifTypes", Arrays.toString(notificationInfo.getNotifTypes()));
            drawDescriptorInfo("Notification Descriptor:", notificationInfo, table);
        }
    }

    private void drawDescriptorInfo(String title, DescriptorRead descriptorRead, TableElement table) {
        Descriptor descriptor = descriptorRead.getDescriptor();
        String[] fieldNames = descriptor.getFieldNames();
        if (fieldNames.length > 0) {
            table.row(new LabelElement(title).style(Decoration.bold.fg(Color.yellow)));
            for (String fieldName : fieldNames) {
                Object fieldValue = descriptor.getFieldValue(fieldName);
                table.row(fieldName, fieldValue == null ? "" : fieldValue.toString());
            }
        }
    }

}
