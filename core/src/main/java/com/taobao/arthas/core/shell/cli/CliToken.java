package com.taobao.arthas.core.shell.cli;

public interface CliToken {
    /**
     * @return the token value
     */
    String value();

    /**
     * @return the raw token value, that may contain unescaped chars, for instance {@literal "ab\"cd"}
     */
    String raw();

    /**
     * @return true when it's a text token
     */
    boolean isText();

    /**
     * @return true when it's a blank token
     */
    boolean isBlank();
}
