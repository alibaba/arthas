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
 * 实现Telnet窗口大小选项的处理器，基于RFC 1073标准。
 *
 * <p>窗口大小选项（NAWS - Negotiate About Window Size）允许客户端
 * 向服务器通告其终端窗口的当前尺寸（列数和行数）。
 * 这使全屏应用程序（如文本编辑器）能够正确调整其显示。</p>
 *
 * <p>窗口大小以两个16位整数的形式发送：宽度（列数）和高度（行数）。</p>
 *
 * @version $Id: WindowSizeOptionHandler.java 1697293 2015-08-24 01:01:00Z sebb $
 * @since 2.0
 * @see TelnetOptionHandler
 * @see TelnetOption
 */
public class WindowSizeOptionHandler extends TelnetOptionHandler
{
    /**
     * 水平尺寸（窗口宽度，字符列数）。
     * 默认值为80（标准终端宽度）
     */
    private int m_nWidth = 80;

    /**
     * 垂直尺寸（窗口高度，字符行数）。
     * 默认值为24（标准终端高度）
     */
    private int m_nHeight = 24;

    /**
     * 窗口大小选项代码。
     * 根据RFC 1073，窗口大小选项的代码为31
     */
    protected static final int WINDOW_SIZE = 31;

    /**
     * WindowSizeOptionHandler的完整构造函数。
     * 允许定义此选项的本地/远程激活的所需初始设置，
     * 以及在接收到本地/远程激活请求时的行为。
     * <p>
     *
     * @param nWidth       窗口宽度（字符列数）
     * @param nHeight      窗口高度（字符行数）
     * @param initlocal   如果设置为true，则在连接时发送WILL请求（本地激活）
     * @param initremote  如果设置为true，则在连接时发送DO请求（远程激活）
     * @param acceptlocal 如果设置为true，则接受任何DO请求
     * @param acceptremote 如果设置为true，则接受任何WILL请求
     */
    public WindowSizeOptionHandler(
        int nWidth,
        int nHeight,
        boolean initlocal,
        boolean initremote,
        boolean acceptlocal,
        boolean acceptremote
    ) {
        // 调用父类构造函数，传递窗口大小选项代码和初始化参数
        super (
            TelnetOption.WINDOW_SIZE,
            initlocal,
            initremote,
            acceptlocal,
            acceptremote
        );

        m_nWidth = nWidth;    // 保存窗口宽度
        m_nHeight = nHeight;  // 保存窗口高度
    }

    /**
     * WindowSizeOptionHandler的简化构造函数。
     * 初始标志和接受标志都设置为false。
     * <p>
     *
     * @param nWidth  窗口宽度（字符列数）
     * @param nHeight 窗口高度（字符行数）
     */
    public WindowSizeOptionHandler(
        int nWidth,
        int nHeight
    ) {
        // 调用父类构造函数，所有参数都设置为false
        super (
            TelnetOption.WINDOW_SIZE,
            false,
            false,
            false,
            false
        );

        m_nWidth = nWidth;    // 保存窗口宽度
        m_nHeight = nHeight;  // 保存窗口高度
    }

    /**
     * 实现TelnetOptionHandler的抽象方法。
     * 启动本地子协商，将客户端的高度和宽度发送到服务器。
     * <p>
     * 此方法构造一个包含窗口大小信息的子协商响应。
     * 窗口大小以4个字节的形式发送：高字节和低字节的宽度和高度。
     * 如果任何字节等于0xFF（Telnet的IAC字符），则需要重复它。
     * <p>
     *
     * @return 要发送到远程系统的字节数组
     */
    @Override
    public int[] startSubnegotiationLocal()
    {
        // 将宽度和高度组合成一个32位整数
        // 宽度占高16位，高度占低16位
        int nCompoundWindowSize = m_nWidth * 0x10000 + m_nHeight;
        int nResponseSize = 5;  // 默认响应大小：选项代码 + 4字节数据
        int nIndex;
        int nShift;
        int nTurnedOnBits;

        // 检查宽度是否包含0xFF字节（需要转义）
        if ((m_nWidth % 0x100) == 0xFF) {
            nResponseSize += 1;  // 宽度低字节需要转义，增加1个字节
        }

        if ((m_nWidth / 0x100) == 0xFF) {
            nResponseSize += 1;  // 宽度高字节需要转义，增加1个字节
        }

        // 检查高度是否包含0xFF字节（需要转义）
        if ((m_nHeight % 0x100) == 0xFF) {
            nResponseSize += 1;  // 高度低字节需要转义，增加1个字节
        }

        if ((m_nHeight / 0x100) == 0xFF) {
            nResponseSize += 1;  // 高度高字节需要转义，增加1个字节
        }

        //
        // 分配响应数组
        //
        int response[] = new int[nResponseSize];

        //
        // 构建响应数组：
        // 1. 放置选项代码
        // 2. 循环遍历窗口大小并填充值
        // 3. 如果需要，复制'ff'（转义0xFF字节）
        //

        response[0] = WINDOW_SIZE;                          // 1. 设置选项代码

        // 2. 从32位整数中提取每个字节并添加到响应数组
        for (
            nIndex=1, nShift = 24;  // 从最高字节开始
            nIndex < nResponseSize;
            nIndex++, nShift -=8     // 每次移动到下一个字节
        ) {
            // 创建掩码以提取当前字节
            nTurnedOnBits = 0xFF;
            nTurnedOnBits <<= nShift;
            // 提取当前字节并右移以获得正确的值
            response[nIndex] = (nCompoundWindowSize & nTurnedOnBits) >>> nShift;

            // 3. 如果字节是0xFF，需要转义（重复它）
            if (response[nIndex] == 0xff) {
                nIndex++;                      // 移动到下一个位置
                response[nIndex] = 0xff;       // 添加转义的0xFF
            }
        }

        return response;  // 返回构造的响应数组
    }

}
