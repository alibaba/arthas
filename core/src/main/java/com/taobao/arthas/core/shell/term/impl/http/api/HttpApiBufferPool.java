package com.taobao.arthas.core.shell.term.impl.http.api;

import com.taobao.arthas.core.util.JsonUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Lazy Buffer pool for http api handler
 * @author gongdewei 2020/9/24
 */
public class HttpApiBufferPool {

    private int jsonBufferSize = 1024 * 128;
    private int poolSize = 8;
    private ArrayBlockingQueue<ByteBuf> byteBufPool = new ArrayBlockingQueue<ByteBuf>(poolSize);
    private ArrayBlockingQueue<char[]> charsBufPool = new ArrayBlockingQueue<char[]>(poolSize);
    private ArrayBlockingQueue<byte[]> bytesPool = new ArrayBlockingQueue<byte[]>(poolSize);

    public HttpApiBufferPool() {
        JsonUtils.setSerializeWriterBufferThreshold(jsonBufferSize);
        for (int i = 0; i < poolSize; i++) {
            byteBufPool.offer(Unpooled.buffer(jsonBufferSize));
            charsBufPool.offer(new char[jsonBufferSize]);
            bytesPool.offer(new byte[jsonBufferSize]);
        }
    }

    public void giveBackByteBuf(ByteBuf byteBuf) {
        byteBuf.clear();
        if (byteBuf.capacity() == jsonBufferSize) {
            if (!byteBufPool.offer(byteBuf)) {
                byteBuf.release();
            }
        } else {
            //replace content ByteBuf
            byteBuf.release();
            if (byteBufPool.remainingCapacity() > 0) {
                byteBufPool.offer(Unpooled.buffer(jsonBufferSize));
            }
        }
    }

    public ByteBuf pollByteBuf(long timeout, TimeUnit unit) throws InterruptedException {
        return byteBufPool.poll(timeout, unit);
    }

    public boolean offerCharArray(char[] chars) {
        return charsBufPool.offer(chars);
    }

    public char[] pollCharArray() {
        return charsBufPool.poll();
    }

    public char[] pollCharArray(long timeout, TimeUnit unit) throws InterruptedException {
        return charsBufPool.poll(timeout, unit);
    }

    public boolean offerByteArray(byte[] bytes) {
        return bytesPool.offer(bytes);
    }

    public byte[] pollByteArray() {
        return bytesPool.poll();
    }

    public byte[] pollByteArray(long timeout, TimeUnit unit) throws InterruptedException {
        return bytesPool.poll(timeout, unit);
    }

    public int getJsonBufferSize() {
        return jsonBufferSize;
    }

    public int getPoolSize() {
        return poolSize;
    }
}
