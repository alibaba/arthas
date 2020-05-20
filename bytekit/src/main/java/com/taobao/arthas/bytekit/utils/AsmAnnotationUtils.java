package com.taobao.arthas.bytekit.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AnnotationNode;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class AsmAnnotationUtils {

    public static List<String> queryAnnotationInfo(List<AnnotationNode> annotations, String annotationType,
            String key) {
        List<String> result = new ArrayList<String>();
        if (annotations != null) {
            for (AnnotationNode annotationNode : annotations) {
                if (annotationNode.desc.equals(annotationType)) {
                    if (annotationNode.values != null) {
                        Iterator<Object> iterator = annotationNode.values.iterator();
                        while (iterator.hasNext()) {
                            String name = (String) iterator.next();
                            Object values = iterator.next();
                            if (key.equals(name)) {
                                result.addAll((List<String>) values);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static void addAnnotationInfo(List<AnnotationNode> annotations, String annotationType, String key,
            String value) {

        AnnotationNode annotationNode = null;
        for (AnnotationNode tmp : annotations) {
            if (tmp.desc.equals(annotationType)) {
                annotationNode = tmp;
            }
        }

        if (annotationNode == null) {
            annotationNode = new AnnotationNode(annotationType);
            annotations.add(annotationNode);
        }

        if (annotationNode.values == null) {
            annotationNode.values = new ArrayList<Object>();
        }

        // 查找有没有对应的key
        String name = null;
        List<String> values = null;
        Iterator<Object> iterator = annotationNode.values.iterator();
        while (iterator.hasNext()) {
            if (key.equals(iterator.next())) {
                values = (List<String>) iterator.next();
            } else {
                iterator.next();
            }
        }
        if (values == null) {
            values = new ArrayList<String>();
            annotationNode.values.add(key);
            annotationNode.values.add(values);
        }
        if (!values.contains(values)) {
            values.add(value);
        }
    }
}
