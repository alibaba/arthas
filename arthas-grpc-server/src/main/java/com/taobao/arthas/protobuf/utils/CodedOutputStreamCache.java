package com.taobao.arthas.protobuf.utils;/**
 * @author: 風楪
 * @date: 2024/8/3 下午7:20
 */

import com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Stack;

/**
 * @author: FengYe
 * @date: 2024/8/3 下午7:20
 * @description: 针对 CodedOutputStream 的缓存处理，避免创建大量 CodedOutputStream；使用 threadLocal 中的 stack 来缓存对象
 */
public class CodedOutputStreamCache {
    private static final ThreadLocal<Stack<CodedOutputStreamCache>> instanceGetter = ThreadLocal.withInitial(Stack::new);
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(byteArrayOutputStream, 0);

    // 每个线程 stack 中最多存储的 buffer 数量
    private static final int MAX_ELEMENT = 5;

    public static CodedOutputStreamCache get() {
        Stack<CodedOutputStreamCache> stack = instanceGetter.get();
        if (!stack.isEmpty()) {
            return stack.pop();
        }
        return new CodedOutputStreamCache();
    }

    public byte[] getData() throws IOException {
        this.codedOutputStream.flush();
        byte[] bytes = this.byteArrayOutputStream.toByteArray();
        this.recycle();
        return bytes;
    }

    public CodedOutputStream getCodedOutputStream() {
        return codedOutputStream;
    }

    private void recycle(){
        this.byteArrayOutputStream.reset();
        Stack<CodedOutputStreamCache> stack = instanceGetter.get();
        if (stack.size() >= MAX_ELEMENT) {
            return;
        }
        stack.push(this);
    }
}
