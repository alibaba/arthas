package com.taobao.arthas.core.command.hidden;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.middleware.cli.annotations.Hidden;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 感谢命令
 *
 * 这是一个隐藏的彩蛋命令，用于显示Arthas项目的贡献者和致谢信息。
 * 当用户输入 "thanks" 命令时，会显示所有为该项目做出贡献的人员和组织列表。
 *
 * 命令特点：
 * - 被标记为 @Hidden，因此在帮助信息中不会显示
 * - 这是一个展示项目社区贡献的方式
 * - 表达对开源社区贡献者的感谢
 *
 * 设计理念：
 * 开源项目的发展离不开社区的支持，这个命令用于：
 * 1. 致谢所有贡献者
 * 2. 展示项目的社区活力
 * 3. 体现开源精神
 *
 * 工具介绍<br/>
 * 感谢
 *
 * @author vlinux on 15/9/1.
 */
@Name("thanks")
@Summary("Credits to all personnel and organization who either contribute or help to this product. Thanks you all!")
@Hidden
public class ThanksCommand extends AnnotatedCommand {

    /**
     * 处理感谢命令
     * 当用户输入 thanks 命令时，此方法被调用，用于显示贡献者信息
     *
     * @param process 命令处理上下文对象，用于输出结果和结束命令
     */
    @Override
    public void process(CommandProcess process) {
        // 从ArthasBanner工具类获取致谢信息并输出
        // credit() 方法返回包含所有贡献者和组织信息的字符串
        process.write(ArthasBanner.credit()).write("\n").end();
    }
}
