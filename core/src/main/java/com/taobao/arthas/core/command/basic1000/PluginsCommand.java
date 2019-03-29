package com.taobao.arthas.core.command.basic1000;

import static com.taobao.text.ui.Element.label;

import com.taobao.arthas.agent.ArthasAgent;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.plugin.Plugin;
import com.taobao.arthas.plugin.PluginManager;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

@Name("plugins")
@Summary("Manage plugins")
public class PluginsCommand extends AnnotatedCommand {
    private static final Logger logger = LogUtil.getArthasLogger();

    private boolean listPlugins;

    private String startPlugin;

    private String stopPlugin;

    private String uninstallPlugin;

    @Option(shortName = "l", longName = "list")
    @Description("List plugins")
    public void setListPlugins(boolean listPlugins) {
        this.listPlugins = listPlugins;
    }

    @Option(longName = "start")
    @Description("Start plugin")
    public void setStartPlugin(String plugin) {
        this.startPlugin = plugin;
    }

    @Option(longName = "stop")
    @Description("Stop plugin")
    public void setStopPlugin(String plugin) {
        this.stopPlugin = plugin;
    }

    @Option(longName = "uninstall")
    @Description("Uninstall plugin")
    public void setUninstallPlugin(String plugin) {
        this.uninstallPlugin = plugin;
    }

    @Override
    public void process(CommandProcess process) {
        int exitCode = 0;
        try {
            PluginManager pluginMaanger = ArthasAgent.getInstance().pluginMaanger();
            if (this.startPlugin != null) {
                Plugin findPlugin = pluginMaanger.findPlugin(startPlugin);
                if (findPlugin != null) {
                    pluginMaanger.startPlugin(startPlugin);
                    process.write("Start plugin " + startPlugin + " success.\n");
                }else {
                    process.write("Can not find plugin " + startPlugin + "\n");
                }
            } else if (this.stopPlugin != null) {
                Plugin findPlugin = pluginMaanger.findPlugin(stopPlugin);
                if (findPlugin != null) {
                    pluginMaanger.stopPlugin(stopPlugin);
                    process.write("Stop plugin " + stopPlugin + " success.\n");
                }else {
                    process.write("Can not find plugin " + stopPlugin + "\n");
                }
            } else if(this.uninstallPlugin != null) {
                Plugin findPlugin = pluginMaanger.findPlugin(uninstallPlugin);
                if (findPlugin != null) {
                    pluginMaanger.uninstallPlugin(uninstallPlugin);
                    process.write("Uninstall plugin " + uninstallPlugin + " success.\n");
                }else {
                    process.write("Can not find plugin " + uninstallPlugin + "\n");
                }
            }
            else {
                TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
                table.row(true, label("Name").style(Decoration.bold.bold()),
                                label("State").style(Decoration.bold.bold()));

                for (Plugin plugin : pluginMaanger.allPlugins()) {
                    table.row(plugin.name(), "" + plugin.state(), plugin.location().toString());
                }
                process.write(RenderUtil.render(table, process.width()));
            }
        } catch (Throwable e) {
            logger.error(null, null, e);
        }
        process.end(exitCode);

    }

    // @Override
    // public void complete(Completion completion) {
    // if (!CompletionUtils.completeFilePath(completion)) {
    // super.complete(completion);
    // }
    // }

}
