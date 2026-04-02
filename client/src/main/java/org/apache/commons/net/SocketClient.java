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

package org.apache.commons.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;


/**
 * SocketClient类提供了访问套接字的客户端对象所需的基本操作。
 * 该类设计为被继承，以避免重复编写打开套接字、关闭套接字、设置超时等相同的代码。
 * 特别值得注意的是 {@link #setSocketFactory  setSocketFactory } 方法，
 * 它允许您控制SocketClient创建的Socket类型以发起网络连接。
 * 这对于添加SSL或代理支持以及更好地支持applet特别有用。
 * 例如，您可以创建一个 {@link javax.net.SocketFactory}，在创建套接字之前请求浏览器安全功能。
 * 所有从SocketClient派生的类都应该使用 {@link #_socketFactory_  _socketFactory_ }
 * 成员变量来创建Socket和ServerSocket实例，而不是直接调用构造函数实例化它们。
 * 通过遵守此约定，您可以保证用户始终能够通过替换自己的SocketFactory来提供自己的Socket实现。
 * @see SocketFactory
 */
public abstract class SocketClient
{
    /**
     * 大多数IETF协议使用的行结束字符序列。
     * 即回车符后跟换行符："\r\n"
     */
    public static final String NETASCII_EOL = "\r\n";

    /**
     * 所有SocketClient实例共享的默认SocketFactory（套接字工厂）。
     * 用于创建默认的Socket实例
     */
    private static final SocketFactory __DEFAULT_SOCKET_FACTORY =
            SocketFactory.getDefault();

    /**
     * 默认的ServerSocketFactory（服务器套接字工厂）。
     * 用于创建默认的ServerSocket实例
     */
    private static final ServerSocketFactory __DEFAULT_SERVER_SOCKET_FACTORY =
            ServerSocketFactory.getDefault();

    /**
     * ProtocolCommandSupport对象，用于管理ProtocolCommandListener的注册
     * 和ProtocolCommandEvent的触发。
     * 该对象用于协议命令的监听和事件分发
     */
    private ProtocolCommandSupport __commandSupport;

    /**
     * 打开套接字后使用的超时时间（单位：毫秒）。
     * 用于设置socket的soTimeout值
     */
    protected int _timeout_;

    /**
     * 用于连接的套接字实例。
     * 保存当前活动的Socket对象
     */
    protected Socket _socket_;

    /**
     * 用于连接的主机名（null表示未提供主机名）。
     * 保存连接时使用的主机名
     */
    protected String _hostname_;

    /**
     * 客户端应该连接的默认端口。
     * 当未指定端口时使用的端口号
     */
    protected int _defaultPort_;

    /**
     * 套接字的输入流。
     * 用于从服务器读取数据
     */
    protected InputStream _input_;

    /**
     * 套接字的输出流。
     * 用于向服务器发送数据
     */
    protected OutputStream _output_;

    /**
     * 套接字的SocketFactory（套接字工厂）。
     * 用于创建Socket实例，可自定义以支持SSL、代理等
     */
    protected SocketFactory _socketFactory_;

    /**
     * 套接字的ServerSocketFactory（服务器套接字工厂）。
     * 用于创建ServerSocket实例
     */
    protected ServerSocketFactory _serverSocketFactory_;

    /**
     * 套接字的连接超时时间（0表示无限超时）。
     * 单位：毫秒
     */
    private static final int DEFAULT_CONNECT_TIMEOUT = 0;
    protected int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /**
     * SO_RCVBUF（接收缓冲区）大小的提示值。
     * -1表示未设置，使用系统默认值
     */
    private int receiveBufferSize = -1;

    /**
     * SO_SNDBUF（发送缓冲区）大小的提示值。
     * -1表示未设置，使用系统默认值
     */
    private int sendBufferSize = -1;

    /**
     * 连接时使用的代理。
     * 如果设置，后续连接将通过该代理进行
     */
    private Proxy connProxy;

    /**
     * 用于字节IO的字符集。
     * 默认使用JVM的默认字符集
     */
    private Charset charset = Charset.defaultCharset();

    /**
     * SocketClient的默认构造函数。
     * 初始化所有成员变量：
     * - _socket_ 设置为 null
     * - _timeout_ 设置为 0
     * - _defaultPort_ 设置为 0
     * - charset 设置为 {@code Charset.defaultCharset()}
     * - _socketFactory_ 设置为共享的默认SocketFactory实例
     */
    public SocketClient()
    {
        _socket_ = null;           // 初始化套接字为null
        _hostname_ = null;         // 初始化主机名为null
        _input_ = null;            // 初始化输入流为null
        _output_ = null;           // 初始化输出流为null
        _timeout_ = 0;             // 初始化超时时间为0
        _defaultPort_ = 0;         // 初始化默认端口为0
        _socketFactory_ = __DEFAULT_SOCKET_FACTORY;  // 使用默认的套接字工厂
        _serverSocketFactory_ = __DEFAULT_SERVER_SOCKET_FACTORY;  // 使用默认的服务器套接字工厂
    }


    /**
     * 由于存在多个connect()方法，提供了_connectAction_()方法作为在建立连接后
     * 立即执行某些操作的手段，而不是重新实现所有的connect()方法。
     * 每个connect()方法在打开套接字后执行的最后一个操作就是调用此方法。
     * <p>
     * 此方法执行以下操作：
     * 1. 将刚打开的套接字的超时时间设置为通过 {@link #setDefaultTimeout  setDefaultTimeout() }
     *    设置的默认超时时间
     * 2. 将_input_和_output_分别设置为套接字的InputStream和OutputStream
     * 3. 设置连接状态为已连接
     * <p>
     * 重写此方法的子类应该首先调用 <code> super._connectAction_() </code>
     * 以确保上述受保护变量的初始化。
     *
     * @throws IOException (SocketException) 如果套接字出现问题则抛出异常
     */
    protected void _connectAction_() throws IOException
    {
        _socket_.setSoTimeout(_timeout_);           // 设置套接字的读取超时时间
        _input_ = _socket_.getInputStream();        // 获取套接字的输入流
        _output_ = _socket_.getOutputStream();      // 获取套接字的输出流
    }


    /**
     * 打开一个连接到指定远程主机端口的套接字，
     * 并从当前主机的系统分配端口发起连接。
     * 在返回之前，调用 {@link #_connectAction_  _connectAction_() }
     * 以执行连接初始化操作。
     * <p>
     *
     * @param host  远程主机的 InetAddress 地址对象
     * @param port  要连接的远程主机上的端口号
     * @exception SocketException 如果无法设置套接字超时则抛出此异常
     * @exception IOException 如果无法打开套接字则抛出此异常。
     *  在大多数情况下，您只需要捕获IOException，因为SocketException是从它派生的
     */
    public void connect(InetAddress host, int port)
    throws SocketException, IOException
    {
        _hostname_ = null;   // 清空主机名，因为使用的是InetAddress对象
        _socket_ = _socketFactory_.createSocket();  // 使用工厂创建套接字实例

        // 如果设置了接收缓冲区大小，则应用到套接字
        if (receiveBufferSize != -1) {
            _socket_.setReceiveBufferSize(receiveBufferSize);
        }

        // 如果设置了发送缓冲区大小，则应用到套接字
        if (sendBufferSize != -1) {
            _socket_.setSendBufferSize(sendBufferSize);
        }

        // 连接到远程主机和端口，使用指定的连接超时时间
        _socket_.connect(new InetSocketAddress(host, port), connectTimeout);

        // 执行连接后的初始化操作
        _connectAction_();
    }

    /**
     * 打开一个连接到指定远程主机名和端口的套接字，
     * 并从当前主机的系统分配端口发起连接。
     * 在返回之前，调用 {@link #_connectAction_  _connectAction_() }
     * 以执行连接初始化操作。
     * <p>
     *
     * @param hostname  远程主机的名称（如 "example.com"）
     * @param port  要连接的远程主机上的端口号
     * @exception SocketException 如果无法设置套接字超时则抛出此异常
     * @exception IOException 如果无法打开套接字则抛出此异常。
     *  在大多数情况下，您只需要捕获IOException，因为SocketException是从它派生的
     * @exception java.net.UnknownHostException 如果无法解析主机名则抛出此异常
     */
    public void connect(String hostname, int port)
    throws SocketException, IOException
    {
        // 将主机名解析为InetAddress对象，然后调用connect方法
        connect(InetAddress.getByName(hostname), port);
        _hostname_ = hostname;  // 保存主机名
    }


    /**
     * 打开一个连接到指定远程主机端口的套接字，
     * 并从指定的本地地址和端口发起连接。
     * 在返回之前，调用 {@link #_connectAction_  _connectAction_() }
     * 以执行连接初始化操作。
     * <p>
     *
     * @param host  远程主机的 InetAddress 地址对象
     * @param port  要连接的远程主机上的端口号
     * @param localAddr  要使用的本地地址
     * @param localPort  要使用的本地端口号
     * @exception SocketException 如果无法设置套接字超时则抛出此异常
     * @exception IOException 如果无法打开套接字则抛出此异常。
     *  在大多数情况下，您只需要捕获IOException，因为SocketException是从它派生的
     */
    public void connect(InetAddress host, int port,
                        InetAddress localAddr, int localPort)
    throws SocketException, IOException
    {
        _hostname_ = null;   // 清空主机名
        _socket_ = _socketFactory_.createSocket();  // 使用工厂创建套接字实例

        // 如果设置了接收缓冲区大小，则应用到套接字
        if (receiveBufferSize != -1) {
            _socket_.setReceiveBufferSize(receiveBufferSize);
        }

        // 如果设置了发送缓冲区大小，则应用到套接字
        if (sendBufferSize != -1) {
            _socket_.setSendBufferSize(sendBufferSize);
        }

        // 绑定到本地地址和端口
        _socket_.bind(new InetSocketAddress(localAddr, localPort));

        // 连接到远程主机和端口，使用指定的连接超时时间
        _socket_.connect(new InetSocketAddress(host, port), connectTimeout);

        // 执行连接后的初始化操作
        _connectAction_();
    }


    /**
     * 打开一个连接到指定远程主机名和端口的套接字，
     * 并从指定的本地地址和端口发起连接。
     * 在返回之前，调用 {@link #_connectAction_  _connectAction_() }
     * 以执行连接初始化操作。
     * <p>
     *
     * @param hostname  远程主机的名称（如 "example.com"）
     * @param port  要连接的远程主机上的端口号
     * @param localAddr  要使用的本地地址
     * @param localPort  要使用的本地端口号
     * @exception SocketException 如果无法设置套接字超时则抛出此异常
     * @exception IOException 如果无法打开套接字则抛出此异常。
     *  在大多数情况下，您只需要捕获IOException，因为SocketException是从它派生的
     * @exception java.net.UnknownHostException 如果无法解析主机名则抛出此异常
     */
    public void connect(String hostname, int port,
                        InetAddress localAddr, int localPort)
    throws SocketException, IOException
    {
       // 将主机名解析为InetAddress对象，然后调用connect方法
       connect(InetAddress.getByName(hostname), port, localAddr, localPort);
       _hostname_ = hostname;  // 保存主机名
    }


    /**
     * 打开一个连接到指定远程主机当前默认端口的套接字，
     * 并从当前主机的系统分配端口发起连接。
     * 在返回之前，调用 {@link #_connectAction_  _connectAction_() }
     * 以执行连接初始化操作。
     * <p>
     *
     * @param host  远程主机的 InetAddress 地址对象
     * @exception SocketException 如果无法设置套接字超时则抛出此异常
     * @exception IOException 如果无法打开套接字则抛出此异常。
     *  在大多数情况下，您只需要捕获IOException，因为SocketException是从它派生的
     */
    public void connect(InetAddress host) throws SocketException, IOException
    {
        _hostname_ = null;   // 清空主机名
        connect(host, _defaultPort_);  // 使用默认端口进行连接
    }


    /**
     * 打开一个连接到指定远程主机名当前默认端口的套接字，
     * 并从当前主机的系统分配端口发起连接。
     * 在返回之前，调用 {@link #_connectAction_  _connectAction_() }
     * 以执行连接初始化操作。
     * <p>
     *
     * @param hostname  远程主机的名称（如 "example.com"）
     * @exception SocketException 如果无法设置套接字超时则抛出此异常
     * @exception IOException 如果无法打开套接字则抛出此异常。
     *  在大多数情况下，您只需要捕获IOException，因为SocketException是从它派生的
     * @exception java.net.UnknownHostException 如果无法解析主机名则抛出此异常
     */
    public void connect(String hostname) throws SocketException, IOException
    {
        connect(hostname, _defaultPort_);  // 使用默认端口进行连接
        _hostname_ = hostname;  // 保存主机名
    }


    /**
     * 断开套接字连接。
     * 您应该在完成使用类实例后调用此方法，
     * 并且在再次调用 {@link #connect connect() } 之前也应该调用此方法。
     * 执行以下操作：
     * - 将连接状态设置为false
     * - 将_socket_设置为null
     * - 将_input_设置为null
     * - 将_output_设置为null
     * <p>
     *
     * @exception IOException  如果关闭套接字时发生错误则抛出此异常
     */
    public void disconnect() throws IOException
    {
        closeQuietly(_socket_);   // 安静地关闭套接字
        closeQuietly(_input_);    // 安静地关闭输入流
        closeQuietly(_output_);   // 安静地关闭输出流
        _socket_ = null;          // 清空套接字引用
        _hostname_ = null;        // 清空主机名引用
        _input_ = null;           // 清空输入流引用
        _output_ = null;          // 清空输出流引用
    }

    /**
     * 安静地关闭套接字，忽略任何异常。
     * 这是一个辅助方法，用于安全地关闭套接字而不抛出异常。
     *
     * @param socket 要关闭的套接字
     */
    private void closeQuietly(Socket socket) {
        if (socket != null){  // 检查套接字是否为null
            try {
                socket.close();  // 尝试关闭套接字
            } catch (IOException e) {
                // 忽略关闭时的异常
            }
        }
    }

    /**
     * 安静地关闭可关闭对象，忽略任何异常。
     * 这是一个辅助方法，用于安全地关闭输入/输出流等可关闭对象而不抛出异常。
     *
     * @param close 要关闭的可关闭对象
     */
    private void closeQuietly(Closeable close){
        if (close != null){  // 检查对象是否为null
            try {
                close.close();  // 尝试关闭对象
            } catch (IOException e) {
                // 忽略关闭时的异常
            }
        }
    }
    /**
     * 检查客户端当前是否已连接到服务器。
     * <p>
     * 委托给 {@link Socket#isConnected()} 方法
     *
     * @return 如果客户端当前已连接到服务器则返回true，否则返回false
     */
    public boolean isConnected()
    {
        if (_socket_ == null) {  // 如果套接字对象不存在
            return false;  // 返回未连接状态
        }

        return _socket_.isConnected();  // 返回套接字的连接状态
    }

    /**
     * 对套接字进行各种检查以测试其是否可用。
     * 请注意，唯一确定的测试方法是实际使用它，但这些检查在某些情况下可能有所帮助。
     * @see <a href="https://issues.apache.org/jira/browse/NET-350">NET-350</a>
     *
     * @return 如果套接字看起来可用则返回 {@code true}
     * @since 3.0
     */
    public boolean isAvailable(){
        if (isConnected()) {  // 首先检查是否已连接
            try
            {
                // 检查远程地址是否有效
                if (_socket_.getInetAddress() == null) {
                    return false;
                }

                // 检查远程端口是否有效（0表示无效）
                if (_socket_.getPort() == 0) {
                    return false;
                }

                // 检查远程套接字地址是否有效
                if (_socket_.getRemoteSocketAddress() == null) {
                    return false;
                }

                // 检查套接字是否已关闭
                if (_socket_.isClosed()) {
                    return false;
                }

                /* 这些不是精确的检查（套接字可能是半开的），
                   但由于我们通常需要双向数据传输，
                   所以我们也要检查这些： */

                // 检查输入流是否已关闭
                if (_socket_.isInputShutdown()) {
                    return false;
                }

                // 检查输出流是否已关闭
                if (_socket_.isOutputShutdown()) {
                    return false;
                }

                /* 忽略结果，捕获异常：
                   尝试获取输入/输出流以验证它们仍然可用 */
                _socket_.getInputStream();
                _socket_.getOutputStream();
            }
            catch (IOException ioex)  // 如果发生任何IO异常
            {
                return false;  // 套接字不可用
            }
            return true;  // 所有检查都通过，套接字可用
        } else {
            return false;  // 未连接，不可用
        }
    }

    /**
     * 设置SocketClient在未指定端口时应连接的默认端口。
     * {@link #_defaultPort_  _defaultPort_ } 变量存储此值。
     * 如果从未设置，默认端口等于0。
     * <p>
     *
     * @param port  要设置的默认端口号
     */
    public void setDefaultPort(int port)
    {
        _defaultPort_ = port;  // 保存默认端口号
    }

    /**
     * 返回默认端口的当前值（存储在 {@link #_defaultPort_  _defaultPort_ } 中）。
     * <p>
     *
     * @return 默认端口的当前值
     */
    public int getDefaultPort()
    {
        return _defaultPort_;  // 返回默认端口号
    }


    /**
     * 设置打开套接字时使用的默认超时时间（单位：毫秒）。
     * 此值仅在调用 {@link #connect connect()} 之前使用，
     * 不应与 {@link #setSoTimeout setSoTimeout()} 混淆，
     * 后者作用于当前已打开的套接字。
     * _timeout_ 包含新的超时值。
     * <p>
     *
     * @param timeout  用于套接字连接的超时时间（单位：毫秒）
     */
    public void setDefaultTimeout(int timeout)
    {
        _timeout_ = timeout;  // 保存默认超时时间
    }


    /**
     * 返回打开套接字时使用的默认超时时间（单位：毫秒）。
     * <p>
     *
     * @return 打开套接字时使用的默认超时时间（单位：毫秒）
     */
    public int getDefaultTimeout()
    {
        return _timeout_;  // 返回默认超时时间
    }


    /**
     * 设置当前打开连接的超时时间（单位：毫秒）。
     * 仅在通过 {@link #connect connect()} 打开连接后调用此方法。
     * <p>
     * 要设置初始超时时间，请使用 {@link #setDefaultTimeout(int)}。
     *
     * @param timeout  用于当前打开的套接字连接的超时时间（单位：毫秒）
     * @exception SocketException 如果操作失败则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public void setSoTimeout(int timeout) throws SocketException
    {
        _socket_.setSoTimeout(timeout);  // 设置套接字的读取超时时间
    }


    /**
     * 设置底层套接字的发送缓冲区大小。
     * <p>
     *
     * @param size 缓冲区大小（单位：字节）
     * @throws SocketException 此方法不会抛出此异常，但子类可能希望抛出
     * @since 2.0
     */
    public void setSendBufferSize(int size) throws SocketException {
        sendBufferSize = size;  // 保存发送缓冲区大小设置
    }

    /**
     * 获取当前的发送缓冲区大小。
     * <p>
     *
     * @return 缓冲区大小，如果未初始化则返回-1
     * @since 3.0
     */
    protected int getSendBufferSize(){
        return sendBufferSize;  // 返回发送缓冲区大小
    }

    /**
     * 设置底层套接字的接收缓冲区大小。
     * <p>
     *
     * @param size 缓冲区大小（单位：字节）
     * @throws SocketException 此方法不会抛出此异常，但子类可能希望抛出
     * @since 2.0
     */
    public void setReceiveBufferSize(int size) throws SocketException  {
        receiveBufferSize = size;  // 保存接收缓冲区大小设置
    }

    /**
     * 获取当前的接收缓冲区大小。
     * <p>
     *
     * @return 缓冲区大小，如果未初始化则返回-1
     * @since 3.0
     */
    protected int getReceiveBufferSize(){
        return receiveBufferSize;  // 返回接收缓冲区大小
    }

    /**
     * 返回当前打开的套接字的超时时间（单位：毫秒）。
     * <p>
     *
     * @return 当前打开的套接字的超时时间（单位：毫秒）
     * @exception SocketException 如果操作失败则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public int getSoTimeout() throws SocketException
    {
        return _socket_.getSoTimeout();  // 获取套接字的读取超时时间
    }

    /**
     * 在当前打开的套接字上启用或禁用Nagle算法（TCP_NODELAY）。
     * <p>
     * Nagle算法用于减少小型分组的数量，通过将小的数据包组合成更大的数据包来提高网络效率。
     * 禁用此算法（设置为true）可以降低延迟，但可能会增加网络流量。
     * <p>
     *
     * @param on  如果要启用Nagle算法则为true，如果不启用则为false
     * @exception SocketException 如果操作失败则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public void setTcpNoDelay(boolean on) throws SocketException
    {
        _socket_.setTcpNoDelay(on);  // 设置TCP_NODELAY选项
    }


    /**
     * 检查当前打开的套接字上是否启用了Nagle算法。
     * <p>
     *
     * @return 如果在当前打开的套接字上启用了Nagle算法则返回true，否则返回false
     * @exception SocketException 如果操作失败则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public boolean getTcpNoDelay() throws SocketException
    {
        return _socket_.getTcpNoDelay();  // 获取TCP_NODELAY选项的值
    }

    /**
     * 在当前打开的套接字上设置SO_KEEPALIVE标志。
     *
     * 根据Javadoc文档，默认的keepalive时间是2小时（尽管这取决于实现）。
     * Windows WSA套接字实现似乎允许设置特定的keepalive值，
     * 但在其他系统上似乎不是这样。
     * <p>
     * SO_KEEPALIVE选项用于定期发送探测包以检测连接是否仍然活跃。
     *
     * @param  keepAlive 如果为true，则启用keepAlive功能
     * @throws SocketException 如果套接字存在问题则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     * @since 2.2
     */
    public void setKeepAlive(boolean keepAlive) throws SocketException {
        _socket_.setKeepAlive(keepAlive);  // 设置SO_KEEPALIVE选项
    }

    /**
     * 返回当前打开的套接字上SO_KEEPALIVE标志的当前值。
     * 委托给 {@link Socket#getKeepAlive()} 方法
     * <p>
     *
     * @return 如果启用了SO_KEEPALIVE则返回true
     * @throws SocketException 如果套接字存在问题则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     * @since 2.2
     */
    public boolean getKeepAlive() throws SocketException {
        return _socket_.getKeepAlive();  // 获取SO_KEEPALIVE选项的值
    }

    /**
     * 在当前打开的套接字上设置SO_LINGER超时时间。
     * <p>
     * SO_LINGER选项控制在关闭套接字时是否等待未发送的数据完成传输。
     *
     * @param on  如果要启用linger则为true，如果不启用则为false
     * @param val linger超时时间（单位：秒）
     * @exception SocketException 如果操作失败则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public void setSoLinger(boolean on, int val) throws SocketException
    {
        _socket_.setSoLinger(on, val);  // 设置SO_LINGER选项
    }


    /**
     * 返回当前打开的套接字的当前SO_LINGER超时时间。
     * <p>
     *
     * @return 当前的SO_LINGER超时时间。如果禁用了SO_LINGER则返回-1
     * @exception SocketException 如果操作失败则抛出此异常
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public int getSoLinger() throws SocketException
    {
        return _socket_.getSoLinger();  // 获取SO_LINGER选项的值
    }


    /**
     * 返回用于连接的本地主机上打开的套接字的端口号。
     * 委托给 {@link Socket#getLocalPort()} 方法
     * <p>
     *
     * @return 用于连接的本地主机上打开的套接字的端口号
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public int getLocalPort()
    {
        return _socket_.getLocalPort();  // 获取本地端口号
    }


    /**
     * 返回客户端套接字绑定的本地地址。
     * 委托给 {@link Socket#getLocalAddress()} 方法
     * <p>
     *
     * @return 客户端套接字绑定的本地地址
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public InetAddress getLocalAddress()
    {
        return _socket_.getLocalAddress();  // 获取本地地址
    }

    /**
     * 返回客户端连接到的远程主机的端口号。
     * 委托给 {@link Socket#getPort()} 方法
     * <p>
     *
     * @return 客户端连接到的远程主机的端口号
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public int getRemotePort()
    {
        return _socket_.getPort();  // 获取远程端口号
    }


    /**
     * 返回客户端连接到的远程地址。
     * 委托给 {@link Socket#getInetAddress()} 方法
     * <p>
     *
     * @return 客户端连接到的远程地址
     * @throws NullPointerException 如果套接字当前未打开则抛出此异常
     */
    public InetAddress getRemoteAddress()
    {
        return _socket_.getInetAddress();  // 获取远程地址
    }


    /**
     * 验证给定套接字的远程端是否连接到SocketClient当前连接到的同一主机。
     * 这在客户端需要接受来自服务器的连接时进行快速安全检查很有用，
     * 例如FTP数据连接或BSD R命令标准错误流。
     * <p>
     *
     * @param socket 要检查的套接字
     * @return 如果远程主机相同则返回true，否则返回false
     */
    public boolean verifyRemote(Socket socket)
    {
        InetAddress host1, host2;

        host1 = socket.getInetAddress();  // 获取给定套接字的远程地址
        host2 = getRemoteAddress();        // 获取当前客户端的远程地址

        return host1.equals(host2);        // 比较两个地址是否相同
    }


    /**
     * 设置SocketClient用于打开套接字连接的SocketFactory。
     * 如果factory值为null，则使用默认工厂（仅在之前修改后重置工厂时执行此操作）。
     * 任何代理设置都将被丢弃。
     * <p>
     *
     * @param factory  SocketClient应该使用的新SocketFactory
     */
    public void setSocketFactory(SocketFactory factory)
    {
        if (factory == null) {
            // 如果传入null，使用默认的套接字工厂
            _socketFactory_ = __DEFAULT_SOCKET_FACTORY;
        } else {
            // 使用传入的自定义套接字工厂
            _socketFactory_ = factory;
        }
        // 重新设置套接字工厂会使代理设置失效，
        // 因此将字段设置为null，以便getProxy()不会返回我们实际未使用的代理
        connProxy = null;
    }

    /**
     * 设置SocketClient用于打开ServerSocket连接的ServerSocketFactory。
     * 如果factory值为null，则使用默认工厂（仅在之前修改后重置工厂时执行此操作）。
     * <p>
     *
     * @param factory  SocketClient应该使用的新ServerSocketFactory
     * @since 2.0
     */
    public void setServerSocketFactory(ServerSocketFactory factory) {
        if (factory == null) {
            // 如果传入null，使用默认的服务器套接字工厂
            _serverSocketFactory_ = __DEFAULT_SERVER_SOCKET_FACTORY;
        } else {
            // 使用传入的自定义服务器套接字工厂
            _serverSocketFactory_ = factory;
        }
    }

    /**
     * 设置连接超时时间（单位：毫秒），该值将传递给 {@link Socket} 对象的connect()方法。
     * <p>
     *
     * @param connectTimeout 要使用的连接超时时间（单位：毫秒）
     * @since 2.0
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;  // 保存连接超时时间
    }

    /**
     * 获取底层套接字连接超时时间。
     * <p>
     *
     * @return 超时时间（单位：毫秒）
     * @since 2.0
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 获取底层的 {@link ServerSocketFactory}（服务器套接字工厂）。
     * <p>
     *
     * @return 服务器套接字工厂实例
     * @since 2.2
     */
    public ServerSocketFactory getServerSocketFactory() {
        return _serverSocketFactory_;  // 返回服务器套接字工厂
    }


    /**
     * 添加一个协议命令监听器。
     * 监听器可以监听协议命令的发送和接收事件。
     * <p>
     *
     * @param listener  要添加的ProtocolCommandListener（协议命令监听器）
     * @since 3.0
     */
    public void addProtocolCommandListener(ProtocolCommandListener listener) {
        getCommandSupport().addProtocolCommandListener(listener);  // 添加监听器到命令支持对象
    }

    /**
     * 移除一个协议命令监听器。
     * <p>
     *
     * @param listener  要移除的ProtocolCommandListener（协议命令监听器）
     * @since 3.0
     */
    public void removeProtocolCommandListener(ProtocolCommandListener listener) {
        getCommandSupport().removeProtocolCommandListener(listener);  // 从命令支持对象中移除监听器
    }

    /**
     * 如果存在任何监听器，向它们发送响应详细信息。
     * 此方法用于通知所有注册的监听器已收到服务器响应。
     * <p>
     *
     * @param replyCode 从响应中提取的响应代码
     * @param reply 完整的响应文本
     * @since 3.0
     */
    protected void fireReplyReceived(int replyCode, String reply) {
        if (getCommandSupport().getListenerCount() > 0) {  // 如果有监听器
            getCommandSupport().fireReplyReceived(replyCode, reply);  // 触发响应接收事件
        }
    }

    /**
     * 如果存在任何监听器，向它们发送命令详细信息。
     * 此方法用于通知所有注册的监听器已发送命令到服务器。
     * <p>
     *
     * @param command 命令名称
     * @param message 完整的消息，包括命令名称
     * @since 3.0
     */
    protected void fireCommandSent(String command, String message) {
        if (getCommandSupport().getListenerCount() > 0) {  // 如果有监听器
            getCommandSupport().fireCommandSent(command, message);  // 触发命令发送事件
        }
    }

    /**
     * 如果需要，创建CommandSupport实例。
     * 此方法初始化协议命令支持对象，用于管理监听器和触发事件。
     */
    protected void createCommandSupport(){
        __commandSupport = new ProtocolCommandSupport(this);  // 创建新的命令支持对象
    }

    /**
     * 子类可以重写此方法，如果它们需要提供自己的实例字段以保持向后兼容性。
     * <p>
     *
     * @return CommandSupport实例，可能为 {@code null}
     * @since 3.0
     */
    protected ProtocolCommandSupport getCommandSupport() {
        return __commandSupport;  // 返回命令支持对象
    }

    /**
     * 设置用于所有连接的代理。
     * 代理用于在调用此方法后建立的连接。
     * <p>
     *
     * @param proxy 用于连接的新代理
     * @since 3.2
     */
    public void setProxy(Proxy proxy) {
        setSocketFactory(new DefaultSocketFactory(proxy));  // 创建使用代理的套接字工厂
        connProxy = proxy;  // 保存代理引用
    }

    /**
     * 获取用于所有连接的代理。
     * <p>
     *
     * @return 当前用于连接的代理
     */
    public Proxy getProxy() {
        return connProxy;  // 返回当前代理
    }

    /**
     * 获取字符集名称。
     * <p>
     *
     * @return 字符集名称
     * @since 3.3
     * @deprecated 由于代码现在要求Java 1.6作为最低版本，此方法已过时。
     *             请使用 {@link #getCharset()} 代替。
     */
    @Deprecated
    public String getCharsetName() {
        return charset.name();  // 返回字符集的名称
    }

    /**
     * 获取字符集。
     * <p>
     *
     * @return 字符集对象
     * @since 3.3
     */
    public Charset getCharset() {
        return charset;  // 返回字符集对象
    }

    /**
     * 设置字符集。
     * 该字符集用于字节IO操作。
     * <p>
     *
     * @param charset 要设置的字符集
     * @since 3.3
     */
    public void setCharset(Charset charset) {
        this.charset = charset;  // 保存字符集设置
    }

    /*
     * 注意：不能在不破坏二进制兼容性的情况下将字段提升到超类，
     * 因此需要此抽象方法将实例传递给移动到此处的方法。
     */
}


