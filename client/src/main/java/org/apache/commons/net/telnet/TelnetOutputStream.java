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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Telnet输出流包装类
 * <p>
 * 该类包装了一个输出流，用于处理Telnet协议的数据输出。
 * 它支持两种工作模式：二进制模式和ASCII模式。
 * <p>
 * 在二进制模式下：
 * - 唯一的转换是将IAC（Interpret As Command，字节值255）字符加倍
 * - 这样做是因为IAC在Telnet协议中有特殊含义，需要转义
 * <p>
 * 在ASCII模式下（当convertCRtoCRLF为true时，当前默认为true）：
 * - 所有的CR（回车，\r）会被转换为CRLF（回车换行，\r\n）
 * - IAC字符会被加倍（转义处理）
 * - 单独的LF（换行，\n）会被转换为CRLF
 * - 单独的CR（后面不跟LF）会被转换为CR\0（符合RFC854规范）
 * <p>
 * 这些转换确保了数据在不同系统和Telnet实现之间的正确传输。
 ***/


/**
 * Telnet输出流类
 * <p>
 * 该类继承自OutputStream，是TelnetClient使用的内部输出流实现。
 * 它负责在将数据发送到底层网络连接之前，根据Telnet协议规范进行必要的字符转换。
 * <p>
 * 主要功能：
 * 1. 支持二进制模式和ASCII模式的数据转换
 * 2. 自动处理Telnet协议中的特殊字符（如IAC）
 * 3. 确保换行符在不同系统间的正确转换
 * 4. 提供线程安全的写操作
 */
final class TelnetOutputStream extends OutputStream
{
    // 关联的Telnet客户端实例，用于实际发送数据
    private final TelnetClient __client;

    // 是否将CR转换为CRLF的标志
    // TODO 这个值目前没有任何方式可以修改，是否应该作为构造函数参数？
    private final boolean __convertCRtoCRLF = true;

    // 标记上一个字符是否为CR（回车符）
    // 用于在ASCII模式下正确处理CRLF序列
    private boolean __lastWasCR = false;

    /**
     * 构造函数
     * <p>
     * 创建一个新的TelnetOutputStream实例
     *
     * @param client 关联的Telnet客户端对象，用于实际发送数据
     */
    TelnetOutputStream(TelnetClient client)
    {
        __client = client;
    }


    /***
     * 向流中写入一个字节
     * <p>
     * 根据当前模式（二进制或ASCII）对字节进行必要的转换后写入。
     * <p>
     * 在ASCII模式下的转换规则：
     * - CR(\r)会被缓存，等待下一个字符来判断是否需要转换
     * - LF(\n)如果前面不是CR，会先写入CR再写入LF
     * - IAC(255)会被写入两次进行转义
     * - 其他字符直接写入
     * <p>
     * 在二进制模式下：
     * - 只有IAC会被加倍写入
     * - 其他字符直接写入
     *
     * @param ch 要写入的字节值
     * @exception IOException 如果写入底层流时发生错误
     ***/
    @Override
    public void write(int ch) throws IOException
    {

        // 使用synchronized确保线程安全
        synchronized (__client)
        {
            // 将ch转换为无符号字节（0-255范围）
            ch &= 0xff;

            // ASCII模式处理（当客户端拒绝使用二进制模式时）
            if (__client._requestedWont(TelnetOption.BINARY)) // i.e. ASCII
            {
                // 处理前一个字符是CR的情况
                if (__lastWasCR)
                {
                    // 如果需要将CR转换为CRLF
                    if (__convertCRtoCRLF)
                    {
                        // 先发送LF完成CRLF序列
                        __client._sendByte('\n');

                        // 如果当前字符也是LF，说明原来的就是CRLF序列
                        if (ch == '\n') // i.e. was CRLF anyway
                        {
                            __lastWasCR = false;
                            return ; // 直接返回，避免重复发送LF
                        }
                    } // __convertCRtoCRLF
                    else if (ch != '\n')
                    {
                        // 如果不需要转换CRtoCRLF，且当前字符不是LF
                        // 则发送\0（RFC854要求：单独的CR后面要跟NUL）
                        __client._sendByte('\0'); // RFC854 requires CR NUL for bare CR
                    }
                }

                // 根据当前字符类型进行处理
                switch (ch)
                {
                // 处理回车符
                case '\r':
                    __client._sendByte('\r');
                    __lastWasCR = true; // 标记上一个字符是CR
                    break;

                // 处理换行符
                case '\n':
                    // 如果前一个字符不是CR，需要先发送CR（将LF转换为CRLF）
                    if (!__lastWasCR) { // convert LF to CRLF
                        __client._sendByte('\r');
                    }
                    __client._sendByte(ch); // 发送LF
                    __lastWasCR = false; // 重置CR标志
                    break;

                // 处理IAC（Telnet命令转义字符）
                case TelnetCommand.IAC:
                    // IAC需要加倍发送以进行转义
                    __client._sendByte(TelnetCommand.IAC);
                    __client._sendByte(TelnetCommand.IAC);
                    __lastWasCR = false; // 重置CR标志
                    break;

                // 处理普通字符
                default:
                    __client._sendByte(ch); // 直接发送
                    __lastWasCR = false; // 重置CR标志
                    break;
                }
            } // end ASCII
            // 二进制模式处理
            else if (ch == TelnetCommand.IAC)
            {
                // IAC字符需要加倍发送以进行转义
                __client._sendByte(ch);
                __client._sendByte(TelnetCommand.IAC);
            } else {
                // 其他字符直接发送
                __client._sendByte(ch);
            }
        }
    }


    /***
     * 向流中写入一个字节数组
     * <p>
     * 该方法通过调用write(byte[], int, int)来实现
     *
     * @param buffer  要写入的字节数组
     * @exception IOException 如果写入底层流时发生错误
     ***/
    @Override
    public void write(byte buffer[]) throws IOException
    {
        // 委托给带偏移量和长度参数的write方法
        write(buffer, 0, buffer.length);
    }


    /***
     * 从字节数组的指定偏移位置开始，写入指定长度的字节到流中
     * <p>
     * 该方法会逐个字节调用write(int)方法，确保每个字节都经过必要的转换处理
     *
     * @param buffer  要写入的字节数组
     * @param offset  数组中开始复制数据的偏移位置
     * @param length  要写入的字节数量
     * @exception IOException 如果写入底层流时发生错误
     ***/
    @Override
    public void write(byte buffer[], int offset, int length) throws IOException
    {
        // 使用synchronized确保线程安全
        synchronized (__client)
        {
            // 循环写入每个字节
            while (length-- > 0) {
                // 逐个字节写入，确保每个字节都经过转换处理
                write(buffer[offset++]);
            }
        }
    }

    /**
     * 刷新输出流
     * <p>
     * 将缓冲区中的数据立即发送到底层连接
     *
     * @exception IOException 如果刷新时发生错误
     ***/
    @Override
    public void flush() throws IOException
    {
        // 委托给TelnetClient的刷新方法
        __client._flushOutputStream();
    }

    /**
     * 关闭输出流
     * <p>
     * 关闭底层的输出流并释放相关资源
     *
     * @exception IOException 如果关闭时发生错误
     ***/
    @Override
    public void close() throws IOException
    {
        // 委托给TelnetClient的关闭方法
        __client._closeOutputStream();
    }
}
