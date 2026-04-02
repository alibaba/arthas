package com.taobao.arthas.core.shell.term.impl.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSession;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.termd.core.http.HttpTtyConnection;

/**
 * 扩展的HTTP TTY连接类
 *
 * 该类继承自termd的HttpTtyConnection，用于处理基于WebSocket的HTTP连接
 * 主要功能包括：
 * 1. 管理与Netty的ChannelHandlerContext的交互，实现WebSocket通信
 * 2. 从HTTP会话中提取用户认证信息（Subject和UserId）
 * 3. 提供任务调度和执行功能
 * 4. 处理连接的关闭操作
 *
 * 从http请求传递过来的 session 信息。解析websocket创建的 term 还需要登录验证问题
 *
 * @author hengyunabc 2021-03-04
 *
 */
public class ExtHttpTtyConnection extends HttpTtyConnection {
    // Netty的通道处理器上下文，用于管理WebSocket连接的输入输出
    private ChannelHandlerContext context;

    /**
     * 构造扩展的HTTP TTY连接
     *
     * @param context Netty的通道处理器上下文，用于管理WebSocket连接
     */
    public ExtHttpTtyConnection(ChannelHandlerContext context) {
        this.context = context;
    }

    /**
     * 向WebSocket连接写入字节数据
     *
     * 该方法将字节数据封装到WebSocket帧中，然后通过Netty通道发送给客户端
     *
     * @param buffer 要写入的字节数组，通常包含终端输出内容
     */
    @Override
    protected void write(byte[] buffer) {
        // 创建一个Netty ByteBuf缓冲区
        ByteBuf byteBuf = Unpooled.buffer();
        // 将字节数组写入缓冲区
        byteBuf.writeBytes(buffer);
        // 如果通道上下文存在，则通过WebSocket发送数据
        if (context != null) {
            // 将字节缓冲区封装到WebSocket文本帧中，并写入通道
            context.writeAndFlush(new TextWebSocketFrame(byteBuf));
        }
    }

    /**
     * 调度一个延迟执行的任务
     *
     * 该方法用于在指定延迟后执行任务，通常用于定时操作或超时处理
     *
     * @param task 要执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位（如秒、毫秒等）
     */
    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        // 如果通道上下文存在，则在通道的事件循环中调度任务
        if (context != null) {
            // 使用通道的事件执行器来调度延迟任务
            context.executor().schedule(task, delay, unit);
        }
    }

    /**
     * 立即执行一个任务
     *
     * 该方法用于在通道的事件循环中异步执行任务
     *
     * @param task 要执行的任务
     */
    @Override
    public void execute(Runnable task) {
        // 如果通道上下文存在，则在通道的事件循环中执行任务
        if (context != null) {
            // 使用通道的事件执行器来执行任务
            context.executor().execute(task);
        }
    }

    /**
     * 关闭WebSocket连接
     *
     * 该方法用于关闭与客户端的WebSocket连接，释放相关资源
     */
    @Override
    public void close() {
        // 如果通道上下文存在，则关闭通道
        if (context != null) {
            // 关闭Netty通道，这会触发连接关闭的清理工作
            context.close();
        }
    }

    /**
     * 从HTTP会话中提取扩展的会话信息
     *
     * 该方法用于从HTTP会话中提取用户认证相关的信息，包括：
     * 1. Subject：用户认证主体，包含用户身份信息
     * 2. UserId：用户ID，用于标识具体的用户
     *
     * 这些信息将传递给Arthas的会话系统，用于权限控制和用户识别
     *
     * @return 包含会话扩展信息的Map，如果没有相关信息则返回空Map
     */
    public Map<String, Object> extSessions() {
        // 如果通道上下文存在，则尝试获取HTTP会话
        if (context != null) {
            // 从通道上下文中获取HTTP会话对象
            HttpSession httpSession = HttpSessionManager.getHttpSessionFromContext(context);
            // 如果HTTP会话存在，则提取其中的认证信息
            if (httpSession != null) {
                // 创建结果Map用于存储会话信息
                Map<String, Object> result = new HashMap<String, Object>();
                // 从HTTP会话中获取用户认证主体（Subject）
                Object subject = httpSession.getAttribute(ArthasConstants.SUBJECT_KEY);
                // 如果Subject存在，则添加到结果中
                if (subject != null) {
                    result.put(ArthasConstants.SUBJECT_KEY, subject);
                }
                // 从HTTP会话中获取用户ID，并将其传递给Arthas会话
                Object userId = httpSession.getAttribute(ArthasConstants.USER_ID_KEY);
                // 如果UserId存在，则添加到结果中
                if (userId != null) {
                    result.put(ArthasConstants.USER_ID_KEY, userId);
                }
                // 如果结果Map不为空，则返回结果
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        // 如果没有找到任何会话信息，返回空的不可变Map
        return Collections.emptyMap();
    }

}
