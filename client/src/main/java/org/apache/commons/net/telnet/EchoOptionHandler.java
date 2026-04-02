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
 * 实现Telnet回显选项的处理器，基于RFC 857标准。
 *
 * <p>回显选项（ECHO option）用于控制Telnet会话中的字符回显行为。
 * 当启用此选项时，一端会要求另一端回显它接收到的数据。</p>
 *
 * <p>此类继承自TelnetOptionHandler，提供了回显选项的具体实现。</p>
 *
 * @see TelnetOptionHandler
 * @see TelnetOption
 */
public class EchoOptionHandler extends TelnetOptionHandler
{
    /**
     * EchoOptionHandler的构造函数。
     * 允许定义此选项的本地/远程激活的所需初始设置，
     * 以及在接收到本地/远程激活请求时的行为。
     * <p>
     *
     * @param initlocal   如果设置为true，则在连接时发送WILL请求（本地激活）
     * @param initremote  如果设置为true，则在连接时发送DO请求（远程激活）
     * @param acceptlocal 如果设置为true，则接受任何DO请求（允许远程端激活本地回显）
     * @param acceptremote 如果设置为true，则接受任何WILL请求（允许远程端激活远程回显）
     */
    public EchoOptionHandler(boolean initlocal, boolean initremote,
                                boolean acceptlocal, boolean acceptremote)
    {
        // 调用父类构造函数，传递回显选项代码和初始化参数
        super(TelnetOption.ECHO, initlocal, initremote,
                                      acceptlocal, acceptremote);
    }

    /**
     * EchoOptionHandler的默认构造函数。
     * 初始标志和接受标志都设置为false。
     * <p>
     * 使用此构造函数创建的处理器不会主动请求激活回显选项，
     * 也不会接受对方的激活请求。
     */
    public EchoOptionHandler()
    {
        // 调用父类构造函数，所有参数都设置为false
        super(TelnetOption.ECHO, false, false, false, false);
    }

}
