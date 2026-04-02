//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.taobao.arthas.core.util;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * 对象工具类
 * 提供对象操作的静态工具方法，包括异常检查、数组操作、比较、哈希计算和字符串转换等功能
 */
public abstract class ObjectUtils {
    /**
     * 初始哈希值，用于计算对象的哈希码
     */
    private static final int INITIAL_HASH = 7;

    /**
     * 哈希乘数，用于计算哈希码时的乘数因子
     */
    private static final int MULTIPLIER = 31;

    /**
     * 空字符串常量
     */
    private static final String EMPTY_STRING = "";

    /**
     * null字符串常量
     */
    private static final String NULL_STRING = "null";

    /**
     * 数组字符串的起始符号
     */
    private static final String ARRAY_START = "{";

    /**
     * 数组字符串的结束符号
     */
    private static final String ARRAY_END = "}";

    /**
     * 空数组字符串常量
     */
    private static final String EMPTY_ARRAY = "{}";

    /**
     * 数组元素分隔符
     */
    private static final String ARRAY_ELEMENT_SEPARATOR = ", ";

    /**
     * 私有构造函数，防止实例化
     */
    public ObjectUtils() {
    }

    /**
     * 检查给定的异常是否为受检异常（Checked Exception）
     * 受检异常是指既不是RuntimeException也不是Error的异常
     *
     * @param ex 要检查的异常对象
     * @return 如果是受检异常返回true，否则返回false
     */
    public static boolean isCheckedException(Throwable ex) {
        return !(ex instanceof RuntimeException) && !(ex instanceof Error);
    }

    /**
     * 检查给定的异常是否与声明的异常类型兼容
     *
     * @param ex 要检查的异常对象
     * @param declaredExceptions 声明的异常类型数组
     * @return 如果异常兼容或不是受检异常返回true，否则返回false
     */
    public static boolean isCompatibleWithThrowsClause(Throwable ex, Class... declaredExceptions) {
        // 如果不是受检异常，则自动兼容
        if(!isCheckedException(ex)) {
            return true;
        } else {
            // 检查声明的异常类型是否包含该异常
            if(declaredExceptions != null) {
                Class[] var2 = declaredExceptions;
                int var3 = declaredExceptions.length;

                // 遍历所有声明的异常类型
                for(int var4 = 0; var4 < var3; ++var4) {
                    Class declaredException = var2[var4];
                    // 检查异常是否是声明异常类型的实例
                    if(declaredException.isInstance(ex)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    /**
     * 检查给定对象是否为数组
     *
     * @param obj 要检查的对象
     * @return 如果对象是数组返回true，否则返回false
     */
    public static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    /**
     * 检查给定数组是否为空
     *
     * @param array 要检查的数组
     * @return 如果数组为null或长度为0返回true，否则返回false
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 检查数组是否包含指定元素
     *
     * @param array 要搜索的数组
     * @param element 要查找的元素
     * @return 如果数组包含该元素返回true，否则返回false
     */
    public static boolean containsElement(Object[] array, Object element) {
        // 如果数组为null，返回false
        if(array == null) {
            return false;
        } else {
            // 遍历数组查找元素
            Object[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Object arrayEle = var2[var4];
                // 使用null安全的方式比较元素
                if(nullSafeEquals(arrayEle, element)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 检查枚举数组是否包含指定的常量（不区分大小写）
     *
     * @param enumValues 枚举值数组
     * @param constant 要查找的常量字符串
     * @return 如果包含返回true，否则返回false
     */
    public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
        return containsConstant(enumValues, constant, false);
    }

    /**
     * 检查枚举数组是否包含指定的常量
     *
     * @param enumValues 枚举值数组
     * @param constant 要查找的常量字符串
     * @param caseSensitive 是否区分大小写
     * @return 如果包含返回true，否则返回false
     */
    public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
        Enum[] var3 = enumValues;
        int var4 = enumValues.length;
        int var5 = 0;

        while(true) {
            // 遍历所有枚举值
            if(var5 >= var4) {
                return false;
            }

            Enum candidate = var3[var5];
            // 根据是否区分大小写进行比较
            if(caseSensitive) {
                if(candidate.toString().equals(constant)) {
                    break;
                }
            } else if(candidate.toString().equalsIgnoreCase(constant)) {
                break;
            }

            ++var5;
        }

        return true;
    }

    /**
     * 将源数组转换为对象数组
     *
     * @param source 源数组
     * @return 对象数组
     * @throws IllegalArgumentException 如果源对象不是数组
     */
    public static Object[] toObjectArray(Object source) {
        // 如果已经是Object数组，直接返回
        if(source instanceof Object[]) {
            return (Object[])((Object[])source);
        } else if(source == null) {
            // 如果源为null，返回空数组
            return new Object[0];
        } else if(!source.getClass().isArray()) {
            // 如果源不是数组，抛出异常
            throw new IllegalArgumentException("Source is not an array: " + source);
        } else {
            // 获取数组长度
            int length = Array.getLength(source);
            if(length == 0) {
                return new Object[0];
            } else {
                // 获取数组元素的包装类型
                Class wrapperType = Array.get(source, 0).getClass();
                // 创建新的对象数组
                Object[] newArray = (Object[])((Object[])Array.newInstance(wrapperType, length));

                // 复制数组元素
                for(int i = 0; i < length; ++i) {
                    newArray[i] = Array.get(source, i);
                }

                return newArray;
            }
        }
    }

    /**
     * 比较两个对象是否相等（null安全）
     * 支持数组类型的比较
     *
     * @param o1 第一个对象
     * @param o2 第二个对象
     * @return 如果两个对象相等返回true，否则返回false
     */
    public static boolean nullSafeEquals(Object o1, Object o2) {
        // 如果两个对象引用相同，直接返回true
        if(o1 == o2) {
            return true;
        } else if(o1 != null && o2 != null) {
            // 使用equals方法比较
            if(o1.equals(o2)) {
                return true;
            } else {
                // 如果两个对象都是数组，进行数组比较
                if(o1.getClass().isArray() && o2.getClass().isArray()) {
                    // Object数组比较
                    if(o1 instanceof Object[] && o2 instanceof Object[]) {
                        return Arrays.equals((Object[])((Object[])o1), (Object[])((Object[])o2));
                    }

                    // boolean数组比较
                    if(o1 instanceof boolean[] && o2 instanceof boolean[]) {
                        return Arrays.equals((boolean[])((boolean[])o1), (boolean[])((boolean[])o2));
                    }

                    // byte数组比较
                    if(o1 instanceof byte[] && o2 instanceof byte[]) {
                        return Arrays.equals((byte[])((byte[])o1), (byte[])((byte[])o2));
                    }

                    // char数组比较
                    if(o1 instanceof char[] && o2 instanceof char[]) {
                        return Arrays.equals((char[])((char[])o1), (char[])((char[])o2));
                    }

                    // double数组比较
                    if(o1 instanceof double[] && o2 instanceof double[]) {
                        return Arrays.equals((double[])((double[])o1), (double[])((double[])o2));
                    }

                    // float数组比较
                    if(o1 instanceof float[] && o2 instanceof float[]) {
                        return Arrays.equals((float[])((float[])o1), (float[])((float[])o2));
                    }

                    // int数组比较
                    if(o1 instanceof int[] && o2 instanceof int[]) {
                        return Arrays.equals((int[])((int[])o1), (int[])((int[])o2));
                    }

                    // long数组比较
                    if(o1 instanceof long[] && o2 instanceof long[]) {
                        return Arrays.equals((long[])((long[])o1), (long[])((long[])o2));
                    }

                    // short数组比较
                    if(o1 instanceof short[] && o2 instanceof short[]) {
                        return Arrays.equals((short[])((short[])o1), (short[])((short[])o2));
                    }
                }

                return false;
            }
        } else {
            // 一个为null，另一个不为null
            return false;
        }
    }

    /**
     * 计算对象的哈希码（null安全）
     * 支持数组类型的哈希码计算
     *
     * @param obj 要计算哈希码的对象
     * @return 对象的哈希码，如果对象为null返回0
     */
    public static int nullSafeHashCode(Object obj) {
        // 如果对象为null，返回0
        if(obj == null) {
            return 0;
        } else {
            // 如果是数组，根据数组类型计算哈希码
            if(obj.getClass().isArray()) {
                // Object数组
                if(obj instanceof Object[]) {
                    return nullSafeHashCode((Object[])((Object[])obj));
                }

                // boolean数组
                if(obj instanceof boolean[]) {
                    return nullSafeHashCode((boolean[])((boolean[])obj));
                }

                // byte数组
                if(obj instanceof byte[]) {
                    return nullSafeHashCode((byte[])((byte[])obj));
                }

                // char数组
                if(obj instanceof char[]) {
                    return nullSafeHashCode((char[])((char[])obj));
                }

                // double数组
                if(obj instanceof double[]) {
                    return nullSafeHashCode((double[])((double[])obj));
                }

                // float数组
                if(obj instanceof float[]) {
                    return nullSafeHashCode((float[])((float[])obj));
                }

                // int数组
                if(obj instanceof int[]) {
                    return nullSafeHashCode((int[])((int[])obj));
                }

                // long数组
                if(obj instanceof long[]) {
                    return nullSafeHashCode((long[])((long[])obj));
                }

                // short数组
                if(obj instanceof short[]) {
                    return nullSafeHashCode((short[])((short[])obj));
                }
            }

            // 非数组对象，直接返回hashCode
            return obj.hashCode();
        }
    }

    /**
     * 计算Object数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(Object[] array) {
        // 如果数组为null，返回0
        if(array == null) {
            return 0;
        } else {
            // 使用初始哈希值7，乘数31
            int hash = 7;
            Object[] var2 = array;
            int var3 = array.length;

            // 遍历数组元素，计算哈希值
            for(int var4 = 0; var4 < var3; ++var4) {
                Object element = var2[var4];
                hash = 31 * hash + nullSafeHashCode(element);
            }

            return hash;
        }
    }

    /**
     * 计算boolean数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(boolean[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            boolean[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                boolean element = var2[var4];
                hash = 31 * hash + hashCode(element);
            }

            return hash;
        }
    }

    /**
     * 计算byte数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(byte[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            byte[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                byte element = var2[var4];
                hash = 31 * hash + element;
            }

            return hash;
        }
    }

    /**
     * 计算char数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(char[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            char[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                char element = var2[var4];
                hash = 31 * hash + element;
            }

            return hash;
        }
    }

    /**
     * 计算double数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(double[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            double[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                double element = var2[var4];
                hash = 31 * hash + hashCode(element);
            }

            return hash;
        }
    }

    /**
     * 计算float数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(float[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            float[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                float element = var2[var4];
                hash = 31 * hash + hashCode(element);
            }

            return hash;
        }
    }

    /**
     * 计算int数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(int[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            int[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                int element = var2[var4];
                hash = 31 * hash + element;
            }

            return hash;
        }
    }

    /**
     * 计算long数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(long[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            long[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                long element = var2[var4];
                hash = 31 * hash + hashCode(element);
            }

            return hash;
        }
    }

    /**
     * 计算short数组的哈希码（null安全）
     *
     * @param array 要计算哈希码的数组
     * @return 数组的哈希码，如果数组为null返回0
     */
    public static int nullSafeHashCode(short[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            short[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                short element = var2[var4];
                hash = 31 * hash + element;
            }

            return hash;
        }
    }

    /**
     * 计算boolean值的哈希码
     *
     * @param bool boolean值
     * @return 哈希码，true返回1231，false返回1237
     */
    public static int hashCode(boolean bool) {
        return bool?1231:1237;
    }

    /**
     * 计算double值的哈希码
     *
     * @param dbl double值
     * @return 哈希码
     */
    public static int hashCode(double dbl) {
        return hashCode(Double.doubleToLongBits(dbl));
    }

    /**
     * 计算float值的哈希码
     *
     * @param flt float值
     * @return 哈希码
     */
    public static int hashCode(float flt) {
        return Float.floatToIntBits(flt);
    }

    /**
     * 计算long值的哈希码
     *
     * @param lng long值
     * @return 哈希码
     */
    public static int hashCode(long lng) {
        return (int)(lng ^ lng >>> 32);
    }

    /**
     * 获取对象的身份字符串
     * 格式为：类名@身份哈希码的十六进制表示
     *
     * @param obj 对象
     * @return 身份字符串，如果对象为null返回空字符串
     */
    public static String identityToString(Object obj) {
        return obj == null?"":obj.getClass().getName() + "@" + getIdentityHexString(obj);
    }

    /**
     * 获取对象身份哈希码的十六进制字符串表示
     *
     * @param obj 对象
     * @return 身份哈希码的十六进制字符串
     */
    public static String getIdentityHexString(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

    /**
     * 获取对象的显示字符串
     *
     * @param obj 对象
     * @return 显示字符串，如果对象为null返回空字符串
     */
    public static String getDisplayString(Object obj) {
        return obj == null?"":nullSafeToString(obj);
    }

    /**
     * 获取对象的类名（null安全）
     *
     * @param obj 对象
     * @return 类名，如果对象为null返回"null"
     */
    public static String nullSafeClassName(Object obj) {
        return obj != null?obj.getClass().getName():"null";
    }

    /**
     * 将对象转换为字符串（null安全）
     * 支持数组类型的字符串转换
     *
     * @param obj 对象
     * @return 字符串表示
     */
    public static String nullSafeToString(Object obj) {
        // 如果对象为null，返回"null"
        if(obj == null) {
            return "null";
        } else if(obj instanceof String) {
            // 如果是String，直接返回
            return (String)obj;
        } else if(obj instanceof Object[]) {
            // Object数组
            return nullSafeToString((Object[])((Object[])obj));
        } else if(obj instanceof boolean[]) {
            // boolean数组
            return nullSafeToString((boolean[])((boolean[])obj));
        } else if(obj instanceof byte[]) {
            // byte数组
            return nullSafeToString((byte[])((byte[])obj));
        } else if(obj instanceof char[]) {
            // char数组
            return nullSafeToString((char[])((char[])obj));
        } else if(obj instanceof double[]) {
            // double数组
            return nullSafeToString((double[])((double[])obj));
        } else if(obj instanceof float[]) {
            // float数组
            return nullSafeToString((float[])((float[])obj));
        } else if(obj instanceof int[]) {
            // int数组
            return nullSafeToString((int[])((int[])obj));
        } else if(obj instanceof long[]) {
            // long数组
            return nullSafeToString((long[])((long[])obj));
        } else if(obj instanceof short[]) {
            // short数组
            return nullSafeToString((short[])((short[])obj));
        } else {
            // 其他对象，调用toString方法
            String str = obj.toString();
            return str != null?str:"";
        }
    }

    /**
     * 将Object数组转换为字符串（null安全）
     * 格式为：{element1, element2, ...}
     *
     * @param array Object数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(Object[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                // 遍历数组元素
                for(int i = 0; i < length; ++i) {
                    // 在元素之间添加分隔符
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将boolean数组转换为字符串（null安全）
     * 格式为：{true, false, ...}
     *
     * @param array boolean数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(boolean[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将byte数组转换为字符串（null安全）
     * 格式为：{1, 2, 3, ...}
     *
     * @param array byte数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(byte[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将char数组转换为字符串（null安全）
     * 格式为：{'a', 'b', 'c', ...}
     *
     * @param array char数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(char[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    // 字符元素用单引号括起来
                    sb.append("\'").append(array[i]).append("\'");
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将double数组转换为字符串（null安全）
     * 格式为：{1.0, 2.0, 3.0, ...}
     *
     * @param array double数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(double[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将float数组转换为字符串（null安全）
     * 格式为：{1.0, 2.0, 3.0, ...}
     *
     * @param array float数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(float[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将int数组转换为字符串（null安全）
     * 格式为：{1, 2, 3, ...}
     *
     * @param array int数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(int[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将long数组转换为字符串（null安全）
     * 格式为：{1, 2, 3, ...}
     *
     * @param array long数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(long[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

    /**
     * 将short数组转换为字符串（null安全）
     * 格式为：{1, 2, 3, ...}
     *
     * @param array short数组
     * @return 字符串表示，如果数组为null返回"null"
     */
    public static String nullSafeToString(short[] array) {
        if(array == null) {
            return "null";
        } else {
            int length = array.length;
            if(length == 0) {
                return "{}";
            } else {
                StringBuilder sb = new StringBuilder("{");

                for(int i = 0; i < length; ++i) {
                    if(i > 0) {
                        sb.append(", ");
                    }

                    sb.append(array[i]);
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }
}
