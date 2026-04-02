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
 * The TelnetOption class cannot be instantiated and only serves as a
 * storehouse for telnet option constants.
 * <p>
 * TelnetOption类不能被实例化，仅作为Telnet协议选项常量的存储库。
 * <p>
 * Details regarding Telnet option specification can be found in RFC 855.
 * 有关Telnet选项规范的详细信息可以在RFC 855中找到。
 *
 *
 * @see org.apache.commons.net.telnet.Telnet
 * @see org.apache.commons.net.telnet.TelnetClient
 ***/

public class TelnetOption
{
    /*** The maximum value an option code can have.  This value is 255. ***/
    /*** 选项代码可以具有的最大值。该值为255。 ***/
    public static final int MAX_OPTION_VALUE = 255;

    /*** 二进制传输选项 ***/
    public static final int BINARY = 0;

    /*** 回显选项 - 控制字符是否回显到终端 ***/
    public static final int ECHO = 1;

    /*** 准备重连选项 ***/
    public static final int PREPARE_TO_RECONNECT = 2;

    /*** 抑制继续进行选项 - 禁用GO_AHEAD信号以提高性能 ***/
    public static final int SUPPRESS_GO_AHEAD = 3;

    /*** 近似消息大小选项 ***/
    public static final int APPROXIMATE_MESSAGE_SIZE = 4;

    /*** 状态选项 - 请求Telnet连接的状态信息 ***/
    public static final int STATUS = 5;

    /*** 定时标记选项 - 用于同步和时间戳 ***/
    public static final int TIMING_MARK = 6;

    /*** 远程控制传输选项 ***/
    public static final int REMOTE_CONTROLLED_TRANSMISSION = 7;

    /*** 协商输出行宽度选项 ***/
    public static final int NEGOTIATE_OUTPUT_LINE_WIDTH = 8;

    /*** 协商输出页面大小选项 ***/
    public static final int NEGOTIATE_OUTPUT_PAGE_SIZE = 9;

    /*** 协商回车符处理选项 ***/
    public static final int NEGOTIATE_CARRIAGE_RETURN = 10;

    /*** 协商水平制表符停止位选项 ***/
    public static final int NEGOTIATE_HORIZONTAL_TAB_STOP = 11;

    /*** 协商水平制表符选项 ***/
    public static final int NEGOTIATE_HORIZONTAL_TAB = 12;

    /*** 协商换页符选项 ***/
    public static final int NEGOTIATE_FORMFEED = 13;

    /*** 协商垂直制表符停止位选项 ***/
    public static final int NEGOTIATE_VERTICAL_TAB_STOP = 14;

    /*** 协商垂直制表符选项 ***/
    public static final int NEGOTIATE_VERTICAL_TAB = 15;

    /*** 协商换行符选项 ***/
    public static final int NEGOTIATE_LINEFEED = 16;

    /*** 扩展ASCII选项 ***/
    public static final int EXTENDED_ASCII = 17;

    /*** 强制登出选项 ***/
    public static final int FORCE_LOGOUT = 18;

    /*** 字节宏选项 ***/
    public static final int BYTE_MACRO = 19;

    /*** 数据输入终端选项 ***/
    public static final int DATA_ENTRY_TERMINAL = 20;

    /*** SUPDUP协议选项 ***/
    public static final int SUPDUP = 21;

    /*** SUPDUP输出选项 ***/
    public static final int SUPDUP_OUTPUT = 22;

    /*** 发送位置选项 ***/
    public static final int SEND_LOCATION = 23;

    /*** 终端类型选项 - 用于交换终端类型信息 ***/
    public static final int TERMINAL_TYPE = 24;

    /*** 记录结束选项 ***/
    public static final int END_OF_RECORD = 25;

    /*** TACACS用户标识选项 - 终端访问控制器访问控制系统 ***/
    public static final int TACACS_USER_IDENTIFICATION = 26;

    /*** 输出标记选项 ***/
    public static final int OUTPUT_MARKING = 27;

    /*** 终端位置号选项 ***/
    public static final int TERMINAL_LOCATION_NUMBER = 28;

    /*** 3270协议选项 - IBM 3270终端支持 ***/
    public static final int REGIME_3270 = 29;

    /*** X.3 PAD选项 - 分组装配器/拆卸器 ***/
    public static final int X3_PAD = 30;

    /*** 窗口大小选项 - 协商终端窗口大小 ***/
    public static final int WINDOW_SIZE = 31;

    /*** 终端速度选项 - 协商终端数据传输速率 ***/
    public static final int TERMINAL_SPEED = 32;

    /*** 远程流控制选项 ***/
    public static final int REMOTE_FLOW_CONTROL = 33;

    /*** 行模式选项 - 在本地编辑行而不是逐字符发送 ***/
    public static final int LINEMODE = 34;

    /*** X显示位置选项 - X Window System显示位置 ***/
    public static final int X_DISPLAY_LOCATION = 35;

    /*** 旧环境变量选项 - 传递环境变量 ***/
    public static final int OLD_ENVIRONMENT_VARIABLES = 36;

    /*** 认证选项 - Telnet认证机制 ***/
    public static final int AUTHENTICATION = 37;

    /*** 加密选项 - Telnet加密机制 ***/
    public static final int ENCRYPTION = 38;

    /*** 新环境变量选项 - 改进的环境变量传递 ***/
    public static final int NEW_ENVIRONMENT_VARIABLES = 39;

    /*** 扩展选项列表选项 ***/
    public static final int EXTENDED_OPTIONS_LIST = 255;

    @SuppressWarnings("unused")
    /*** 第一个选项代码的值 ***/
    private static final int __FIRST_OPTION = BINARY;
    /*** 最后一个选项代码的值 ***/
    private static final int __LAST_OPTION = EXTENDED_OPTIONS_LIST;

    /*** 选项代码到字符串名称的映射数组 ***/
    private static final String __optionString[] = {
                "BINARY", "ECHO", "RCP", "SUPPRESS GO AHEAD", "NAME", "STATUS",
                "TIMING MARK", "RCTE", "NAOL", "NAOP", "NAOCRD", "NAOHTS", "NAOHTD",
                "NAOFFD", "NAOVTS", "NAOVTD", "NAOLFD", "EXTEND ASCII", "LOGOUT",
                "BYTE MACRO", "DATA ENTRY TERMINAL", "SUPDUP", "SUPDUP OUTPUT",
                "SEND LOCATION", "TERMINAL TYPE", "END OF RECORD", "TACACS UID",
                "OUTPUT MARKING", "TTYLOC", "3270 REGIME", "X.3 PAD", "NAWS", "TSPEED",
                "LFLOW", "LINEMODE", "XDISPLOC", "OLD-ENVIRON", "AUTHENTICATION",
                "ENCRYPT", "NEW-ENVIRON", "TN3270E", "XAUTH", "CHARSET", "RSP",
                "Com Port Control", "Suppress Local Echo", "Start TLS",
                "KERMIT", "SEND-URL", "FORWARD_X", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "TELOPT PRAGMA LOGON", "TELOPT SSPI LOGON",
                "TELOPT PRAGMA HEARTBEAT", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "",
                "Extended-Options-List"
            };


    /***
     * Returns the string representation of the telnet protocol option
     * corresponding to the given option code.
     * 返回给定选项代码对应的Telnet协议选项的字符串表示形式。
     *
     * @param code The option code of the telnet protocol option
     *             Telnet协议选项的选项代码
     * @return The string representation of the telnet protocol option.
     *         Telnet协议选项的字符串表示形式。如果选项未分配，返回"UNASSIGNED"。
     ***/
    public static final String getOption(int code)
    {
        if(__optionString[code].length() == 0)
        {
            return "UNASSIGNED";
        }
        else
        {
            return __optionString[code];
        }
    }


    /***
     * Determines if a given option code is valid.  Returns true if valid,
     * false if not.
     * 确定给定的选项代码是否有效。如果有效返回true，否则返回false。
     *
     * @param code  The option code to test.
     *              要测试的选项代码。
     * @return True if the option code is valid, false if not.
     *         如果选项代码有效返回true，否则返回false。
     **/
    public static final boolean isValidOption(int code)
    {
        return (code <= __LAST_OPTION);
    }

    // Cannot be instantiated
    // 私有构造函数，防止类被实例化
    private TelnetOption()
    { }
}
