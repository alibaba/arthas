package com.taobao.arthas.core.util;

import static com.taobao.text.ui.Element.label;

import java.security.CodeSource;

import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;

/**
 *
 * @author hengyunabc 2018-10-18
 *
 */
public class ClassUtils {

    public static String getCodeSource(final CodeSource cs) {
        if (null == cs || null == cs.getLocation() || null == cs.getLocation().getFile()) {
            return com.taobao.arthas.core.util.Constants.EMPTY_STRING;
        }

        return cs.getLocation().getFile();
    }

    public static Element renderClassInfo(Class<?> clazz) {
        return renderClassInfo(clazz, false, null);
    }

    public static Element renderClassInfo(Class<?> clazz, boolean isPrintField, Integer expand) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        CodeSource cs = clazz.getProtectionDomain().getCodeSource();

        table.row(label("class-info").style(Decoration.bold.bold()), label(StringUtils.classname(clazz)))
                .row(label("code-source").style(Decoration.bold.bold()), label(getCodeSource(cs)))
                .row(label("name").style(Decoration.bold.bold()), label(StringUtils.classname(clazz)))
                .row(label("isInterface").style(Decoration.bold.bold()), label("" + clazz.isInterface()))
                .row(label("isAnnotation").style(Decoration.bold.bold()), label("" + clazz.isAnnotation()))
                .row(label("isEnum").style(Decoration.bold.bold()), label("" + clazz.isEnum()))
                .row(label("isAnonymousClass").style(Decoration.bold.bold()), label("" + clazz.isAnonymousClass()))
                .row(label("isArray").style(Decoration.bold.bold()), label("" + clazz.isArray()))
                .row(label("isLocalClass").style(Decoration.bold.bold()), label("" + clazz.isLocalClass()))
                .row(label("isMemberClass").style(Decoration.bold.bold()), label("" + clazz.isMemberClass()))
                .row(label("isPrimitive").style(Decoration.bold.bold()), label("" + clazz.isPrimitive()))
                .row(label("isSynthetic").style(Decoration.bold.bold()), label("" + clazz.isSynthetic()))
                .row(label("simple-name").style(Decoration.bold.bold()), label(clazz.getSimpleName()))
                .row(label("modifier").style(Decoration.bold.bold()), label(StringUtils.modifier(clazz.getModifiers(), ',')))
                .row(label("annotation").style(Decoration.bold.bold()), label(TypeRenderUtils.drawAnnotation(clazz)))
                .row(label("interfaces").style(Decoration.bold.bold()), label(TypeRenderUtils.drawInterface(clazz)))
                .row(label("super-class").style(Decoration.bold.bold()), TypeRenderUtils.drawSuperClass(clazz))
                .row(label("class-loader").style(Decoration.bold.bold()), TypeRenderUtils.drawClassLoader(clazz))
                .row(label("classLoaderHash").style(Decoration.bold.bold()), label(StringUtils.classLoaderHash(clazz)));

        if (isPrintField) {
            table.row(label("fields"), TypeRenderUtils.drawField(clazz, expand));
        }
        return table;
    }

}
