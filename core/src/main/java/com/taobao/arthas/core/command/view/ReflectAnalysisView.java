package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ReflectAnalysisModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author: Ares
 * @time: 2023-12-21 11:45:05
 * @description: ReflectAnalysis view
 * @version: JDK 1.8
 */
public class ReflectAnalysisView extends ResultView<ReflectAnalysisModel> {

  @Override
  public void draw(CommandProcess process, ReflectAnalysisModel result) {
    String line = String.format("count=%s, ref=%s, names=%s", result.getRefName(),
        result.getGeneratedMethodAccessorNameCount(), result.getGeneratedMethodAccessorNames());
    process.write(line).write("\n");
  }

}
