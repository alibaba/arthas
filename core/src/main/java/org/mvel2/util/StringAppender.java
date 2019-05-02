/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.util;

import static java.lang.System.arraycopy;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author Mike Brock
 */
public class StringAppender implements CharSequence {

    private static final int DEFAULT_SIZE = 15;

    private char[] str;
    private int capacity;
    private int size = 0;
    private byte[] btr;
    private String encoding;

    public StringAppender() {
        str = new char[capacity = DEFAULT_SIZE];
    }

    public StringAppender(final int capacity) {
        str = new char[this.capacity = capacity];
    }

    public StringAppender(final int capacity, final String encoding) {
        str = new char[this.capacity = capacity];
        this.encoding = encoding;
    }

    public StringAppender(final char c) {
        (str = new char[this.capacity = DEFAULT_SIZE])[0] = c;
    }

    public StringAppender(final char[] s) {
        capacity = size = (str = s).length;
    }

    public StringAppender(final CharSequence s) {
        str = new char[this.capacity = size = s.length()];
        for (int i = 0; i < str.length; i++)
            str[i] = s.charAt(i);

    }

    public StringAppender(final String s) {
        capacity = size = (str = s.toCharArray()).length;
    }

    public StringAppender append(final char[] chars) {
        if (chars.length > (capacity - size)) grow(chars.length);
        for (int i = 0; i < chars.length; size++) {
            str[size] = chars[i++];
        }
        return this;
    }

    public StringAppender append(final byte[] chars) {
        if (chars.length > (capacity - size)) grow(chars.length);
        for (int i = 0; i < chars.length; size++) {
            str[size] = (char) chars[i++];
        }
        return this;
    }

    public StringAppender append(final char[] chars, final int start, final int length) {
        if (length > (capacity - size)) grow(length);
        int x = start + length;
        for (int i = start; i < x; i++) {
            str[size++] = chars[i];
        }
        return this;
    }

    public StringAppender append(final byte[] chars, final int start, final int length) {
        if (length > (capacity - size)) grow(length);
        int x = start + length;
        for (int i = start; i < x; i++) {
            str[size++] = (char) chars[i];
        }
        return this;
    }

    public StringAppender append(final Object o) {
        return append(String.valueOf(o));
    }

    public StringAppender append(final CharSequence s) {
        if (s.length() > (capacity - size)) grow(s.length());
        for (int i = 0; i < s.length(); size++) {
            str[size] = s.charAt(i++);
        }
        return this;
    }

    public StringAppender append(final String s) {
        if (s == null) return this;

        final int len = s.length();
        if (len > (capacity - size)) {
            grow(len);
        }

        s.getChars(0, len, str, size);
        size += len;

        return this;
    }

    public StringAppender append(final char c) {
        if (size >= capacity) grow(size);
        str[size++] = c;
        return this;
    }

    public StringAppender append(final byte b) {
        if (btr == null) btr = new byte[capacity = DEFAULT_SIZE];
        if (size >= capacity) growByte(size * 2);
        btr[size++] = b;
        return this;
    }

    public int length() {
        return size;
    }

    private void grow(final int s) {
        if (capacity == 0) capacity = DEFAULT_SIZE;
        final char[] newArray = new char[capacity += s * 2];
        arraycopy(str, 0, newArray, 0, size);
        str = newArray;
    }

    private void growByte(final int s) {
        final byte[] newByteArray = new byte[capacity += s];
        arraycopy(btr, 0, newByteArray, 0, size);
        btr = newByteArray;
    }

    public char[] getChars(final int start, final int count) {
        char[] chars = new char[count];
        arraycopy(str, start, chars, 0, count);
        return chars;
    }

    public char[] toChars() {
        if (btr != null) {
            if (encoding == null) encoding = System.getProperty("file.encoding");
            String s;
            try {
                s = new String(btr, encoding);
            } catch (UnsupportedEncodingException e) {
                s = new String(btr);
            }
            return s.toCharArray();
        }
        final char[] chars = new char[size];
        arraycopy(str, 0, chars, 0, size);
        return chars;
    }

    public String toString() {
        if (btr != null) {
            if (encoding == null) encoding = System.getProperty("file.encoding");
            String s;
            try {
                s = new String(btr, 0, size, encoding);
            } catch (UnsupportedEncodingException e) {
                s = new String(btr, 0, size);
            }
            return s;
        }
        if (size == capacity) return new String(str);
        else return new String(str, 0, size);
    }

    public void getChars(int start, int count, char[] target, int offset) {
        int delta = offset;
        for (int i = start; i < count; i++) {
            target[delta++] = str[i];
        }
    }

    public void reset() {
        size = 0;
    }

    public char charAt(int index) {
        return str[index];
    }

    public CharSequence substring(int start, int end) {
        return new String(str, start, (end - start));
    }

    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }
}
