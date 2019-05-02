package org.mvel2.templates.util.io;

import org.mvel2.templates.util.TemplateOutputStream;

public class StringBuilderStream implements TemplateOutputStream {

    private StringBuilder appender;

    public StringBuilderStream(StringBuilder appender) {
        this.appender = appender;
    }

    public TemplateOutputStream append(CharSequence c) {
        appender.append(c);
        return this;
    }

    public TemplateOutputStream append(char[] c) {
        appender.append(c);
        return this;
    }

    @Override
    public String toString() {
        return appender.toString();
    }
}
