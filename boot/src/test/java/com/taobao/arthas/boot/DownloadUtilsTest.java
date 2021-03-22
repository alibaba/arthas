package com.taobao.arthas.boot;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DownloadUtilsTest {
    @Rule
    public TemporaryFolder rootFolder = new TemporaryFolder();

    @Test
    public void testReadReleaseVersion() {
        String releaseVersion = DownloadUtils.readLatestReleaseVersion();
        Assert.assertNotNull(releaseVersion);
        Assert.assertNotEquals("releaseVersion is empty", "", releaseVersion.trim());
        System.err.println(releaseVersion);
    }

    @Test
    public void testReadAllVersions() {
        List<String> versions = DownloadUtils.readRemoteVersions();
        Assert.assertEquals("", true, versions.contains("3.1.7"));
    }

    @Test
    public void testAliyunDownload() throws IOException {
        // fix travis-ci failed problem
        if (TimeUnit.MILLISECONDS.toHours(TimeZone.getDefault().getOffset(System.currentTimeMillis())) == 8) {
            String version = "3.3.7";
            File folder = rootFolder.newFolder();
            System.err.println(folder.getAbsolutePath());
            DownloadUtils.downArthasPackaging("aliyun", false, version, folder.getAbsolutePath());

            File as = new File(folder, version + File.separator + "arthas" + File.separator + "as.sh");
            Assert.assertTrue(as.exists());
        }
    }

    @Test
    public void testCenterDownload() throws IOException {
        String version = "3.1.7";
        File folder = rootFolder.newFolder();
        System.err.println(folder.getAbsolutePath());
        DownloadUtils.downArthasPackaging("center", false, version, folder.getAbsolutePath());

        File as = new File(folder, version + File.separator + "arthas" + File.separator + "as.sh");
        Assert.assertTrue(as.exists());
    }
}
