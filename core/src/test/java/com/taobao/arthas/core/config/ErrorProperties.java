package com.taobao.arthas.core.config;

public class ErrorProperties {

	/**
	 * Path of the error controller.
	 */
	// @Value("${error.path:/error}")
	private String path = "/error";

	/**
	 * When to include a "stacktrace" attribute.
	 */
	private IncludeStacktrace includeStacktrace = IncludeStacktrace.NEVER;

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public IncludeStacktrace getIncludeStacktrace() {
		return this.includeStacktrace;
	}

	public void setIncludeStacktrace(IncludeStacktrace includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
	}


	@Override
	public String toString() {
		return "ErrorProperties [path=" + path + ", includeStacktrace=" + includeStacktrace + "]";
	}


	/**
	 * Include Stacktrace attribute options.
	 */
	public enum IncludeStacktrace {

		/**
		 * Never add stacktrace information.
		 */
		NEVER,

		/**
		 * Always add stacktrace information.
		 */
		ALWAYS,

		/**
		 * Add stacktrace information when the "trace" request parameter is
		 * "true".
		 */
		ON_TRACE_PARAM

	}

}