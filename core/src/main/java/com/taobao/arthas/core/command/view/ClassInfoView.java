package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ClassInfoModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.text.util.RenderUtil;

/**
 * @author gongdewei 2020/4/8
 */
public class ClassInfoView extends ResultView<ClassInfoModel> {
    @Override
    public void draw(CommandProcess process, ClassInfoModel result) {
        if (result.detail()) {
            process.write(RenderUtil.render(ClassUtils.renderClassInfo(result.getClassInfo(), result.withField(), result.expand()), process.width()));
        } else {
            process.write(result.getClassInfo().getName());
        }
        process.write("\n");
    }

}
