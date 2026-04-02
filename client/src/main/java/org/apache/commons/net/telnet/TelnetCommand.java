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
 * TelnetCommand类定义了Telnet协议的所有命令常量。
 *
 * Telnet协议是一种应用层协议，用于在TCP/IP网络上提供虚拟终端功能。
 * 该类不能被实例化，仅作为存储Telnet命令常量的仓库使用。
 *
 * Telnet协议使用命令来控制连接和协商选项。这些命令以字节形式传输，
 * 其中大多数命令字节的值在240-255之间（十进制）。
 *
 * 主要命令包括：
 * - IAC（Interpret As Command）：解释为命令，用于转义
 * - 协商命令：DO、DONT、WILL、WONT，用于选项协商
 * - 控制命令：AO、IP、BRK等，用于控制终端行为
 * - 子协商：SB和SE，用于选项的详细参数协商
 *
 * @see org.apache.commons.net.telnet.Telnet
 * @see org.apache.commons.net.telnet.TelnetClient
 */

public final class TelnetCommand
{
    /***
     * Telnet命令码的最大值。
     * 该值为255，这是Telnet协议中单字节能表示的最大值。
     ***/
    public static final int MAX_COMMAND_VALUE = 255;

    /***
     * 解释为命令（Interpret As Command）代码。
     * 根据RFC 854标准，该值为255（0xFF）。
     *
     * IAC是Telnet协议中最基础的命令。当数据流中出现值为255的字节时，
     * 接收方需要将其解释为命令，而不是普通数据。如果需要传输值为255的数据，
     * 需要连续发送两个255字节（IAC IAC）进行转义。
     *
     * IAC通常作为其他命令的前缀出现，例如：IAC DO、IAC WILL等。
     ***/
    public static final int IAC = 255;

    /***
     * 不要使用选项（Don't）代码。
     * 根据RFC 854标准，该值为254（0xFE）。
     *
     * DONT命令用于拒绝对方使用某个Telnet选项的建议。
     * 例如，服务器发送"IAC DONT X"表示请求客户端不要启用选项X。
     * 它与DO命令配对使用，用于选项协商的拒绝回应。
     ***/
    public static final int DONT = 254;

    /***
     * 请求使用选项（Do）代码。
     * 根据RFC 854标准，该值为253（0xFD）。
     *
     * DO命令用于请求对方使用某个Telnet选项。
     * 例如，服务器发送"IAC DO X"表示请求客户端启用选项X。
     * 对方可以回应WILL（同意）或WONT（拒绝）。
     *
     * 常见的选项包括：ECHO（回显）、SUPPRESS_GO_AHEAD（抑制继续信号）等。
     ***/
    public static final int DO = 253;

    /***
     * 拒绝使用选项（Won't）代码。
     * 根据RFC 854标准，该值为252（0xFC）。
     *
     * WONT命令用于拒绝对方关于使用某个Telnet选项的请求。
     * 例如，服务器发送"IAC WILL X"请求使用选项X，
     * 客户端可以回应"IAC WONT X"表示拒绝。
     * 它是对DO或WILL命令的否定回应。
     ***/
    public static final int WONT = 252;

    /***
     * 同意使用选项（Will）代码。
     * 根据RFC 854标准，该值为251（0xFB）。
     *
     * WILL命令用于表示同意使用某个Telnet选项，
     * 或主动请求对方使用某个选项。
     * 例如，客户端发送"IAC WILL X"表示能够使用选项X，
     * 或请求服务器也启用选项X。
     * 它是对DO命令的肯定回应。
     ***/
    public static final int WILL = 251;

    /***
     * 开始子协商（Subnegotiation Begin）代码。
     * 根据RFC 854标准，该值为250（0xFA）。
     *
     * SB命令开始一个子协商过程，用于交换特定选项的详细参数。
     * 格式为：IAC SB <选项代码> <参数...> IAC SE
     *
     * 例如，协商终端类型时：
     * IAC SB TERMINAL-TYPE SEND IAC SE
     * IAC SB TERMINAL-TYPE IS "VT100" IAC SE
     *
     * 子协商必须与SE（End of Subnegotiation）配对使用。
     ***/
    public static final int SB = 250;

    /***
     * 继续信号（Go Ahead）代码。
     * 根据RFC 854标准，该值为249（0xF9）。
     *
     * GA命令用于半双工通信模式，通知对方可以继续发送数据。
     * 在早期Telnet实现中，系统一次只允许一方发送数据，
     * GA命令用于切换发送权。
     *
     * 现代Telnet通常使用SUPPRESS_GO_AHEAD选项来禁用GA命令，
     * 实现全双工通信，双方可以同时发送数据。
     ***/
    public static final int GA = 249;

    /***
     * 删除行（Erase Line）代码。
     * 根据RFC 854标准，该值为248（0xF8）。
     *
     * EL命令请求终端删除当前行的所有内容。
     * 与EC（Erase Character）不同，EL删除整行而不是单个字符。
     * 这通常用于提供更好的用户体验，允许用户快速清空当前输入。
     ***/
    public static final int EL = 248;

    /***
     * 删除字符（Erase Character）代码。
     * 根据RFC 854标准，该值为247（0xF7）。
     *
     * EC命令请求终端删除光标前一个字符。
     * 这是实现退格键功能的基础命令。
     * 在终端实现中，EC通常等同于按下Backspace键。
     ***/
    public static final int EC = 247;

    /***
     * 你在吗（Are You There）代码。
     * 根据RFC 854标准，该值为246（0xF6）。
     *
     * AYT命令用于确认连接是否仍然活跃。
     * 当用户不确定连接是否正常时，可以发送AYT命令，
     * 远程系统应该返回某种确认信号。
     *
     * 这类似于网络中的"ping"功能，用于检测连接状态。
     * 在某些实现中，用户可以通过特定的按键组合触发AYT命令。
     ***/
    public static final int AYT = 246;

    /***
     * 中止输出（Abort Output）代码。
     * 根据RFC 854标准，该值为245（0xF5）。
     *
     * AO命令用于请求远程系统停止发送数据到用户终端。
     * 但不同于IP（Interrupt Process），AO不会中断正在运行的进程，
     * 只是停止向终端显示输出。
     *
     * 使用场景：当程序产生大量输出时，用户可以使用AO停止显示，
     * 但程序继续在后台运行。
     ***/
    public static final int AO = 245;

    /***
     * 中断进程（Interrupt Process）代码。
     * 根据RFC 854标准，该值为244（0xF4）。
     *
     * IP命令用于中断或终止远程系统上当前运行的进程。
     * 这类似于在本地终端按下Ctrl+C键。
     *
     * IP是紧急命令，应该优先处理。系统通常会立即中断
     * 当前正在执行的操作，并返回到提示符状态。
     * 它是实现远程控制的重要命令。
     ***/
    public static final int IP = 244;

    /***
     * 中断信号（Break）代码。
     * 根据RFC 854标准，该值为243（0xF3）。
     *
     * BREAK命令用于向远程系统发送中断信号。
     * 这类似于旧式终端上的Break键或现代系统中的信号中断。
     *
     * Break信号的具体含义取决于系统和应用程序：
     * - 在某些系统中可能用于调试或进入监控模式
     * - 在某些通信程序中可能用于暂停数据传输
     * - 在某些实现中可能与IP类似
     ***/
    public static final int BREAK = 243;

    /***
     * 数据标记（Data Mark）代码。
     * 根据RFC 854标准，该值为242（0xF2）。
     *
     * DM命令是同步信号的一部分，用于标记数据流中的特定位置。
     * 它通常与TCP紧急数据（urgent data）机制配合使用。
     *
     * DM的作用是：
     * 1. 标记一个同步点，指示数据流中的某个位置
     * 2. 清除之前的缓冲数据
     * 3. 用于Telnet同步机制，确保命令能够及时处理
     *
     * DM是Telnet协议处理网络延迟和数据同步的重要机制。
     ***/
    public static final int DM = 242;

    /***
     * 无操作（No Operation）代码。
     * 根据RFC 854标准，该值为241（0xF1）。
     *
     * NOP命令不执行任何操作，主要用于以下目的：
     * 1. 保持连接活跃，防止超时断开
     * 2. 测试连接状态
     * 3. 在数据流中填充时间
     *
     * 类似于网络协议中的"心跳"机制，定期发送NOP可以
     * 确保连接不会因为长时间无数据传输而被中间设备（如防火墙）关闭。
     ***/
    public static final int NOP = 241;

    /***
     * 结束子协商（Subnegotiation End）代码。
     * 根据RFC 854标准，该值为240（0xF0）。
     *
     * SE命令标记子协商过程的结束。
     * 每个SB（Subnegotiation Begin）命令都必须有一个对应的SE命令。
     *
     * 格式：IAC SB <选项代码> <参数...> IAC SE
     *
     * SE和SB必须配对使用，它们之间的数据是特定选项的协商参数。
     * 例如：IAC SB TERMINAL-TYPE IS "VT100" IAC SE
     ***/
    public static final int SE = 240;

    /***
     * 记录结束（End of Record）代码。
     * 该值为239（0xEF）。
     *
     * EOR命令用于标记数据记录的结束。
     * 在某些Telnet实现中，特别是用于数据传输的场景，
     * EOR用于分隔不同的数据记录或数据块。
     *
     * 它类似于文件中的换行符或数据流中的记录分隔符，
     * 帮助接收方识别完整的记录边界。
     ***/
    public static final int EOR = 239;

    /***
     * 中止（Abort）代码。
     * 该值为238（0xEE）。
     *
     * ABORT命令用于中止当前的Telnet会话或操作。
     * 与其他中断命令相比，ABORT通常更彻底地终止操作。
     *
     * 使用场景：
     * - 中止正在进行的文件传输
     * - 取消当前的命令执行
     * - 紧急情况下断开会话
     *
     * ABORT的影响取决于具体实现，可能需要清理资源或保存状态。
     ***/
    public static final int ABORT = 238;

    /***
     * 挂起进程（Suspend Process）代码。
     * 该值为237（0xED）。
     *
     * SUSP命令用于挂起或暂停远程系统上当前运行的进程。
     * 这类似于在Unix/Linux系统中按下Ctrl+Z键。
     *
     * 挂起的进程通常被放入后台，可以使用作业控制命令
     * （如fg、bg）恢复运行。SUSP提供了远程作业控制的能力。
     *
     * 注意：不是所有系统都支持SUSP命令，其行为也因实现而异。
     ***/
    public static final int SUSP = 237;

    /***
     * 文件结束（End of File）代码。
     * 该值为236（0xEC）。
     *
     * EOF命令用于指示数据流的结束。
     * 在文件传输或标准输入操作中，EOF告诉接收方没有更多数据。
     *
     * 使用场景：
     * - 文件传输完成
     * - 标准输入结束（类似于Unix中的Ctrl+D）
     * - 数据流终止
     *
     * EOF帮助应用程序正确处理数据边界，避免等待更多的数据。
     ***/
    public static final int EOF = 236;

    /***
     * 同步（Synchronize）代码。
     * 该值为242（0xF2）。
     *
     * SYNCH命令用于Telnet连接的同步机制。
     * 它与DM（Data Mark）配合使用，通过TCP紧急数据机制实现。
     *
     * 同步过程：
     * 1. 发送方发送带有紧急标记的DM
     * 2. 接收方收到紧急数据通知后，清除缓冲区
     * 3. 接收方开始处理DM之后的数据
     *
     * SYNCH确保重要的命令（如IP、AO）能够被及时处理，
     * 不受网络延迟或缓冲的影响。
     *
     * 注意：SYNCH的值与DM相同（242），它们在功能上是相关联的。
     ***/
    public static final int SYNCH = 242;

    /***
     * 命令的字符串表示数组。
     *
     * 该数组存储了各个Telnet命令的可读字符串形式，
     * 用于调试、日志记录和错误消息显示。
     *
     * 数组的索引与命令代码相关：索引位置对应于
     * __FIRST_COMMAND（IAC）减去命令代码的值。
     *
     * 例如：getCommand(IAC) 返回 "IAC"
     *       getCommand(DO) 返回 "DO"
     ***/
    private static final String __commandString[] = {
                "IAC", "DONT", "DO", "WONT", "WILL", "SB", "GA", "EL", "EC", "AYT",
                "AO", "IP", "BRK", "DMARK", "NOP", "SE", "EOR", "ABORT", "SUSP", "EOF"
            };

    /**
     * 第一个（最高值）命令代码常量。
     * 该值为IAC（255），用于数组索引计算和命令验证。
     *
     * Telnet命令代码从255向下递减到236，IAC是最高值的命令。
     */
    private static final int __FIRST_COMMAND = IAC;

    /**
     * 最后一个（最低值）命令代码常量。
     * 该值为EOF（236），用于数组索引计算和命令验证。
     *
     * EOF是当前定义的最低值Telnet命令代码。
     */
    private static final int __LAST_COMMAND = EOF;

    /***
     * 获取指定Telnet命令代码的字符串表示。
     *
     * 该方法将数值型的命令代码转换为可读的字符串形式，
     * 主要用于调试、日志记录和错误消息显示。
     *
     * 例如：
     * - getCommand(255) 返回 "IAC"
     * - getCommand(253) 返回 "DO"
     * - getCommand(250) 返回 "SB"
     *
     * 实现原理：
     * 使用__FIRST_COMMAND（IAC=255）减去代码值作为数组索引，
     * 从__commandString数组中获取对应的字符串表示。
     *
     * @param code 要转换的Telnet协议命令代码（例如：IAC、DO、WILL等）
     * @return Telnet协议命令的字符串表示（例如："IAC"、"DO"、"WILL"等）
     ***/
    public static final String getCommand(int code)
    {
        return __commandString[__FIRST_COMMAND - code];
    }

    /***
     * 判断给定的命令代码是否有效。
     *
     * 该方法验证指定的数值是否为有效的Telnet命令代码。
     * 有效的命令代码应该在__LAST_COMMAND（EOF=236）到
     * __FIRST_COMMAND（IAC=255）的范围内。
     *
     * 验证规则：
     * - 有效范围：236 ≤ code ≤ 255
     * - 返回true表示该代码是有效的Telnet命令
     * - 返回false表示该代码不在定义的命令范围内
     *
     * 使用场景：
     * - 输入验证：确保接收到的数据是有效的命令
     * - 错误检测：识别无效或损坏的命令代码
     * - 协议解析：在解析Telnet数据流时验证命令
     *
     * @param code 要测试的命令代码
     * @return 如果命令代码有效返回true，否则返回false
     **/
    public static final boolean isValidCommand(int code)
    {
        return (code <= __FIRST_COMMAND && code >= __LAST_COMMAND);
    }

    /**
     * 私有构造函数，防止类被实例化。
     *
     * TelnetCommand类是一个工具类，只包含静态常量和静态方法，
     * 不需要创建实例。将构造函数设为private可以防止
     * 外部代码尝试创建该类的实例。
     *
     * 这种设计模式称为"工具类模式"或"静态工具类模式"，
     * 常用于只包含静态成员的类。
     */
    private TelnetCommand()
    { }
}
