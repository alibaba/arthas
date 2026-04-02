package com.taobao.arthas.core.shell.term.impl.http;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

import java.net.http.WebSocket;


/**
 * TTY 服务器初始化器
 *
 * 该类用于初始化 HTTP/WebSocket 服务器的通道处理器链。
 * 继承自 Netty 的 ChannelInitializer，在每个新连接建立时配置相应的处理器。
 *
 * 配置的处理器包括：
 * 1. HTTP 服务器编解码器 - 处理 HTTP 请求和响应
 * 2. 分块写入处理器 - 支持大文件和分块传输
 * 3. HTTP 消息聚合器 - 将 HTTP 内容聚合成完整消息
 * 4. HTTP 认证处理器 - 进行身份验证
 * 5. HTTP 请求处理器 - 处理普通 HTTP 请求
 * 6. WebSocket 协议处理器 - 处理 WebSocket 握手和协议升级
 * 7. 空闲状态处理器 - 处理连接空闲超时
 * 8. TTY WebSocket 帧处理器 - 处理 WebSocket 文本帧和 TTY 连接
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyServerInitializer extends ChannelInitializer<SocketChannel> {

  /**
   * 通道组，用于管理所有活跃的连接通道
   * 可以用于向所有连接广播消息或关闭所有连接
   */
  private final ChannelGroup group;

  /**
   * TTY 连接处理器
   * 当有新的 TTY 连接建立时，会调用该处理器
   */
  private final Consumer<TtyConnection> handler;

  /**
   * 工作线程组
   * 用于执行耗时任务，避免阻塞 I/O 线程
   */
  private EventExecutorGroup workerGroup;

  /**
   * HTTP 会话管理器
   * 用于管理 HTTP 会话，包括会话的创建、验证和销毁
   */
  private HttpSessionManager httpSessionManager;

  /**
   * 构造 TTY 服务器初始化器
   *
   * @param group 通道组，用于管理所有活跃连接
   * @param handler TTY 连接处理器，处理新的 TTY 连接
   * @param workerGroup 工作线程组，用于执行耗时任务
   * @param httpSessionManager HTTP 会话管理器
   */
  public TtyServerInitializer(ChannelGroup group, Consumer<TtyConnection> handler, EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
      this.group = group;
      this.handler = handler;
      this.workerGroup = workerGroup;
      this.httpSessionManager = httpSessionManager;
  }

  /**
   * 初始化通道
   *
   * 该方法在每个新连接建立时被调用，用于配置该连接的处理器链。
   * 处理器将按照添加的顺序依次处理消息。
   *
   * @param ch Socket 通道
   * @throws Exception 可能抛出的异常
   */
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {

    // 获取通道的处理器管道
    ChannelPipeline pipeline = ch.pipeline();

    // 添加 HTTP 服务器编解码器
    // 该处理器自动处理 HTTP 请求的解码和 HTTP 响应的编码
    // 支持 HTTP/1.1 和 HTTP/1.0
    pipeline.addLast(new HttpServerCodec());

    // 添加分块写入处理器
    // 支持大文件传输和分块编码（Chunked Encoding）
    // 允许在不确定内容大小的情况下发送数据
    pipeline.addLast(new ChunkedWriteHandler());

    // 添加 HTTP 消息聚合器
    // 将 HttpContent 聚合成完整的 FullHttpRequest 或 FullHttpResponse
    // 最大内容长度为 ArthasConstants.MAX_HTTP_CONTENT_LENGTH
    // 这样可以避免处理分块消息，简化后续处理逻辑
    pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));

    // 添加基础 HTTP 认证处理器
    // 用于验证客户端的身份，检查用户名和密码
    // 只有通过认证的请求才能继续访问
    pipeline.addLast(new BasicHttpAuthenticatorHandler(httpSessionManager));

    // 添加 HTTP 请求处理器
    // 处理普通的 HTTP 请求，如静态资源请求
    // 使用 workerGroup 来处理耗时操作，避免阻塞 I/O 线程
    // DEFAULT_WEBSOCKET_PATH 是 WebSocket 的路径
    pipeline.addLast(workerGroup, "HttpRequestHandler", new HttpRequestHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH));

    /** 添加 WebSocket 服务器协议处理器
    * 处理 WebSocket 握手和协议升级
    * 参数说明：
    * - websocketPath: WebSocket 路径
    * - subprotocols: 支持的子协议（null 表示不支持）
    * - allowExtensions: 是否允许扩展（false）
    * - maxFrameSize: 最大帧大小
    * - allowMaskMismatch: 是否允许掩码不匹配（false）
    * - checkStartsWith: 是否检查请求以 GET 开头（true）
    */
    pipeline.addLast(new WebSocketServerProtocolHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH, null, false, ArthasConstants.MAX_HTTP_CONTENT_LENGTH, false, true));

    /** 添加空闲状态处理器
    * 用于检测连接的空闲状态
    * 参数说明：
    * - readerIdleTime: 读空闲时间（0 表示不检测）
    * - writerIdleTime: 写空闲时间（0 表示不检测）
    * - allIdleTime: 读写空闲时间（秒）
    * 当连接空闲超过指定时间时，会触发 IdleStateEvent
    */
    pipeline.addLast(new IdleStateHandler(0, 0, ArthasConstants.WEBSOCKET_IDLE_SECONDS));

    /** 添加 TTY WebSocket 帧处理器
    * 处理 WebSocket 文本帧，并将其转换为 TTY 连接
    * 这是处理器链的最后一个处理器，负责实际的终端交互
    */
    pipeline.addLast(new TtyWebSocketFrameHandler(group, handler));
  }
}
