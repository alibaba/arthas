package com.taobao.arthas.core.command.hidden;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.middleware.cli.annotations.Hidden;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 工具介绍<br/>
 * 感谢
 *
 * @author vlinux on 15/9/1.
 */
@Name("thanks")
@Summary("Credits to all personnel and organization who either contribute or help to this product. Thanks you all!")
@Hidden
public class ThanksCommand extends AnnotatedCommand {
    @Override
    public void process(CommandProcess process) {
        process.write(ArthasBanner.credit()).write("\n").end();
    }
}
