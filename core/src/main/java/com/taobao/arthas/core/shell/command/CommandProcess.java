package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;
import com.taobao.middleware.cli.CommandLine;

import java.lang.instrument.ClassFileTransformer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 命令处理接口
 *
 * 提供了与命令执行过程交互的能力
 * 命令处理对象封装了命令执行的上下文信息，包括参数、会话、终端等
 * 继承自Tty接口，支持终端相关的操作
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface CommandProcess extends Tty {
    /**
     * 获取未解析的参数令牌列表
     *
     * 返回原始的命令行参数令牌，保留了参数的原始格式和分隔符
     * 令牌包含了参数的类型（普通参数、选项等）和位置信息
     *
     * @return 未解析的参数令牌列表
     */
    List<CliToken> argsTokens();

    /**
     * 获取命令的实际字符串参数列表
     *
     * 返回命令的字符串形式的参数数组
     * 这些是经过基本处理后的参数值
     *
     * @return 命令的字符串参数列表
     */
    List<String> args();

    /**
     * 获取命令行对象
     *
     * 如果命令使用了CLI描述符，返回解析后的命令行对象
     * 命令行对象提供了访问选项、参数等的结构化方式
     * 如果命令没有使用CLI描述符，返回null
     *
     * @return 命令行对象，如果没有使用CLI则返回null
     */
    CommandLine commandLine();

    /**
     * 获取Shell会话对象
     *
     * 会话对象保存了命令执行期间的上下文信息
     * 包括变量、历史记录、Job管理等
     *
     * @return Shell会话对象
     */
    Session session();

    /**
     * 判断命令是否在前台运行
     *
     * 前台运行的命令可以接收用户输入，输出直接显示给用户
     * 后台运行的命令则不占用终端
     *
     * @return 如果命令在前台运行返回true，否则返回false
     */
    boolean isForeground();

    /**
     * 设置标准输入处理器
     *
     * 当命令从标准输入接收到数据时，处理器会被调用
     * 用于处理用户的交互式输入
     *
     * @param handler 输入处理器，接收输入的字符串数据
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess stdinHandler(Handler<String> handler);

    /**
     * 设置中断处理器
     *
     * 当命令被中断时处理器会被调用
     * 中断通常由用户按下 Ctrl-C 触发
     * 可以用于清理资源或优雅地退出
     *
     * @param handler 中断处理器
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess interruptHandler(Handler<Void> handler);

    /**
     * 设置挂起处理器
     *
     * 当命令被挂起时处理器会被调用
     * 挂起通常由用户按下 Ctrl-Z 触发
     * 命令被挂起后会暂停执行，但不会被终止
     *
     * @param handler 挂起处理器
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess suspendHandler(Handler<Void> handler);

    /**
     * 设置恢复处理器
     *
     * 当命令从挂起状态恢复时处理器会被调用
     * 恢复通常由用户执行 bg 或 fg 命令触发
     * 可以用于恢复命令的执行状态
     *
     * @param handler 恢复处理器
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess resumeHandler(Handler<Void> handler);

    /**
     * 设置结束处理器
     *
     * 当命令结束时处理器会被调用
     * 结束可能是正常完成、被中断或Shell关闭等情况
     * 可以用于执行清理操作、释放资源等
     *
     * @param handler 结束处理器
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess endHandler(Handler<Void> handler);

    /**
     * 向标准输出写入文本
     *
     * 将指定的文本数据输出到命令的标准输出
     * 输出会显示在用户的终端上
     *
     * @param data 要输出的文本内容
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess write(String data);

    /**
     * 设置后台运行处理器
     *
     * 当正在运行的命令被放到后台时处理器会被调用
     * 命令进入后台后不再接收用户输入
     *
     * @param handler 后台运行处理器
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess backgroundHandler(Handler<Void> handler);

    /**
     * 设置前台运行处理器
     *
     * 当正在后台运行的命令被切换到前台时处理器会被调用
     * 命令进入前台后可以接收用户输入
     *
     * @param handler 前台运行处理器
     * @return 当前命令处理对象，支持链式调用
     */
    CommandProcess foregroundHandler(Handler<Void> handler);

    /**
     * 设置终端大小变化处理器
     *
     * 当终端窗口大小发生变化时处理器会被调用
     * 可以用于调整输出格式或重新渲染界面
     *
     * @param handler 终端大小变化处理器
     * @return 当前命令处理对象，支持链式调用
     */
    @Override
    CommandProcess resizehandler(Handler<Void> handler);

    /**
     * 结束命令处理
     *
     * 使用退出状态码0结束命令，表示命令成功执行
     * 必须在命令处理完成后调用此方法来结束命令
     */
    void end();

    /**
     * 使用指定的退出状态码结束命令处理
     *
     * 退出状态码用于表示命令的执行结果
     * 通常0表示成功，非0表示错误或异常
     *
     * @param status 退出状态码，0表示成功，非0表示失败
     */
    void end(int status);

    /**
     * 使用指定的退出状态码和消息结束命令处理
     *
     * 在结束时还可以提供一条消息，用于说明命令结束的原因或结果
     *
     * @param status 退出状态码，0表示成功，非0表示失败
     * @param message 结束消息，描述命令结束的原因或结果
     */
    void end(int status, String message);


    /**
     * 注册监听器
     *
     * 注册一个Advice监听器和对应的类文件转换器
     * 用于在Java方法调用时插入增强逻辑
     * 这是Arthas实现方法监控和增强的核心机制
     *
     * @param listener Advice监听器，在方法调用时会被触发
     * @param transformer 类文件转换器，用于修改字节码
     */
    void register(AdviceListener listener, ClassFileTransformer transformer);

    /**
     * 注销监听器
     *
     * 移除之前注册的监听器和转换器
     * 停止对目标方法的增强和监控
     * 通常在命令结束或被中断时调用
     */
    void unregister();

    /**
     * 获取命令执行次数
     *
     * 返回一个原子计数器，记录命令被调用的次数
     * 可用于限制命令的执行次数或实现其他计数相关的功能
     *
     * @return 执行次数计数器
     */
    AtomicInteger times();

    /**
     * 恢复进程执行
     *
     * 将处于挂起状态的命令恢复运行
     * 使命令继续执行之前暂停的工作
     */
    void resume();

    /**
     * 挂起进程执行
     *
     * 暂停当前正在运行的命令
     * 命令进入挂起状态，等待后续恢复
     */
    void suspend();

    /**
     * 输出提示信息
     *
     * 向终端输出提示信息，用于引导用户操作或显示重要信息
     * 提示信息会显示给用户
     *
     * @param tips 要显示的提示信息
     */
    void echoTips(String tips);

    /**
     * 获取缓存文件位置
     *
     * 返回命令使用的缓存文件路径
     * 缓存文件可以用于存储临时数据或中间结果
     *
     * @return 缓存文件的位置路径
     */
    String cacheLocation();

    /**
     * 判断进程是否正在运行
     *
     * 检查命令是否处于活跃的执行状态
     *
     * @return 如果命令正在运行返回true，否则返回false
     */
    boolean isRunning();

    /**
     * 将阶段性结果追加到结果队列
     *
     * 命令可能会产生多个阶段性结果，而不是一次性返回所有结果
     * 此方法将每个阶段性结果追加到队列中，供前端或其他组件消费
     *
     * @param result 命令的阶段性结果对象
     */
    void appendResult(ResultModel result);

}
