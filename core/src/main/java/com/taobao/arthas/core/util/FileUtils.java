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

    public static File getTempDirectory() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException              in case deletion is unsuccessful
     * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
     */
    public static void deleteDirectory(final File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (!isSymlink(directory)) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            final String message =
                    "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     * <p>
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     * <p>
     * When using jdk1.7, this method delegates to {@code boolean java.nio.file.Files.isSymbolicLink(Path path)}
     *
     * <b>Note:</b> the current implementation always returns {@code false} if running on
     * jkd1.6 and the system is detected as Windows using {@link FilenameUtils#isSystemWindows()}
     * <p>
     * For code that runs on Java 1.7 or later, use the following method instead:
     * <br>
     * {@code boolean java.nio.file.Files.isSymbolicLink(Path path)}
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     * @since 2.0
     */
    public static boolean isSymlink(final File file) throws IOException {
        if ( Java7Support.isAtLeastJava7() )
        {
            return Java7Support.isSymLink( file );
        }

        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        if (FilenameUtils.isSystemWindows()) {
            return false;
        }
        File fileInCanonicalDir = null;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            final File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
            return isBrokenSymlink(file);
        } else {
            return true;
        }
    }

    /**
     * Determines if the specified file is possibly a broken symbolic link.
     *
     * @param file the file to check

     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     */
    private static boolean isBrokenSymlink(final File file) throws IOException {
        // if file exists then if it is a symlink it's not broken
        if (file.exists()) {
            return false;
        }
        // a broken symlink will show up in the list of files of its parent directory
        final File canon = file.getCanonicalFile();
        File parentDir = canon.getParentFile();
        if (parentDir == null || !parentDir.exists()) {
            return false;
        }

        // is it worthwhile to create a FileFilterUtil method for this?
        // is it worthwhile to create an "identity"  IOFileFilter for this?
        File[] fileInDir = parentDir.listFiles(
                new FileFilter() {
                    public boolean accept(File aFile) {
                        return aFile.equals(canon);
                    }
                }
        );
        return fileInDir != null && fileInDir.length > 0;
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException              in case cleaning is unsuccessful
     * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
     */
    public static void cleanDirectory(final File directory) throws IOException {
        final File[] files = verifiedListFiles(directory);

        IOException exception = null;
        for (final File file : files) {
            try {
                forceDelete(file);
            } catch (final IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file file or directory to delete, must not be {@code null}
     * @throws NullPointerException  if the directory is {@code null}
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */
    public static void forceDelete(final File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            final boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                final String message =
                        "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Lists files in a directory, asserting that the supplied directory satisfies exists and is a directory
     * @param directory The directory to list
     * @return The files in the directory, never null.
     * @throws IOException if an I/O error occurs
     */
    private static File[] verifiedListFiles(File directory) throws IOException {
        if (!directory.exists()) {
            final String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            final String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        final File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }
        return files;
    }
}

