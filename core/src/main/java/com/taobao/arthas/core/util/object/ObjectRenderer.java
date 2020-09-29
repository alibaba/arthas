package com.taobao.arthas.core.util.object;

import com.taobao.arthas.core.command.model.ObjectVO;

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
            //kv entry
            render(vo.getKey(), deep, sb);
            sb.append(" : ");
            render((ObjectVO) vo.getValue(), deep, sb);
            return;
        }

        if (vo.getName() != null) {
            sb.append(vo.getName()).append('=');
        }
        if (vo.getType() != null) {
            //object
            sb.append('@').append(vo.getType()).append('[');
        }

        int nextDeep = deep+1;
        if (vo.getFields() != null) {
            //fields
            sb.append('\n');
            for (ObjectVO field : vo.getFields()) {
                renderTab(sb, nextDeep);
                render(field, nextDeep, sb);
                sb.append(",\n");
            }
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
        if (vo.getType() != null) {
            if (sb.charAt(sb.length() - 1) == '\n') {
                renderTab(sb, deep);
            }
            sb.append(']');
        }
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
                if (e instanceof ObjectVO) {
                    ObjectVO objectVO = (ObjectVO) e;
                    renderTab(sb, nextDeep);
                    render(objectVO, nextDeep, sb);
                    sb.append(",\n");
                } else {
                    renderTab(sb, nextDeep);
                    sb.append(e).append(",\n");
                }
            }
            //如果没有完全显示所有元素，则添加省略提示
            int count = collection.size();
            if (vo.getSize() > count) {
                String msg = count + " out of " + vo.getSize() + " displayed, " + (vo.getSize() - count) + " remaining.\n";
                renderTab(sb, nextDeep);
                sb.append(msg);
            }
        } else if (value instanceof Object[]) {
            sb.append('\n');
            Object[] objs = (Object[]) value;
            for (int i = 0; i < objs.length; i++) {
                Object obj = objs[i];
                if (obj instanceof ObjectVO) {
                    ObjectVO objectVO = (ObjectVO) obj;
                    renderTab(sb, nextDeep);
                    render(objectVO, nextDeep, sb);
                    sb.append(",\n");
                } else {
                    renderTab(sb, nextDeep);
                    sb.append(obj).append(",\n");
                }
            }
            //如果没有完全显示所有元素，则添加省略提示
            int count = objs.length;
            if (vo.getSize() > count) {
                String msg = count + " out of " + vo.getSize() + " displayed, " + (vo.getSize() - count) + " remaining.\n";
                renderTab(sb, nextDeep).append(msg);
            }
        } else {
            sb.append(value);
        }
    }

}
