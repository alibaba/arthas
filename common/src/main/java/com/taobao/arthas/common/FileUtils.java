package com.taobao.arthas.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件工具类
 * 提供文件读写等常用操作的静态方法
 *
 * @see org.apache.commons.io.FileUtils
 * @author hengyunabc 2020-05-03
 *
 */
public class FileUtils {

	/**
	 * 获取系统临时目录
	 *
	 * @return 系统临时目录的File对象
	 */
	public static File getTempDirectory() {
		// 从系统属性中获取临时目录路径
		return new File(System.getProperty("java.io.tmpdir"));
	}

	/**
	 * 将字节数组写入文件，如果文件不存在则创建文件
	 * <p>
	 * 注意：从v1.3版本开始，如果文件的父目录不存在，会自动创建
	 *
	 * @param file 要写入的文件对象
	 * @param data 要写入文件的内容字节数组
	 * @throws IOException 如果发生I/O错误
	 * @since 1.1
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data) throws IOException {
		// 调用重载方法，默认不追加模式
		writeByteArrayToFile(file, data, false);
	}

	/**
	 * 将字节数组写入文件，如果文件不存在则创建文件
	 *
	 * @param file   要写入的文件对象
	 * @param data   要写入文件的内容字节数组
	 * @param append 如果为{@code true}，则将字节添加到文件末尾而不是覆盖
	 * @throws IOException 如果发生I/O错误
	 * @since 2.1
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data, final boolean append)
			throws IOException {
		// 调用完整参数的重载方法，写入整个字节数组
		writeByteArrayToFile(file, data, 0, data.length, append);
	}

	/**
	 * 从指定字节数组的偏移位置{@code off}开始，将{@code len}个字节写入文件
	 * 如果文件不存在则创建文件
	 *
	 * @param file 要写入的文件对象
	 * @param data 要写入文件的内容字节数组
	 * @param off  数据的起始偏移位置
	 * @param len  要写入的字节数量
	 * @throws IOException 如果发生I/O错误
	 * @since 2.5
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data, final int off, final int len)
			throws IOException {
		// 调用完整参数的重载方法，默认不追加模式
		writeByteArrayToFile(file, data, off, len, false);
	}

	/**
	 * 从指定字节数组的偏移位置{@code off}开始，将{@code len}个字节写入文件
	 * 如果文件不存在则创建文件
	 *
	 * @param file   要写入的文件对象
	 * @param data   要写入文件的内容字节数组
	 * @param off    数据的起始偏移位置
	 * @param len    要写入的字节数量
	 * @param append 如果为{@code true}，则将字节添加到文件末尾而不是覆盖
	 * @throws IOException 如果发生I/O错误
	 * @since 2.5
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data, final int off, final int len,
			final boolean append) throws IOException {
		FileOutputStream out = null;
		try {
			// 打开文件输出流
			out = openOutputStream(file, append);
			// 将数据从指定偏移位置开始写入
			out.write(data, off, len);
		} finally {
			// 确保关闭输出流
			IOUtils.close(out);
		}
	}

	/**
	 * 为指定文件打开一个{@link FileOutputStream}，检查并创建父目录（如果不存在）
	 * <p>
	 * 方法结束时，要么成功打开流，要么抛出异常
	 * <p>
	 * 如果父目录不存在会创建。如果文件不存在会创建。如果文件对象存在但是目录会抛出异常。
	 * 如果文件存在但无法写入会抛出异常。如果无法创建父目录会抛出异常。
	 *
	 * @param file   要打开输出的文件，不能为{@code null}
	 * @param append 如果为{@code true}，则将字节添加到文件末尾而不是覆盖
	 * @return 指定文件的新{@link FileOutputStream}对象
	 * @throws IOException 如果文件对象是目录
	 * @throws IOException 如果文件无法写入
	 * @throws IOException 如果需要创建父目录但创建失败
	 * @since 2.1
	 */
	public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
		// 检查文件是否已存在
		if (file.exists()) {
			// 如果存在但是目录，抛出异常
			if (file.isDirectory()) {
				throw new IOException("File '" + file + "' exists but is a directory");
			}
			// 如果文件无法写入，抛出异常
			if (!file.canWrite()) {
				throw new IOException("File '" + file + "' cannot be written to");
			}
		} else {
			// 文件不存在，需要创建父目录
			final File parent = file.getParentFile();
			if (parent != null) {
				// 尝试创建父目录（包括所有必需的父目录）
				if (!parent.mkdirs() && !parent.isDirectory()) {
					throw new IOException("Directory '" + parent + "' could not be created");
				}
			}
		}
		// 返回文件输出流
		return new FileOutputStream(file, append);
	}

	/**
	 * 读取文件内容到字节数组
	 * 文件读取后总是会关闭
	 *
	 * @param file 要读取的文件，不能为{@code null}
	 * @return 文件内容的字节数组，永远不会为{@code null}
	 * @throws IOException 如果发生I/O错误
	 * @since 1.1
	 */
	public static byte[] readFileToByteArray(final File file) throws IOException {
		InputStream in = null;
		try {
			// 创建文件输入流
			in = new FileInputStream(file);
			// 读取输入流中的所有字节
			return IOUtils.getBytes(in);
		} finally {
			// 确保关闭输入流
			IOUtils.close(in);
		}
	}
}
