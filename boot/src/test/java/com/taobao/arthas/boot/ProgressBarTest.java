package com.taobao.arthas.boot;

import org.junit.Assert;

import java.io.File;
import java.io.IOException;

/**
 * @author: xunjunjie
 * @date: 2020/9/9 3:53 下午
 * @description:
 */
public class ProgressBarTest {

    private static void recursiveDelete(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File each : files) {
                recursiveDelete(each);
            }
        }
        file.delete();
    }

    public static void main(String[] args) throws IOException {
        String version = "3.3.7";
        File folder = new File("./boot/src/test/java/com/taobao/arthas/boot");
        System.err.println(folder.getAbsolutePath());
        DownloadUtils.downArthasPackaging("aliyun", false, version, folder.getAbsolutePath());

        File as = new File(folder, version + File.separator + "arthas" + File.separator + "as.sh");
        Assert.assertTrue(as.exists());
        recursiveDelete(new File(folder.getAbsolutePath() + File.separator + version));
    }
}
