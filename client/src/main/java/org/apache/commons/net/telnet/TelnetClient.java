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
import java.io.OutputStream;

/***
 * TelnetClient类根据RFC 854实现了Telnet协议的简单网络虚拟终端(NVT)。
 * 它不实现任何额外的Telnet选项，因为它是为了在Java程序中提供自动化的Telnet资源访问而设计的。
 * <p>
 * 该类的使用方法是：首先使用SocketClient的
 * {@link org.apache.commons.net.SocketClient#connect connect}
 * 方法连接到服务器。然后可以使用
 * {@link #getInputStream  getInputStream() }和
 * {@link #getOutputStream  getOutputStream() }方法
 * 获取用于通过Telnet连接发送和接收数据的InputStream和OutputStream。
 * 当完成使用这些流后，必须调用
 * {@link #disconnect  disconnect }而不是简单地关闭这些流。
 ***/

public class TelnetClient extends Telnet
{
    // Telnet连接的输入流，用于从服务器接收数据
    private InputStream __input;
    // Telnet连接的输出流，用于向服务器发送数据
    private OutputStream __output;
    // 读取线程标志，控制是否启用独立的读取线程来处理传入数据
    protected boolean readerThread = true;
    // 输入监听器，用于在有新数据可读时接收通知
    private TelnetInputListener inputListener;

    /***
     * 默认的TelnetClient构造函数，设置终端类型为{@code VT100}。
     ***/
    public TelnetClient()
    {
        /* TERMINAL-TYPE option (start)*/
        // 调用父类构造函数，设置默认终端类型为VT100
        super ("VT100");
        /* TERMINAL-TYPE option (end)*/
        // 初始化输入流为null
        __input = null;
        // 初始化输出流为null
        __output = null;
    }

    /**
     * 使用指定的终端类型构造实例。
     *
     * @param termtype 要使用的终端类型，例如{@code VT100}
     */
    /* TERMINAL-TYPE option (start)*/
    public TelnetClient(String termtype)
    {
        // 调用父类构造函数，使用指定的终端类型
        super (termtype);
        // 初始化输入流为null
        __input = null;
        // 初始化输出流为null
        __output = null;
    }
    /* TERMINAL-TYPE option (end)*/

    /**
     * 刷新输出流，将缓冲的数据发送到服务器。
     *
     * @exception IOException 如果发生I/O错误
     */
    void _flushOutputStream() throws IOException
    {
        // 刷新底层输出流
        _output_.flush();
    }

    /**
     * 关闭输出流。
     *
     * @exception IOException 如果发生I/O错误
     */
    void _closeOutputStream() throws IOException
    {
        // 关闭底层输出流
        _output_.close();
    }

    /***
     * 处理特殊的连接要求。在建立连接后初始化输入输出流。
     *
     * @exception IOException 如果连接设置过程中发生错误
     ***/
    @Override
    protected void _connectAction_() throws IOException
    {
        // 首先调用父类的连接动作处理
        super._connectAction_();
        // 创建Telnet输入流，使用底层输入流、当前客户端和读取线程标志
        TelnetInputStream tmp = new TelnetInputStream(_input_, this, readerThread);
        // 如果启用了读取线程
        if(readerThread)
        {
            // 启动输入流的读取线程，用于异步处理传入数据
            tmp._start();
        }
        // __input不能直接引用TelnetInputStream。当某些类使用TelnetInputStream时，
        // 我们会遇到阻塞问题，所以我们用BufferedInputStream包装它，这样是安全的。
        // 这种阻塞行为需要进一步研究，但目前看起来像InputStreamReader这样的类
        // 没有以安全的方式实现。
        __input = new BufferedInputStream(tmp);
        // 创建Telnet输出流，用于发送数据到服务器
        __output = new TelnetOutputStream(this);
    }

    /***
     * 断开telnet会话，关闭输入和输出流以及socket。
     * 如果你持有telnet连接的输入和输出流的引用，你不应该自己关闭它们，
     * 而应该调用disconnect来正确地关闭连接。
     ***/
    @Override
    public void disconnect() throws IOException
    {
        // 如果输入流不为null，关闭它
        if (__input != null) {
            __input.close();
        }
        // 如果输出流不为null，关闭它
        if (__output != null) {
            __output.close();
        }
        // 调用父类的disconnect方法关闭底层socket连接
        super.disconnect();
    }

    /***
     * 返回telnet连接的输出流。完成使用后不应该关闭该流。
     * 相反，你应该调用{@link #disconnect  disconnect }方法。
     *
     * @return telnet连接的输出流
     ***/
    public OutputStream getOutputStream()
    {
        // 返回Telnet输出流
        return __output;
    }

    /***
     * 返回telnet连接的输入流。完成使用后不应该关闭该流。
     * 相反，你应该调用{@link #disconnect  disconnect }方法。
     *
     * @return telnet连接的输入流
     ***/
    public InputStream getInputStream()
    {
        // 返回Telnet输入流（被BufferedInputStream包装）
        return __input;
    }

    /***
     * 返回本地侧选项的状态。
     *
     * @param option - 要检查的选项代码
     *
     * @return 本地侧选项的状态，true表示选项已激活
     ***/
    public boolean getLocalOptionState(int option)
    {
        /* BUG (option active when not already acknowledged) (start)*/
        // 返回选项是否已激活（WILL状态）并且已请求该选项
        return (_stateIsWill(option) && _requestedWill(option));
        /* BUG (option active when not already acknowledged) (end)*/
    }

    /***
     * 返回远程侧选项的状态。
     *
     * @param option - 要检查的选项代码
     *
     * @return 远程侧选项的状态，true表示选项已激活
     ***/
    public boolean getRemoteOptionState(int option)
    {
        /* BUG (option active when not already acknowledged) (start)*/
        // 返回选项是否已激活（DO状态）并且已请求该选项
        return (_stateIsDo(option) && _requestedDo(option));
        /* BUG (option active when not already acknowledged) (end)*/
    }
    /* open TelnetOptionHandler functionality (end)*/

    /* Code Section added for supporting AYT (start)*/

    /***
     * 发送一个"你在吗"(Are You There)序列并等待结果。
     * 这是Telnet协议的一个功能，用于检查远程主机是否仍在响应。
     *
     * @param timeout - 等待响应的时间（毫秒）
     *
     * @return 如果AYT收到响应返回true，否则返回false
     *
     * @throws InterruptedException 如果发生中断错误
     * @throws IllegalArgumentException 如果参数无效
     * @throws IOException 如果发生I/O错误
     ***/
    public boolean sendAYT(long timeout)
    throws IOException, IllegalArgumentException, InterruptedException
    {
        // 调用父类的_sendAYT方法发送AYT请求并等待响应
        return (_sendAYT(timeout));
    }
    /* Code Section added for supporting AYT (start)*/

    /***
     * 向远程对等体发送协议特定的子协商消息。
     * {@link TelnetClient}将添加IAC SB和IAC SE帧字节；
     * {@code message}中的第一个字节应该是相应的telnet选项代码。
     *
     * <p>
     * 此方法不等待任何响应。远程端发送的子协商消息可以通过
     * 注册相应的{@link TelnetOptionHandler}来处理。
     * </p>
     *
     * @param message 选项代码后跟子协商负载数据
     * @throws IllegalArgumentException 如果{@code message}长度为零
     * @throws IOException 如果写入消息时发生I/O错误
     * @since 3.0
     ***/
    public void sendSubnegotiation(int[] message)
    throws IOException, IllegalArgumentException
    {
        // 验证消息长度不能为零
        if (message.length < 1) {
            throw new IllegalArgumentException("zero length message");
        }
        // 调用父类方法发送子协商消息
        _sendSubnegotiation(message);
    }

    /***
     * 向远程对等体发送命令字节，添加IAC前缀。
     *
     * <p>
     * 此方法不等待任何响应。远程端发送的消息可以通过
     * 注册相应的{@link TelnetOptionHandler}来处理。
     * </p>
     *
     * @param command 命令代码
     * @throws IOException 如果写入消息时发生I/O错误
     * @throws IllegalArgumentException 如果参数无效
     * @since 3.0
     ***/
    public void sendCommand(byte command)
    throws IOException, IllegalArgumentException
    {
        // 调用父类方法发送命令
        _sendCommand(command);
    }

    /* open TelnetOptionHandler functionality (start)*/

    /***
     * 为此telnet客户端注册一个新的TelnetOptionHandler。
     * 选项处理器用于处理特定的Telnet选项协商。
     *
     * @param opthand - 要注册的选项处理器
     *
     * @throws InvalidTelnetOptionException 如果选项无效
     * @throws IOException 如果发生I/O错误
     ***/
    @Override
    public void addOptionHandler(TelnetOptionHandler opthand)
    throws InvalidTelnetOptionException, IOException
    {
        // 调用父类方法注册选项处理器
        super.addOptionHandler(opthand);
    }
    /* open TelnetOptionHandler functionality (end)*/

    /***
     * 注销一个TelnetOptionHandler。
     *
     * @param optcode - 要注销的选项代码
     *
     * @throws InvalidTelnetOptionException 如果选项无效
     * @throws IOException 如果发生I/O错误
     ***/
    @Override
    public void deleteOptionHandler(int optcode)
    throws InvalidTelnetOptionException, IOException
    {
        // 调用父类方法删除选项处理器
        super.deleteOptionHandler(optcode);
    }

    /* Code Section added for supporting spystreams (start)*/
    /***
     * 注册一个OutputStream用于监视TelnetClient会话中发生的情况。
     * 所有会话活动将被回显到这个输出流，便于调试和监控。
     *
     * @param spystream - 会话活动将被回显到的输出流
     ***/
    public void registerSpyStream(OutputStream  spystream)
    {
        // 调用父类方法注册监视流
        super._registerSpyStream(spystream);
    }

    /***
     * 停止监视此TelnetClient。
     *
     ***/
    public void stopSpyStream()
    {
        // 调用父类方法停止监视
        super._stopSpyStream();
    }
    /* Code Section added for supporting spystreams (end)*/

    /***
     * 注册一个通知处理器，用于接收telnet选项协商命令的通知。
     *
     * @param notifhand - 要注册的TelnetNotificationHandler
     ***/
    @Override
    public void registerNotifHandler(TelnetNotificationHandler  notifhand)
    {
        // 调用父类方法注册通知处理器
        super.registerNotifHandler(notifhand);
    }

    /***
     * 注销当前的通知处理器。
     *
     ***/
    @Override
    public void unregisterNotifHandler()
    {
        // 调用父类方法注销通知处理器
        super.unregisterNotifHandler();
    }

    /***
     * 设置读取线程的状态。
     *
     * <p>
     * 当启用时，将为新连接创建一个独立的内部读取线程来读取传入的数据。
     * 这会立即处理选项协商、通知等（至少直到固定大小的内部缓冲区填满）。
     * 否则，不会创建线程，所有协商和选项处理将被延迟，直到在
     * {@link #getInputStream 输入流}上执行read()操作。
     * </p>
     *
     * <p>
     * 要支持{@link TelnetInputListener}，必须启用读取线程。
     * </p>
     *
     * <p>
     * 调用此方法时，读取线程状态将应用于所有后续连接；
     * 当前连接（如果有）不受影响。
     * </p>
     *
     * @param flag true表示启用读取线程，false表示禁用
     * @see #registerInputListener
     ***/
    public void setReaderThread(boolean flag)
    {
        // 设置读取线程标志
        readerThread = flag;
    }

    /***
     * 获取读取线程的状态。
     *
     * @return 如果读取线程已启用返回true，否则返回false
     ***/
    public boolean getReaderThread()
    {
        // 返回读取线程标志
        return (readerThread);
    }

    /***
     * 注册一个监听器，当有新的传入数据可以在{@link #getInputStream 输入流}上读取时，
     * 该监听器将被通知。一次只支持一个监听器。
     *
     * <p>
     * 更准确地说，每当立即可读的字节数（即由{@link InputStream#available}返回的值）
     * 从零变为非零时，就会发出通知。请注意，（通常）可能需要多次读取才能清空缓冲区
     * 并重置此通知，因为传入字节正被异步添加到内部缓冲区。
     * </p>
     *
     * <p>
     * 只有在为连接启用了{@link #setReaderThread 读取线程}时，才支持通知。
     * </p>
     *
     * @param listener 要注册的监听器；将替换任何之前的监听器
     * @since 3.0
     ***/
    public synchronized void registerInputListener(TelnetInputListener listener)
    {
        // 设置输入监听器，替换之前的监听器
        this.inputListener = listener;
    }

    /***
     * 注销当前的{@link TelnetInputListener}（如果有）。
     *
     * @since 3.0
     ***/
    public synchronized void unregisterInputListener()
    {
        // 清除输入监听器
        this.inputListener = null;
    }

    // 通知输入监听器有新数据可读
    void notifyInputListener() {
        TelnetInputListener listener;
        // 同步获取监听器引用
        synchronized (this) {
            listener = this.inputListener;
        }
        // 如果监听器不为null，通知它有新数据可读
        if (listener != null) {
            listener.telnetInputAvailable();
        }
    }
}
