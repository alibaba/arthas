package com.taobao.arthas.boot;

import com.taobao.arthas.common.IOUtils;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Assert;
import static com.taobao.arthas.boot.DownloadUtils.*;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;


public class DownloadUtilsTest {

	@Rule public final ExpectedException thrown = ExpectedException.none();

	@Rule public final Timeout globalTimeout = new Timeout(10000);

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

	// Test written by Diffblue Cover.

	@Test
	public void downArthasPackagingInputNullFalseNullNullOutputNullPointerException()
			throws IOException, SAXException, ParserConfigurationException {

		// Act
		thrown.expect(NullPointerException.class);
		DownloadUtils.downArthasPackaging(null, false, null, null);

		// The method is not expected to return due to exception thrown
	}

	// Test written by Diffblue Cover.

	@Test
	public void getRepoUrlInputNotNullFalseOutputNotNull() {

		// Arrange
		final String repoMirror = "a,b,c";
		final boolean http = false;

		// Act
		final String actual = DownloadUtils.getRepoUrl(repoMirror, http);

		// Assert result
		Assert.assertEquals("a,b,c", actual);
	}

	// Test written by Diffblue Cover.
	@Test
	public void getRepoUrlInputNotNullFalseOutputNotNull2() {

		// Act and Assert result
		Assert.assertEquals(
				"https://repo1.maven.org/maven2",
				DownloadUtils.getRepoUrl(
						"                         center\u0001\u0001" +
								"   \u0000\u0001 \u0001\u0001    \u0003\u0001" +
								"  \u0003 \u0003\u0002\u0001\u0001 \u0003 \u0001" +
								" \u0002  \u0003\u0001  \u0001\u0001\u0001\u0000" +
								"\u0000\u0000\u0000\u0000",
						false));
	}

	// Test written by Diffblue Cover.

	@Test
	public void getRepoUrlInputNotNullTrueOutputNotNull() {

		// Arrange
		final String repoMirror = "a,b,c";
		final boolean http = true;

		// Act
		final String actual = DownloadUtils.getRepoUrl(repoMirror, http);

		// Assert result
		Assert.assertEquals("a,b,c", actual);
	}

	// Test written by Diffblue Cover.

	@Test
	public void getRepoUrlInputNotNullTrueOutputNotNull2() {

		// Arrange
		final String repoMirror = "/";
		final boolean http = true;

		// Act
		final String actual = DownloadUtils.getRepoUrl(repoMirror, http);

		// Assert result
		Assert.assertEquals("", actual);
	}

	// Test written by Diffblue Cover.
	@Test
	public void getRepoUrlInputNotNullTrueOutputNotNull3() {

		// Act and Assert result
		Assert.assertEquals(
				"http\ue009\ue009\ue009\ue009\ue009\ue009\ue009\ue009" +
						"\ue009\ue009\ue009\ue008\ue002\ue002\ue002\ue002\ue009" +
						"\ue009\ue009\ue009\ue009\ue009\ue009\ue009\ue009\ue009" +
						"\ue009\ue009\ue009\ue009\ue009\ue009\ue009\ue009\ue009" +
						"\ue009\ue009\ue008\ue008\ue008\ue008\ue008\ue008\ue008" +
						"\ue008\ue008\ue008\ue008\ue008\ue008\ue008\ue008\ue008" +
						"\ue008\ue008\ue008\ue008\ue008\ue008\ue008\ue008\ue008" +
						"\ue008\ue008\ue008\ue008\ue008\ue008\ue008",
				DownloadUtils.getRepoUrl(
						"https\ue009\ue009\ue009\ue009\ue009\ue009" +
								"\ue009\ue009\ue009\ue009\ue009\ue008\ue002" +
								"\ue002\ue002\ue002\ue009\ue009\ue009\ue009" +
								"\ue009\ue009\ue009\ue009\ue009\ue009\ue009" +
								"\ue009\ue009\ue009\ue009\ue009\ue009\ue009" +
								"\ue009\ue009\ue009\ue008\ue008\ue008\ue008" +
								"\ue008\ue008\ue008\ue008\ue008\ue008\ue008" +
								"\ue008\ue008\ue008\ue008\ue008\ue008\ue008" +
								"\ue008\ue008\ue008\ue008\ue008\ue008\ue008" +
								"\ue008\ue008\ue008\ue008\ue008\ue008\ue008\t",
						true));
	}

	// Test written by Diffblue Cover.
	@Test
	public void readMavenReleaseVersionInputNullOutputNull() {

		// Act and Assert result
		Assert.assertNull(DownloadUtils.readMavenReleaseVersion(null));
	}
}
