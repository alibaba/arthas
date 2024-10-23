package com.taobao.arthas.core.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加密算法相关工具
 */
public abstract class EncryptUtils {

    private static final String MD5_ALGORITHM_NAME = "MD5";

    private static final char[] HEX_CHARS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    /**
     * md5 as hex
     */
    public static String md5DigestAsHex(byte[] bytes) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(MD5_ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + MD5_ALGORITHM_NAME + "\"", ex);
        }
        byte[] digest = messageDigest.digest(bytes);
        char[] hexDigest = encodeHex(digest);
        return new String(hexDigest);
    }

    /**
     * bytes转换成hex
     */
    private static char[] encodeHex(byte[] bytes) {
        char[] chars = new char[32];
        for (int i = 0; i < chars.length; i = i + 2) {
            byte b = bytes[i / 2];
            chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
            chars[i + 1] = HEX_CHARS[b & 0xf];
        }
        return chars;
    }

}



