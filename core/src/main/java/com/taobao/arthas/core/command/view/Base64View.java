package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.Base64Model;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 
 * @author hengyunabc 2021-01-05
 *
 */
public class Base64View extends ResultView<Base64Model> {

    @Override
    public void draw(CommandProcess process, Base64Model result) {
        String content = result.getContent();
        if (content != null) {
            process.write(content);
        }
        process.write("\n");
    }

}
