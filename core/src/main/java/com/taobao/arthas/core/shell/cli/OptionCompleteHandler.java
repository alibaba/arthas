package com.taobao.arthas.core.shell.cli;

/**
 * 选项补全处理器接口
 * <p>
 * 该接口定义了命令行选项补全的处理规范，用于在用户输入命令时提供智能补全功能。
 * 通过实现此接口，可以为特定的命令选项提供自定义的补全逻辑。
 * </p>
 *
 * @author hengyunabc 2021-04-29
 */
public interface OptionCompleteHandler {
    /**
     * 判断给定的token是否匹配当前处理器
     * <p>
     * 该方法用于检查用户输入的token是否符合当前补全处理器的匹配条件。
     * 通常根据选项名称或前缀来进行匹配判断。
     * </p>
     *
     * @param token 用户输入的token字符串，通常是命令行中的某个选项或参数
     * @return 如果token匹配当前处理器，返回true；否则返回false
     */
    boolean matchName(String token);

    /**
     * 执行补全操作
     * <p>
     * 当token匹配成功后，调用此方法来执行实际的补全逻辑。
     * 补全结果会通过传入的Completion对象返回给用户。
     * </p>
     *
     * @param completion 补全上下文对象，包含了补全所需的所有信息和状态
     * @return 如果补全成功执行，返回true；如果补全失败或无法提供补全建议，返回false
     */
    boolean complete(Completion completion);
}
