package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ThreadPoolModel;
import com.taobao.arthas.core.command.model.ThreadPoolVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.*;
import com.taobao.text.util.RenderUtil;

import java.util.Collection;

/**
 * @author HJ
 * @date 2021-07-09
 **/
public class ThreadPoolView extends ResultView<ThreadPoolModel> {

    @Override
    public void draw(CommandProcess process, ThreadPoolModel result) {
        TableElement table = new TableElement(6, 3, 3, 3, 3)
                .rightCellPadding(1);

        // Header
        table.add(
                new RowElement().style(Decoration.bold.bold()).add(
                        "stackInfo",
                        "corePoolSize",
                        "maximumPoolSize",
                        "activeThreadCount",
                        "currentSizeOfWorkQueue"
                )
        );

        Collection<ThreadPoolVO> threadPoolVOS = result.getThreadPools();
        if (threadPoolVOS != null && threadPoolVOS.size() > 0) {
            for (ThreadPoolVO threadPool : threadPoolVOS) {
                LabelElement currentSizeOfWorkQueueLabel = new LabelElement(threadPool.getCurrentSizeOfWorkQueue());
                if (threadPool.getCurrentSizeOfWorkQueue() > 0) {
                    currentSizeOfWorkQueueLabel.style(Style.style(Color.red));
                }
                LabelElement activeThreadCountLabel = new LabelElement(threadPool.getActiveThreadCount());
                if (threadPool.getActiveThreadCount() >= threadPool.getMaximumPoolSize()) {
                    activeThreadCountLabel.style(Style.style(Color.red));
                }
                table.row(true,
                        new LabelElement(threadPool.getStackInfo()),
                        new LabelElement(threadPool.getCorePoolSize()),
                        new LabelElement(threadPool.getMaximumPoolSize()),
                        activeThreadCountLabel,
                        currentSizeOfWorkQueueLabel
                );
            }
        }
        process.write(RenderUtil.render(table, process.width()));
    }


}
