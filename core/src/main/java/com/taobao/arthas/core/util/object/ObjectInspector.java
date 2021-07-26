package com.taobao.arthas.core.util.object;

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


/**
 * Create Object VO
 * @author gongdewei 2020/9/24
 */
public class ObjectInspector {

    public static final int DEFAULT_OBJECT_NUMBER_LIMIT = 500;
    public static final int DEFAULT_ARRAY_LEN_LIMIT = 100;
    public static final int DEFAULT_STRING_LEN_LIMIT = 4096;


    private int objectNumberLimit = DEFAULT_OBJECT_NUMBER_LIMIT;
    private int arrayLenLimit = DEFAULT_ARRAY_LEN_LIMIT;
    private int stringLenLimit = DEFAULT_STRING_LEN_LIMIT;
    //子对象数量
    private int objectCount;

    public ObjectInspector() {
    }

    public ObjectInspector(int objectNumberLimit) {
        this.objectNumberLimit = objectNumberLimit;
    }

    public ObjectVO inspect(Object object, int expand) {
        try {
            ObjectVO objectVO = inspectObject(object, 0, expand);
            if (objectVO != null && isExceedNumLimit()) {
                objectVO.setExceedLimit(isExceedNumLimit());
                objectVO.setObjectNumberLimit(objectNumberLimit);
            }
            return objectVO;
        } catch (ObjectTooLargeException e) {
            // unreachable statement
            return new ObjectVO(object != null ? getTypeName(object.getClass()) : "", e.getMessage());
        }
    }

    private ObjectVO inspectObject(Object obj, int deep, int expand) throws ObjectTooLargeException {
        ObjectVO objectVO = this.inspectObject0(obj, deep, expand);
        if (objectVO != null && objectVO.getValue() != null && objectVO.getValue() instanceof String) {
            String stringValue = (String) objectVO.getValue();
            if (stringValue.length() > stringLenLimit) {
                // truncate string value
                objectVO.setValue(stringValue.substring(0, stringLenLimit) + "...(truncated " +
                        (stringValue.length() - stringLenLimit) + " chars)");
            }
        }
        return objectVO;
    }

    private ObjectVO inspectObject0(Object obj, int deep, int expand) throws ObjectTooLargeException {
        checkObjectAmount();

        if (null == obj) {
            return null;
        } else {

            final Class<?> clazz = obj.getClass();
            final String className = getTypeName(clazz);

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
                    List<ObjectVO> list = new ArrayList<ObjectVO>(Math.min(collection.size(), arrayLenLimit));
                    for (Object el : collection) {
                        try {
                            list.add(inspectObject(el, deep+1, expand));
                            if (list.size() >= arrayLenLimit) {
                                break;
                            }
                        } catch (ObjectTooLargeException ex) {
                            //ignore
                            list.add(new ObjectVO(el != null ? getTypeName(el.getClass()) : "", "..."));
                            break;
                        }
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

                    List<ObjectVO> list = new ArrayList<ObjectVO>(Math.min(map.size(), arrayLenLimit));
                    for (Map.Entry<Object, Object> entry : map.entrySet()) {
                        ObjectVO keyObj = null;
                        ObjectVO valueObj = null;
                        try {
                            keyObj = inspectObject(entry.getKey(), deep + 1, expand);
                            valueObj = inspectObject(entry.getValue(), deep + 1, expand);
                            list.add(ObjectVO.ofKeyValue(keyObj, valueObj));
                            if (list.size() >= arrayLenLimit) {
                                break;
                            }
                        } catch (ObjectTooLargeException e) {
                            //ignore error
                            if (keyObj == null) {
                                keyObj = new ObjectVO(entry.getKey() != null ? getTypeName(entry.getKey().getClass()) : "", "...");
                            }
                            if (valueObj == null) {
                                valueObj = new ObjectVO(entry.getValue() != null ? getTypeName(entry.getValue().getClass()) : "", "...");
                            }
                            list.add(ObjectVO.ofKeyValue(keyObj, valueObj));
                            break;
                        }
                    }
                    return ObjectVO.ofCollection(className, map.size(), list);
                }
            }


            // 数组类输出
            else if (obj.getClass().isArray()) {

                final String typeName = getTypeName(obj.getClass());

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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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
                        return ObjectVO.ofArray(typeName, arrays.length, toArray(arrays, arrayLenLimit));
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

                        ObjectVO[] elements = new ObjectVO[Math.min(arrays.length, arrayLenLimit)];
                        for (int i = 0; i < elements.length; i++) {
                            try {
                                elements[i] = inspectObject(arrays[i], deep+1, expand);
                            } catch (ObjectTooLargeException ex) {
                                //ignore error
                                elements[i] = new ObjectVO(arrays[i] != null ? getTypeName(arrays[i].getClass()) : "", "...");
                                break;
                            }
                        }
                        return ObjectVO.ofArray(typeName, arrays.length, elements);
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
                String strDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(obj);
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

                            } catch (ObjectTooLargeException t) {
                                fieldVOList.add(new ObjectVO(field.getName(), getTypeName(field.getType()), "..."));
                                break;
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

    protected String getTypeName(Class<?> objClass) {
        return objClass.isArray() ? objClass.getSimpleName() : objClass.getName();
    }

    private void checkObjectAmount() throws ObjectTooLargeException {
        if (objectCount >= objectNumberLimit){
            throw new ObjectTooLargeException("Number of objects exceeds limit: "+ objectNumberLimit);
        }
        objectCount++;
    }

    public boolean isExceedNumLimit() {
        return objectCount >= objectNumberLimit;
    }

    public int getObjectNumberLimit() {
        return objectNumberLimit;
    }

    public void setObjectNumberLimit(int objectNumberLimit) {
        this.objectNumberLimit = objectNumberLimit < 10 ? 10 : objectNumberLimit;
    }

    public int getArrayLenLimit() {
        return arrayLenLimit;
    }

    public void setArrayLenLimit(int arrayLenLimit) {
        this.arrayLenLimit = arrayLenLimit < 10 ? 10 :arrayLenLimit;
    }

    public int getStringLenLimit() {
        return stringLenLimit;
    }

    public void setStringLenLimit(int stringLenLimit) {
        this.stringLenLimit = stringLenLimit < 100 ? 100 : stringLenLimit;
    }

// --------------- static methods --------------------//

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

    private static Object escapeChar(char c) {
        // ASCII的可见字符
        if (c >= 32 && c <= 126) {
            return c;
        } else if (c < 32 || c == 127) {
            // ASCII的控制字符
            //return ASCII_MAP.get((byte) c);

            // 改为Unicode表示法: \u0001
            String s = Integer.toHexString(c).toUpperCase();
            if (s.length() == 1) {
                s = "\\u000" + s;
            } else if (s.length() == 2) {
                s = "\\u00" + s;
            } else if (s.length() == 3) {
                s = "\\u0" + s;
            } else {
                s = "\\u" + s;
            }
            return s;
        } else {
            // 超过ASCII的编码范围
            return c;
        }
    }

    private static class ObjectTooLargeException extends Exception {

        public ObjectTooLargeException(String message) {
            super(message);
        }
    }
}
