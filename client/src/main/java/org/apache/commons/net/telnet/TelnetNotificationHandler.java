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

/***
 * TelnetNotificationHandler 接口可用于处理
 * 在 telnet 会话上接收到的选项协商命令的通知。
 * <p>
 * 用户可以实现此接口并通过使用 TelnetClient 的
 * registerNotificationHandler() 方法注册一个
 * TelnetNotificationHandler，以便接收选项协商命令的通知。
 ***/

public interface TelnetNotificationHandler
{
    /***
     * 远程方发送了一个 DO 命令。
     * DO 命令用于请求对方启用或确认某个选项。
     ***/
    public static final int RECEIVED_DO =   1;

    /***
     * 远程方发送了一个 DONT 命令。
     * DONT 命令用于请求对方禁用或拒绝某个选项。
     ***/
    public static final int RECEIVED_DONT = 2;

    /***
     * 远程方发送了一个 WILL 命令。
     * WILL 命令用于表示愿意启用或确认某个选项。
     ***/
    public static final int RECEIVED_WILL = 3;

    /***
     * 远程方发送了一个 WONT 命令。
     * WONT 命令用于表示拒绝启用或确认某个选项。
     ***/
    public static final int RECEIVED_WONT = 4;

    /***
     * 远程方发送了一个命令（COMMAND）。
     * @since 2.2
     ***/
    public static final int RECEIVED_COMMAND = 5;

    /***
     * 当 TelnetClient 接收到命令或选项协商命令时调用的回调方法。
     * 此方法允许应用程序响应和处理 Telnet 协议的各种协商命令。
     *
     * @param negotiation_code - 接收到的（协商）命令类型
     * (RECEIVED_DO, RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT, RECEIVED_COMMAND)
     * 这些常量值指示远程方发送的具体命令类型。
     *
     * @param option_code - 被协商的选项代码，或命令代码本身（例如 NOP）。
     * 选项代码标识正在协商的具体 Telnet 选项，如终端类型、回显等。
     ***/
    public void receivedNegotiation(int negotiation_code, int option_code);
}
