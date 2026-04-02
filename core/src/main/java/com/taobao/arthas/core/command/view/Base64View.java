package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.Base64Model;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Base64命令的结果视图类
 *
 * 负责将Base64编码/解码命令的结果渲染并输出到命令行界面。
 * 继承自ResultView基类，用于处理Base64Model类型的结果数据。
 *
 * @author hengyunabc 2021-01-05
 *
 */
public class Base64View extends ResultView<Base64Model> {

    /**
     * 绘制Base64命令的执行结果
     *
     * 该方法将Base64编码或解码后的内容输出到命令行界面。
     * 如果结果内容不为空，则写入内容，最后统一添加换行符。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param result Base64命令的执行结果模型，包含编码或解码后的内容
     */
    @Override
    public void draw(CommandProcess process, Base64Model result) {
        // 获取Base64处理后的内容
        String content = result.getContent();
        // 如果内容不为空，则将其写入命令行
        if (content != null) {
            process.write(content);
        }
        // 输出换行符，使显示更清晰
        process.write("\n");
    }

}
