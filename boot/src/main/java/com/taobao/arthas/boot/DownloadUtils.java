package com.taobao.arthas.boot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.common.IOUtils;

/**
 *
 * @author hengyunabc 2018-11-06
 *
 */
public class DownloadUtils {
    private static final String MAVEN_METADATA_URL = "${REPO}/com/taobao/arthas/arthas-packaging/maven-metadata.xml";
    private static final String REMOTE_DOWNLOAD_URL = "${REPO}/com/taobao/arthas/arthas-packaging/${VERSION}/arthas-packaging-${VERSION}-bin.zip";

    private static final int CONNECTION_TIMEOUT = 3000;

    /**
     * Read release version from maven-metadata.xml
     *
     * @param mavenMetaDataUrl
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static String readMavenReleaseVersion(String mavenMetaDataUrl) {
        InputStream inputStream = null;
        try {
            URLConnection connection = openURLConnection(mavenMetaDataUrl);
            inputStream = connection.getInputStream();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(inputStream);

            NodeList nodeList = document.getDocumentElement().getElementsByTagName("release");

            return nodeList.item(0).getTextContent();
        } catch (Throwable t) {
            AnsiLog.debug("Can not read release version from: " + mavenMetaDataUrl);
            AnsiLog.debug(t);
        } finally {
            IOUtils.close(inputStream);
        }
        return null;
    }

    public static String getLastestVersion(String repoMirror, boolean https) {
        String repoUrl = getRepoUrl(repoMirror, https);
        return readMavenReleaseVersion(MAVEN_METADATA_URL.replace("${REPO}", repoUrl));
    }

    public static String getRepoUrl(String repoMirror, boolean https) {
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
        return repoUrl;
    }

    public static void downArthasPackaging(String repoMirror, boolean https, String arthasVersion, String savePath)
                    throws ParserConfigurationException, SAXException, IOException {
        String repoUrl = getRepoUrl(repoMirror, https);

        File unzipDir = new File(savePath, arthasVersion + File.separator + "arthas");

        File tempFile = File.createTempFile("arthas", "arthas");

        String remoteDownloadUrl = REMOTE_DOWNLOAD_URL.replace("${REPO}", repoUrl).replace("${VERSION}", arthasVersion);
        AnsiLog.info("Start download arthas from remote server: " + remoteDownloadUrl);
        saveUrl(tempFile.getAbsolutePath(), remoteDownloadUrl, true);
        AnsiLog.info("Download arthas success.");
        IOUtils.unzip(tempFile.getAbsolutePath(), unzipDir.getAbsolutePath());
    }

    public static void saveUrl(final String filename, final String urlString, boolean printProgress)
                    throws MalformedURLException, IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            URLConnection connection = openURLConnection(urlString);
            in = new BufferedInputStream(connection.getInputStream());
            List<String> values = connection.getHeaderFields().get("Content-Length");
            int fileSize = 0;
            if (values != null && !values.isEmpty()) {
                String contentLength = (String) values.get(0);
                if (contentLength != null) {
                    // parse the length into an integer...
                    fileSize = Integer.parseInt(contentLength);
                }
            }

            fout = new FileOutputStream(filename);

            final byte data[] = new byte[1024 * 1024];
            int totalCount = 0;
            int count;
            long lastPrintTime = System.currentTimeMillis();
            while ((count = in.read(data, 0, 1024 * 1024)) != -1) {
                totalCount += count;
                long now = System.currentTimeMillis();
                if (now - lastPrintTime > 3000) {
                    AnsiLog.info("File size: {}, Downloaded size: {}", formatFileSize(fileSize),
                                    formatFileSize(totalCount));
                    lastPrintTime = now;
                }

                fout.write(data, 0, count);
            }
        } finally {
            IOUtils.close(in);
            IOUtils.close(fout);
        }
    }

    /**
     * support redirect
     *
     * @param url
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private static URLConnection openURLConnection(String url) throws MalformedURLException, IOException {
        URLConnection connection = new URL(url).openConnection();
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setConnectTimeout(CONNECTION_TIMEOUT);
            // normally, 3xx is redirect
            int status = ((HttpURLConnection) connection).getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                                || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = ((HttpURLConnection) connection).getHeaderField("Location");
                    AnsiLog.debug("Try to open url: {}, redirect to: {}", url, newUrl);
                    return openURLConnection(newUrl);
                }
            }
        }
        return connection;
    }

    private static String formatFileSize(long size) {
        String hrSize = null;

        double b = size;
        double k = size / 1024.0;
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);
        double t = ((((size / 1024.0) / 1024.0) / 1024.0) / 1024.0);

        DecimalFormat dec = new DecimalFormat("0.00");

        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(b).concat(" Bytes");
        }

        return hrSize;
    }

}
