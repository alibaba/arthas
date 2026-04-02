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

import java.io.PrintStream;
import java.io.PrintWriter;

/***
 * 这是一个支持类，用于一些示例程序。它是ProtocolCommandListener接口的
 * 示例实现，将所有命令/回复流量打印到指定的输出流中。
 *
 * @since 2.0
 ***/

// PrintCommandListener类：打印命令监听器
// 该类实现了ProtocolCommandListener接口，用于将协议命令和回复打印到指定的输出流
public class PrintCommandListener implements ProtocolCommandListener
{
    // PrintWriter对象，用于写入命令和响应数据
    private final PrintWriter __writer;

    // 是否隐藏登录信息的标志
    // 如果为true，则在打印登录命令时只显示命令名称，隐藏密码等敏感信息
    private final boolean __nologin;

    // 行结束标记字符
    // 如果非零，则在行结束符之前添加此标记
    private final char __eolMarker;

    // 是否显示方向标记的标志
    // 如果为true，则在输出前添加"> "（发送）或"< "（接收）前缀
    private final boolean __directionMarker;

    /**
     * 创建默认实例，打印所有内容。
     *
     * @param stream 写入命令和响应的流
     * 例如：System.out
     * @since 3.0
     */
    public PrintCommandListener(PrintStream stream)
    {
        // 调用带PrintWriter参数的构造函数
        this(new PrintWriter(stream));
    }

    /**
     * 创建一个实例，可选择是否隐藏登录命令文本。
     *
     * @param stream 写入命令和响应的流
     * @param suppressLogin 如果为{@code true}，则只打印登录命令的名称
     *
     * @since 3.0
     */
    public PrintCommandListener(PrintStream stream, boolean suppressLogin) {
        // 调用带PrintWriter参数的构造函数
        this(new PrintWriter(stream), suppressLogin);
    }

    /**
     * 创建一个实例，可选择是否隐藏登录命令文本，并使用指定字符标记行结束位置。
     *
     * @param stream 写入命令和响应的流
     * @param suppressLogin 如果为{@code true}，则只打印登录命令的名称
     * @param eolMarker 如果非零，则在行结束符之前添加标记
     *
     * @since 3.0
     */
    public PrintCommandListener(PrintStream stream, boolean suppressLogin, char eolMarker) {
        // 调用带PrintWriter参数的构造函数
        this(new PrintWriter(stream), suppressLogin, eolMarker);
    }

    /**
     * 创建一个实例，可选择是否隐藏登录命令文本，使用指定字符标记行结束位置，
     * 并显示通信方向。
     *
     * @param stream 写入命令和响应的流
     * @param suppressLogin 如果为{@code true}，则只打印登录命令的名称
     * @param eolMarker 如果非零，则在行结束符之前添加标记
     * @param showDirection 如果为{@code true}，则在输出中添加适当的{@code "> "}或{@code "< "}前缀
     *
     * @since 3.0
     */
    public PrintCommandListener(PrintStream stream, boolean suppressLogin, char eolMarker, boolean showDirection) {
        // 调用带PrintWriter参数的构造函数
        this(new PrintWriter(stream), suppressLogin, eolMarker, showDirection);
    }

    /**
     * 创建默认实例，打印所有内容。
     *
     * @param writer 写入命令和响应的PrintWriter对象
     */
    public PrintCommandListener(PrintWriter writer)
    {
        // 调用构造函数，suppressLogin设为false（不隐藏登录信息）
        this(writer, false);
    }

    /**
     * 创建一个实例，可选择是否隐藏登录命令文本。
     *
     * @param writer 写入命令和响应的PrintWriter对象
     * @param suppressLogin 如果为{@code true}，则只打印登录命令的名称
     *
     * @since 3.0
     */
    public PrintCommandListener(PrintWriter writer, boolean suppressLogin)
    {
        // 调用构造函数，eolMarker设为0（不使用行结束标记）
        this(writer, suppressLogin, (char) 0);
    }

    /**
     * 创建一个实例，可选择是否隐藏登录命令文本，并使用指定字符标记行结束位置。
     *
     * @param writer 写入命令和响应的PrintWriter对象
     * @param suppressLogin 如果为{@code true}，则只打印登录命令的名称
     * @param eolMarker 如果非零，则在行结束符之前添加标记
     *
     * @since 3.0
     */
    public PrintCommandListener(PrintWriter writer, boolean suppressLogin, char eolMarker)
    {
        // 调用构造函数，showDirection设为false（不显示方向标记）
        this(writer, suppressLogin, eolMarker, false);
    }

    /**
     * 创建一个实例，可选择是否隐藏登录命令文本，使用指定字符标记行结束位置，
     * 并显示通信方向。
     *
     * @param writer 写入命令和响应的PrintWriter对象
     * @param suppressLogin 如果为{@code true}，则只打印登录命令的名称
     * @param eolMarker 如果非零，则在行结束符之前添加标记
     * @param showDirection 如果为{@code true}，则在输出中添加适当的{@code "> "}或{@code "< "}前缀
     *
     * @since 3.0
     */
    public PrintCommandListener(PrintWriter writer, boolean suppressLogin, char eolMarker, boolean showDirection)
    {
        // 初始化成员变量
        __writer = writer;          // 保存PrintWriter对象
        __nologin = suppressLogin;  // 保存是否隐藏登录信息的标志
        __eolMarker = eolMarker;    // 保存行结束标记字符
        __directionMarker = showDirection; // 保存是否显示方向标记的标志
    }

    // 当协议命令发送到服务器时调用此方法
    @Override
    public void protocolCommandSent(ProtocolCommandEvent event)
    {
        // 如果启用了方向标记，打印发送符号"> "
        if (__directionMarker) {
            __writer.print("> ");
        }

        // 如果启用了登录信息隐藏
        if (__nologin) {
            // 获取命令字符串
            String cmd = event.getCommand();

            // 检查是否为FTP的PASS或USER命令
            if ("PASS".equalsIgnoreCase(cmd) || "USER".equalsIgnoreCase(cmd)) {
                // 只打印命令名称，隐藏具体参数
                __writer.print(cmd);
                __writer.println(" *******"); // 不需要行结束标记
            } else {
                // IMAP协议的LOGIN命令
                final String IMAP_LOGIN = "LOGIN";

                // 检查是否为IMAP的LOGIN命令
                if (IMAP_LOGIN.equalsIgnoreCase(cmd)) { // IMAP协议
                    // 获取消息内容
                    String msg = event.getMessage();
                    // 截取命令名称部分，隐藏登录参数
                    msg=msg.substring(0, msg.indexOf(IMAP_LOGIN)+IMAP_LOGIN.length());
                    __writer.print(msg);
                    __writer.println(" *******"); // 不需要行结束标记
                } else {
                    // 其他命令，打印完整的可打印字符串（包含行结束标记）
                    __writer.print(getPrintableString(event.getMessage()));
                }
            }
        } else {
            // 未启用登录信息隐藏，直接打印完整的可打印字符串
            __writer.print(getPrintableString(event.getMessage()));
        }

        // 刷新输出流，确保内容立即写入
        __writer.flush();
    }

    /**
     * 获取可打印的字符串，在行结束符之前添加标记（如果配置了）。
     *
     * @param msg 原始消息字符串
     * @return 处理后的可打印字符串
     */
    private String getPrintableString(String msg){
        // 如果没有设置行结束标记，直接返回原消息
        if (__eolMarker == 0) {
            return msg;
        }

        // 查找NETASCII行结束符的位置
        int pos = msg.indexOf(SocketClient.NETASCII_EOL);

        // 如果找到了行结束符且不在开头
        if (pos > 0) {
            // 在行结束符之前插入标记字符
            return msg.substring(0, pos) +
                    __eolMarker +
                    msg.substring(pos);
        }

        // 未找到行结束符，返回原消息
        return msg;
    }

    // 当从服务器接收到协议回复时调用此方法
    @Override
    public void protocolReplyReceived(ProtocolCommandEvent event)
    {
        // 如果启用了方向标记，打印接收符号"< "
        if (__directionMarker) {
            __writer.print("< ");
        }

        // 打印接收到的消息内容
        __writer.print(event.getMessage());

        // 刷新输出流，确保内容立即写入
        __writer.flush();
    }
}

