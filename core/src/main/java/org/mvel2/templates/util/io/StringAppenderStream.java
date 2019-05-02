package org.mvel2.templates.util.io;

import org.mvel2.templates.util.TemplateOutputStream;
import org.mvel2.util.StringAppender;

public class StringAppenderStream implements TemplateOutputStream {

    private StringAppender appender;

    public StringAppenderStream(StringAppender appender) {
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
