package com.taobao.arthas.bytekit.utils;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.stereotype.Service;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.taobao.arthas.bytekit.utils.AsmAnnotationUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class AsmAnnotationUtilsTest {

    @Target(value = { ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface AdviceInfo {

        public String[] adviceInfos();
    }

    @Service
    @AdviceInfo(adviceInfos = { "xxxx", "yyy" })
    static class AAA {

        @AdviceInfo(adviceInfos = { "mmm", "yyy" })
        public void test() {

        }

    }

    @Service
    static class BBB {
        public void test() {
        }
    }

    @Test
    public void test() throws IOException {
        ClassNode classNodeA = AsmUtils.loadClass(AAA.class);

        ClassNode classNodeB = AsmUtils.loadClass(BBB.class);

        Assertions.assertThat(AsmAnnotationUtils.queryAnnotationInfo(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos")).isEqualTo(Arrays.asList("xxxx", "yyy"));

        AsmAnnotationUtils.addAnnotationInfo(classNodeA.visibleAnnotations, Type.getDescriptor(AdviceInfo.class),
                "adviceInfos", "fff");

        Assertions
                .assertThat(AsmAnnotationUtils.queryAnnotationInfo(classNodeA.visibleAnnotations,
                        Type.getDescriptor(AdviceInfo.class), "adviceInfos"))
                .isEqualTo(Arrays.asList("xxxx", "yyy", "fff"));

        Assertions.assertThat(AsmAnnotationUtils.queryAnnotationInfo(classNodeB.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos")).isEmpty();

        AsmAnnotationUtils.addAnnotationInfo(classNodeB.visibleAnnotations, Type.getDescriptor(AdviceInfo.class),
                "adviceInfos", "fff");

        Assertions.assertThat(AsmAnnotationUtils.queryAnnotationInfo(classNodeB.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos")).isEqualTo(Arrays.asList("fff"));

    }

}
