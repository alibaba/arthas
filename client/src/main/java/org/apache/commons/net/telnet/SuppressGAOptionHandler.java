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
 * Telnet 抑制继续前进选项处理器
 *
 * 实现了 Telnet 协议的抑制继续前进（Suppress Go Ahead）选项，该选项定义在 RFC 858 中。
 * 该选项用于禁止 Telnet 协议中的 GA（Go Ahead）信号，允许双向同时传输数据，
 * 从而提高 Telnet 会话的效率和响应速度。
 *
 * 在 Telnet 协议的早期版本中，由于使用半双工通信方式，需要在数据传输结束后发送 GA 信号
 * 来通知对方可以发送数据了。但在现代全双工通信中，这个信号已经不再需要，
 * 使用 Suppress GA 选项可以禁用这个信号，实现更流畅的交互体验。
 ***/
public class SuppressGAOptionHandler extends TelnetOptionHandler
{
    /**
     * 完整构造函数
     *
     * 创建一个 SuppressGAOptionHandler 实例，允许自定义初始激活状态和请求接受行为。
     *
     * @param initlocal - 如果设置为 true，连接建立时会发送 WILL 请求（主动要求本地激活此选项）
     * @param initremote - 如果设置为 true，连接建立时会发送 DO 请求（主动要求远程激活此选项）
     * @param acceptlocal - 如果设置为 true，接受任何 DO 请求（同意远程端激活此选项）
     * @param acceptremote - 如果设置为 true，接受任何 WILL 请求（同意本地激活此选项）
     ***/
    public SuppressGAOptionHandler(boolean initlocal, boolean initremote,
                                boolean acceptlocal, boolean acceptremote)
    {
        // 调用父类构造函数，传入 SUPPRESS_GO_AHEAD 选项和各项配置参数
        super(TelnetOption.SUPPRESS_GO_AHEAD, initlocal, initremote,
                                      acceptlocal, acceptremote);
    }

    /**
     * 默认构造函数
     *
     * 创建一个 SuppressGAOptionHandler 实例，所有标志位都设置为 false。
     * 这意味着：
     * - 连接建立时不会主动发送激活请求
     * - 不会接受对方的激活请求
     *
     * 使用此构造函数创建的处理器需要通过其他方式来协商和激活该选项。
     ***/
    public SuppressGAOptionHandler()
    {
        // 调用父类构造函数，所有参数都设置为 false（不主动激活，不接受激活请求）
        super(TelnetOption.SUPPRESS_GO_AHEAD, false, false, false, false);
    }

}
