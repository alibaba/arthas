package org.mvel2.templates.util;

public interface TemplateOutputStream {

    public TemplateOutputStream append(CharSequence c);

    public TemplateOutputStream append(char[] c);
}
