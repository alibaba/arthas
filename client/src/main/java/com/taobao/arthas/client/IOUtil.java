package com.taobao.arthas.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;

/**
 * IO工具类
 *
 * 这是一个提供读/写功能的实用工具类，主要供weatherTelnet、rexec、rshell和rlogin示例程序使用。
 * 该类的唯一目的是持有静态方法readWrite，该方法会生成一个读线程和一个写线程。
 *
 * 工作原理：
 * - 读线程：从本地输入源（通常是标准输入stdin）读取数据，并将数据写入到远程输出目标
 * - 写线程：从远程输入源读取数据，并写入到本地输出目标
 * - 当远程输入源关闭时，这两个线程都会终止
 ***/
public final class IOUtil {

    /**
     * 读写方法 - 在本地和远程之间进行数据双向传输
     *
     * 该方法创建两个线程来实现双向数据传输：
     * 1. 读线程（reader）：从本地输入读取数据并发送到远程输出
     * 2. 写线程（writer）：从远程输入读取数据并发送到本地输出
     *
     * @param remoteInput 远程输入流，数据来源于远程服务器
     * @param remoteOutput 远程输出流，数据将发送到远程服务器
     * @param localInput 本地输入流，通常来自用户控制台输入（stdin）
     * @param localOutput 本地输出流，通常输出到用户控制台（stdout）
     */
    public static final void readWrite(final InputStream remoteInput, final OutputStream remoteOutput,
                    final InputStream localInput, final Writer localOutput) {
        // 声明读线程和写线程
        Thread reader, writer;

        // 创建读线程 - 负责从本地读取数据并发送到远程
        reader = new Thread() {
            @Override
            public void run() {
                int ch; // 用于存储读取的单个字符

                try {
                    // 循环读取本地输入，直到线程被中断或到达输入流末尾
                    while (!interrupted() && (ch = localInput.read()) != -1) {
                        // 将读取的字符写入到远程输出流
                        remoteOutput.write(ch);
                        // 立即刷新输出流，确保数据及时发送
                        remoteOutput.flush();
                    }
                } catch (IOException e) {
                    // 发生IO异常时，不做处理（静默失败）
                    // e.printStackTrace();
                }
            }
        };

        // 创建写线程 - 负责从远程读取数据并发送到本地
        writer = new Thread() {
            @Override
            public void run() {
                try {
                    // 使用InputStreamReader将字节流转换为字符流，便于按字符读取
                    InputStreamReader reader = new InputStreamReader(remoteInput);
                    // 无限循环读取远程输入数据
                    while (true) {
                        // 读取单个字符
                        int singleChar = reader.read();
                        // 如果读取到-1，表示远程输入流已关闭
                        if (singleChar == -1) {
                            break; // 退出循环
                        }
                        // 将读取的字符写入到本地输出
                        localOutput.write(singleChar);
                        // 立即刷新输出流，确保数据及时显示
                        localOutput.flush();
                    }
                } catch (IOException e) {
                    // 发生IO异常时，打印异常堆栈跟踪
                    e.printStackTrace();
                }
            }
        };

        // 设置写线程的优先级比当前线程高一级
        // 这样可以确保远程数据能够及时地被读取和处理
        writer.setPriority(Thread.currentThread().getPriority() + 1);

        // 启动写线程
        writer.start();
        // 将读线程设置为守护线程，当主线程退出时，读线程也会自动退出
        reader.setDaemon(true);
        // 启动读线程
        reader.start();

        try {
            // 等待写线程完成
            // 因为写线程的结束意味着远程连接已关闭
            writer.join();
            // 中断读线程，使其能够正常退出
            reader.interrupt();
        } catch (InterruptedException e) {
            // 线程被中断时，忽略该异常
            // Ignored
        }
    }

}
