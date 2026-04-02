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
 * Telnet输入监听器接口
 *
 * 该接口用于通知监听器，Telnet客户端接收到新的传入数据，并且这些数据已经可以被读取。
 *
 * 此接口主要用于异步处理Telnet协议的输入数据流。当Telnet客户端的输入流中有新数据到达时，
 * 所有已注册的监听器将收到通知，从而可以及时读取和处理这些数据。
 *
 * 这种事件驱动的机制使得应用程序可以更高效地处理Telnet通信，而不需要持续轮询输入流，
 * 提高了程序的性能和响应速度。
 *
 * @see TelnetClient
 * @since 3.0
 ***/
public interface TelnetInputListener
{

    /***
     * Telnet输入数据可用时的回调方法
     *
     * 当Telnet客户端的输入流中有新的传入数据可供读取时，将调用此方法。
     *
     * 此方法是监听器的核心回调函数，用于处理Telnet客户端接收到的数据。
     * 当 {@link TelnetClient} 检测到其 {@link TelnetClient#getInputStream 输入流}
     * 中有新数据到达时，会通知所有已注册的监听器，并触发此方法的调用。
     *
     * 使用场景：
     * 1. 实时监控Telnet会话的输入数据
     * 2. 异步处理Telnet协议的通信内容
     * 3. 实现基于事件驱动的Telnet客户端应用
     *
     * 注意事项：
     * - 此方法应该尽快返回，避免阻塞其他监听器的执行
     * - 如果需要处理大量数据，建议使用单独的线程进行处理
     * - 在此方法中读取数据时，应当处理可能的IO异常
     *
     * @see TelnetClient#registerInputListener
     ***/
    public void telnetInputAvailable();
}
