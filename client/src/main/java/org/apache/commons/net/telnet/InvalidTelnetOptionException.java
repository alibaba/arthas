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
 * 无效Telnet选项异常
 *
 * 当使用TelnetClient的addOptionHandler方法注册TelnetOptionHandler时，
 * 如果选项码无效，则抛出此异常。
 *
 * 该异常用于标识在Telnet协议通信过程中，尝试使用无效或不支持的选项码时的错误情况。
 ***/
public class InvalidTelnetOptionException extends Exception
{

    // 序列化版本号，用于保证序列化兼容性
    private static final long serialVersionUID = -2516777155928793597L;

    /***
     * 选项码
     *
     * 存储导致异常的无效Telnet选项码
     ***/
    private final int optionCode;

    /***
     * 错误消息
     *
     * 描述异常原因的详细信息
     ***/
    private final String msg;

    /***
     * 异常构造函数
     * <p>
     * @param message - 错误消息，描述异常的具体原因
     * @param optcode - 无效的选项码
     ***/
    public InvalidTelnetOptionException(String message, int optcode)
    {
        // 保存选项码
        optionCode = optcode;
        // 保存错误消息
        msg = message;
    }

    /***
     * 获取异常的完整错误消息
     * <p>
     * @return 包含错误描述和选项码的完整错误消息
     ***/
    @Override
    public String getMessage()
    {
        // 拼接错误消息和选项码，返回完整的错误信息
        return (msg + ": " + optionCode);
    }
}
