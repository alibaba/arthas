package com.taobao.arthas.core.command.klass100.test.aa;

/**
 * @author guanyum
 * @date 2019/10/18
 */
public class T1 {

    /*
    public static void main2(String[] args) {
        System.err.print(1);
        System.err.println();
        System.err.println("1");
    }
    */

    public static void main(String[] args) {
        System.err.println("aaa");
        System.err.println(new T1());
        System.err.println(new char[] {(char)65, (char)66});
        System.err.println(true);
        System.err.println((char)3);
        System.err.println(4d);
        System.err.println(5f);
        System.err.println(6);
        System.err.println(7L);
        System.err.println(new Object());
        System.err.println(new long[] {1L});

        System.err.print("aaa");
        System.err.println();
        System.err.print(new T1());
        System.err.println();
        System.err.print(new char[] {(char)65, (char)66});
        System.err.println();
        System.err.print(true);
        System.err.println();
        System.err.print((char)3);
        System.err.println();
        System.err.print(4d);
        System.err.println();
        System.err.print(5f);
        System.err.println();
        System.err.print(6);
        System.err.println();
        System.err.print(7L);
        System.err.println();
        System.err.print(new Object());
        System.err.println();
        System.err.print(new long[] {1L});

        //System.exit(0);
    }

    /*
    public static void main0(String[] args) {
        System.err.println("hello world");
        for (int i = 0; i < 3; i++) {
            String a = "1";
            System.err.println(JSON.toJSONString("hello rs!"));
            System.err.println(1 + a);
        }

        boolean i = true;
        if (i) {
            System.err.println("aaa");
        }
        System.err.println(i);

        System.err.println(true);
    }
    */
}
