package com.taobao.arthas.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * @see org.apache.commons.io.FileUtils
 * @author hengyunabc 2020-05-03
 *
 */
public class FileUtils {

	public static File getTempDirectory() {
		return new File(System.getProperty("java.io.tmpdir"));
	}

	/**
	 * Writes a byte array to a file creating the file if it does not exist.
	 * <p>
	 * NOTE: As from v1.3, the parent directories of the file will be created if
	 * they do not exist.
	 *
	 * @param file the file to write to
	 * @param data the content to write to the file
	 * @throws IOException in case of an I/O error
	 * @since 1.1
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data) throws IOException {
		writeByteArrayToFile(file, data, false);
	}

	/**
	 * Writes a byte array to a file creating the file if it does not exist.
	 *
	 * @param file   the file to write to
	 * @param data   the content to write to the file
	 * @param append if {@code true}, then bytes will be added to the end of the
	 *               file rather than overwriting
	 * @throws IOException in case of an I/O error
	 * @since 2.1
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data, final boolean append)
			throws IOException {
		writeByteArrayToFile(file, data, 0, data.length, append);
	}

	/**
	 * Writes {@code len} bytes from the specified byte array starting at offset
	 * {@code off} to a file, creating the file if it does not exist.
	 *
	 * @param file the file to write to
	 * @param data the content to write to the file
	 * @param off  the start offset in the data
	 * @param len  the number of bytes to write
	 * @throws IOException in case of an I/O error
	 * @since 2.5
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data, final int off, final int len)
			throws IOException {
		writeByteArrayToFile(file, data, off, len, false);
	}

	/**
	 * Writes {@code len} bytes from the specified byte array starting at offset
	 * {@code off} to a file, creating the file if it does not exist.
	 *
	 * @param file   the file to write to
	 * @param data   the content to write to the file
	 * @param off    the start offset in the data
	 * @param len    the number of bytes to write
	 * @param append if {@code true}, then bytes will be added to the end of the
	 *               file rather than overwriting
	 * @throws IOException in case of an I/O error
	 * @since 2.5
	 */
	public static void writeByteArrayToFile(final File file, final byte[] data, final int off, final int len,
			final boolean append) throws IOException {
		FileOutputStream out = null;
		try {
			out = openOutputStream(file, append);
			out.write(data, off, len);
		} finally {
			IOUtils.close(out);
		}
	}

	/**
	 * Opens a {@link FileOutputStream} for the specified file, checking and
	 * creating the parent directory if it does not exist.
	 * <p>
	 * At the end of the method either the stream will be successfully opened, or an
	 * exception will have been thrown.
	 * <p>
	 * The parent directory will be created if it does not exist. The file will be
	 * created if it does not exist. An exception is thrown if the file object
	 * exists but is a directory. An exception is thrown if the file exists but
	 * cannot be written to. An exception is thrown if the parent directory cannot
	 * be created.
	 *
	 * @param file   the file to open for output, must not be {@code null}
	 * @param append if {@code true}, then bytes will be added to the end of the
	 *               file rather than overwriting
	 * @return a new {@link FileOutputStream} for the specified file
	 * @throws IOException if the file object is a directory
	 * @throws IOException if the file cannot be written to
	 * @throws IOException if a parent directory needs creating but that fails
	 * @since 2.1
	 */
	public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				throw new IOException("File '" + file + "' exists but is a directory");
			}
			if (file.canWrite() == false) {
				throw new IOException("File '" + file + "' cannot be written to");
			}
		} else {
			final File parent = file.getParentFile();
			if (parent != null) {
				if (!parent.mkdirs() && !parent.isDirectory()) {
					throw new IOException("Directory '" + parent + "' could not be created");
				}
			}
		}
		return new FileOutputStream(file, append);
	}
	
    /**
     * Reads the contents of a file into a byte array.
     * The file is always closed.
     *
     * @param file the file to read, must not be {@code null}
     * @return the file contents, never {@code null}
     * @throws IOException in case of an I/O error
     * @since 1.1
     */
    public static byte[] readFileToByteArray(final File file) throws IOException {
    	InputStream in = null;
    	try {
    		in = new FileInputStream(file);
    		return IOUtils.getBytes(in);
		} finally {
			IOUtils.close(in);
		}
    }
}
