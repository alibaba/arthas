package com.taobao.arthas.core.util;

import com.taobao.arthas.core.view.ObjectView;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.ui.TreeElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * @author beiwei30 on 24/11/2016.
 */
public class TypeRenderUtils {

    public static String drawAnnotation(Class<?> clazz) {
        return drawAnnotation(clazz.getDeclaredAnnotations());
    }

    public static String drawAnnotation(Method method) {
        return drawAnnotation(method.getDeclaredAnnotations());
    }

    public static String drawInterface(Class<?> clazz) {
        return StringUtils.concat(",", clazz.getInterfaces());
    }

    public static String drawParameters(Method method) {
        return StringUtils.concat("\n", method.getParameterTypes());
    }

    public static String drawParameters(Constructor constructor) {
        return StringUtils.concat("\n", constructor.getParameterTypes());
    }

    public static String drawReturn(Method method) {
        return StringUtils.classname(method.getReturnType());
    }

    public static String drawExceptions(Method method) {
        return StringUtils.concat("\n", method.getExceptionTypes());
    }

    public static String drawExceptions(Constructor constructor) {
        return StringUtils.concat("\n", constructor.getExceptionTypes());
    }

    public static Element drawSuperClass(Class<?> clazz) {
        TreeElement root = new TreeElement();
        TreeElement parent = root;

        Class<?> superClass = clazz.getSuperclass();
        if (null != superClass) {
            TreeElement child = new TreeElement(label(StringUtils.classname(superClass)));
            parent.addChild(child);
            parent = child;

            while (true) {
                superClass = superClass.getSuperclass();
                if (null == superClass) {
                    break;
                }
                TreeElement tempChild = new TreeElement(label(StringUtils.classname(superClass)));
                parent.addChild(tempChild);
                parent = tempChild;
            }
        }
        return root;
    }

    public static Element drawClassLoader(Class<?> clazz) {
        TreeElement root = new TreeElement();
        TreeElement parent = root;
        ClassLoader loader = clazz.getClassLoader();
        if (null != loader) {
            TreeElement child = new TreeElement(label(loader.toString()));
            parent.addChild(child);
            parent = child;
            while (true) {
                loader = loader.getParent();
                if (null == loader) {
                    break;
                }
                TreeElement tempChild = new TreeElement(label(loader.toString()));
                parent.addChild(tempChild);
                parent = tempChild;
            }
        }
        return root;
    }

    public static Element drawField(Class<?> clazz, Integer expand) {
        TableElement fieldsTable = new TableElement().leftCellPadding(0).rightCellPadding(0);
        Field[] fields = clazz.getDeclaredFields();
        if (fields == null || fields.length == 0) {
            return fieldsTable;
        }

        for (Field field : fields) {
            TableElement fieldTable = new TableElement().leftCellPadding(0).rightCellPadding(0);
            fieldTable.row("modifier", StringUtils.modifier(field.getModifiers(), ','))
                    .row("type", StringUtils.classname(field.getType()))
                    .row("name", field.getName());

            Annotation[] annotations = field.getAnnotations();
            if (annotations != null && annotations.length > 0) {
                fieldTable.row("annotation", drawAnnotation(annotations));
            }

            if (Modifier.isStatic(field.getModifiers())) {
                fieldTable.row("value", drawFieldValue(field, expand));
            }

            fieldTable.row(label(""));
            fieldsTable.row(fieldTable);
        }

        return fieldsTable;
    }

    public static String renderMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.modifier(method.getModifiers(), ' ')).append(" ");
        sb.append(TypeRenderUtils.drawReturn(method)).append(" ");
        sb.append(method.getName()).append(" ");
        sb.append("(");
        sb.append(StringUtils.concat(", ", method.getParameterTypes()));
        sb.append(")");
        return sb.toString();
    }

    private static String drawFieldValue(Field field, Integer expand) {
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            Object value = field.get(null);
            Object o = (expand != null && expand >= 0) ? new ObjectView(value, expand).draw() : value;
            return StringUtils.objectToString(o);
        } catch (IllegalAccessException e) {
            // no op
        } finally {
            field.setAccessible(isAccessible);
        }
        return Constants.EMPTY_STRING;
    }

    public static String drawAnnotation(Annotation... annotations) {
        List<Class<?>> types = Collections.emptyList();
        if (annotations != null && annotations.length > 0) {
            types = new LinkedList<Class<?>>();
            for (Annotation annotation : annotations) {
                types.add(annotation.annotationType());
            }
        }
        return StringUtils.concat(",", types.toArray(new Class<?>[0]));
    }
}
