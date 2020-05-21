package com.taobao.arthas.bytekit.utils;
import java.util.ArrayList;
import java.util.Stack;

/**
 * from org.apache.commons.io.FilenameUtils
 * 
 * @author hengyunabc
 *
 */
public class MatchUtils {
    
    /**
     * The wildcard matcher uses the characters '?' and '*' to represent a
     * single or multiple wildcard characters.
     * 
     * @param str
     * @param wildcardMatcher
     * @return
     */
    public static boolean wildcardMatch(String str, String wildcardMatcher) {
        return wildcardMatch(str, wildcardMatcher, false);
    }

    /**
     * The wildcard matcher uses the characters '?' and '*' to represent a
     * single or multiple wildcard characters.
     * 
     * @param str
     * @param wildcardMatcher
     * @param sensitive
     *            if sensitive is true, str and wildcardMatcher will
     *            toLowerCase.
     * @return
     */
    public static boolean wildcardMatch(String str, String wildcardMatcher, boolean sensitive) {
        if (str == null && wildcardMatcher == null) {
            return true;
        }
        if (str == null || wildcardMatcher == null) {
            return false;
        }
        str = convertCase(str, sensitive);
        wildcardMatcher = convertCase(wildcardMatcher, sensitive);
        String[] wcs = splitOnTokens(wildcardMatcher);
        boolean anyChars = false;
        int textIdx = 0;
        int wcsIdx = 0;
        Stack backtrack = new Stack();

        // loop around a backtrack stack, to handle complex * matching
        do {
            if (backtrack.size() > 0) {
                int[] array = (int[]) backtrack.pop();
                wcsIdx = array[0];
                textIdx = array[1];
                anyChars = true;
            }

            // loop whilst tokens and text left to process
            while (wcsIdx < wcs.length) {

                if (wcs[wcsIdx].equals("?")) {
                    // ? so move to next text char
                    textIdx++;
                    anyChars = false;

                } else if (wcs[wcsIdx].equals("*")) {
                    // set any chars status
                    anyChars = true;
                    if (wcsIdx == wcs.length - 1) {
                        textIdx = str.length();
                    }

                } else {
                    // matching text token
                    if (anyChars) {
                        // any chars then try to locate text token
                        textIdx = str.indexOf(wcs[wcsIdx], textIdx);
                        if (textIdx == -1) {
                            // token not found
                            break;
                        }
                        int repeat = str.indexOf(wcs[wcsIdx], textIdx + 1);
                        if (repeat >= 0) {
                            backtrack.push(new int[] { wcsIdx, repeat });
                        }
                    } else {
                        // matching from current position
                        if (!str.startsWith(wcs[wcsIdx], textIdx)) {
                            // couldnt match token
                            break;
                        }
                    }

                    // matched text token, move text index to end of matched
                    // token
                    textIdx += wcs[wcsIdx].length();
                    anyChars = false;
                }

                wcsIdx++;
            }

            // full match
            if (wcsIdx == wcs.length && textIdx == str.length()) {
                return true;
            }

        } while (backtrack.size() > 0);

        return false;
    }

    /**
     * Splits a string into a number of tokens.
     * 
     * @param text
     *            the text to split
     * @return the tokens, never null
     */
    static String[] splitOnTokens(String text) {
        // used by wildcardMatch
        // package level so a unit test may run on this

        if (text.indexOf("?") == -1 && text.indexOf("*") == -1) {
            return new String[] { text };
        }

        char[] array = text.toCharArray();
        ArrayList list = new ArrayList();
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (array[i] == '?' || array[i] == '*') {
                if (buffer.length() != 0) {
                    list.add(buffer.toString());
                    buffer.setLength(0);
                }
                if (array[i] == '?') {
                    list.add("?");
                } else if (list.size() == 0 || (i > 0 && list.get(list.size() - 1).equals("*") == false)) {
                    list.add("*");
                }
            } else {
                buffer.append(array[i]);
            }
        }
        if (buffer.length() != 0) {
            list.add(buffer.toString());
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * Converts the case of the input String to a standard format. Subsequent
     * operations can then use standard String methods.
     * 
     * @param str
     *            the string to convert, null returns null
     * @return the lower-case version if case-insensitive
     */
    static String convertCase(String str, boolean sensitive) {
        if (str == null) {
            return null;
        }
        return sensitive ? str : str.toLowerCase();
    }
}