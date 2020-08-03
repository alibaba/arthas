package com.taobao.arthas.boot;

import com.taobao.arthas.common.IOUtils;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Assert;
import org.w3c.dom.Document;

import static com.taobao.arthas.boot.DownloadUtils.*;

public class DownloadUtilsTest {

    @Test
    public void testReadMavenReleaseVersion() {
        //check 'center' repo
        String releaseVersion = readMavenReleaseVersion(readMavenMetaData("center", false));
        Assert.assertNotNull(releaseVersion);
        Assert.assertNotEquals("releaseVersion is empty", "", releaseVersion.trim());
        //check 'aliyun' repo
        String aliyunReleaseVersion = readMavenReleaseVersion(readMavenMetaData("aliyun", false));
        Assert.assertEquals("releaseVersion is not match between repo 'center' and 'aliyun'", releaseVersion, aliyunReleaseVersion);
    }

    @Test
    public void testReadAllMavenVersion() {
        Assert.assertNotEquals(new ArrayList<String>(), readAllMavenVersion(readMavenMetaData("center", false)));
    }

    @Test
    public void testGetRepoUrl() {
        Assert.assertEquals("http", getRepoUrl("https/", true));
        Assert.assertEquals("https://repo1.maven.org/maven2", getRepoUrl("center", false));
        Assert.assertEquals("https://maven.aliyun.com/repository/public", getRepoUrl("aliyun", false));
    }

    @Test
    public void testReadMavenMetaData() throws IOException {
        String url = "https://repo1.maven.org/maven2/com/taobao/arthas/arthas-packaging/maven-metadata.xml";
        Assert.assertEquals(IOUtils.toString(new URL(url).openStream()), readMavenMetaData("center", false));
    }

    @Test
    public void testXXE() throws Exception {
        try {
            //from https://blog.spoock.com/2018/10/23/java-xxe/
            String playload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE root [\n" +
                    "        <!ENTITY xxe SYSTEM \"../NOTICE\">\n" +
                    "        ]>\n" +
                    "<evil>&xxe;</evil>";
            Document document = transformMavenMetaData(playload);
        } catch (org.xml.sax.SAXParseException e) {
            String message = e.getMessage();
            Assert.assertTrue("XXE is not disabled", message.contains("disallow-doctype-decl"));
            return;
        }
        Assert.fail("XXE is not disabled");
    }
}
