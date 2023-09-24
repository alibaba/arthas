package com.taobao.arthas.core.util;

/**
 * Copied from {@link org.apache.commons.io.FileUtils}
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
     * Writes a byte array to a file creating the file if it does not exist.
     * <p>
     * NOTE: As from v1.3, the parent directories of the file will be created
     * if they do not exist.
     *
     * @param file  the file to write to
     * @param data  the content to write to the file
     * @throws IOException in case of an I/O error
     * @since 1.1
     */
    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        writeByteArrayToFile(file, data, false);
    }

    /**
     * Writes a byte array to a file creating the file if it does not exist.
     *
     * @param file  the file to write to
     * @param data  the content to write to the file
     * @param append if {@code true}, then bytes will be added to the
     * end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since IO 2.1
     */
    public static void writeByteArrayToFile(File file, byte[] data, boolean append) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file, append);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    /**
     * Opens a {@link FileOutputStream} for the specified file, checking and
     * creating the parent directory if it does not exist.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * <p>
     * The parent directory will be created if it does not exist.
     * The file will be created if it does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be written to.
     * An exception is thrown if the parent directory cannot be created.
     *
     * @param file  the file to open for output, must not be {@code null}
     * @param append if {@code true}, then bytes will be added to the
     * end of the file rather than overwriting
     * @return a new {@link FileOutputStream} for the specified file
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be written to
     * @throws IOException if a parent directory needs creating but that fails
     * @since 2.1
     */
    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canWrite()) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    private static boolean isAuthCommand(String command) {
        // 需要改写 auth command, TODO 更准确应该是用mask去掉密码信息
        return command != null && command.trim().startsWith(ArthasConstants.AUTH);
    }

    private static final int[] AUTH_CODEPOINTS = Helper.toCodePoints(ArthasConstants.AUTH);
    /**
     * save the command history to the given file, data will be overridden.
     * @param history the command history, each represented by an int array
     * @param file the file to save the history
     */
    public static void saveCommandHistory(List<int[]> history, File file) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(openOutputStream(file, false));
            for (int[] command: history) {
                String commandStr = Helper.fromCodePoints(command);
                if (isAuthCommand(commandStr)) {
                    command = AUTH_CODEPOINTS;
                }

                for (int i : command) {
                    out.write(i);
                }
                out.write('\n');
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static List<int[]> loadCommandHistory(File file) {
        BufferedReader br = null;
        List<int[]> history = new ArrayList<int[]>();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = br.readLine()) != null) {
                history.add(Helper.toCodePoints(line));
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
        return history;
    }

    /**
     * save the command history to the given file, data will be overridden.
     * @param history the command history
     * @param file the file to save the history
     */
    public static void saveCommandHistoryString(List<String> history, File file) {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(openOutputStream(file, false));
            for (String command: history) {
                if (!StringUtils.isBlank(command)) {
                    if (isAuthCommand(command)) {
                        command = ArthasConstants.AUTH;
                    }
                    out.write(command.getBytes("utf-8"));
                    out.write('\n');
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static List<String> loadCommandHistoryString(File file) {
        BufferedReader br = null;
        List<String> history = new ArrayList<String>();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                if (!StringUtils.isBlank(line)) {
                    history.add(line);
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
        return history;
    }

    public static String readFileToString(File file, Charset encoding) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            Reader reader = new BufferedReader(new InputStreamReader(stream, encoding));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            stream.close();
        }
    }

    public static Properties readProperties(String file) throws IOException {
        Properties properties = new Properties();

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            properties.load(in);
            return properties;
        } finally {
            com.taobao.arthas.common.IOUtils.close(in);
        }

    }

    /**
     * Check if the given path is a directory or not exists.
     * @param path path of file.
     * @return {@code true} if the path is not exist or is an existing directory, otherwise returns {@code false}.
     */
    public static boolean isDirectoryOrNotExist(String path) {
        File file = new File(path);
        return !file.exists() || file.isDirectory();
    }
}

