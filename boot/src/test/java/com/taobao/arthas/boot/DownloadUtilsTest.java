package com.taobao.arthas.boot;

import com.taobao.arthas.common.IOUtils;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Assert;
import static com.taobao.arthas.boot.DownloadUtils.*;

public class DownloadUtilsTest {

    @Test
    public void testReadMavenReleaseVersion() {
        Assert.assertNull(readMavenReleaseVersion(""));
    }

    @Test
    public void testReadAllMavenVersion() {
        Assert.assertEquals(new ArrayList<String>(), readAllMavenVersion(""));
    }

    @Test
    public void testGetRepoUrl() {
        Assert.assertEquals("http", getRepoUrl("https/", true));
        Assert.assertEquals("https://repo1.maven.org/maven2", getRepoUrl("center", false));
        Assert.assertEquals("https://maven.aliyun.com/repository/public", getRepoUrl("aliyun", false));
    }

    @Test
    public void testReadMavenMetaData() throws IOException {
        String url = "http://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/maven-metadata.xml";
        Assert.assertEquals(IOUtils.toString(new URL(url).openStream()), readMavenMetaData("center", true));

        Assert.assertNull(readMavenMetaData("", false));
        Assert.assertNull(readMavenMetaData("https/", false));
    }
}
