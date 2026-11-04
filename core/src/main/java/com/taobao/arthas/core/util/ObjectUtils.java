//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.taobao.arthas.core.util;

import java.lang.reflect.Array;
import java.util.Arrays;

public abstract class ObjectUtils {
    private static final int INITIAL_HASH = 7;
    private static final int MULTIPLIER = 31;
    private static final String EMPTY_STRING = "";
    private static final String NULL_STRING = "null";
    private static final String ARRAY_START = "{";
    private static final String ARRAY_END = "}";
    private static final String EMPTY_ARRAY = "{}";
    private static final String ARRAY_ELEMENT_SEPARATOR = ", ";

    public ObjectUtils() {
    }

    public static boolean isCheckedException(Throwable ex) {
        return !(ex instanceof RuntimeException) && !(ex instanceof Error);
    }

    public static boolean isCompatibleWithThrowsClause(Throwable ex, Class... declaredExceptions) {
        if(!isCheckedException(ex)) {
            return true;
        } else {
            if(declaredExceptions != null) {
                Class[] var2 = declaredExceptions;
                int var3 = declaredExceptions.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    Class declaredException = var2[var4];
                    if(declaredException.isInstance(ex)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean containsElement(Object[] array, Object element) {
        if(array == null) {
            return false;
        } else {
            Object[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Object arrayEle = var2[var4];
                if(nullSafeEquals(arrayEle, element)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
        return containsConstant(enumValues, constant, false);
    }

    public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
        Enum[] var3 = enumValues;
        int var4 = enumValues.length;
        int var5 = 0;

        while(true) {
            if(var5 >= var4) {
                return false;
            }

            Enum candidate = var3[var5];
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

    public static Object[] toObjectArray(Object source) {
        if(source instanceof Object[]) {
            return (Object[])((Object[])source);
        } else if(source == null) {
            return new Object[0];
        } else if(!source.getClass().isArray()) {
            throw new IllegalArgumentException("Source is not an array: " + source);
        } else {
            int length = Array.getLength(source);
            if(length == 0) {
                return new Object[0];
            } else {
                Class wrapperType = Array.get(source, 0).getClass();
                Object[] newArray = (Object[])((Object[])Array.newInstance(wrapperType, length));

                for(int i = 0; i < length; ++i) {
                    newArray[i] = Array.get(source, i);
                }

                return newArray;
            }
        }
    }

    public static boolean nullSafeEquals(Object o1, Object o2) {
        if(o1 == o2) {
            return true;
        } else if(o1 != null && o2 != null) {
            if(o1.equals(o2)) {
                return true;
            } else {
                if(o1.getClass().isArray() && o2.getClass().isArray()) {
                    if(o1 instanceof Object[] && o2 instanceof Object[]) {
                        return Arrays.equals((Object[])((Object[])o1), (Object[])((Object[])o2));
                    }

                    if(o1 instanceof boolean[] && o2 instanceof boolean[]) {
                        return Arrays.equals((boolean[])((boolean[])o1), (boolean[])((boolean[])o2));
                    }

                    if(o1 instanceof byte[] && o2 instanceof byte[]) {
                        return Arrays.equals((byte[])((byte[])o1), (byte[])((byte[])o2));
                    }

                    if(o1 instanceof char[] && o2 instanceof char[]) {
                        return Arrays.equals((char[])((char[])o1), (char[])((char[])o2));
                    }

                    if(o1 instanceof double[] && o2 instanceof double[]) {
                        return Arrays.equals((double[])((double[])o1), (double[])((double[])o2));
                    }

                    if(o1 instanceof float[] && o2 instanceof float[]) {
                        return Arrays.equals((float[])((float[])o1), (float[])((float[])o2));
                    }

                    if(o1 instanceof int[] && o2 instanceof int[]) {
                        return Arrays.equals((int[])((int[])o1), (int[])((int[])o2));
                    }

                    if(o1 instanceof long[] && o2 instanceof long[]) {
                        return Arrays.equals((long[])((long[])o1), (long[])((long[])o2));
                    }

                    if(o1 instanceof short[] && o2 instanceof short[]) {
                        return Arrays.equals((short[])((short[])o1), (short[])((short[])o2));
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public static int nullSafeHashCode(Object obj) {
        if(obj == null) {
            return 0;
        } else {
            if(obj.getClass().isArray()) {
                if(obj instanceof Object[]) {
                    return nullSafeHashCode((Object[])((Object[])obj));
                }

                if(obj instanceof boolean[]) {
                    return nullSafeHashCode((boolean[])((boolean[])obj));
                }

                if(obj instanceof byte[]) {
                    return nullSafeHashCode((byte[])((byte[])obj));
                }

                if(obj instanceof char[]) {
                    return nullSafeHashCode((char[])((char[])obj));
                }

                if(obj instanceof double[]) {
                    return nullSafeHashCode((double[])((double[])obj));
                }

                if(obj instanceof float[]) {
                    return nullSafeHashCode((float[])((float[])obj));
                }

                if(obj instanceof int[]) {
                    return nullSafeHashCode((int[])((int[])obj));
                }

                if(obj instanceof long[]) {
                    return nullSafeHashCode((long[])((long[])obj));
                }

                if(obj instanceof short[]) {
                    return nullSafeHashCode((short[])((short[])obj));
                }
            }

            return obj.hashCode();
        }
    }

    public static int nullSafeHashCode(Object[] array) {
        if(array == null) {
            return 0;
        } else {
            int hash = 7;
            Object[] var2 = array;
            int var3 = array.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Object element = var2[var4];
                hash = 31 * hash + nullSafeHashCode(element);
            }

            return hash;
        }
    }

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

    public static int hashCode(boolean bool) {
        return bool?1231:1237;
    }

    public static int hashCode(double dbl) {
        return hashCode(Double.doubleToLongBits(dbl));
    }

    public static int hashCode(float flt) {
        return Float.floatToIntBits(flt);
    }

    public static int hashCode(long lng) {
        return (int)(lng ^ lng >>> 32);
    }

    public static String identityToString(Object obj) {
        return obj == null?"":obj.getClass().getName() + "@" + getIdentityHexString(obj);
    }

    public static String getIdentityHexString(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

    public static String getDisplayString(Object obj) {
        return obj == null?"":nullSafeToString(obj);
    }

    public static String nullSafeClassName(Object obj) {
        return obj != null?obj.getClass().getName():"null";
    }

    public static String nullSafeToString(Object obj) {
        if(obj == null) {
            return "null";
        } else if(obj instanceof String) {
            return (String)obj;
        } else if(obj instanceof Object[]) {
            return nullSafeToString((Object[])((Object[])obj));
        } else if(obj instanceof boolean[]) {
            return nullSafeToString((boolean[])((boolean[])obj));
        } else if(obj instanceof byte[]) {
            return nullSafeToString((byte[])((byte[])obj));
        } else if(obj instanceof char[]) {
            return nullSafeToString((char[])((char[])obj));
        } else if(obj instanceof double[]) {
            return nullSafeToString((double[])((double[])obj));
        } else if(obj instanceof float[]) {
            return nullSafeToString((float[])((float[])obj));
        } else if(obj instanceof int[]) {
            return nullSafeToString((int[])((int[])obj));
        } else if(obj instanceof long[]) {
            return nullSafeToString((long[])((long[])obj));
        } else if(obj instanceof short[]) {
            return nullSafeToString((short[])((short[])obj));
        } else {
            String str = obj.toString();
            return str != null?str:"";
        }
    }

    public static String nullSafeToString(Object[] array) {
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

                    sb.append("\'").append(array[i]).append("\'");
                }

                sb.append("}");
                return sb.toString();
            }
        }
    }

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
