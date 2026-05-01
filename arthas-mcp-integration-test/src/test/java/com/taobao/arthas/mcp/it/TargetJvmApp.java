package com.taobao.arthas.mcp.it;

public class TargetJvmApp {

    private static final TargetJvmApp INSTANCE = new TargetJvmApp();

    /**
     * 持续调用的方法，用于触发 watch/trace/monitor/stack/tt 等需要方法执行事件的工具。
     */
    public int hotMethod(int value) {
        return compute(value) + 1;
    }

    public int hotMethodA(int value) {
        return computeA(value) + 1;
    }

    public int hotMethodB(int value) {
        return computeB(value) + 2;
    }

    public int hotMethodC(int value) {
        return computeC(value) + 3;
    }

    private int compute(int value) {
        return value * 2;
    }

    private int computeA(int value) {
        return value * 2;
    }

    private int computeB(int value) {
        return value * 3;
    }

    private int computeC(int value) {
        return value * 4;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TargetJvmApp started.");
        while (true) {
            int value = (int) (System.nanoTime() & 0xFF);
            INSTANCE.hotMethod(value);
            INSTANCE.hotMethodA(value);
            INSTANCE.hotMethodB(value);
            INSTANCE.hotMethodC(value);
            Thread.sleep(50);
        }
    }
}
