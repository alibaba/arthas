package com.taobao.arthas.core.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Java7 feature detection and reflection based feature access.
 * <p/>
 * Taken from maven-shared-utils, only for private usage until we go full java7
 * <p/>
 * Copied from {@link org.apache.commons.io.Java7Support}
 */
class Java7Support {

    private static final boolean IS_JAVA7;

    private static Method isSymbolicLink;

    private static Method delete;

    private static Method toPath;

    private static Method exists;

    private static Method toFile;

    private static Method readSymlink;

    private static Method createSymlink;

    private static Object emptyLinkOpts;

    private static Object emptyFileAttributes;

    static {
        boolean isJava7x = true;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> files = cl.loadClass("java.nio.file.Files");
            Class<?> path = cl.loadClass("java.nio.file.Path");
            Class<?> fa = cl.loadClass("java.nio.file.attribute.FileAttribute");
            Class<?> linkOption = cl.loadClass("java.nio.file.LinkOption");
            isSymbolicLink = files.getMethod("isSymbolicLink", path);
            delete = files.getMethod("delete", path);
            readSymlink = files.getMethod("readSymbolicLink", path);

            emptyFileAttributes = Array.newInstance(fa, 0);
            createSymlink = files.getMethod("createSymbolicLink", path, path, emptyFileAttributes.getClass());
            emptyLinkOpts = Array.newInstance(linkOption, 0);
            exists = files.getMethod("exists", path, emptyLinkOpts.getClass());
            toPath = File.class.getMethod("toPath");
            toFile = path.getMethod("toFile");
        } catch (ClassNotFoundException e) {
            isJava7x = false;
        } catch (NoSuchMethodException e) {
            isJava7x = false;
        }
        IS_JAVA7 = isJava7x;
    }

    /**
     * Invokes java7 isSymbolicLink
     * @param file The file to check
     * @return true if the file is a symbolic link
     */
    public static boolean isSymLink(File file) {
        try {
            Object path = toPath.invoke(file);
            Boolean result = (Boolean) isSymbolicLink.invoke(null, path);
            return result.booleanValue();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the target of a symbolic link
     * @param symlink The symlink to read
     * @return The location the symlink is pointing to
     * @throws IOException Upon failure
     */

    public static File readSymbolicLink(File symlink)
            throws IOException {
        try {
            Object path = toPath.invoke(symlink);
            Object resultPath = readSymlink.invoke(null, path);
            return (File) toFile.invoke(resultPath);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Indicates if a symlunk target exists
     * @param file The symlink file
     * @return true if the target exists
     * @throws IOException upon error
     */
    private static boolean exists(File file)
            throws IOException {
        try {
            Object path = toPath.invoke(file);
            final Boolean result = (Boolean) exists.invoke(null, path, emptyLinkOpts);
            return result.booleanValue();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getTargetException();
        }

    }

    /**
     * Creates a symbolic link
     * @param symlink The symlink to create
     * @param target Where it should point
     * @return The newly created symlink
     * @throws IOException upon error
     */
    public static File createSymbolicLink(File symlink, File target)
            throws IOException {
        try {
            if (!exists(symlink)) {
                Object link = toPath.invoke(symlink);
                Object path = createSymlink.invoke(null, link, toPath.invoke(target), emptyFileAttributes);
                return (File) toFile.invoke(path);
            }
            return symlink;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            final Throwable targetException = e.getTargetException();
            throw (IOException) targetException;
        }

    }

    /**
     * Performs a nio delete
     *
     * @param file the file to delete
     * @throws IOException Upon error
     */
    public static void delete(File file)
            throws IOException {
        try {
            Object path = toPath.invoke(file);
            delete.invoke(null, path);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (IOException) e.getTargetException();
        }
    }

    /**
     * Indicates if the current vm has java7 lubrary support
     * @return true if java7 library support
     */
    public static boolean isAtLeastJava7() {
        return IS_JAVA7;
    }

}
