package com.taobao.arthas.core.util;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.command.model.ObjectVO;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.taobao.arthas.core.view.ObjectView.ASCII_MAP;

/**
 * Create Object VO
 * @author gongdewei 2020/9/24
 */
public class ObjectVOUtils {

    private static final int ARRAY_LEN_LIMIT = 100;
    private final static String TAB = "    ";

    public static ObjectVO inspectObject(Object object, int expand) {
        return inspectObject(object, 0, expand);
    }

    private static ObjectVO inspectObject(Object obj, int deep, int expand) {
        expand = expand > 4? 4 : expand;
        if (null == obj) {
            return null;
        } else {

            final Class<?> clazz = obj.getClass();
            final String className = clazz.getSimpleName();

            // 7种基础类型,直接输出@类型[值]
            if (Integer.class.isInstance(obj)
                    || Long.class.isInstance(obj)
                    || Float.class.isInstance(obj)
                    || Double.class.isInstance(obj)
                    //                    || Character.class.isInstance(obj)
                    || Short.class.isInstance(obj)
                    || Byte.class.isInstance(obj)
                    || Boolean.class.isInstance(obj)) {
                return new ObjectVO(className, obj);
            }

            // Char要特殊处理,因为有不可见字符的因素
            else if (Character.class.isInstance(obj)) {

                final Character c = (Character) obj;
                return new ObjectVO(className, escapeChar(c));
            }

            // 字符串类型单独处理
            else if (String.class.isInstance(obj)) {
                StringBuffer sb = new StringBuffer();
                for (char c : ((String) obj).toCharArray()) {
                    switch (c) {
                        case '\n':
                            sb.append("\\n");
                            break;
                        case '\r':
                            sb.append("\\r");
                            break;
                        default:
                            sb.append(c);
                    }//switch
                }//for

                return new ObjectVO(className, sb.toString());
            }

            // 集合类输出
            else if (Collection.class.isInstance(obj)) {

                @SuppressWarnings("unchecked") final Collection<Object> collection = (Collection<Object>) obj;

                // 非根节点或空集合只展示摘要信息
                if (!isExpand(deep, expand)
                        || collection.isEmpty()) {

                    return ObjectVO.ofCollection(className, collection.size(), null);
                }

                // 展开展示
                else {
                    List<ObjectVO> list = new ArrayList<ObjectVO>(collection.size());
                    for (Object e : collection) {
                        list.add(inspectObject(e, deep+1, expand));
                    }
                    return ObjectVO.ofCollection(className, collection.size(), list);
                }

            }


            // Map类输出
            else if (Map.class.isInstance(obj)) {
                @SuppressWarnings("unchecked") final Map<Object, Object> map = (Map<Object, Object>) obj;

                // 非根节点或空集合只展示摘要信息
                if (!isExpand(deep, expand)
                        || map.isEmpty()) {

                    return ObjectVO.ofCollection(className, map.size(), null);

                } else {

                    List<ObjectVO> list = new ArrayList<ObjectVO>(map.size());
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        ObjectVO keyObj = inspectObject(entry.getKey(), deep + 1, expand);
                        ObjectVO valueObj = inspectObject(entry.getValue(), deep + 1, expand);
                        list.add(ObjectVO.ofKeyValue(keyObj, valueObj));
                    }
                    return ObjectVO.ofCollection(className, map.size(), list);
                }
            }


            // 数组类输出
            else if (obj.getClass().isArray()) {

                final String typeName = obj.getClass().getSimpleName();

                // int[]
                if (typeName.equals("int[]")) {

                    final int[] arrays = (int[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }
                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }

                }

                //Integer[]
                else if (typeName.equals("Integer[]")) {
                    Integer[] arrays = (Integer[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }
                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // long[]
                else if (typeName.equals("long[]")) {

                    final long[] arrays = (long[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Long[]
                else if ( typeName.equals("Long[]")) {

                    final Long[] arrays = (Long[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // short[]
                else if (typeName.equals("short[]")) {

                    final short[] arrays = (short[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Short[]
                else if (typeName.equals("Short[]")) {

                    final Short[] arrays = (Short[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // float[]
                else if (typeName.equals("float[]")) {

                    final float[] arrays = (float[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Float[]
                else if (typeName.equals("Float[]")) {

                    final Float[] arrays = (Float[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // double[]
                else if (typeName.equals("double[]")) {

                    final double[] arrays = (double[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Double[]
                else if (typeName.equals("Double[]")) {

                    final Double[] arrays = (Double[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // boolean[]
                else if (typeName.equals("boolean[]")) {

                    final boolean[] arrays = (boolean[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Boolean[]
                else if (typeName.equals("Boolean[]")) {

                    final Boolean[] arrays = (Boolean[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // char[]
                else if (typeName.equals("char[]")) {

                    final char[] arrays = (char[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Character[]
                else if (typeName.equals("Character[]")) {

                    final Character[] arrays = (Character[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // byte[]
                else if (typeName.equals("byte[]")) {

                    final byte[] arrays = (byte[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Byte[]
                else if (typeName.equals("Byte[]")) {

                    final Byte[] arrays = (Byte[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, ARRAY_LEN_LIMIT));
                    }
                }

                // Object[]
                else {
                    final Object[] arrays = (Object[]) obj;
                    // 非根节点或空集合只展示摘要信息
                    if (!isExpand(deep, expand)
                            || arrays.length == 0) {

                        return ObjectVO.ofArray(typeName, arrays.length, null);
                    }

                    // 展开展示
                    else {

                        List<ObjectVO> list = new ArrayList<ObjectVO>(arrays.length);
                        for (Object e : arrays) {
                            list.add(inspectObject(e, deep+1, expand));
                            if (list.size() >= ARRAY_LEN_LIMIT) {
                                break;
                            }
                        }
                        //TODO improve list to array
                        return ObjectVO.ofArray(typeName, arrays.length, list.toArray());
                    }
                }

            }

            // Throwable输出
            else if (Throwable.class.isInstance(obj)) {

                if (!isExpand(deep, expand)) {
                    return new ObjectVO(className, obj.toString());
                } else {

                    final Throwable throwable = (Throwable) obj;
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    return new ObjectVO(className, sw.toString());
                }

            }

            // Date输出
            else if (Date.class.isInstance(obj)) {
                String strDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(obj);
                return new ObjectVO(className, strDate);
            }

            else if (obj instanceof Enum<?>) {
                return new ObjectVO(className, obj.toString());
            }

            // 普通Object输出
            else {

                if (!isExpand(deep, expand)) {
                    return new ObjectVO(className, obj.toString());
                } else {
                    List<Field> fields = new ArrayList<Field>();
                    Class objClass = obj.getClass();
                    if (GlobalOptions.printParentFields) {
                        // 当父类为null的时候说明到达了最上层的父类(Object类).
                        while (objClass != null) {
                            for (Field field : objClass.getDeclaredFields()) {
                                fields.add(field);
                            }
                            objClass = objClass.getSuperclass();
                        }
                    } else {
                        for (Field field : objClass.getDeclaredFields()) {
                            fields.add(field);
                        }
                    }

                    if (null != fields) {
                        List<ObjectVO> fieldVOList = new ArrayList<ObjectVO>(fields.size());
                        for (Field field : fields) {

                            field.setAccessible(true);

                            try {

                                final Object value = field.get(obj);

                                ObjectVO fieldObjectVO = inspectObject(value, deep + 1, expand);
                                fieldObjectVO.setName(field.getName());
                                fieldVOList.add(fieldObjectVO);

//                            } catch (ObjectView.ObjectTooLargeException t) {
//                                buf.append("...");
//                                break;
                            } catch (Throwable t) {
                                // ignore
                            }
                        }//for

                        return ObjectVO.ofFields(className, fieldVOList);
                    } else {
                        return null;
                    }

                }

            }
        }
    }

    public static String toString(ObjectVO vo, String prefix) {
        StringBuffer sb = new StringBuffer();
        toString(vo, sb, prefix);
        return sb.toString();
    }

    public static void toString(ObjectVO vo, StringBuffer sb, String prefix) {
        if (vo.getKey() != null) {
            //kv entry
            toString(vo.getKey(), sb, prefix);
            sb.append(" : ");
            toString((ObjectVO) vo.getValue(), sb, prefix);
            return;
        }

        if (vo.getName() != null) {
            sb.append(vo.getName()).append('=');
        }
        if (vo.getType() != null) {
            //object
            sb.append('@').append(vo.getType()).append('[');
        }

        if (vo.getFields() != null) {
            //fields
            String subPrefix = prefix + TAB;
            sb.append('\n');
            sb.append(subPrefix);
            for (ObjectVO field : vo.getFields()) {
                toString(field, sb, subPrefix);
                sb.append(",\n");
                sb.append(subPrefix);
            }
        } else {
            //value
            if (vo.getSize() != null) {
                sb.append("size=").append(vo.getSize()).append(";");
                if (vo.getSize() > 0) {
                    renderValue(vo, sb, prefix);
                }
            } else {
                renderValue(vo, sb, prefix);
            }
        }
        if (vo.getType() != null) {
            if (sb.charAt(sb.length() - 1) == '\n') {
                sb.append(prefix);
            }
            sb.append(']');
        }
    }

    private static void renderValue(ObjectVO vo, StringBuffer sb, String prefix) {
        String subPrefix = prefix+TAB;
        Object value = vo.getValue();

        if (value instanceof Collection) {
            sb.append('\n');
            Collection collection = (Collection) value;
            for (Object e : collection) {
                if (e instanceof ObjectVO) {
                    ObjectVO objectVO = (ObjectVO) e;
                    sb.append(subPrefix);
                    toString(objectVO, sb, subPrefix);
                    sb.append(",\n");
                } else {
                    sb.append(subPrefix).append(e).append(",\n");
                }
            }
            //如果没有完全显示所有元素，则添加省略提示
            int count = collection.size();
            if (vo.getSize() > count) {
                String msg = count + " out of " + vo.getSize() + " displayed, " + (vo.getSize() - count) + " more.\n";
                sb.append(subPrefix).append(msg);
            }
        } else if (value instanceof Object[]) {
            sb.append('\n');
            Object[] objs = (Object[]) value;
            for (int i = 0; i < objs.length; i++) {
                Object obj = objs[i];
                if (obj instanceof ObjectVO) {
                    ObjectVO objectVO = (ObjectVO) obj;
                    sb.append(subPrefix);
                    toString(objectVO, sb, subPrefix);
                    sb.append(",\n");
                } else {
                    sb.append(subPrefix).append(obj).append(",\n");
                }
            }
            //如果没有完全显示所有元素，则添加省略提示
            int count = objs.length;
            if (vo.getSize() > count) {
                String msg = count + " out of " + vo.getSize() + " displayed, " + (vo.getSize() - count) + " more.\n";
                sb.append(subPrefix).append(msg);
            }
        } else {
            if (vo.getSize() == null) {
                sb.append(value);
            }
        }
    }

    private static Object[] toArray(int[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Integer[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(long[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Long[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(short[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Short[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(byte[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Byte[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(char[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = escapeChar(arrays[i]);
        }
        return objects;
    }

    private static Object[] toArray(Character[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = escapeChar(arrays[i]);
        }
        return objects;
    }

    private static Object[] toArray(float[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Float[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(double[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Double[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(boolean[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Boolean[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    private static Object[] toArray(Object[] arrays, int limit) {
        limit = Math.min(arrays.length, limit);
        Object[] objects = new Object[limit];
        for (int i = 0; i < limit; i++) {
            objects[i] = arrays[i];
        }
        return objects;
    }

    /**
     * 是否展开当前深度的节点
     *
     * @param deep   当前节点的深度
     * @param expand 展开极限
     * @return true:当前节点需要展开 / false:当前节点不需要展开
     */
    private static boolean isExpand(int deep, int expand) {
        return deep < expand;
    }

    //from com.alibaba.fastjson.util.IOUtils.ASCII_CHARS
    private final static char[]    ASCII_CHARS                = { '0', '0', '0', '1', '0', '2', '0', '3', '0', '4', '0',
            '5', '0', '6', '0', '7', '0', '8', '0', '9', '0', 'A', '0', 'B', '0', 'C', '0', 'D', '0', 'E', '0', 'F',
            '1', '0', '1', '1', '1', '2', '1', '3', '1', '4', '1', '5', '1', '6', '1', '7', '1', '8', '1', '9', '1',
            'A', '1', 'B', '1', 'C', '1', 'D', '1', 'E', '1', 'F', '2', '0', '2', '1', '2', '2', '2', '3', '2', '4',
            '2', '5', '2', '6', '2', '7', '2', '8', '2', '9', '2', 'A', '2', 'B', '2', 'C', '2', 'D', '2', 'E', '2',
            'F',                                            };

    private static Object escapeChar(char c) {
        // ASCII的可见字符
        if (c >= 32 && c <= 126) {
            return c;
        } else if (ASCII_MAP.containsKey((byte) c)) {
            // ASCII的控制字符
            //return ASCII_MAP.get((byte) c);

            // 改为Unicode表示法: \u0001
            String str = "\\u00";
            str += ASCII_CHARS[c * 2];
            str += ASCII_CHARS[c * 2 + 1];
            return str;
        } else {
            // 超过ASCII的编码范围
            return c;
        }
    }
}
