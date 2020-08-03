package com.taobao.arthas.core.config;

import java.util.Arrays;

public class Ssl {

	String protocol;
	boolean enabled;

	Boolean testBoolean;

	Long testLong;

	Double testDouble;

	String[] ciphers;

	public String[] getCiphers() {
		return ciphers;
	}

	public void setCiphers(String[] ciphers) {
		this.ciphers = ciphers;
	}

	public Long getTestLong() {
		return testLong;
	}

	public void setTestLong(Long testLong) {
		this.testLong = testLong;
	}

	public Double getTestDouble() {
		return testDouble;
	}

	public void setTestDouble(Double testDouble) {
		this.testDouble = testDouble;
	}

	public Boolean getTestBoolean() {
		return testBoolean;
	}

	public void setTestBoolean(Boolean testBoolean) {
		this.testBoolean = testBoolean;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return "Ssl [protocol=" + protocol + ", enabled=" + enabled + ", testBoolean=" + testBoolean + ", testLong="
				+ testLong + ", testDouble=" + testDouble + ", ciphers=" + Arrays.toString(ciphers) + "]";
	}

}