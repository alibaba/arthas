/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.net.telnet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * Telnet输入流类
 *
 * 该类实现了Telnet协议的输入流处理功能，继承自BufferedInputStream并实现Runnable接口。
 * 主要功能包括：
 * 1. 解析Telnet协议命令和子协商序列
 * 2. 管理状态机以处理Telnet协议的各种状态
 * 3. 提供队列机制缓冲接收到的数据
 * 4. 支持线程化读取模式，提高IO效率
 * 5. 处理Telnet命令（WILL, WONT, DO, DONT等）
 * 6. 支持终端类型等子选项协商
 *
 * @author Apache Software Foundation
 */
final class TelnetInputStream extends BufferedInputStream implements Runnable
{
    /** 文件结束标志，已到达流的末尾 */
    private static final int EOF = -1;

    /** 读操作将会阻塞，当不允许阻塞但没有数据可用时返回 */
    private static final int WOULD_BLOCK = -2;

    // TODO should these be private enums?
    /** Telnet协议状态机状态常量 */
    /** 普通数据状态 */
    static final int _STATE_DATA = 0, _STATE_IAC = 1, _STATE_WILL = 2,
                     /** WONT命令状态 */
                     _STATE_WONT = 3, _STATE_DO = 4, _STATE_DONT = 5,
                     /** 子协商开始状态 */
                     _STATE_SB = 6, _STATE_SE = 7, _STATE_CR = 8, _STATE_IAC_SB = 9;

    /** 是否已到达EOF，由"__queue"保护访问 */
    private boolean __hasReachedEOF; // @GuardedBy("__queue")
    /** 流是否已关闭，使用volatile确保多线程可见性 */
    private volatile boolean __isClosed;
    /** 是否有读操作正在等待 */
    private boolean __readIsWaiting;
    /** 接收状态机的当前状态 */
    private int __receiveState, __queueHead, __queueTail, __bytesAvailable;
    /** 数据队列，用于缓冲接收到的字节 */
    private final int[] __queue;
    /** 关联的Telnet客户端实例 */
    private final TelnetClient __client;
    /** 用于异步读取的工作线程 */
    private final Thread __thread;
    /** 可能发生的IO异常 */
    private IOException __ioException;

    /* TERMINAL-TYPE option (start)*/
    /** 子选项数据缓冲区，用于存储子协商序列 */
    private final int __suboption[] = new int[512];
    /** 子选项缓冲区中当前的字节数 */
    private int __suboption_count = 0;
    /* TERMINAL-TYPE option (end)*/

    /** 是否使用线程化模式运行 */
    private volatile boolean __threaded;

    /**
     * 构造函数，创建Telnet输入流
     *
     * @param input 底层输入流
     * @param client 关联的Telnet客户端实例
     * @param readerThread 是否使用独立的读取线程
     */
    TelnetInputStream(InputStream input, TelnetClient client,
                      boolean readerThread)
    {
        super(input);
        __client = client;
        __receiveState = _STATE_DATA;
        __isClosed = true;
        __hasReachedEOF = false;
        // 设置队列大小为2049，因为当队列满时会有一个槽位未使用
        // 我们想要2048字节的缓冲区大小（2的幂次方）
        __queue = new int[2049];
        __queueHead = 0;
        __queueTail = 0;
        __bytesAvailable = 0;
        __ioException = null;
        __readIsWaiting = false;
        __threaded = false;
        if(readerThread) {
            __thread = new Thread(this);
        } else {
            __thread = null;
        }
    }

    /**
     * 构造函数，创建Telnet输入流（默认使用独立读取线程）
     *
     * @param input 底层输入流
     * @param client 关联的Telnet客户端实例
     */
    TelnetInputStream(InputStream input, TelnetClient client) {
        this(input, client, true);
    }

    /**
     * 启动Telnet输入流处理
     *
     * 如果配置了读取线程，则启动该线程。
     * 线程将被设置为守护线程，优先级比当前线程高一级。
     */
    void _start()
    {
        if(__thread == null) {
            return;
        }

        int priority;
        __isClosed = false;
        // TODO remove this
        // 需要设置更高的优先级，以防JVM不使用抢占式线程
        // 这可以防止调度器引起的死锁（而不是代码bug引起的死锁）
        priority = Thread.currentThread().getPriority() + 1;
        if (priority > Thread.MAX_PRIORITY) {
            priority = Thread.MAX_PRIORITY;
        }
        __thread.setPriority(priority);
        __thread.setDaemon(true);
        __thread.start();
        __threaded = true; // 告诉_processChar我们正在线程化运行
    }


    // synchronized(__client) critical sections are to protect against
    // TelnetOutputStream writing through the telnet client at same time
    // as a processDo/Will/etc. command invoked from TelnetInputStream
    // tries to write.
    /**
     * 获取下一个数据字节
     * IAC命令在内部处理，不会返回给调用者
     *
     * @param mayBlock true表示允许方法阻塞等待数据
     * @return 下一个数据字节，
     *         如果到达流末尾返回-1（EOF），
     *         如果mayBlock为false且没有可用数据返回-2（WOULD_BLOCK）
     * @throws IOException 如果发生IO错误
     */
    private int __read(boolean mayBlock) throws IOException
    {
        int ch;

        while (true)
        {

            // 如果没有更多数据且不允许阻塞，直接返回WOULD_BLOCK (-2)
            // 这比抛出异常更高效
            if(!mayBlock && super.available() == 0) {
                return WOULD_BLOCK;
            }

            // 否则，只在到达流末尾时退出
            if ((ch = super.read()) < 0) {
                return EOF;
            }

            ch = (ch & 0xff);

            /* Code Section added for supporting AYT (start)*/
            // 处理Are You There (AYT)响应
            synchronized (__client)
            {
                __client._processAYTResponse();
            }
            /* Code Section added for supporting AYT (end)*/

            /* Code Section added for supporting spystreams (start)*/
            // 将读取的字节发送到spy流（用于调试）
            __client._spyRead(ch);
            /* Code Section added for supporting spystreams (end)*/

            // 根据当前接收状态机状态处理字符
            switch (__receiveState)
            {

            case _STATE_CR:
                // 处理回车符后的空字符（CR-NUL序列）
                if (ch == '\0')
                {
                    // 剥离空字符
                    continue;
                }
                // 如何处理回车后的换行符？
                //  else if (ch == '\n' && _requestedDont(TelnetOption.ECHO) &&

                // 通过fall-through到_STATE_DATA状态作为普通数据处理

                //$FALL-THROUGH$
            case _STATE_DATA:
                // 检测到IAC（Interpret As Command）命令字节
                if (ch == TelnetCommand.IAC)
                {
                    __receiveState = _STATE_IAC;
                    continue;
                }


                // 处理回车符
                if (ch == '\r')
                {
                    synchronized (__client)
                    {
                        // 如果客户端请求DONT BINARY模式，则进入CR状态
                        if (__client._requestedDont(TelnetOption.BINARY)) {
                            __receiveState = _STATE_CR;
                        } else {
                            __receiveState = _STATE_DATA;
                        }
                    }
                } else {
                    __receiveState = _STATE_DATA;
                }
                break;

            case _STATE_IAC:
                // 处理IAC后的各种Telnet命令
                switch (ch)
                {
                case TelnetCommand.WILL:
                    // 对方表示愿意执行某个选项
                    __receiveState = _STATE_WILL;
                    continue;
                case TelnetCommand.WONT:
                    // 对方表示拒绝执行某个选项
                    __receiveState = _STATE_WONT;
                    continue;
                case TelnetCommand.DO:
                    // 对方请求我们执行某个选项
                    __receiveState = _STATE_DO;
                    continue;
                case TelnetCommand.DONT:
                    // 对方请求我们不要执行某个选项
                    __receiveState = _STATE_DONT;
                    continue;
                /* TERMINAL-TYPE option (start)*/
                case TelnetCommand.SB:
                    // 子协商开始
                    __suboption_count = 0;
                    __receiveState = _STATE_SB;
                    continue;
                /* TERMINAL-TYPE option (end)*/
                case TelnetCommand.IAC:
                    // 双IAC表示数据字节255
                    __receiveState = _STATE_DATA;
                    break; // 退出到外层switch以从read返回IAC
                case TelnetCommand.SE: // 意外的字节！忽略它（不作为命令发送）
                    __receiveState = _STATE_DATA;
                    continue;
                default:
                    __receiveState = _STATE_DATA;
                    __client._processCommand(ch); // 通知用户
                    continue; // 继续处理下一个字符
                }
                break; // 退出并从read返回
            case _STATE_WILL:
                // 处理WILL命令
                synchronized (__client)
                {
                    __client._processWill(ch);
                    __client._flushOutputStream();
                }
                __receiveState = _STATE_DATA;
                continue;
            case _STATE_WONT:
                // 处理WONT命令
                synchronized (__client)
                {
                    __client._processWont(ch);
                    __client._flushOutputStream();
                }
                __receiveState = _STATE_DATA;
                continue;
            case _STATE_DO:
                // 处理DO命令
                synchronized (__client)
                {
                    __client._processDo(ch);
                    __client._flushOutputStream();
                }
                __receiveState = _STATE_DATA;
                continue;
            case _STATE_DONT:
                // 处理DONT命令
                synchronized (__client)
                {
                    __client._processDont(ch);
                    __client._flushOutputStream();
                }
                __receiveState = _STATE_DATA;
                continue;
            /* TERMINAL-TYPE option (start)*/
            case _STATE_SB:
                // 处理子协商字节
                switch (ch)
                {
                case TelnetCommand.IAC:
                    // 在SB阶段收到IAC，可能表示SE或双IAC
                    __receiveState = _STATE_IAC_SB;
                    continue;
                default:
                    // 存储子选项字符
                    if (__suboption_count < __suboption.length) {
                        __suboption[__suboption_count++] = ch;
                    }
                    break;
                }
                __receiveState = _STATE_SB;
                continue;
            case _STATE_IAC_SB: // 在SB阶段收到IAC
                switch (ch)
                {
                case TelnetCommand.SE:
                    // 子协商结束，处理子选项
                    synchronized (__client)
                    {
                        __client._processSuboption(__suboption, __suboption_count);
                        __client._flushOutputStream();
                    }
                    __receiveState = _STATE_DATA;
                    continue;
                case TelnetCommand.IAC: // 去重重复的IAC（双IAC表示数据255）
                    if (__suboption_count < __suboption.length) {
                        __suboption[__suboption_count++] = ch;
                    }
                    break;
                default:            // 意外的字节！忽略它
                    break;
                }
                __receiveState = _STATE_SB;
                continue;
            /* TERMINAL-TYPE option (end)*/
            }

            break;
        }

        return ch;
    }

    // synchronized(__client)临界区用于防止TelnetOutputStream通过telnet客户端
    // 同时写入，而TelnetInputStream调用的processDo/Will等命令也尝试写入
    // 如果缓冲区之前为空则返回true
    private boolean __processChar(int ch) throws InterruptedException
    {
        // 临界区，因为我们要修改__bytesAvailable、__queueTail和_queue的内容
        boolean bufferWasEmpty;
        synchronized (__queue)
        {
            bufferWasEmpty = (__bytesAvailable == 0);
            while (__bytesAvailable >= __queue.length - 1)
            {
                // 队列已满。在添加更多数据之前需要等待。
                // 希望流的所有者能尽快消费一些数据！
                if(__threaded)
                {
                    __queue.notify();
                    try
                    {
                        __queue.wait();
                    }
                    catch (InterruptedException e)
                    {
                        throw e;
                    }
                }
                else
                {
                    // 我们被要求向队列添加另一个字符，但队列已满且没有其他线程来排空它
                    // 这不应该发生！
                    throw new IllegalStateException("Queue is full! Cannot process another character.");
                }
            }

            // 如果队列未满但读操作正在阻塞，需要通知
            if (__readIsWaiting && __threaded)
            {
                __queue.notify();
            }

            // 将字符添加到队列尾部
            __queue[__queueTail] = ch;
            ++__bytesAvailable;

            // 循环队列，如果到达末尾则回到开头
            if (++__queueTail >= __queue.length) {
                __queueTail = 0;
            }
        }
        return bufferWasEmpty;
    }

    @Override
    public int read() throws IOException
    {
        // 临界区，因为我们要修改__bytesAvailable、__queueHead和_queue的内容，
        // 还要测试__hasReachedEOF的值
        synchronized (__queue)
        {

            while (true)
            {
                // 如果有IO异常需要抛出
                if (__ioException != null)
                {
                    IOException e;
                    e = __ioException;
                    __ioException = null;
                    throw e;
                }

                if (__bytesAvailable == 0)
                {
                    // 如果到达文件末尾，返回EOF
                    if (__hasReachedEOF) {
                        return EOF;
                    }

                    // 否则，需要等待队列获取数据
                    if(__threaded)
                    {
                        // 线程化模式：通知读取线程并等待
                        __queue.notify();
                        try
                        {
                            __readIsWaiting = true;
                            __queue.wait();
                            __readIsWaiting = false;
                        }
                        catch (InterruptedException e)
                        {
                            throw new InterruptedIOException("Fatal thread interruption during read.");
                        }
                    }
                    else
                    {
                        // 非线程化模式：直接读取数据
                        //__alreadyread = false;
                        __readIsWaiting = true;
                        int ch;
                        boolean mayBlock = true;    // 只在第一次读取时阻塞

                        do
                        {
                            try
                            {
                                if ((ch = __read(mayBlock)) < 0) { // 必须是EOF
                                    if(ch != WOULD_BLOCK) {
                                        return (ch);
                                    }
                                }
                            }
                            catch (InterruptedIOException e)
                            {
                                synchronized (__queue)
                                {
                                    __ioException = e;
                                    __queue.notifyAll();
                                    try
                                    {
                                        __queue.wait(100);
                                    }
                                    catch (InterruptedException interrupted)
                                    {
                                        // 忽略
                                    }
                                }
                                return EOF;
                            }


                            try
                            {
                                if(ch != WOULD_BLOCK)
                                {
                                    __processChar(ch);
                                }
                            }
                            catch (InterruptedException e)
                            {
                                if (__isClosed) {
                                    return EOF;
                                }
                            }

                            // 后续迭代不应该阻塞。这可能在剩余的缓冲socket数据
                            // 完全由Telnet命令序列组成而没有"用户"数据时发生
                            mayBlock = false;

                        }
                        // 只要有可用数据且队列未满，继续读取
                        while (super.available() > 0 && __bytesAvailable < __queue.length - 1);

                        __readIsWaiting = false;
                    }
                    continue;
                }
                else
                {
                    // 从队列头部读取数据
                    int ch;

                    ch = __queue[__queueHead];

                    // 循环队列，如果到达末尾则回到开头
                    if (++__queueHead >= __queue.length) {
                        __queueHead = 0;
                    }

                    --__bytesAvailable;

            // 需要显式notify()以便available()正常工作
            if(__bytesAvailable == 0 && __threaded) {
                __queue.notify();
            }

                    return ch;
                }
            }
        }
    }


    /**
     * 从流中读取多个字节到数组中，并返回读取的字节数
     * 如果到达流末尾则返回-1
     * <p>
     * @param buffer  用于存储数据的字节数组
     * @return 读取的字节数。如果到达消息末尾则返回-1
     * @exception IOException 如果读取底层流时发生错误
     ***/
    @Override
    public int read(byte buffer[]) throws IOException
    {
        return read(buffer, 0, buffer.length);
    }


    /**
     * 从流中读取指定数量的字节到数组中并返回读取的字节数
     * 如果到达消息末尾则返回-1。字符从给定的偏移量开始存储，
     * 直到达到指定的长度
     * <p>
     * @param buffer 用于存储数据的字节数组
     * @param offset  开始存储数据的数组偏移量
     * @param length   要读取的字节数
     * @return 读取的字节数。如果到达流末尾则返回-1
     * @exception IOException 如果读取底层流时发生错误
     ***/
    @Override
    public int read(byte buffer[], int offset, int length) throws IOException
    {
        int ch, off;

        if (length < 1) {
            return 0;
        }

        // 临界区，因为run()可能修改__bytesAvailable
        synchronized (__queue)
        {
            if (length > __bytesAvailable) {
                length = __bytesAvailable;
            }
        }

        if ((ch = read()) == EOF) {
            return EOF;
        }

        off = offset;

        // 循环读取字节到缓冲区
        do
        {
            buffer[offset++] = (byte)ch;
        }
        while (--length > 0 && (ch = read()) != EOF);

        //__client._spyRead(buffer, off, offset - off);
        return (offset - off);
    }


    /**
     * 返回false，不支持mark功能
     ***/
    @Override
    public boolean markSupported()
    {
        return false;
    }

    /**
     * 返回可以无阻塞读取的字节数
     *
     * @return 可用字节数
     * @exception IOException 如果发生IO错误
     */
    @Override
    public int available() throws IOException
    {
        // 临界区，因为run()可能修改__bytesAvailable
        synchronized (__queue)
        {
            if (__threaded) { // 线程化运行时不能调用super.available()：NET-466
                return __bytesAvailable;
            } else {
                // 非线程化模式：返回队列中的字节数加上底层流的可用字节数
                return __bytesAvailable + super.available();
            }
        }
    }


    // 不能同步。如果run()在read中阻塞会导致死锁，
    // 因为BufferedInputStream的read()是同步的
    @Override
    public void close() throws IOException
    {
        // 完全忽略线程可能仍在运行的事实
        // 我们无法承担在close时阻塞等待线程终止的代价，
        // 因为很少有JVM会实际从interrupt()方法中断系统read()
        super.close();

        synchronized (__queue)
        {
            __hasReachedEOF = true;
            __isClosed      = true;

            // 中断读取线程
            if (__thread != null && __thread.isAlive())
            {
                __thread.interrupt();
            }

            // 通知所有等待的线程
            __queue.notifyAll();
        }

    }

    /**
     * 读取线程的运行方法
     *
     * 持续从底层流读取数据并处理Telnet协议命令，
     * 将用户数据放入队列供read()方法消费
     */
    @Override
    public void run()
    {
        int ch;

        try
        {
_outerLoop:
            // 主循环：持续读取直到流关闭
            while (!__isClosed)
            {
                try
                {
                    // 从底层流读取一个字符（允许阻塞）
                    if ((ch = __read(true)) < 0) {
                        break;
                    }
                }
                catch (InterruptedIOException e)
                {
                    // 处理中断IO异常
                    synchronized (__queue)
                    {
                        __ioException = e;
                        __queue.notifyAll();
                        try
                        {
                            __queue.wait(100);
                        }
                        catch (InterruptedException interrupted)
                        {
                            if (__isClosed) {
                                break _outerLoop;
                            }
                        }
                        continue;
                    }
                } catch(RuntimeException re) {
                    // 将任何运行时异常视为流已关闭
                    // 我们关闭底层流以确保
                    super.close();
                    // 跳出循环会在方法末尾将状态设置为关闭
                    break _outerLoop;
                }

                // 处理新字符
                boolean notify = false;
                try
                {
                    notify = __processChar(ch);
                }
                catch (InterruptedException e)
                {
                    if (__isClosed) {
                        break _outerLoop;
                    }
                }

                // 如果缓冲区之前为空，通知输入监听器
                if (notify) {
                    __client.notifyInputListener();
                }
            }
        }
        catch (IOException ioe)
        {
            // 捕获IO异常并通知监听器
            synchronized (__queue)
            {
                __ioException = ioe;
            }
            __client.notifyInputListener();
        }

        // 清理并设置终止状态
        synchronized (__queue)
        {
            __isClosed      = true; // 可能是冗余的
            __hasReachedEOF = true;
            __queue.notify();
        }

        __threaded = false;
    }
}

/* Emacs configuration
 * Local variables:        **
 * mode:             java  **
 * c-basic-offset:   4     **
 * indent-tabs-mode: nil   **
 * End:                    **
 */
