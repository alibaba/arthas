package com.taobao.arthas.core.command.basic1000;


import com.taobao.arthas.core.command.result.ExecResult;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArthasBanner;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 输出版本
 *
 * @author vlinux
 */
@Name("version")
@Summary("Display Arthas version")
public class VersionCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        VersionResult result = new VersionResult();
        result.setVersion(ArthasBanner.version());
        process.appendResult(result);
        process.end();
    }

    public static class VersionResult extends ExecResult {

        private String version;

        @Override
        public String getType() {
            return "version";
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        protected void write(CommandProcess process) {
            writeln(process, this.version);
        }
    }
}
