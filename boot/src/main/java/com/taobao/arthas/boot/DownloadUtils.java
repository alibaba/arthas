package com.taobao.arthas.boot;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
     * @param mavenMetaData
     * @return
     */
    public static String readMavenReleaseVersion(String mavenMetaData) {
        try {
            Document document = transformMavenMetaData(mavenMetaData);
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("release");
            return nodeList.item(0).getTextContent();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Read all versions from maven-metadata.xml
     *
     * @param mavenMetaData
     * @return
     */
    public static List<String> readAllMavenVersion(String mavenMetaData) {
        List<String> result = new ArrayList<String>();
        try {
            Document document = transformMavenMetaData(mavenMetaData);
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("version");
            int length = nodeList.getLength();
            for (int i = 0; i < length; ++i) {
                result.add(nodeList.item(i).getTextContent());
            }
        } catch (Exception e) {
            // ignore
        }
        return result;
    }

    public static String readMavenMetaData(String repoMirror, boolean http) {
        String repoUrl = getRepoUrl(repoMirror, http);
        String metaDataUrl = MAVEN_METADATA_URL.replace("${REPO}", repoUrl);
        AnsiLog.debug("Download maven-metadata.xml from: {}", metaDataUrl);
        InputStream inputStream = null;
        try {
            URLConnection connection = openURLConnection(metaDataUrl);
            inputStream = connection.getInputStream();
            return IOUtils.toString(inputStream);
        } catch (javax.net.ssl.SSLException e) {
            AnsiLog.error("TLS connect error, please try to use --use-http argument.");
            AnsiLog.error("URL: " + metaDataUrl);
            AnsiLog.error(e);
        } catch (Throwable t) {
            AnsiLog.error("Can not read maven-metadata.xml from: " + metaDataUrl);
            AnsiLog.debug(t);
        } finally {
            IOUtils.close(inputStream);
        }
        return null;
    }

    public static String getRepoUrl(String repoMirror, boolean http) {
        repoMirror = repoMirror.trim();
        String repoUrl;
        if (repoMirror.equals("center")) {
            repoUrl = "https://repo1.maven.org/maven2";
        } else if (repoMirror.equals("aliyun")) {
            repoUrl = "https://maven.aliyun.com/repository/public";
        } else {
            repoUrl = repoMirror;
        }
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }

        if (http && repoUrl.startsWith("https")) {
            repoUrl = "http" + repoUrl.substring("https".length());
        }
        return repoUrl;
    }

    public static void downArthasPackaging(String repoMirror, boolean http, String arthasVersion, String savePath)
            throws IOException {
        String repoUrl = getRepoUrl(repoMirror, http);

        File unzipDir = new File(savePath, arthasVersion + File.separator + "arthas");

        File tempFile = File.createTempFile("arthas", "arthas");

        AnsiLog.debug("Arthas download temp file: " + tempFile.getAbsolutePath());

        String remoteDownloadUrl = REMOTE_DOWNLOAD_URL.replace("${REPO}", repoUrl).replace("${VERSION}", arthasVersion);
        AnsiLog.info("Start download arthas from remote server: " + remoteDownloadUrl);
        saveUrl(tempFile.getAbsolutePath(), remoteDownloadUrl, true);
        AnsiLog.info("Download arthas success.");
        IOUtils.unzip(tempFile.getAbsolutePath(), unzipDir.getAbsolutePath());
    }

    public static void saveUrl(final String filename, final String urlString, boolean printProgress)
            throws IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            URLConnection connection = openURLConnection(urlString);
            in = new BufferedInputStream(connection.getInputStream());
            List<String> values = connection.getHeaderFields().get("Content-Length");
            int fileSize = 0;
            if (values != null && !values.isEmpty()) {
                String contentLength = values.get(0);
                if (contentLength != null) {
                    // parse the length into an integer...
                    fileSize = Integer.parseInt(contentLength);
                }
            }

            fout = new FileOutputStream(filename);

            final byte[] data = new byte[1024 * 1024];
            int totalCount = 0;
            int count;
            long lastPrintTime = System.currentTimeMillis();
            while ((count = in.read(data, 0, 1024 * 1024)) != -1) {
                totalCount += count;
                if (printProgress) {
                    long now = System.currentTimeMillis();
                    if (now - lastPrintTime > 1000) {
                        AnsiLog.info("File size: {}, downloaded size: {}, downloading ...", formatFileSize(fileSize),
                                formatFileSize(totalCount));
                        lastPrintTime = now;
                    }
                }
                fout.write(data, 0, count);
            }
        } catch (javax.net.ssl.SSLException e) {
            AnsiLog.error("TLS connect error, please try to add --use-http argument.");
            AnsiLog.error("URL: " + urlString);
            AnsiLog.error(e);
        } finally {
            IOUtils.close(in);
            IOUtils.close(fout);
        }
    }

    /**
     * transform the maven meta data which is in the format of String into document object.
     *
     * @param mavenMetaData
     * @return
     * @throws Exception
     */
    static Document transformMavenMetaData(String mavenMetaData) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(mavenMetaData.getBytes("UTF-8"));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        //disable XXE before newDocumentBuilder
        dbFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbFactory.setXIncludeAware(false);
        dbFactory.setExpandEntityReferences(false);
        //create doc builder
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(inputStream);
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
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            // normally, 3xx is redirect
            int status = ((HttpURLConnection) connection).getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = connection.getHeaderField("Location");
                    AnsiLog.debug("Try to open url: {}, redirect to: {}", url, newUrl);
                    return openURLConnection(newUrl);
                }
            }
        }
        return connection;
    }

    private static String formatFileSize(long size) {
        String hrSize;

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
