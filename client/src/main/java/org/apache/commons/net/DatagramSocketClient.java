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

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;

/**
 * DatagramSocketClient 类为访问数据报套接字的客户端对象提供了基本操作。
 * 该类设计为被继承，以避免重复编写打开套接字、关闭套接字、设置超时等相同的代码。
 * 特别值得注意的是 {@link #setDatagramSocketFactory  setDatagramSocketFactory }
 * 方法，它允许你控制 DatagramSocketClient 为网络通信创建的 DatagramSocket 类型。
 * 这对于添加代理支持以及更好地支持小程序（applet）特别有用。
 * 例如，你可以创建一个 {@link org.apache.commons.net.DatagramSocketFactory}
 * 在创建套接字之前请求浏览器安全能力。
 * 所有派生自 DatagramSocketClient 的类都应该使用
 * {@link #_socketFactory_  _socketFactory_ } 成员变量来
 * 创建 DatagramSocket 实例，而不是直接调用构造函数实例化它们。
 * 遵守这个契约，你可以保证用户总是能够通过替换自己的 SocketFactory 来提供自己的 Socket 实现。
 *
 *
 * @see DatagramSocketFactory
 ***/

public abstract class DatagramSocketClient
{
    /**
     * 所有 DatagramSocketClient 实例共享的默认 DatagramSocketFactory。
     ***/
    private static final DatagramSocketFactory __DEFAULT_SOCKET_FACTORY =
        new DefaultDatagramSocketFactory();

    /**
     * 用于字节输入输出的字符集。
     */
    private Charset charset = Charset.defaultCharset();

    /**
     * 打开套接字后使用的超时时间（单位：毫秒）。
     ***/
    protected int _timeout_;

    /**
     * 用于连接的数据报套接字。
     ***/
    protected DatagramSocket _socket_;

    /**
     * 状态变量，指示客户端的套接字当前是否处于打开状态。
     ***/
    protected boolean _isOpen_;

    /**
     * 数据报套接字的 DatagramSocketFactory。
     ***/
    protected DatagramSocketFactory _socketFactory_;

    /**
     * DatagramSocketClient 的默认构造函数。
     * 初始化 _socket_ 为 null，_timeout_ 为 0，_isOpen_ 为 false。
     ***/
    public DatagramSocketClient()
    {
        _socket_ = null;
        _timeout_ = 0;
        _isOpen_ = false;
        _socketFactory_ = __DEFAULT_SOCKET_FACTORY;
    }


    /**
     * 在本地主机的第一个可用端口上打开一个 DatagramSocket。
     * 同时将套接字的超时时间设置为通过 {@link #setDefaultTimeout  setDefaultTimeout() } 设置的默认超时时间。
     * <p>
     * 调用此方法后，_isOpen_ 被设置为 true，_socket_ 被设置为新打开的套接字。
     *
     * @exception SocketException 如果套接字无法打开或无法设置超时时间。
     ***/
    public void open() throws SocketException
    {
        // 使用 socketFactory 创建数据报套接字
        _socket_ = _socketFactory_.createDatagramSocket();
        // 设置套接字的超时时间
        _socket_.setSoTimeout(_timeout_);
        // 标记套接字为打开状态
        _isOpen_ = true;
    }


    /**
     * 在本地主机的指定端口上打开一个 DatagramSocket。
     * 同时将套接字的超时时间设置为通过 {@link #setDefaultTimeout  setDefaultTimeout() } 设置的默认超时时间。
     * <p>
     * 调用此方法后，_isOpen_ 被设置为 true，_socket_ 被设置为新打开的套接字。
     *
     * @param port 套接字使用的端口号。
     * @exception SocketException 如果套接字无法打开或无法设置超时时间。
     ***/
    public void open(int port) throws SocketException
    {
        // 使用 socketFactory 在指定端口创建数据报套接字
        _socket_ = _socketFactory_.createDatagramSocket(port);
        // 设置套接字的超时时间
        _socket_.setSoTimeout(_timeout_);
        // 标记套接字为打开状态
        _isOpen_ = true;
    }


    /**
     * 在本地主机的指定地址和端口上打开一个 DatagramSocket。
     * 同时将套接字的超时时间设置为通过 {@link #setDefaultTimeout  setDefaultTimeout() } 设置的默认超时时间。
     * <p>
     * 调用此方法后，_isOpen_ 被设置为 true，_socket_ 被设置为新打开的套接字。
     *
     * @param port 套接字使用的端口号。
     * @param laddr 使用的本地地址。
     * @exception SocketException 如果套接字无法打开或无法设置超时时间。
     ***/
    public void open(int port, InetAddress laddr) throws SocketException
    {
        // 使用 socketFactory 在指定地址和端口创建数据报套接字
        _socket_ = _socketFactory_.createDatagramSocket(port, laddr);
        // 设置套接字的超时时间
        _socket_.setSoTimeout(_timeout_);
        // 标记套接字为打开状态
        _isOpen_ = true;
    }



    /**
     * 关闭用于连接的 DatagramSocket。
     * 你应该在完成使用类实例后调用此方法，并且在再次调用 {@link #open open() } 之前也要调用。
     * _isOpen_ 被设置为 false，_socket_ 被设置为 null。
     * 如果在客户端套接字未打开时调用此方法，将抛出 NullPointerException。
     ***/
    public void close()
    {
        // 检查套接字是否不为空，如果不为空则关闭它
        if (_socket_ != null) {
            _socket_.close();
        }
        // 将套接字引用设置为 null
        _socket_ = null;
        // 标记套接字为关闭状态
        _isOpen_ = false;
    }


    /**
     * 如果客户端当前有打开的套接字，则返回 true。
     *
     * @return 如果客户端当前有打开的套接字，则返回 true，否则返回 false。
     ***/
    public boolean isOpen()
    {
        return _isOpen_;
    }


    /**
     * 设置打开套接字时使用的默认超时时间（单位：毫秒）。
     * 在调用 open 方法后，套接字的超时时间将使用此值设置。
     * 此方法应该在调用 {@link #open open()} 之前使用，
     * 不要与 {@link #setSoTimeout setSoTimeout()} 混淆，
     * 后者是操作当前已打开的套接字。
     * _timeout_ 包含新的超时值。
     *
     * @param timeout  用于数据报套接字连接的超时时间（单位：毫秒）。
     ***/
    public void setDefaultTimeout(int timeout)
    {
        _timeout_ = timeout;
    }


    /**
     * 返回打开套接字时使用的默认超时时间（单位：毫秒）。
     *
     * @return 打开套接字时使用的默认超时时间（单位：毫秒）。
     ***/
    public int getDefaultTimeout()
    {
        return _timeout_;
    }


    /**
     * 设置当前打开连接的超时时间（单位：毫秒）。
     * 仅在通过 {@link #open open()} 打开连接后调用此方法。
     *
     * @param timeout  当前打开的数据报套接字连接使用的超时时间（单位：毫秒）。
     * @throws SocketException 如果设置超时时发生错误。
     ***/
    public void setSoTimeout(int timeout) throws SocketException
    {
        _socket_.setSoTimeout(timeout);
    }


    /**
     * 返回当前打开套接字的超时时间（单位：毫秒）。
     * 如果在客户端套接字未打开时调用此方法，将抛出 NullPointerException。
     *
     * @return 当前打开套接字的超时时间（单位：毫秒）。
     * @throws SocketException 如果获取超时时发生错误。
     ***/
    public int getSoTimeout() throws SocketException
    {
        return _socket_.getSoTimeout();
    }


    /**
     * 返回用于连接的本地主机上打开套接字的端口号。
     * 如果在客户端套接字未打开时调用此方法，将抛出 NullPointerException。
     *
     * @return 用于连接的本地主机上打开套接字的端口号。
     ***/
    public int getLocalPort()
    {
        return _socket_.getLocalPort();
    }


    /**
     * 返回客户端套接字绑定的本地地址。
     * 如果在客户端套接字未打开时调用此方法，将抛出 NullPointerException。
     *
     * @return 客户端套接字绑定的本地地址。
     ***/
    public InetAddress getLocalAddress()
    {
        return _socket_.getLocalAddress();
    }


    /**
     * 设置 DatagramSocketClient 用于打开 DatagramSockets 的 DatagramSocketFactory。
     * 如果 factory 参数为 null，则使用默认工厂（仅在之前修改过工厂后要重置工厂时才这样做）。
     *
     * @param factory  DatagramSocketClient 应该使用的新 DatagramSocketFactory。
     ***/
    public void setDatagramSocketFactory(DatagramSocketFactory factory)
    {
        // 如果传入的工厂为 null，则使用默认工厂
        if (factory == null) {
            _socketFactory_ = __DEFAULT_SOCKET_FACTORY;
        } else {
            // 否则使用传入的工厂
            _socketFactory_ = factory;
        }
    }

    /**
     * 获取字符集名称。
     *
     * @return 字符集名称。
     * @since 3.3
     * TODO 一旦代码要求 Java 1.6 作为最低版本，此方法将被废弃。
     */
    public String getCharsetName() {
        return charset.name();
    }

    /**
     * 获取字符集。
     *
     * @return 字符集对象。
     * @since 3.3
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * 设置字符集。
     *
     * @param charset 要设置的字符集。
     * @since 3.3
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
