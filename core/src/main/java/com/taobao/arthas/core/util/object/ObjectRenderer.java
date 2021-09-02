package com.taobao.arthas.core.util.object;

import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.util.StringUtils;

import java.util.Collection;

/**
 * Render Object VO
 * @author gongdewei 2020/9/27
 */
public abstract class ObjectRenderer {

    private final static String TAB = "    ";

    public static String render(ObjectVO vo) {
        StringBuffer sb = new StringBuffer();
        render(vo, 0, sb);
        if (vo != null && vo.getExceedLimit() != null && vo.getExceedLimit()) {
            sb.append(" Number of objects exceeds limit: ").append(vo.getObjectNumberLimit());
            sb.append(", try to reduce the expand levels by -x expand_value or increase the number of objects displayed by -M size_limit," +
                    " check the help command for more.");
        }
        return sb.toString();
    }

    private static void render(ObjectVO vo, int deep, StringBuffer sb) {
        if (vo == null) {
            sb.append((String) null);
            return;
        }
        if (vo.getKey() != null) {
            renderKeyValueEntry(vo, deep, sb);
            return;
        }

        if (vo.getName() != null) {
            // object field name
            sb.append(vo.getName()).append('=');
        }

        if (vo.getType() != null) {
            //object type
            String type = toSimpleType(vo.getType());
            sb.append('@').append(type).append('[');
        }

        // object value start
        int nextDeep = deep+1;
        if (vo.getFields() != null) {
            // complex object: fields is not null
            renderComplexObject(vo, nextDeep, sb);
        } else {
            //value
            if (vo.getSize() != null) {
                // array or collection
                if (vo.getValue() == null) {
                    //isEmpty=false;size=1
                    sb.append("isEmpty=").append(vo.getSize() == 0 ? "true" : "false").append(";size=").append(vo.getSize());
                } else {
                    renderValue(vo, deep, sb);
                }
            } else {
                //simple object
                renderValue(vo, deep, sb);
            }
        }

        // object value end
        if (vo.getType() != null) {
            if (sb.charAt(sb.length() - 1) == '\n') {
                renderTab(sb, deep);
            }
            sb.append(']');
        }
    }

    private static String toSimpleType(String typeName) {
        int p = typeName.lastIndexOf('.'); // trim package name
        return p >= 0 ? typeName.substring(p+1) : typeName;
    }

    private static void renderComplexObject(ObjectVO vo, int deep, StringBuffer sb) {
        sb.append('\n');
        for (ObjectVO field : vo.getFields()) {
            if (StringUtils.isEmpty(field.getName())) {
                throw new IllegalArgumentException("Complex object's field name is empty: " + sb + "... ");
            }
            renderTab(sb, deep);
            render(field, deep, sb);
            sb.append(",\n");
        }
    }

    private static void renderKeyValueEntry(ObjectVO vo, int deep, StringBuffer sb) {
        //kv entry
        render(vo.getKey(), deep, sb);
        sb.append(" : ");
        render((ObjectVO) vo.getValue(), deep, sb);
    }

    private static StringBuffer renderTab(StringBuffer sb, int deep) {
        for (int i = 0; i < deep; i++) {
            sb.append(TAB);
        }
        return sb;
    }

    private static void renderValue(ObjectVO vo, int deep, StringBuffer sb) {
        Object value = vo.getValue();
        int nextDeep = deep+1;

        if (value instanceof Collection) {
            sb.append('\n');
            Collection collection = (Collection) value;
            for (Object e : collection) {
                renderElement(e, nextDeep, sb);
            }
            renderSize(vo, collection.size(), nextDeep, sb);
        } else if (value instanceof Object[]) {
            sb.append('\n');
            Object[] objs = (Object[]) value;
            for (int i = 0; i < objs.length; i++) {
                renderElement(objs[i], nextDeep, sb);
            }
            renderSize(vo, objs.length, nextDeep, sb);
        } else {
            sb.append(value);
        }
    }

    private static void renderElement(Object obj, int deep, StringBuffer sb) {
        if (obj instanceof ObjectVO) {
            ObjectVO objectVO = (ObjectVO) obj;
            renderTab(sb, deep);
            render(objectVO, deep, sb);
            sb.append(",\n");
        } else {
            renderTab(sb, deep);
            sb.append(obj).append(",\n");
        }
    }

    private static void renderSize(ObjectVO vo, int elemCount, int deep, StringBuffer sb) {
        //如果没有完全显示所有元素，则添加省略提示
        if (vo.getSize() > elemCount) {
            String msg = elemCount + " out of " + vo.getSize() + " displayed, " + (vo.getSize() - elemCount) + " remaining.\n";
            renderTab(sb, deep);
            sb.append(msg);
        }
    }

}
