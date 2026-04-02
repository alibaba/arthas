package com.taobao.arthas.core.shell.term.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.term.TermServer;
import io.termd.core.readline.Keymap;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * 辅助工具类，用于加载键盘映射配置文件
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Helper {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    /**
     * 加载键盘映射配置
     *
     * @return 键盘映射对象
     */
    public static Keymap loadKeymap() {
        return new Keymap(loadInputRcFile());
    }

    /**
     * 加载inputrc配置文件
     * 按照以下优先级顺序加载：
     * 1. 用户自定义的配置文件：~/.arthas/conf/inputrc
     * 2. Arthas默认的配置文件：从类路径中加载
     * 3. termd库的默认配置文件：从termd库的jar包中加载
     *
     * @return inputrc配置文件的输入流
     * @throws IllegalStateException 如果所有加载尝试都失败
     */
    public static InputStream loadInputRcFile() {
        InputStream inputrc;
        // 步骤1：尝试加载用户自定义的键盘映射配置文件
        try {
            String customInputrc = System.getProperty("user.home") + "/.arthas/conf/inputrc";
            inputrc = new FileInputStream(customInputrc);
            logger.info("Loaded custom keymap file from " + customInputrc);
            return inputrc;
        } catch (Throwable e) {
            // 忽略异常，继续尝试下一个加载方式
        }

        // 步骤2：尝试加载Arthas默认的键盘映射配置文件
        inputrc = TermServer.class.getClassLoader().getResourceAsStream(ShellServerOptions.DEFAULT_INPUTRC);
        if (inputrc != null) {
            logger.info("Loaded arthas keymap file from " + ShellServerOptions.DEFAULT_INPUTRC);
            return inputrc;
        }

        // 步骤3：回退到termd库的默认键盘映射配置文件
        inputrc = Keymap.class.getResourceAsStream("inputrc");
        if (inputrc != null) {
            return inputrc;
        }

        throw new IllegalStateException("Could not load inputrc file.");
    }

    // 注释掉的代码：从文件系统加载资源
    //    public static Buffer loadResource(FileSystem fs, String path) {
    //        try {
    //            return fs.readFileBlocking(path);
    //        } catch (Exception e) {
    //            return loadResource(path);
    //        }
    //    }

    // 注释掉的代码：从类路径加载资源
    //    public static Buffer loadResource(String path) {
    //        URL resource = HttpTermServer.class.getResource(path);
    //        if (resource != null) {
    //            try {
    //                byte[] tmp = new byte[512];
    //                InputStream in = resource.openStream();
    //                Buffer buffer = Buffer.buffer();
    //                while (true) {
    //                    int l = in.read(tmp);
    //                    if (l == -1) {
    //                        break;
    //                    }
    //                    buffer.appendBytes(tmp, 0, l);
    //                }
    //                return buffer;
    //            } catch (IOException ignore) {
    //            }
    //        }
    //        return null;
    //    }
}
