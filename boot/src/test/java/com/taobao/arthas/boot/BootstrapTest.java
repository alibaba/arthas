package com.taobao.arthas.boot;

import com.taobao.arthas.boot.Bootstrap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class BootstrapTest {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public final Timeout globalTimeout = new Timeout(10000);

	// Test written by Diffblue Cover.
	@Test
	public void getArthasHomeOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getArthasHome());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getBatchFileOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getBatchFile());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getCommandOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getCommand());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getHeightOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getHeight());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getHttpPortOutputPositive() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertEquals(8563, bootstrap.getHttpPort());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getPidOutputNegative() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertEquals(-1, bootstrap.getPid());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getRepoMirrorOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getRepoMirror());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getSessionTimeoutOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getSessionTimeout());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getTargetIpOutputNotNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertEquals("127.0.0.1", bootstrap.getTargetIp());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getTelnetPortOutputPositive() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertEquals(3658, bootstrap.getTelnetPort());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getUseVersionOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getUseVersion());
	}

	// Test written by Diffblue Cover.
	@Test
	public void getWidthOutputNull() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertNull(bootstrap.getWidth());
	}

	// Test written by Diffblue Cover.
	@Test
	public void isAttachOnlyOutputFalse() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertFalse(bootstrap.isAttachOnly());
	}

	// Test written by Diffblue Cover.
	@Test
	public void isHelpOutputFalse() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertFalse(bootstrap.isHelp());
	}

	// Test written by Diffblue Cover.
	@Test
	public void isuseHttpOutputFalse() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertFalse(bootstrap.isuseHttp());
	}

	// Test written by Diffblue Cover.
	@Test
	public void isVerboseOutputFalse() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertFalse(bootstrap.isVerbose());
	}

	// Test written by Diffblue Cover.
	@Test
	public void isVersionsOutputFalse() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act and Assert result
		Assert.assertFalse(bootstrap.isVersions());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setBatchFileInputNotNullOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setBatchFile("3");

		// Assert side effects
		Assert.assertEquals("3", bootstrap.getBatchFile());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setCommandInputNotNullOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setCommand("3");

		// Assert side effects
		Assert.assertEquals("3", bootstrap.getCommand());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setHeightInputZeroOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setHeight(0);

		// Assert side effects
		Assert.assertEquals(new Integer(0), bootstrap.getHeight());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setHttpPortInputZeroOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setHttpPort(0);

		// Assert side effects
		Assert.assertEquals(0, bootstrap.getHttpPort());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setPidInputZeroOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setPid(0);

		// Assert side effects
		Assert.assertEquals(0, bootstrap.getPid());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setRepoMirrorInputNotNullOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setRepoMirror("3");

		// Assert side effects
		Assert.assertEquals("3", bootstrap.getRepoMirror());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setTelnetPortInputZeroOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setTelnetPort(0);

		// Assert side effects
		Assert.assertEquals(0, bootstrap.getTelnetPort());
	}

	// Test written by Diffblue Cover.
	@Test
	public void setWidthInputZeroOutputVoid() {

		// Arrange
		final Bootstrap bootstrap = new Bootstrap();

		// Act
		bootstrap.setWidth(0);

		// Assert side effects
		Assert.assertEquals(new Integer(0), bootstrap.getWidth());
	}
}
