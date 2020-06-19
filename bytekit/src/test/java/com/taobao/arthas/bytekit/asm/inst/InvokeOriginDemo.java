package com.taobao.arthas.bytekit.asm.inst;

import java.util.Date;

/**
 * @author hengyunabc 2019-03-13
 *
 */
public class InvokeOriginDemo {

    public void returnVoid() {
    }

    public Void returnVoidObject() {
        int i = 0;
        try {
            int parseInt = Integer.parseInt("1000");
            i += parseInt;
        } catch (Exception e) {
            System.err.println(i + " " + e);
        }

        return null;
    }

    public int returnInt(int i) {
        return 9998;
    }

    public int returnIntToObject(int i) {

        return 9998;
    }

    public int returnIntToInteger(int i) {

        return 9998;
    }

    public static int returnIntStatic(int i) {
        return 9998;
    }

    public long returnLong() {
        return 9998L;
    }

    public long returnLongToObject() {
        return 9998L;
    }

    public String[] returnStrArray() {
        String[] result = new String[] {"abc", "xyz" , "ufo"};
        return result;
    }

    public String[] returnStrArrayWithArgs(int i, String s, long l) {
        String[] result = new String[] {"abc" + i, "xyz" + s , "ufo" + l};
        return result;
    }

    public String returnStr() {
        return new Date().toString();
    }

    public Object returnObject() {
        return InvokeOriginDemo.class;
    }


    public int recursive(int i) {
        if (i == 1) {
            return 1;
        }
        return i + recursive(i - 1);
    }

    public int tryCatch1(int i) {
        try {
            if (i < 1) {
                throw new IllegalArgumentException("input i is less than 1");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("inner catch: "+e.getMessage());
            return 1;
        }
        return i*10;
    }

    public int tryCatch2(int i) {
        if (i < 1) {
            throw new IllegalArgumentException("input i is less than 1");
        }
        return i*10;
    }
}
