package com.taobao.arthas.boot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author hengyunabc 2018-11-06
 *
 */
public class DownloadUtils {

    private static final Logger logger = LoggerFactory.getLogger(DownloadUtils.class);

    private static final String MAVEN_METADATA_URL = "${REPO}/com/taobao/arthas/arthas-packaging/maven-metadata.xml";
    private static final String REMOTE_DOWNLOAD_URL = "${REPO}/com/taobao/arthas/arthas-packaging/${VERSION}/arthas-packaging-${VERSION}-bin.zip";

    /**
     * Read release version from maven-metadata.xml
     *
     * @param mavenMetaDataUrl
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static String readMavenReleaseVersion(String mavenMetaDataUrl)
                    throws ParserConfigurationException, SAXException, IOException {
        InputStream inputStream = new URL(mavenMetaDataUrl).openStream();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(inputStream);

            NodeList nodeList = document.getDocumentElement().getElementsByTagName("release");

            return nodeList.item(0).getTextContent();
        } finally {
            IOUtils.close(inputStream);
        }
    }

    public static void downArthasPackaging(String repoMirror, boolean https, String savePath)
                    throws ParserConfigurationException, SAXException, IOException {
        repoMirror = repoMirror.trim();
        String repoUrl = "";
        if (repoMirror.equals("center")) {
            repoUrl = "http://repo1.maven.org/maven2";
        } else if (repoMirror.equals("aliyun")) {
            repoUrl = "http://maven.aliyun.com/repository/public";
        } else {
            repoUrl = repoMirror;
        }
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }

        if (https && repoUrl.startsWith("http")) {
            repoUrl = "https" + repoUrl.substring("http".length(), repoUrl.length());
        }

        String arthasVersion = readMavenReleaseVersion(MAVEN_METADATA_URL.replace("${REPO}", repoUrl));

        File unzipDir = new File(savePath, arthasVersion + File.separator + "arthas");

        File tempFile = File.createTempFile("arthas", "arthas");

        String remoteDownloadUrl = REMOTE_DOWNLOAD_URL.replace("${REPO}", repoUrl).replace("${VERSION}", arthasVersion);
        logger.info("Start download arthas from remote server: " + remoteDownloadUrl);
        saveUrl(tempFile.getAbsolutePath(), remoteDownloadUrl);

        IOUtils.unzip(tempFile.getAbsolutePath(), unzipDir.getAbsolutePath());
    }

    public static void saveUrl(final String filename, final String urlString)
                    throws MalformedURLException, IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(new URL(urlString).openStream());
            fout = new FileOutputStream(filename);

            final byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } finally {
            IOUtils.close(in);
            IOUtils.close(fout);
        }
    }

}
