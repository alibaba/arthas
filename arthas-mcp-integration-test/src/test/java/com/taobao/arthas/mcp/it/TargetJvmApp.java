package com.taobao.arthas.mcp.it;

public class TargetJvmApp {

    private static final TargetJvmApp INSTANCE = new TargetJvmApp();

    /**
     * 持续调用的方法，用于触发 watch/trace/monitor/stack/tt 等需要方法执行事件的工具。
     */
    public int hotMethod(int value) {
        return compute(value) + 1;
    }

    private int compute(int value) {
        return value * 2;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TargetJvmApp started.");
        while (true) {
            INSTANCE.hotMethod((int) (System.nanoTime() & 0xFF));
            Thread.sleep(50);
        }
    }
}
