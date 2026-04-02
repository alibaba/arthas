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

import java.io.IOException;

/**
 * 此异常用于指示服务器的回复无法被解释。
 * 大多数 NetComponents 类在接收服务器回复时尝试尽可能宽容。
 * 许多服务器实现偏离了 IETF 协议规范，因此需要尽可能灵活。
 * 然而，在某些情况下，由于服务器回复无法以有意义的方式解释，
 * 无法继续操作。在这些情况下，应该抛出 MalformedServerReplyException。
 *
 *
 ***/

public class MalformedServerReplyException extends IOException
{

    /**
     * 序列化版本 UID，用于类的序列化兼容性。
     */
    private static final long serialVersionUID = 6006765264250543945L;

    /**
     * 构造一个没有详细消息的 MalformedServerReplyException。
     ***/
    public MalformedServerReplyException()
    {
        // 调用父类构造函数，创建无消息的异常
        super();
    }

    /**
     * 构造一个带有指定消息的 MalformedServerReplyException。
     *
     * @param message  解释异常原因的消息。
     ***/
    public MalformedServerReplyException(String message)
    {
        // 调用父类构造函数，创建带消息的异常
        super(message);
    }

}
