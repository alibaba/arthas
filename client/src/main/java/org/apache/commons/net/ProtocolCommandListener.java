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
import java.util.EventListener;

/***
 * 存在一大类IETF协议，它们通过向服务器发送ASCII文本命令和参数，
 * 然后接收ASCII文本回复来工作。为了调试和其他目的，
 * 记录或跟踪协议消息的内容非常有用。
 * ProtocolCommandListener接口与
 * {@link ProtocolCommandEvent}类配合使用，可简化此过程。
 * <p>
 * 要接收ProtocolCommandEvents，只需实现ProtocolCommandListener接口
 * 并将类作为监听器注册到ProtocolCommandEvent源，
 * 例如{@link org.apache.commons.net.ftp.FTPClient}。
 *
 *
 * @see ProtocolCommandEvent
 * @see ProtocolCommandSupport
 ***/

// ProtocolCommandListener接口：协议命令监听器
// 该接口继承EventListener，用于监听协议命令事件
// 实现此接口的类可以监听协议命令的发送和服务器回复的接收
public interface ProtocolCommandListener extends EventListener
{

    /***
     * 此方法由ProtocolCommandEvent源在向服务器发送协议命令后调用。
     *
     * @param event 触发的ProtocolCommandEvent事件对象
     ***/
    // 当协议命令发送到服务器后调用此方法
    // event参数包含了已发送命令的相关信息，如命令名称和完整消息
    public void protocolCommandSent(ProtocolCommandEvent event);

    /***
     * 此方法由ProtocolCommandEvent源在从服务器接收到回复后调用。
     *
     * @param event 触发的ProtocolCommandEvent事件对象
     ***/
    // 当从服务器接收到回复后调用此方法
    // event参数包含了接收到的回复的相关信息，如回复码和完整消息
    public void protocolReplyReceived(ProtocolCommandEvent event);

}

