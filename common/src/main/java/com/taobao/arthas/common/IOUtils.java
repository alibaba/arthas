package com.taobao.arthas.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * IO工具类
 * 提供输入输出流相关的通用工具方法，包括流的读写、关闭、文件解压等功能
 *
 * @author hengyunabc 2018-11-06
 *
 */
public class IOUtils {

    /**
     * 私有构造函数，防止实例化
     * 这是一个工具类，所有方法都是静态的，不需要创建实例
     */
    private IOUtils() {
    }

    /**
     * 将输入流转换为字符串
     * 使用UTF-8编码读取输入流中的所有内容并转换为字符串
     *
     * @param inputStream 输入流
     * @return 读取到的字符串内容
     * @throws IOException 如果发生I/O错误
     */
    public static String toString(InputStream inputStream) throws IOException {
        // 创建字节数组输出流，用于存储读取的数据
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        // 创建1KB的缓冲区，用于每次读取的数据块
        byte[] buffer = new byte[1024];
        int length;
        // 循环读取输入流，直到读取完毕（返回-1）
        while ((length = inputStream.read(buffer)) != -1) {
            // 将读取到的数据写入输出流
            result.write(buffer, 0, length);
        }
        // 使用UTF-8编码将字节数组转换为字符串
        return result.toString("UTF-8");
    }

    /**
     * 将输入流的数据复制到输出流
     * 使用1KB的缓冲区进行数据传输，提高传输效率
     *
     * @param in 输入流
     * @param out 输出流
     * @throws IOException 如果发生I/O错误
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        // 创建1KB的缓冲区
        byte[] buffer = new byte[1024];
        int len;
        // 循环读取输入流并写入输出流
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    /**
     * 将输入流转换为字节数组
     *
     * @return 包含输入流中所有信息的字节数组
     * @throws java.io.IOException 如果发生I/O错误
     */
    public static byte[] getBytes(InputStream input) throws IOException {
        // 创建字节数组输出流
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        // 将输入流的数据复制到输出流
        copy(input, result);
        // 关闭输出流
        result.close();
        // 返回字节数组
        return result.toByteArray();
    }

    /**
     * 关闭输入流
     * 如果关闭失败，返回异常对象；否则返回null
     *
     * @param input 要关闭的输入流
     * @return 关闭时抛出的异常，如果关闭成功则返回null
     */
    public static IOException close(InputStream input) {
        return close((Closeable) input);
    }

    /**
     * 关闭输出流
     * 如果关闭失败，返回异常对象；否则返回null
     *
     * @param output 要关闭的输出流
     * @return 关闭时抛出的异常，如果关闭成功则返回null
     */
    public static IOException close(OutputStream output) {
        return close((Closeable) output);
    }

    /**
     * 关闭字符读取器
     * 如果关闭失败，返回异常对象；否则返回null
     *
     * @param input 要关闭的字符读取器
     * @return 关闭时抛出的异常，如果关闭成功则返回null
     */
    public static IOException close(final Reader input) {
        return close((Closeable) input);
    }

    /**
     * 关闭字符写入器
     * 如果关闭失败，返回异常对象；否则返回null
     *
     * @param output 要关闭的字符写入器
     * @return 关闭时抛出的异常，如果关闭成功则返回null
     */
    public static IOException close(final Writer output) {
        return close((Closeable) output);
    }

    /**
     * 关闭可关闭对象
     * 如果关闭失败，返回异常对象；否则返回null
     * 该方法不会抛出异常，而是将异常返回给调用者处理
     *
     * @param closeable 要关闭的可关闭对象
     * @return 关闭时抛出的异常，如果关闭成功则返回null
     */
    public static IOException close(final Closeable closeable) {
        try {
            // 检查对象不为空
            if (closeable != null) {
                // 关闭对象
                closeable.close();
            }
        } catch (final IOException ioe) {
            // 返回捕获到的异常
            return ioe;
        }
        // 关闭成功，返回null
        return null;
    }

    /**
     * 关闭ZipFile对象
     * 兼容JDK6，ZipFile在JDK7之前没有实现Closeable接口
     * 如果关闭失败，返回异常对象；否则返回null
     *
     * @param zip 要关闭的ZipFile对象
     * @return 关闭时抛出的异常，如果关闭成功则返回null
     */
    // support jdk6
    public static IOException close(final ZipFile zip) {
        try {
            // 检查对象不为空
            if (zip != null) {
                // 关闭ZipFile
                zip.close();
            }
        } catch (final IOException ioe) {
            // 返回捕获到的异常
            return ioe;
        }
        // 关闭成功，返回null
        return null;
    }

    /**
     * 判断一个文件是否是另一个文件的子文件
     * 通过比较规范化的文件路径来判断
     *
     * @param parent 父文件目录
     * @param child 子文件
     * @return 如果child是parent的子文件则返回true，否则返回false
     * @throws IOException 如果获取规范化路径时发生I/O错误
     */
    public static boolean isSubFile(File parent, File child) throws IOException {
        // 检查子文件的规范化路径是否以父文件的规范化路径加文件分隔符开头
        return child.getCanonicalPath().startsWith(parent.getCanonicalPath() + File.separator);
    }

    /**
     * 判断一个文件是否是另一个文件的子文件
     * 通过文件路径字符串判断
     *
     * @param parent 父文件路径
     * @param child 子文件路径
     * @return 如果child是parent的子文件则返回true，否则返回false
     * @throws IOException 如果获取规范化路径时发生I/O错误
     */
    public static boolean isSubFile(String parent, String child) throws IOException {
        // 将字符串转换为File对象后调用重载方法
        return isSubFile(new File(parent), new File(child));
    }

    /**
     * 解压zip文件到指定目录
     * 会创建必要的目录结构，并检查Zip Slip漏洞
     *
     * @param zipFile 要解压的zip文件路径
     * @param extractFolder 解压目标目录
     * @throws IOException 如果发生I/O错误或检测到恶意zip条目
     */
    public static void unzip(String zipFile, String extractFolder) throws IOException {
        // 创建zip文件对象
        File file = new File(zipFile);
        ZipFile zip = null;
        try {
            // 设置缓冲区大小为8KB，提高解压效率
            int BUFFER = 1024 * 8;

            // 创建ZipFile对象
            zip = new ZipFile(file);
            // 创建解压目标目录
            File newPath = new File(extractFolder);
            newPath.mkdirs();

            // 获取zip文件中所有条目的枚举
            Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();

            // 遍历zip文件中的每个条目
            while (zipFileEntries.hasMoreElements()) {
                // 获取当前zip条目
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();

                // 创建目标文件对象
                File destFile = new File(newPath, currentEntry);
                // 检查是否为Zip Slip漏洞（防止恶意zip条目写入任意位置）
                if (!isSubFile(newPath, destFile)) {
                    throw new IOException("Bad zip entry: " + currentEntry);
                }

                // 获取目标文件的父目录
                File destinationParent = destFile.getParentFile();

                // 如果需要，创建父目录结构
                destinationParent.mkdirs();

                // 如果当前条目不是目录，则解压文件
                if (!entry.isDirectory()) {
                    BufferedInputStream is = null;
                    BufferedOutputStream dest = null;
                    try {
                        // 创建缓冲输入流，读取zip条目内容
                        is = new BufferedInputStream(zip.getInputStream(entry));
                        int currentByte;
                        // 建立写入文件的缓冲区
                        byte data[] = new byte[BUFFER];

                        // 将当前文件写入磁盘
                        FileOutputStream fos = new FileOutputStream(destFile);
                        dest = new BufferedOutputStream(fos, BUFFER);

                        // 读取并写入，直到遇到最后一个字节
                        while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                            dest.write(data, 0, currentByte);
                        }
                        // 刷新输出流，确保所有数据写入磁盘
                        dest.flush();
                    } finally {
                        // 关闭输出流
                        close(dest);
                        // 关闭输入流
                        close(is);
                    }

                }

            }
        } finally {
            // 确保关闭ZipFile
            close(zip);
        }

    }
}
