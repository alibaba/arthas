package com.taobao.arthas.core.util;

/**
 * 文件操作工具类
 * 从 {@link org.apache.commons.io.FileUtils} 复制而来，提供文件读写、命令历史保存等功能
 * @author ralf0131 2016-12-28 11:46.
 */
import io.termd.core.util.Helper;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.taobao.arthas.common.ArthasConstants;

public class FileUtils {

    /**
     * 将字节数组写入文件，如果文件不存在则创建文件
     * <p>
     * 注意：从 v1.3 版本开始，如果父目录不存在，会自动创建父目录
     *
     * @param file  要写入的文件
     * @param data  要写入文件的内容
     * @throws IOException 如果发生 I/O 错误
     * @since 1.1
     */
    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        writeByteArrayToFile(file, data, false);
    }

    /**
     * 将字节数组写入文件，如果文件不存在则创建文件
     *
     * @param file  要写入的文件
     * @param data  要写入文件的内容
     * @param append 如果为 {@code true}，则将字节添加到文件末尾而不是覆盖
     * @throws IOException 如果发生 I/O 错误
     * @since IO 2.1
     */
    public static void writeByteArrayToFile(File file, byte[] data, boolean append) throws IOException {
        try (OutputStream out = openOutputStream(file, append)) {
            // 将数据写入输出流
            out.write(data);
        }
        // 忽略异常
    }

    /**
     * 为指定文件打开 {@link FileOutputStream}，检查并创建父目录（如果不存在）
     * <p>
     * 在方法结束时，流将成功打开，或者抛出异常
     * <p>
     * 如果父目录不存在，将创建父目录
     * 如果文件不存在，将创建文件
     * 如果文件对象存在但是目录，将抛出异常
     * 如果文件存在但无法写入，将抛出异常
     * 如果无法创建父目录，将抛出异常
     *
     * @param file  要打开输出的文件，不能为 {@code null}
     * @param append 如果为 {@code true}，则将字节添加到文件末尾而不是覆盖
     * @return 为指定文件创建的新 {@link FileOutputStream}
     * @throws IOException 如果文件对象是目录
     * @throws IOException 如果文件无法写入
     * @throws IOException 如果需要创建父目录但失败
     * @since 2.1
     */
    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        // 检查文件是否存在
        if (file.exists()) {
            // 如果是目录，抛出异常
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            // 如果文件不可写，抛出异常
            if (!file.canWrite()) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            // 获取父目录
            File parent = file.getParentFile();
            if (parent != null) {
                // 创建父目录，如果失败且目录不存在则抛出异常
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    /**
     * 判断命令是否为认证命令
     * 用于识别需要隐藏密码信息的 auth 命令
     *
     * @param command 要检查的命令字符串
     * @return 如果是 auth 命令返回 true，否则返回 false
     */
    private static boolean isAuthCommand(String command) {
        // 需要改写 auth command，TODO 更准确应该是用mask去掉密码信息
        return command != null && command.trim().startsWith(ArthasConstants.AUTH);
    }

    /**
     * AUTH 命令的 Unicode 码点数组
     * 用于在保存命令历史时替换包含密码的 auth 命令
     */
    private static final int[] AUTH_CODEPOINTS = Helper.toCodePoints(ArthasConstants.AUTH);
    /**
     * 保存命令历史到指定文件，数据会被覆盖
     * 使用码点数组格式保存命令，支持 Unicode 字符
     *
     * @param history 命令历史列表，每个命令由 int 数组表示（Unicode 码点）
     * @param file 要保存历史记录的文件
     */
    public static void saveCommandHistory(List<int[]> history, File file) {
        try (OutputStream out = new BufferedOutputStream(openOutputStream(file, false))) {
            // 遍历每条命令
            for (int[] command : history) {
                // 将码点数组转换为字符串，用于检查是否为 auth 命令
                String commandStr = Helper.fromCodePoints(command);
                // 如果是认证命令，替换为安全的 AUTH 常量（去除密码信息）
                if (isAuthCommand(commandStr)) {
                    command = AUTH_CODEPOINTS;
                }

                // 将每个码点写入输出流
                for (int i : command) {
                    out.write(i);
                }
                // 写入换行符
                out.write('\n');
            }
        } catch (IOException e) {
            // 忽略异常
        }
        // 忽略异常
    }

    /**
     * 从文件加载命令历史
     * 读取之前保存的码点数组格式的命令历史
     *
     * @param file 包含命令历史的文件
     * @return 命令历史列表，每个命令由 int 数组表示（Unicode 码点）
     */
    public static List<int[]> loadCommandHistory(File file) {
        BufferedReader br = null;
        List<int[]> history = new ArrayList<>();
        try {
            // 创建缓冲读取器
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            // 逐行读取文件
            while ((line = br.readLine()) != null) {
                // 将每行转换为码点数组并添加到历史记录
                history.add(Helper.toCodePoints(line));
            }
        } catch (IOException e) {
            // 忽略异常
        } finally {
            // 确保关闭读取器
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                // 忽略异常
            }
        }
        return history;
    }

    /**
     * 保存命令历史到指定文件，数据会被覆盖
     * 使用字符串格式保存命令
     *
     * @param history 命令历史列表，每个命令由字符串表示
     * @param file 要保存历史记录的文件
     */
    public static void saveCommandHistoryString(List<String> history, File file) {
        try (OutputStream out = new BufferedOutputStream(openOutputStream(file, false))) {
            // 遍历每条命令
            for (String command : history) {
                // 跳过空白命令
                if (!StringUtils.isBlank(command)) {
                    // 如果是认证命令，替换为安全的 AUTH 常量（去除密码信息）
                    if (isAuthCommand(command)) {
                        command = ArthasConstants.AUTH;
                    }
                    // 将命令转换为 UTF-8 字节数组并写入
                    out.write(command.getBytes("utf-8"));
                    // 写入换行符
                    out.write('\n');
                }
            }
        } catch (IOException e) {
            // 忽略异常
        }
        // 忽略异常
    }

    /**
     * 从文件加载命令历史
     * 读取之前保存的字符串格式的命令历史
     *
     * @param file 包含命令历史的文件
     * @return 命令历史列表，每个命令由字符串表示
     */
    public static List<String> loadCommandHistoryString(File file) {
        BufferedReader br = null;
        List<String> history = new ArrayList<>();
        try {
            // 创建 UTF-8 编码的缓冲读取器
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String line;
            // 逐行读取文件
            while ((line = br.readLine()) != null) {
                // 跳过空白行，添加非空白行到历史记录
                if (!StringUtils.isBlank(line)) {
                    history.add(line);
                }
            }
        } catch (IOException e) {
            // 忽略异常
        } finally {
            // 确保关闭读取器
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                // 忽略异常
            }
        }
        return history;
    }

    /**
     * 将文件内容读取为字符串
     * 使用指定编码读取文件全部内容
     *
     * @param file 要读取的文件
     * @param encoding 字符编码
     * @return 文件内容的字符串表示
     * @throws IOException 如果发生 I/O 错误
     */
    public static String readFileToString(File file, Charset encoding) throws IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            // 创建带缓冲的读取器
            Reader reader = new BufferedReader(new InputStreamReader(stream, encoding));
            StringBuilder builder = new StringBuilder();
            // 创建 8KB 的字符缓冲区
            char[] buffer = new char[8192];
            int read;
            // 循环读取文件内容
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                // 将读取的内容追加到字符串构建器
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
    }

    /**
     * 从文件读取属性配置
     * 加载 Java Properties 格式的配置文件
     *
     * @param file 属性文件的路径
     * @return 包含文件中所有属性的 Properties 对象
     * @throws IOException 如果发生 I/O 错误
     */
    public static Properties readProperties(String file) throws IOException {
        Properties properties = new Properties();

        FileInputStream in = null;
        try {
            // 创建文件输入流
            in = new FileInputStream(file);
            // 从输入流加载属性
            properties.load(in);
            return properties;
        } finally {
            // 确保关闭输入流
            com.taobao.arthas.common.IOUtils.close(in);
        }

    }

    /**
     * 检查给定路径是否是目录或不存在
     * 用于验证路径是否可以创建为新文件
     *
     * @param path 文件路径
     * @return 如果路径不存在或是已存在的目录返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isDirectoryOrNotExist(String path) {
        File file = new File(path);
        // 路径不存在或者是目录时返回 true
        return !file.exists() || file.isDirectory();
    }
}

