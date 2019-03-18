package com.taobao.arthas.bytekit.asm.inst;

public class InstDemo {

    public int returnInt(int i) {
        System.out.println(new Object[] { i });
        return 9998;
    }

    public static void onEnter(Object[] args) {
        System.out.println(args);
    }

    public static int returnIntStatic(int i) {
        return 9998;
    }

}
