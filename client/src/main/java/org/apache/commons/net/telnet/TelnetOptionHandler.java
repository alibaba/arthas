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
 * TelnetOptionHandler类是用于实现telnet选项处理器的基类。
 * <p>
 * TelnetOptionHandler实现了基本的选项处理功能，
 * 并定义了必须实现的抽象方法来定义子协商行为。
 *
 * Telnet协议选项说明：
 * Telnet协议使用选项协商机制来扩展基本功能。选项协商涉及四种命令：
 * - WILL（发送方希望启用某个选项）
 * - WONT（发送方不希望启用某个选项）
 * - DO（接收方被请求启用某个选项）
 * - DONT（接收方被请求禁用某个选项）
 *
 * 本地端和远程端：
 * - 本地端：指当前运行的Telnet客户端
 * - 远程端：指连接的对端Telnet服务器
 ***/
public abstract class TelnetOptionHandler
{
    /***
     * 选项代码
     * 用于标识Telnet选项的类型，例如：ECHO(1)、SUPPRESS_GO_AHEAD(3)等
     ***/
    private int optionCode = -1;

    /***
     * 是否在本地端激活该选项
     * 如果为true，建立连接时会向对端发送WILL请求，表示本地端希望启用该选项
     ***/
    private boolean initialLocal = false;

    /***
     * 是否在远程端激活该选项
     * 如果为true，建立连接时会向对端发送DO请求，要求远程端启用该选项
     ***/
    private boolean initialRemote = false;

    /***
     * 是否接受来自远程端的DO请求
     * 如果为true，当远程端发送DO请求要求本地端启用该选项时，将被接受
     ***/
    private boolean acceptLocal = false;

    /***
     * 是否接受来自远程端的WILL请求
     * 如果为true，当远程端发送WILL请求表示希望启用该选项时，将被接受
     ***/
    private boolean acceptRemote = false;

    /***
     * 选项在本地端是否已激活
     * 当本地端发送的DO请求被远程端确认（WILL响应）时，该标志设置为true
     ***/
    private boolean doFlag = false;

    /***
     * 选项在远程端是否已激活
     * 当本地端发送的WILL请求被远程端确认（DO响应）时，该标志设置为true
     ***/
    private boolean willFlag = false;

    /***
     * TelnetOptionHandler的构造函数。
     * 允许定义该选项在本地/远程端激活时的初始设置，
     * 以及接收到本地/远程端激活请求时的行为。
     * <p>
     * @param optcode - 选项代码，用于标识Telnet选项的类型（如ECHO=1、SUPPRESS_GO_AHEAD=3等）
     * @param initlocal - 如果设置为true，建立连接时会发送WILL请求，表示本地端希望启用该选项
     * @param initremote - 如果设置为true，建立连接时会发送DO请求，要求远程端启用该选项
     * @param acceptlocal - 如果设置为true，将接受来自远程端的任何DO请求（要求本地端启用该选项）
     * @param acceptremote - 如果设置为true，将接受来自远程端的任何WILL请求（远程端希望启用该选项）
     ***/
    public TelnetOptionHandler(int optcode,
                                boolean initlocal,
                                boolean initremote,
                                boolean acceptlocal,
                                boolean acceptremote)
    {
        optionCode = optcode;
        initialLocal = initlocal;
        initialRemote = initremote;
        acceptLocal = acceptlocal;
        acceptRemote = acceptremote;
    }


    /***
     * 获取此选项的选项代码。
     * <p>
     * @return 选项代码，用于标识Telnet选项的类型
     ***/
    public int getOptionCode()
    {
        return (optionCode);
    }

    /***
     * 返回一个布尔值，指示是否接受来自对端的DO请求。
     * DO请求表示对端要求本地端启用该选项。
     * <p>
     * @return 如果应该接受DO请求则返回true
     ***/
    public boolean getAcceptLocal()
    {
        return (acceptLocal);
    }

    /***
     * 返回一个布尔值，指示是否接受来自对端的WILL请求。
     * WILL请求表示对端希望启用该选项。
     * <p>
     * @return 如果应该接受WILL请求则返回true
     ***/
    public boolean getAcceptRemote()
    {
        return (acceptRemote);
    }

    /***
     * 设置该选项对来自对端的DO请求的行为。
     * DO请求表示对端要求本地端启用该选项。
     * <p>
     * @param accept - 如果为true，后续的DO请求将被接受；如果为false，将被拒绝
     ***/
    public void setAcceptLocal(boolean accept)
    {
        acceptLocal = accept;
    }

    /***
     * 设置该选项对来自对端的WILL请求的行为。
     * WILL请求表示对端希望启用该选项。
     * <p>
     * @param accept - 如果为true，后续的WILL请求将被接受；如果为false，将被拒绝
     ***/
    public void setAcceptRemote(boolean accept)
    {
        acceptRemote = accept;
    }

    /***
     * 返回一个布尔值，指示是否在建立连接时向对端发送WILL请求。
     * WILL请求表示本地端希望启用该选项。
     * <p>
     * @return 如果应该在建立连接时发送WILL请求则返回true
     ***/
    public boolean getInitLocal()
    {
        return (initialLocal);
    }

    /***
     * 返回一个布尔值，指示是否在建立连接时向对端发送DO请求。
     * DO请求表示要求对端启用该选项。
     * <p>
     * @return 如果应该在建立连接时发送DO请求则返回true
     ***/
    public boolean getInitRemote()
    {
        return (initialRemote);
    }

    /***
     * 设置该选项是否在建立连接时发送WILL请求。
     * WILL请求表示本地端希望启用该选项。
     * <p>
     * @param init - 如果为true，后续建立连接时将发送WILL请求
     ***/
    public void setInitLocal(boolean init)
    {
        initialLocal = init;
    }

    /***
     * 设置该选项是否在建立连接时发送DO请求。
     * DO请求表示要求对端启用该选项。
     * <p>
     * @param init - 如果为true，后续建立连接时将发送DO请求
     ***/
    public void setInitRemote(boolean init)
    {
        initialRemote = init;
    }

    /***
     * 当接收到来自对端的该选项的子协商数据时调用此方法。
     * <p>
     * 子协商是Telnet协议中用于在选项启用后交换额外数据的机制。
     * 例如，终端类型选项会使用子协商来协商具体的终端类型。
     * <p>
     * 此实现返回null，实际的TelnetOptionHandler子类必须重写此方法
     * 以指定对子协商请求的响应。
     * <p>
     * @param suboptionData - 接收到的子协商数据序列（不包含IAC SB和IAC SE标记）
     * @param suboptionLength - suboptionData中有效数据的长度
     * <p>
     * @return 要发送给子协商序列的响应。TelnetClient会自动添加IAC SB和IAC SE标记。
     *         返回null表示不发送任何响应
     ***/
    public int[] answerSubnegotiation(int suboptionData[], int suboptionLength) {
        return null;
    }

    /***
     * 当该选项在本地端被确认为激活状态时调用此方法
     * （即TelnetClient发送了WILL请求，远程端响应了DO确认）。
     * <p>
     * 此方法用于指定当选项激活时TelnetClient应该发送的子协商序列。
     * 某些选项在激活后需要立即进行子协商以交换参数或配置信息。
     * <p>
     * 此实现返回null，实际的TelnetOptionHandler子类必须重写此方法
     * 以指定选项激活时需要发送的子协商内容。
     * <p>
     * @return TelnetClient应该发送的子协商序列。TelnetClient会自动添加IAC SB和IAC SE标记。
     *         返回null表示不发送任何子协商
     ***/
    public int[] startSubnegotiationLocal() {
        return null;
    }

    /***
     * 当该选项在远程端被确认为激活状态时调用此方法
     * （即TelnetClient发送了DO请求，远程端响应了WILL确认）。
     * <p>
     * 此方法用于指定当选项激活时TelnetClient应该发送的子协商序列。
     * 某些选项在激活后需要立即进行子协商以交换参数或配置信息。
     * <p>
     * 此实现返回null，实际的TelnetOptionHandler子类必须重写此方法
     * 以指定选项激活时需要发送的子协商内容。
     * <p>
     * @return TelnetClient应该发送的子协商序列。TelnetClient会自动添加IAC SB和IAC SE标记。
     *         返回null表示不发送任何子协商
     ***/
    public int[] startSubnegotiationRemote() {
        return null;
    }

    /***
     * 返回一个布尔值，指示发送到对端的WILL请求是否已被确认。
     * WILL请求被确认意味着对端响应了DO，同意启用该选项。
     * <p>
     * @return 如果发送到对端的WILL请求已被确认则返回true
     ***/
    boolean getWill()
    {
        return willFlag;
    }

    /***
     * 设置该选项的WILL请求确认状态
     * （由TelnetClient内部调用）。
     * <p>
     * @param state - 如果为true，表示WILL请求已被对端确认
     ***/
    void setWill(boolean state)
    {
        willFlag = state;
    }

    /***
     * 返回一个布尔值，指示发送到对端的DO请求是否已被确认。
     * DO请求被确认意味着对端响应了WILL，同意启用该选项。
     * <p>
     * @return 如果发送到对端的DO请求已被确认则返回true
     ***/
    boolean getDo()
    {
        return doFlag;
    }


    /***
     * 设置该选项的DO请求确认状态
     * （由TelnetClient内部调用）。
     * <p>
     * @param state - 如果为true，表示DO请求已被对端确认
     ***/
    void setDo(boolean state)
    {
        doFlag = state;
    }
}
