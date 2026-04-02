package com.taobao.arthas.core.util;

/**
 * IO 工具类
 * 从 {@link org.apache.commons.io.IOUtils} 复制而来
 * 提供输入输出流操作的工具方法，包括流复制、字节数组转换等
 *
 * @author ralf0131 2016-12-28 11:41.
 */

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class IOUtils {

    /** 结束标记，表示已到达流的末尾 */
    private static final int EOF = -1;

    /**
     * 默认缓冲区大小（{@value} 字节）
     * 用于 {@link #copyLarge(InputStream, OutputStream)} 方法
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * 将输入流的内容读取为字节数组
     * <p>
     * 此方法内部使用缓冲，因此不需要使用 BufferedInputStream
     *
     * @param input  要读取的输入流
     * @return 包含输入流所有内容的字节数组
     * @throws NullPointerException 如果输入为 null
     * @throws IOException 如果发生 I/O 错误
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        // 创建字节数组输出流用于存储读取的数据
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // 将输入流的内容复制到输出流
        copy(input, output);
        // 返回字节数组
        return output.toByteArray();
    }


    /**
     * 将字节从输入流复制到输出流
     * <p>
     * 此方法内部使用缓冲，因此不需要使用 BufferedInputStream
     * <p>
     * 对于大流（超过 2GB），复制完成后将返回 -1，因为正确的字节数无法用 int 表示
     * 对于大流，请使用 <code>copyLarge(InputStream, OutputStream)</code> 方法
     *
     * @param input  要读取的输入流
     * @param output  要写入的输出流
     * @return 复制的字节数，如果大于 Integer.MAX_VALUE 则返回 -1
     * @throws NullPointerException 如果输入或输出为 null
     * @throws IOException 如果发生 I/O 错误
     * @since 1.1
     */
    public static int copy(InputStream input, OutputStream output) throws IOException {
        // 调用 copyLarge 方法进行实际复制
        long count = copyLarge(input, output);
        // 如果复制的字节数超过 int 最大值，返回 -1
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        // 将 long 类型转换为 int 类型返回
        return (int) count;
    }

    /**
     * 将字节从大流（超过 2GB）的输入流复制到输出流
     * <p>
     * 此方法内部使用缓冲，因此不需要使用 BufferedInputStream
     * <p>
     * 缓冲区大小由 {@link #DEFAULT_BUFFER_SIZE} 指定
     *
     * @param input  要读取的输入流
     * @param output  要写入的输出流
     * @return 复制的字节数
     * @throws NullPointerException 如果输入或输出为 null
     * @throws IOException 如果发生 I/O 错误
     * @since 1.3
     */
    public static long copyLarge(InputStream input, OutputStream output)
            throws IOException {
        // 使用默认缓冲区大小调用 copyLarge 方法
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    /**
     * 将字节从大流（超过 2GB）的输入流复制到输出流
     * <p>
     * 此方法使用提供的缓冲区，因此不需要使用 BufferedInputStream
     * <p>
     *
     * @param input  要读取的输入流
     * @param output  要写入的输出流
     * @param buffer 用于复制的缓冲区
     * @return 复制的字节数
     * @throws NullPointerException 如果输入或输出为 null
     * @throws IOException 如果发生 I/O 错误
     * @since 2.2
     */
    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
            throws IOException {
        // 记录已复制的字节数
        long count = 0;
        // 每次读取的字节数
        int n = 0;
        // 循环读取输入流，直到到达末尾
        while (EOF != (n = input.read(buffer))) {
            // 将读取的数据写入输出流
            output.write(buffer, 0, n);
            // 累加已复制的字节数
            count += n;
        }
        return count;
    }

    /**
     * 将输入流的内容读取为字符串
     * 使用平台的默认字符编码
     * <p>
     * 此方法内部使用缓冲，因此不需要使用 BufferedInputStream
     *
     * @param input  要读取的输入流
     * @return 包含输入流所有内容的字符串
     * @throws NullPointerException 如果输入为 null
     * @throws IOException 如果发生 I/O 错误
     */
    public static String toString(InputStream input) throws IOException {
        BufferedReader br = null;
        try {
            // 创建字符串构建器用于存储结果
            StringBuilder sb = new StringBuilder();
            // 创建缓冲读取器
            br = new BufferedReader(new InputStreamReader(input));
            String line;
            // 逐行读取输入流
            while ((line = br.readLine()) != null) {
                // 将每行追加到构建器，添加换行符
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            // 确保关闭读取器
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // 忽略关闭时的异常
                }
            }
        }
    }


}
