package com.taobao.arthas.common;

/**
 * @author: kaixinbaba
 * @date: 2020/9/9 2:59 下午
 * @description:
 */
public abstract class ProgressBarUtils {

    private static final char FINISHED = '█';
    private static final char UNFINISHED = '-';
    private static final int PROGRESS_SIZE = 50;
    private static final int BITE = 2;

    private static String getNChar(int num, char ch) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < num; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }

    public static void printProgressBar(String format, double currentProgress, double totalSize) {
        int index = Double.valueOf(currentProgress / totalSize * 100).intValue();
        String finished = getNChar(index / BITE, FINISHED);
        String unFinished = getNChar(PROGRESS_SIZE - index / BITE, UNFINISHED);
        String progressBar = String.format("%3d%%├%s%s┤", index, finished, unFinished);
        String target = String.format("\r" + format, progressBar);
        System.out.print(target);
    }
}
