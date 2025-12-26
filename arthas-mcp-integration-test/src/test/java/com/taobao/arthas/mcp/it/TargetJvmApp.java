package com.taobao.arthas.mcp.it;

public class TargetJvmApp {

    public static void main(String[] args) throws Exception {
        System.out.println("TargetJvmApp started.");
        while (true) {
            Thread.sleep(1000);
        }
    }
}

