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

/**
 * 实现Telnet终端类型选项的处理器，基于RFC 1091标准。
 *
 * <p>终端类型选项（TERMINAL TYPE option）允许客户端向服务器通告其终端类型，
 * 如"VT100"、"ANSI"等。这使服务器能够根据终端类型调整其输出格式。</p>
 *
 * <p>协商过程：</p>
 * <ul>
 *   <li>服务器发送DO TERMINAL-TYPE请求</li>
 *   <li>客户端响应WILL TERMINAL-TYPE</li>
 *   <li>服务器发送SB TERMINAL-TYPE SEND请求</li>
 *   <li>客户端响应SB TERMINAL-TYPE IS <terminal-type></li>
 * </ul>
 *
 * @see TelnetOptionHandler
 * @see TelnetOption
 */
public class TerminalTypeOptionHandler extends TelnetOptionHandler
{
    /**
     * 终端类型字符串。
     * 存储要协商的终端类型，如"VT100"、"ANSI"、"xterm"等
     */
    private final String termType;

    /**
     * 终端类型选项代码。
     * 根据RFC 1091，终端类型选项的代码为24
     */
    protected static final int TERMINAL_TYPE = 24;

    /**
     * 发送子命令（用于子协商）。
     * 值为1，表示请求发送终端类型信息
     */
    protected static final int TERMINAL_TYPE_SEND =  1;

    /**
     * 是子命令（用于子协商）。
     * 值为0，表示后续数据是终端类型信息
     */
    protected static final int TERMINAL_TYPE_IS =  0;

    /**
     * TerminalTypeOptionHandler的构造函数。
     * 允许定义此选项的本地/远程激活的所需初始设置，
     * 以及在接收到本地/远程激活请求时的行为。
     * <p>
     *
     * @param termtype    要协商的终端类型（如"VT100"、"ANSI"等）
     * @param initlocal   如果设置为true，则在连接时发送WILL请求（本地激活）
     * @param initremote  如果设置为true，则在连接时发送DO请求（远程激活）
     * @param acceptlocal 如果设置为true，则接受任何DO请求
     * @param acceptremote 如果设置为true，则接受任何WILL请求
     */
    public TerminalTypeOptionHandler(String termtype,
                                boolean initlocal,
                                boolean initremote,
                                boolean acceptlocal,
                                boolean acceptremote)
    {
        // 调用父类构造函数，传递终端类型选项代码和初始化参数
        super(TelnetOption.TERMINAL_TYPE, initlocal, initremote,
                                      acceptlocal, acceptremote);
        termType = termtype;  // 保存终端类型
    }

    /**
     * TerminalTypeOptionHandler的简化构造函数。
     * 初始标志和接受标志都设置为false。
     * <p>
     *
     * @param termtype 要协商的终端类型（如"VT100"、"ANSI"等）
     */
    public TerminalTypeOptionHandler(String termtype)
    {
        // 调用父类构造函数，所有参数都设置为false
        super(TelnetOption.TERMINAL_TYPE, false, false, false, false);
        termType = termtype;  // 保存终端类型
    }

    /**
     * 实现TelnetOptionHandler的抽象方法。
     * 处理终端类型的子协商请求。
     * <p>
     * 当收到TERMINAL-TYPE SEND请求时，此方法构造一个响应，
     * 包含TERMINAL-TYPE IS命令和实际的终端类型字符串。
     * <p>
     *
     * @param suboptionData   接收到的子协商数据序列（不包含IAC SB和IAC SE）
     * @param suboptionLength suboption_data中的数据长度
     *
     * @return 终端类型信息响应数组，如果无法处理请求则返回null
     */
    @Override
    public int[] answerSubnegotiation(int suboptionData[], int suboptionLength)
    {
        // 检查子协商数据是否有效
        if ((suboptionData != null) && (suboptionLength > 1)
            && (termType != null))
        {
            // 检查是否是终端类型发送请求
            if ((suboptionData[0] == TERMINAL_TYPE)
                && (suboptionData[1] == TERMINAL_TYPE_SEND))
            {
                // 创建响应数组：[选项代码, IS命令, 终端类型字符...]
                int response[] = new int[termType.length() + 2];

                response[0] = TERMINAL_TYPE;      // 设置选项代码
                response[1] = TERMINAL_TYPE_IS;   // 设置IS命令

                // 将终端类型字符串的每个字符添加到响应数组
                for (int ii = 0; ii < termType.length(); ii++)
                {
                    response[ii + 2] = termType.charAt(ii);
                }

                return response;  // 返回构造的响应
            }
        }
        return null;  // 无效的请求或数据，返回null
    }
}
