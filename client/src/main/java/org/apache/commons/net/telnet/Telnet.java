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
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.SocketClient;

/**
 * Telnet协议客户端实现类
 *
 * 该类实现了Telnet协议的核心功能，继承自SocketClient。
 * Telnet是一种应用层协议，用于在TCP/IP网络上提供双向交互式文本导向通信功能。
 *
 * 主要功能包括：
 * - Telnet选项协商（Option Negotiation）
 * - 命令处理（Command Processing）
 * - 子协商处理（Subnegotiation Processing）
 * - 终端类型协商
 * - AYT（Are You There）支持
 * - 选项处理器管理
 * - 监视流支持
 *
 * Telnet协议关键概念：
 * - IAC (Interpret As Command): 字节值255，标识命令序列的开始
 * - WILL: 发送方表示愿意启用某个选项
 * - WONT: 发送方表示不愿意启用某个选项
 * - DO: 发送方请求接收方启用某个选项
 * - DONT: 发送方请求接收方不启用某个选项
 * - SB (Subnegotiation Begin): 开始子协商
 * - SE (Subnegotiation End): 结束子协商
 * - AYT (Are You There): 查询对方是否还在连接
 */
class Telnet extends SocketClient
{
    // 调试标志，用于启用/禁用调试输出
    static final boolean debug =  /*true;*/ false;

    // 选项调试标志，用于启用/禁用选项协商的调试输出
    static final boolean debugoptions =  /*true;*/ false;

    // DO命令的字节数组，格式为[IAC, DO]
    // DO命令表示发送方请求接收方启用某个选项
    static final byte[] _COMMAND_DO = {
                                          (byte)TelnetCommand.IAC, (byte)TelnetCommand.DO
                                      };

    // DONT命令的字节数组，格式为[IAC, DONT]
    // DONT命令表示发送方请求接收方不启用某个选项
    static final byte[] _COMMAND_DONT = {
                                            (byte)TelnetCommand.IAC, (byte)TelnetCommand.DONT
                                        };

    // WILL命令的字节数组，格式为[IAC, WILL]
    // WILL命令表示发送方愿意启用某个选项
    static final byte[] _COMMAND_WILL = {
                                            (byte)TelnetCommand.IAC, (byte)TelnetCommand.WILL
                                        };

    // WONT命令的字节数组，格式为[IAC, WONT]
    // WONT命令表示发送方不愿意启用某个选项
    static final byte[] _COMMAND_WONT = {
                                            (byte)TelnetCommand.IAC, (byte)TelnetCommand.WONT
                                        };

    // SB命令的字节数组，格式为[IAC, SB]
    // SB (Subnegotiation Begin) 命令表示开始子协商
    static final byte[] _COMMAND_SB = {
                                          (byte)TelnetCommand.IAC, (byte)TelnetCommand.SB
                                      };

    // SE命令的字节数组，格式为[IAC, SE]
    // SE (Subnegotiation End) 命令表示结束子协商
    static final byte[] _COMMAND_SE = {
                                          (byte)TelnetCommand.IAC, (byte)TelnetCommand.SE
                                      };

    // 选项状态掩码常量，用于跟踪选项的协商状态
    static final int _WILL_MASK = 0x01, _DO_MASK = 0x02,
                                  _REQUESTED_WILL_MASK = 0x04, _REQUESTED_DO_MASK = 0x08;

    /* public */
    // Telnet协议默认端口号
    static final int DEFAULT_PORT =  23;

    // DO响应计数数组，跟踪每个选项的DO响应次数
    // WILL响应计数数组，跟踪每个选项的WILL响应次数
    // 选项状态数组，存储每个选项的当前状态
    int[] _doResponse, _willResponse, _options;

    /* TERMINAL-TYPE option (start)*/
    // 终端类型选项代码，值为24
    // 这是Telnet选项协商中用于交换终端类型的标准选项
    protected static final int TERMINAL_TYPE = 24;

    // 终端类型发送命令，用于子协商
    // 值为1，表示请求发送终端类型信息
    protected static final int TERMINAL_TYPE_SEND =  1;

    // 终端类型标识命令，用于子协商
    // 值为0，表示接下来是终端类型数据
    protected static final int TERMINAL_TYPE_IS =  0;

    // IS命令序列，用于子协商中发送终端类型
    // 格式为[TERMINAL_TYPE, TERMINAL_TYPE_IS]
    static final byte[] _COMMAND_IS = {
                                          (byte) TERMINAL_TYPE, (byte) TERMINAL_TYPE_IS
                                      };

    // 终端类型字符串，例如"VT100"、"ANSI"等
    private String terminalType = null;
    /* TERMINAL-TYPE option (end)*/

    /* open TelnetOptionHandler functionality (start)*/
    // 选项处理器数组，用于处理各种Telnet选项
    // 每个选项可以有一个对应的处理器来管理该选项的行为
    private final TelnetOptionHandler optionHandlers[];

    /* open TelnetOptionHandler functionality (end)*/

    /* Code Section added for supporting AYT (start)*/
    // AYT (Are You There) 命令序列
    // 用于查询对方是否仍在连接，格式为[IAC, AYT]
    static final byte[] _COMMAND_AYT = {
                                          (byte) TelnetCommand.IAC, (byte) TelnetCommand.AYT
                                       };

    // AYT响应监视器，用于等待AYT响应
    private final Object aytMonitor = new Object();

    // AYT标志，标识是否收到AYT响应
    // volatile确保多线程可见性
    private volatile boolean aytFlag = true;
    /* Code Section added for supporting AYT (end)*/

    // 监视流，用于记录Telnet会话的所有活动
    // 所有发送和接收的数据都会被写入此流
    private volatile OutputStream spyStream = null;

    // 通知处理器，用于接收Telnet选项协商事件的通知
    private TelnetNotificationHandler __notifhand = null;
    /**
     * 默认构造函数
     *
     * 初始化Telnet客户端，设置默认端口为23，
     * 并初始化所有选项相关的数组。
     */
    Telnet()
    {
        // 设置默认端口为23（Telnet标准端口）
        setDefaultPort(DEFAULT_PORT);
        // 初始化DO响应计数数组，大小为最大选项值+1
        _doResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        // 初始化WILL响应计数数组，大小为最大选项值+1
        _willResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        // 初始化选项状态数组，大小为最大选项值+1
        _options = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        // 初始化选项处理器数组，大小为最大选项值+1
        optionHandlers =
            new TelnetOptionHandler[TelnetOption.MAX_OPTION_VALUE + 1];
    }

    /* TERMINAL-TYPE option (start)*/
    /**
     * 带终端类型参数的构造函数
     *
     * 创建一个指定终端类型的Telnet客户端。
     * 终端类型将在选项协商时发送给服务器。
     *
     * @param termtype - 要协商的终端类型（例如：VT100、ANSI等）
     */
    Telnet(String termtype)
    {
        // 设置默认端口为23
        setDefaultPort(DEFAULT_PORT);
        // 初始化响应和选项数组
        _doResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        _willResponse = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        _options = new int[TelnetOption.MAX_OPTION_VALUE + 1];
        // 设置终端类型
        terminalType = termtype;
        // 初始化选项处理器数组
        optionHandlers =
            new TelnetOptionHandler[TelnetOption.MAX_OPTION_VALUE + 1];
    }
    /* TERMINAL-TYPE option (end)*/

    /**
     * 检查选项的WILL状态
     *
     * 判断指定选项是否已确认启用WILL状态。
     * WILL表示本地端愿意启用该选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果WILL已被确认返回true，否则返回false
     */
    boolean _stateIsWill(int option)
    {
        // 使用位掩码检查选项状态中是否设置了WILL位
        return ((_options[option] & _WILL_MASK) != 0);
    }

    /**
     * 检查选项的WONT状态
     *
     * 判断指定选项是否处于WONT状态。
     * WONT表示本地端不愿意启用该选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果WONT已被确认返回true，否则返回false
     */
    boolean _stateIsWont(int option)
    {
        // WONT状态与WILL状态互斥
        return !_stateIsWill(option);
    }

    /**
     * 检查选项的DO状态
     *
     * 判断指定选项是否已确认启用DO状态。
     * DO表示远程端已同意启用该选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果DO已被确认返回true，否则返回false
     */
    boolean _stateIsDo(int option)
    {
        // 使用位掩码检查选项状态中是否设置了DO位
        return ((_options[option] & _DO_MASK) != 0);
    }

    /**
     * 检查选项的DONT状态
     *
     * 判断指定选项是否处于DONT状态。
     * DONT表示远程端不同意启用该选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果DONT已被确认返回true，否则返回false
     */
    boolean _stateIsDont(int option)
    {
        // DONT状态与DO状态互斥
        return !_stateIsDo(option);
    }

    /**
     * 检查是否已请求WILL状态
     *
     * 判断是否已向远程端请求启用指定选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果已请求WILL返回true，否则返回false
     */
    boolean _requestedWill(int option)
    {
        // 使用位掩码检查是否设置了REQUESTED_WILL位
        return ((_options[option] & _REQUESTED_WILL_MASK) != 0);
    }

    /**
     * 检查是否已请求WONT状态
     *
     * 判断是否已向远程端请求禁用指定选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果已请求WONT返回true，否则返回false
     */
    boolean _requestedWont(int option)
    {
        // WONT请求状态与WILL请求状态互斥
        return !_requestedWill(option);
    }

    /**
     * 检查是否已请求DO状态
     *
     * 判断是否已请求远程端启用指定选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果已请求DO返回true，否则返回false
     */
    boolean _requestedDo(int option)
    {
        // 使用位掩码检查是否设置了REQUESTED_DO位
        return ((_options[option] & _REQUESTED_DO_MASK) != 0);
    }

    /**
     * 检查是否已请求DONT状态
     *
     * 判断是否已请求远程端禁用指定选项。
     *
     * @param option - 要查询的选项代码
     * @return 如果已请求DONT返回true，否则返回false
     */
    boolean _requestedDont(int option)
    {
        // DONT请求状态与DO请求状态互斥
        return !_requestedDo(option);
    }

    /**
     * 设置选项的WILL状态
     *
     * 将指定选项设置为WILL状态，表示本地端愿意启用该选项。
     * 如果已请求WILL且存在选项处理器，则触发处理器的子协商。
     *
     * @param option - 要设置的选项代码
     * @throws IOException 如果在子协商过程中发生I/O错误
     */
    void _setWill(int option) throws IOException
    {
        // 设置WILL位，表示启用该选项
        _options[option] |= _WILL_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        // 如果之前已请求WILL状态
        if (_requestedWill(option))
        {
            // 如果该选项有注册的处理器
            if (optionHandlers[option] != null)
            {
                // 通知处理器WILL状态已设置
                optionHandlers[option].setWill(true);

                // 获取处理器要发送的子协商数据
                int subneg[] =
                    optionHandlers[option].startSubnegotiationLocal();

                // 如果有子协商数据，则发送
                if (subneg != null)
                {
                    _sendSubnegotiation(subneg);
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /**
     * 设置选项的DO状态
     *
     * 将指定选项设置为DO状态，表示远程端已同意启用该选项。
     * 如果已请求DO且存在选项处理器，则触发处理器的子协商。
     *
     * @param option - 要设置的选项代码
     * @throws IOException 如果在子协商过程中发生I/O错误
     */
    void _setDo(int option) throws IOException
    {
        // 设置DO位，表示远程端同意启用该选项
        _options[option] |= _DO_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        // 如果之前已请求DO状态
        if (_requestedDo(option))
        {
            // 如果该选项有注册的处理器
            if (optionHandlers[option] != null)
            {
                // 通知处理器DO状态已设置
                optionHandlers[option].setDo(true);

                // 获取处理器要发送的子协商数据（针对远程端）
                int subneg[] =
                    optionHandlers[option].startSubnegotiationRemote();

                // 如果有子协商数据，则发送
                if (subneg != null)
                {
                    _sendSubnegotiation(subneg);
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /**
     * 设置请求WILL状态标志
     *
     * 标记已请求启用指定选项（本地端）。
     *
     * @param option - 要设置的选项代码
     */
    void _setWantWill(int option)
    {
        // 设置REQUESTED_WILL位，表示已请求WILL
        _options[option] |= _REQUESTED_WILL_MASK;
    }

    /**
     * 设置请求DO状态标志
     *
     * 标记已请求启用指定选项（远程端）。
     *
     * @param option - 要设置的选项代码
     */
    void _setWantDo(int option)
    {
        // 设置REQUESTED_DO位，表示已请求DO
        _options[option] |= _REQUESTED_DO_MASK;
    }

    /**
     * 设置选项的WONT状态
     *
     * 将指定选项设置为WONT状态，表示本地端不愿意启用该选项。
     *
     * @param option - 要设置的选项代码
     */
    void _setWont(int option)
    {
        // 清除WILL位，设置为WONT状态
        _options[option] &= ~_WILL_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        // 如果该选项有注册的处理器，通知它WONT状态
        if (optionHandlers[option] != null)
        {
            optionHandlers[option].setWill(false);
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /**
     * 设置选项的DONT状态
     *
     * 将指定选项设置为DONT状态，表示远程端不同意启用该选项。
     *
     * @param option - 要设置的选项代码
     */
    void _setDont(int option)
    {
        // 清除DO位，设置为DONT状态
        _options[option] &= ~_DO_MASK;

        /* open TelnetOptionHandler functionality (start)*/
        // 如果该选项有注册的处理器，通知它DONT状态
        if (optionHandlers[option] != null)
        {
            optionHandlers[option].setDo(false);
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /**
     * 清除请求WILL状态标志
     *
     * 清除已请求启用指定选项的标志（本地端）。
     *
     * @param option - 要设置的选项代码
     */
    void _setWantWont(int option)
    {
        // 清除REQUESTED_WILL位
        _options[option] &= ~_REQUESTED_WILL_MASK;
    }

    /**
     * 清除请求DO状态标志
     *
     * 清除已请求启用指定选项的标志（远程端）。
     *
     * @param option - 要设置的选项代码
     */
    void _setWantDont(int option)
    {
        // 清除REQUESTED_DO位
        _options[option] &= ~_REQUESTED_DO_MASK;
    }

    /**
     * 处理接收到的Telnet命令
     *
     * 处理从远程端接收到的Telnet命令，并通知注册的监听器。
     *
     * @param command - 接收到的命令代码
     */
    void _processCommand(int command)
    {
        // 如果启用了选项调试，输出接收到的命令
        if (debugoptions)
        {
            System.err.println("RECEIVED COMMAND: " + command);
        }

        // 如果注册了通知处理器，通知它接收到命令
        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_COMMAND, command);
        }
    }

    /**
     * 处理接收到的DO请求
     *
     * DO请求表示远程端请求本地端启用某个选项。
     * 此方法决定是否接受该请求，并发送相应的响应。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在处理过程中发生I/O错误
     */
    void _processDo(int option) throws IOException
    {
        // 如果启用了选项调试，输出接收到的DO请求
        if (debugoptions)
        {
            System.err.println("RECEIVED DO: "
                + TelnetOption.getOption(option));
        }

        // 如果注册了通知处理器，通知它接收到DO请求
        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_DO,
                option);
        }

        // 标识是否接受新的状态
        boolean acceptNewState = false;


        /* open TelnetOptionHandler functionality (start)*/
        // 检查是否有该选项的处理器
        if (optionHandlers[option] != null)
        {
            // 询问处理器是否接受本地启用该选项
            acceptNewState = optionHandlers[option].getAcceptLocal();
        }
        else
        {
        /* open TelnetOptionHandler functionality (end)*/
            /* TERMINAL-TYPE option (start)*/
            // 如果是终端类型选项且有有效的终端类型，则接受
            if (option == TERMINAL_TYPE)
            {
                if ((terminalType != null) && (terminalType.length() > 0))
                {
                    acceptNewState = true;
                }
            }
            /* TERMINAL-TYPE option (end)*/
        /* open TelnetOptionHandler functionality (start)*/
        }
        /* open TelnetOptionHandler functionality (end)*/

        // 处理响应计数，防止无限循环
        if (_willResponse[option] > 0)
        {
            --_willResponse[option];
            if (_willResponse[option] > 0 && _stateIsWill(option))
            {
                --_willResponse[option];
            }
        }

        // 如果响应计数归零，处理协商逻辑
        if (_willResponse[option] == 0)
        {
            // 如果之前请求了WONT状态
            if (_requestedWont(option))
            {

                switch (option)
                {

                default:
                    break;

                }


                // 根据是否接受新状态，发送WILL或WONT响应
                if (acceptNewState)
                {
                    // 接受：设置想要WILL并发送WILL
                    _setWantWill(option);
                    _sendWill(option);
                }
                else
                {
                    // 拒绝：发送WONT
                    ++_willResponse[option];
                    _sendWont(option);
                }
            }
            else
            {
                // 远程端已经确认了选项

                switch (option)
                {

                default:
                    break;

                }

            }
        }

        // 最终设置WILL状态
        _setWill(option);
    }

    /**
     * 处理接收到的DONT请求
     *
     * DONT请求表示远程端请求本地端禁用某个选项。
     * 此方法处理该请求并发送WONT响应。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在处理过程中发生I/O错误
     */
    void _processDont(int option) throws IOException
    {
        // 如果启用了选项调试，输出接收到的DONT请求
        if (debugoptions)
        {
            System.err.println("RECEIVED DONT: "
                + TelnetOption.getOption(option));
        }
        // 如果注册了通知处理器，通知它接收到DONT请求
        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_DONT,
                option);
        }
        // 处理响应计数
        if (_willResponse[option] > 0)
        {
            --_willResponse[option];
            if (_willResponse[option] > 0 && _stateIsWont(option))
            {
                --_willResponse[option];
            }
        }

        // 如果响应计数归零且之前请求了WILL状态
        if (_willResponse[option] == 0 && _requestedWill(option))
        {

            switch (option)
            {

            default:
                break;

            }

            /* FIX for a BUG in the negotiation (start)*/
            // 修复协商中的BUG：如果当前是WILL状态或请求了WILL，需要发送WONT
            if ((_stateIsWill(option)) || (_requestedWill(option)))
            {
                _sendWont(option);
            }

            // 设置想要WONT状态
            _setWantWont(option);
            /* FIX for a BUG in the negotiation (end)*/
        }

        // 最终设置WONT状态
        _setWont(option);
    }


    /**
     * 处理接收到的WILL请求
     *
     * WILL请求表示远程端愿意启用某个选项。
     * 此方法决定是否接受该请求，并发送相应的响应。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在处理过程中发生I/O错误
     */
    void _processWill(int option) throws IOException
    {
        // 如果启用了选项调试，输出接收到的WILL请求
        if (debugoptions)
        {
            System.err.println("RECEIVED WILL: "
                + TelnetOption.getOption(option));
        }

        // 如果注册了通知处理器，通知它接收到WILL请求
        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_WILL,
                option);
        }

        // 标识是否接受新的状态
        boolean acceptNewState = false;

        /* open TelnetOptionHandler functionality (start)*/
        // 检查是否有该选项的处理器
        if (optionHandlers[option] != null)
        {
            // 询问处理器是否接受远程启用该选项
            acceptNewState = optionHandlers[option].getAcceptRemote();
        }
        /* open TelnetOptionHandler functionality (end)*/

        // 处理响应计数
        if (_doResponse[option] > 0)
        {
            --_doResponse[option];
            if (_doResponse[option] > 0 && _stateIsDo(option))
            {
                --_doResponse[option];
            }
        }

        // 如果响应计数归零且之前请求了DONT状态
        if (_doResponse[option] == 0 && _requestedDont(option))
        {

            switch (option)
            {

            default:
                break;

            }


            // 根据是否接受新状态，发送DO或DONT响应
            if (acceptNewState)
            {
                // 接受：设置想要DO并发送DO
                _setWantDo(option);
                _sendDo(option);
            }
            else
            {
                // 拒绝：发送DONT
                ++_doResponse[option];
                _sendDont(option);
            }
        }

        // 最终设置DO状态
        _setDo(option);
    }

    /**
     * 处理接收到的WONT请求
     *
     * WONT请求表示远程端不愿意启用某个选项。
     * 此方法处理该请求并发送DONT响应。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在处理过程中发生I/O错误
     */
    void _processWont(int option) throws IOException
    {
        // 如果启用了选项调试，输出接收到的WONT请求
        if (debugoptions)
        {
            System.err.println("RECEIVED WONT: "
                + TelnetOption.getOption(option));
        }

        // 如果注册了通知处理器，通知它接收到WONT请求
        if (__notifhand != null)
        {
            __notifhand.receivedNegotiation(
                TelnetNotificationHandler.RECEIVED_WONT,
                option);
        }

        // 处理响应计数
        if (_doResponse[option] > 0)
        {
            --_doResponse[option];
            if (_doResponse[option] > 0 && _stateIsDont(option))
            {
                --_doResponse[option];
            }
        }

        // 如果响应计数归零且之前请求了DO状态
        if (_doResponse[option] == 0 && _requestedDo(option))
        {

            switch (option)
            {

            default:
                break;

            }

            /* FIX for a BUG in the negotiation (start)*/
            // 修复协商中的BUG：如果当前是DO状态或请求了DO，需要发送DONT
            if ((_stateIsDo(option)) || (_requestedDo(option)))
            {
                _sendDont(option);
            }

            // 设置想要DONT状态
            _setWantDont(option);
            /* FIX for a BUG in the negotiation (end)*/
        }

        // 最终设置DONT状态
        _setDont(option);
    }

    /* TERMINAL-TYPE option (start)*/
    /**
     * 处理子协商数据
     *
     * 处理从远程端接收到的子协商数据。
     * 子协商用于在选项启用后传输额外的配置信息。
     *
     * @param suboption - 接收到的子协商数据数组
     * @param suboptionLength - 子协商数据的长度
     * @throws IOException - 如果在处理过程中发生I/O错误
     */
    void _processSuboption(int suboption[], int suboptionLength)
    throws IOException
    {
        // 如果启用了调试，输出子协商信息
        if (debug)
        {
            System.err.println("PROCESS SUBOPTION.");
        }

        /* open TelnetOptionHandler functionality (start)*/
        // 确保子协商数据有效
        if (suboptionLength > 0)
        {
            // 检查是否有该选项的处理器
            if (optionHandlers[suboption[0]] != null)
            {
                // 让处理器处理子协商并获取响应
                int responseSuboption[] =
                  optionHandlers[suboption[0]].answerSubnegotiation(suboption,
                  suboptionLength);
                // 发送响应子协商
                _sendSubnegotiation(responseSuboption);
            }
            else
            {
                // 如果没有处理器，处理内置的子协商类型
                if (suboptionLength > 1)
                {
                    // 调试模式下输出所有子协商字节
                    if (debug)
                    {
                        for (int ii = 0; ii < suboptionLength; ii++)
                        {
                            System.err.println("SUB[" + ii + "]: "
                                + suboption[ii]);
                        }
                    }
                    // 处理终端类型请求
                    if ((suboption[0] == TERMINAL_TYPE)
                        && (suboption[1] == TERMINAL_TYPE_SEND))
                    {
                        // 发送终端类型信息
                        _sendTerminalType();
                    }
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /**
     * 发送终端类型信息
     *
     * 将本地终端类型通过子协商发送给远程端。
     * 格式为：IAC SB TERMINAL-TYPE TERMINAL-TYPE-IS <终端类型> IAC SE
     *
     * @throws IOException - 如果在发送过程中发生I/O错误
     */
    final synchronized void _sendTerminalType()
    throws IOException
    {
        // 如果启用了调试，输出要发送的终端类型
        if (debug)
        {
            System.err.println("SEND TERMINAL-TYPE: " + terminalType);
        }
        // 确保终端类型不为空
        if (terminalType != null)
        {
            // 发送子协商开始命令
            _output_.write(_COMMAND_SB);
            // 发送IS命令序列
            _output_.write(_COMMAND_IS);
            // 发送终端类型字节数据
            _output_.write(terminalType.getBytes(getCharset()));
            // 发送子协商结束命令
            _output_.write(_COMMAND_SE);
            // 刷新输出流
            _output_.flush();
        }
    }

    /* TERMINAL-TYPE option (end)*/

    /* open TelnetOptionHandler functionality (start)*/
    /**
     * 发送子协商数据
     *
     * 发送子协商数据给远程端。IAC字节需要被转义（双写）。
     * 格式为：IAC SB <数据...> IAC SE
     *
     * @param subn - 要发送的子协商数据数组
     * @throws IOException - 如果在发送过程中发生I/O错误
     */
    final synchronized void _sendSubnegotiation(int subn[])
    throws IOException
    {
        // 如果启用了调试，输出要发送的子协商数据
        if (debug)
        {
            System.err.println("SEND SUBNEGOTIATION: ");
            if (subn != null)
            {
                System.err.println(Arrays.toString(subn));
            }
        }
        // 确保子协商数据不为空
        if (subn != null)
        {
            // 发送子协商开始命令
            _output_.write(_COMMAND_SB);
            // 遍历所有子协商字节并写入
            // 注意：_output_是缓冲流，可以逐字节写入以简化逻辑
            for (int element : subn)
            {
                byte b = (byte) element;
                // 如果字节是IAC，需要双写以进行转义
                // 强制转换是必要的，因为IAC超出有符号字节范围
                if (b == (byte) TelnetCommand.IAC) {
                    _output_.write(b); // 双写IAC字节
                }
                _output_.write(b);
            }
            // 发送子协商结束命令
            _output_.write(_COMMAND_SE);

            /* Code Section added for sending the negotiation ASAP (start)*/
            // 立即刷新输出流，确保协商尽快发送
            _output_.flush();
            /* Code Section added for sending the negotiation ASAP (end)*/
        }
    }
    /* open TelnetOptionHandler functionality (end)*/

    /**
     * 发送命令
     *
     * 发送一个Telnet命令，自动添加IAC前缀并刷新输出。
     *
     * @param cmd - 要发送的命令字节
     * @throws IOException - 如果在发送过程中发生I/O错误
     * @since 3.0
     */
    final synchronized void _sendCommand(byte cmd) throws IOException
    {
            // 写入IAC前缀
            _output_.write(TelnetCommand.IAC);
            // 写入命令字节
            _output_.write(cmd);
            // 刷新输出流
            _output_.flush();
    }

    /* Code Section added for supporting AYT (start)*/
    /**
     * 处理AYT响应
     *
     * 当接收到AYT（Are You There）响应时调用此方法。
     * 唤醒等待AYT响应的线程。
     */
    final synchronized void _processAYTResponse()
    {
        // 如果AYT标志为false（表示正在等待响应）
        if (!aytFlag)
        {
            synchronized (aytMonitor)
            {
                // 设置标志为true，表示已收到响应
                aytFlag = true;
                // 唤醒所有等待的线程
                aytMonitor.notifyAll();
            }
        }
    }
    /* Code Section added for supporting AYT (end)*/

    /**
     * 连接建立时的初始化操作
     *
     * 在建立Telnet连接时调用此方法。
     * 清除之前的选项状态，初始化输入输出流，
     * 并启动已注册选项处理器的初始化协商。
     *
     * @throws IOException - 如果在连接过程中发生I/O错误
     */
    @Override
    protected void _connectAction_() throws IOException
    {
        /* (start). BUGFIX: clean the option info for each connection*/
        // 清除所有选项的状态信息，确保每次连接都是干净的
        for (int ii = 0; ii < TelnetOption.MAX_OPTION_VALUE + 1; ii++)
        {
            _doResponse[ii] = 0;           // 清除DO响应计数
            _willResponse[ii] = 0;         // 清除WILL响应计数
            _options[ii] = 0;              // 清除选项状态
            // 如果有选项处理器，重置其状态
            if (optionHandlers[ii] != null)
            {
                optionHandlers[ii].setDo(false);
                optionHandlers[ii].setWill(false);
            }
        }
        /* (end). BUGFIX: clean the option info for each connection*/

        // 调用父类的连接操作
        super._connectAction_();
        // 使用缓冲流包装输入输出流以提高性能
        _input_ = new BufferedInputStream(_input_);
        _output_ = new BufferedOutputStream(_output_);

        /* open TelnetOptionHandler functionality (start)*/
        // 遍历所有选项处理器，启动需要初始化的选项
        for (int ii = 0; ii < TelnetOption.MAX_OPTION_VALUE + 1; ii++)
        {
            if (optionHandlers[ii] != null)
            {
                // 如果选项需要本地初始化，请求WILL
                if (optionHandlers[ii].getInitLocal())
                {
                    _requestWill(optionHandlers[ii].getOptionCode());
                }

                // 如果选项需要远程初始化，请求DO
                if (optionHandlers[ii].getInitRemote())
                {
                    _requestDo(optionHandlers[ii].getOptionCode());
                }
            }
        }
        /* open TelnetOptionHandler functionality (end)*/
    }

    /**
     * 发送DO命令
     *
     * 向远程端发送DO命令，请求远程端启用指定选项。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在发送过程中发生I/O错误
     */
    final synchronized void _sendDo(int option)
    throws IOException
    {
        // 如果启用了调试，输出DO命令信息
        if (debug || debugoptions)
        {
            System.err.println("DO: " + TelnetOption.getOption(option));
        }
        // 写入DO命令（IAC + DO）
        _output_.write(_COMMAND_DO);
        // 写入选项代码
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        // 立即刷新，确保协商尽快发送
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * 请求DO状态
     *
     * 向远程端请求启用指定选项。
     * 如果选项已经是DO状态或已请求DO，则不重复请求。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在请求过程中发生I/O错误
     */
    final synchronized void _requestDo(int option)
    throws IOException
    {
        // 如果选项已经是DO状态或已请求DO，直接返回
        if ((_doResponse[option] == 0 && _stateIsDo(option))
            || _requestedDo(option))
        {
            return ;
        }
        // 设置想要DO状态
        _setWantDo(option);
        // 增加响应计数
        ++_doResponse[option];
        // 发送DO命令
        _sendDo(option);
    }

    /**
     * 发送DONT命令
     *
     * 向远程端发送DONT命令，请求远程端禁用指定选项。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在发送过程中发生I/O错误
     */
    final synchronized void _sendDont(int option)
    throws IOException
    {
        // 如果启用了调试，输出DONT命令信息
        if (debug || debugoptions)
        {
            System.err.println("DONT: " + TelnetOption.getOption(option));
        }
        // 写入DONT命令（IAC + DONT）
        _output_.write(_COMMAND_DONT);
        // 写入选项代码
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        // 立即刷新，确保协商尽快发送
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * 请求DONT状态
     *
     * 向远程端请求禁用指定选项。
     * 如果选项已经是DONT状态或已请求DONT，则不重复请求。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在请求过程中发生I/O错误
     */
    final synchronized void _requestDont(int option)
    throws IOException
    {
        // 如果选项已经是DONT状态或已请求DONT，直接返回
        if ((_doResponse[option] == 0 && _stateIsDont(option))
            || _requestedDont(option))
        {
            return ;
        }
        // 设置想要DONT状态
        _setWantDont(option);
        // 增加响应计数
        ++_doResponse[option];
        // 发送DONT命令
        _sendDont(option);
    }


    /**
     * 发送WILL命令
     *
     * 向远程端发送WILL命令，表示本地端愿意启用指定选项。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在发送过程中发生I/O错误
     */
    final synchronized void _sendWill(int option)
    throws IOException
    {
        // 如果启用了调试，输出WILL命令信息
        if (debug || debugoptions)
        {
            System.err.println("WILL: " + TelnetOption.getOption(option));
        }
        // 写入WILL命令（IAC + WILL）
        _output_.write(_COMMAND_WILL);
        // 写入选项代码
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        // 立即刷新，确保协商尽快发送
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * 请求WILL状态
     *
     * 向远程端表明本地端愿意启用指定选项。
     * 如果选项已经是WILL状态或已请求WILL，则不重复请求。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在请求过程中发生I/O错误
     */
    final synchronized void _requestWill(int option)
    throws IOException
    {
        // 如果选项已经是WILL状态或已请求WILL，直接返回
        if ((_willResponse[option] == 0 && _stateIsWill(option))
            || _requestedWill(option))
        {
            return ;
        }
        // 设置想要WILL状态
        _setWantWill(option);
        // 增加响应计数
        ++_doResponse[option];
        _sendWill(option);
    }

    /**
     * 发送WONT命令
     *
     * 向远程端发送WONT命令，表示本地端不愿意启用指定选项。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在发送过程中发生I/O错误
     */
    final synchronized void _sendWont(int option)
    throws IOException
    {
        // 如果启用了调试，输出WONT命令信息
        if (debug || debugoptions)
        {
            System.err.println("WONT: " + TelnetOption.getOption(option));
        }
        // 写入WONT命令（IAC + WONT）
        _output_.write(_COMMAND_WONT);
        // 写入选项代码
        _output_.write(option);

        /* Code Section added for sending the negotiation ASAP (start)*/
        // 立即刷新，确保协商尽快发送
        _output_.flush();
        /* Code Section added for sending the negotiation ASAP (end)*/
    }

    /**
     * 请求WONT状态
     *
     * 向远程端表明本地端不愿意启用指定选项。
     * 如果选项已经是WONT状态或已请求WONT，则不重复请求。
     *
     * @param option - 选项代码
     * @throws IOException - 如果在请求过程中发生I/O错误
     */
    final synchronized void _requestWont(int option)
    throws IOException
    {
        // 如果选项已经是WONT状态或已请求WONT，直接返回
        if ((_willResponse[option] == 0 && _stateIsWont(option))
            || _requestedWont(option))
        {
            return ;
        }
        // 设置想要WONT状态
        _setWantWont(option);
        // 增加响应计数
        ++_doResponse[option];
        // 发送WONT命令
        _sendWont(option);
    }

    /**
     * 发送单个字节
     *
     * 向远程端发送一个字节，并记录到监视流（如果启用）。
     *
     * @param b - 要发送的字节
     * @throws IOException - 如果在发送过程中发生I/O错误
     */
    final synchronized void _sendByte(int b)
    throws IOException
    {
        // 写入字节到输出流
        _output_.write(b);

        /* Code Section added for supporting spystreams (start)*/
        // 如果启用了监视流，记录写入的字节
        _spyWrite(b);
        /* Code Section added for supporting spystreams (end)*/

    }

    /* Code Section added for supporting AYT (start)*/
    /**
     * 发送AYT（Are You There）序列并等待响应
     *
     * 发送AYT命令查询远程端是否仍在连接，并等待响应。
     *
     * @param timeout - 等待响应的超时时间（毫秒）
     * @return 如果收到AYT响应返回true，否则返回false
     * @throws IOException - 如果在发送过程中发生I/O错误
     * @throws IllegalArgumentException - 如果参数非法
     * @throws InterruptedException - 如果在等待过程中被中断
     */
    final boolean _sendAYT(long timeout)
    throws IOException, IllegalArgumentException, InterruptedException
    {
        boolean retValue = false;
        // 在AYT监视器上同步
        synchronized (aytMonitor)
        {
            // 在this对象上同步，确保线程安全
            synchronized (this)
            {
                // 设置AYT标志为false，表示正在等待响应
                aytFlag = false;
                // 发送AYT命令
                _output_.write(_COMMAND_AYT);
                // 刷新输出流
                _output_.flush();
            }
            // 等待响应，最多等待timeout毫秒
            aytMonitor.wait(timeout);
            // 检查是否收到响应
            if (!aytFlag)
            {
                // 超时未收到响应
                retValue = false;
                aytFlag = true;
            }
            else
            {
                // 收到响应
                retValue = true;
            }
        }

        // 返回是否收到响应
        return (retValue);
    }
    /* Code Section added for supporting AYT (end)*/

    /* open TelnetOptionHandler functionality (start)*/

    /**
     * 注册Telnet选项处理器
     *
     * 为指定的Telnet选项注册一个处理器，用于管理该选项的行为。
     * 如果已经连接，将根据处理器的配置启动选项协商。
     *
     * @param opthand - 要注册的选项处理器
     * @throws InvalidTelnetOptionException - 如果选项代码无效
     * @throws IOException - 如果在协商过程中发生I/O错误
     */
    void addOptionHandler(TelnetOptionHandler opthand)
    throws InvalidTelnetOptionException, IOException
    {
        // 获取选项代码
        int optcode = opthand.getOptionCode();
        // 验证选项代码是否有效
        if (TelnetOption.isValidOption(optcode))
        {
            // 检查该选项是否已有处理器
            if (optionHandlers[optcode] == null)
            {
                // 注册选项处理器
                optionHandlers[optcode] = opthand;
                // 如果已经连接，根据处理器配置启动协商
                if (isConnected())
                {
                    // 如果处理器需要本地初始化，请求WILL
                    if (opthand.getInitLocal())
                    {
                        _requestWill(optcode);
                    }

                    // 如果处理器需要远程初始化，请求DO
                    if (opthand.getInitRemote())
                    {
                        _requestDo(optcode);
                    }
                }
            }
            else
            {
                // 选项已被注册
                throw (new InvalidTelnetOptionException(
                    "Already registered option", optcode));
            }
        }
        else
        {
            // 选项代码无效
            throw (new InvalidTelnetOptionException(
                "Invalid Option Code", optcode));
        }
    }

    /**
     * 注销Telnet选项处理器
     *
     * 注销指定选项的处理器，并禁用该选项。
     *
     * @param optcode - 要注销的选项代码
     * @throws InvalidTelnetOptionException - 如果选项代码无效或未注册
     * @throws IOException - 如果在协商过程中发生I/O错误
     */
    void deleteOptionHandler(int optcode)
    throws InvalidTelnetOptionException, IOException
    {
        // 验证选项代码是否有效
        if (TelnetOption.isValidOption(optcode))
        {
            // 检查该选项是否已注册处理器
            if (optionHandlers[optcode] == null)
            {
                // 选项未注册
                throw (new InvalidTelnetOptionException(
                    "Unregistered option", optcode));
            }
            else
            {
                // 获取当前处理器
                TelnetOptionHandler opthand = optionHandlers[optcode];
                // 移除处理器
                optionHandlers[optcode] = null;

                // 如果选项处于WILL状态，请求WONT
                if (opthand.getWill())
                {
                    _requestWont(optcode);
                }

                if (opthand.getDo())
                {
                    _requestDont(optcode);
                }
            }
        }
        else
        {
            throw (new InvalidTelnetOptionException(
                "Invalid Option Code", optcode));
        }
    }
    /* open TelnetOptionHandler functionality (end)*/

    /* Code Section added for supporting spystreams (start)*/
    /**
     * 注册监视流
     *
     * 注册一个输出流用于监视Telnet会话的所有活动。
     * 所有发送和接收的数据都会被写入此流。
     *
     * @param spystream - 用于回显会话活动的输出流
     */
    void _registerSpyStream(OutputStream  spystream)
    {
        spyStream = spystream;
    }

    /**
     * 停止监视
     *
     * 停止对当前Telnet会话的监视。
     */
    void _stopSpyStream()
    {
        spyStream = null;
    }

    /**
     * 将读取的字符写入监视流
     *
     * 当从会话中读取字符时，将其写入监视流。
     * 处理换行符的转换（\n前添加\r）。
     *
     * @param ch - 从会话中读取的字符
     */
    void _spyRead(int ch)
    {
        OutputStream spy = spyStream;
        if (spy != null)
        {
            try
            {
                // 不单独写入'\r'
                if (ch != '\r')
                {
                    // 如果是换行符，先写入'\r'
                    if (ch == '\n')
                    {
                        spy.write('\r'); // 在'\n'之前添加'\r'
                    }
                    // 写入原始字符
                    spy.write(ch);
                    // 刷新流
                    spy.flush();
                }
            }
            catch (IOException e)
            {
                // 如果发生错误，清除监视流
                spyStream = null;
            }
        }
    }

    /**
     * 将写入的字符写入监视流
     *
     * 当向会话写入字符时，将其写入监视流。
     * 如果启用了ECHO选项，则不记录写入的字符（因为会从远程端回显）。
     *
     * @param ch - 写入会话的字符
     */
    void _spyWrite(int ch)
    {
        // 如果启用了ECHO选项，不记录写入的字符
        // 因为远程端会回显这些字符
        if (!(_stateIsDo(TelnetOption.ECHO)
            && _requestedDo(TelnetOption.ECHO)))
        {
            OutputStream spy = spyStream;
            if (spy != null)
            {
                try
                {
                    // 写入字符到监视流
                    spy.write(ch);
                    // 刷新流
                    spy.flush();
                }
                catch (IOException e)
                {
                    // 如果发生错误，清除监视流
                    spyStream = null;
                }
            }
        }
    }
    /* Code Section added for supporting spystreams (end)*/

    /**
     * 注册通知处理器
     *
     * 注册一个通知处理器，用于接收Telnet选项协商命令的通知。
     * 当接收到DO、DONT、WILL、WONT等命令时，会通知该处理器。
     *
     * @param notifhand - 要注册的TelnetNotificationHandler
     */
    public void registerNotifHandler(TelnetNotificationHandler  notifhand)
    {
        __notifhand = notifhand;
    }

    /**
     * 注销通知处理器
     *
     * 注销当前的通知处理器，停止接收选项协商通知。
     */
    public void unregisterNotifHandler()
    {
        __notifhand = null;
    }
}
