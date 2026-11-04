package com.taobao.arthas.core.util;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * 
 * @author hengyunabc 2021-02-09
 *
 */
public class DecompilerTest {

    @Test
    public void test() {
        String dir = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

        File classFile = new File(dir, this.getClass().getName().replace('.', '/') + ".class");

        String code = Decompiler.decompile(classFile.getAbsolutePath(), null, true);

        System.err.println(code);

        Assertions.assertThat(code).contains("/*23*/         System.err.println(code);").contains("/*32*/         int i = 0;");
    }

    public void aaa() {

        int jjj = 0;

        for (int i = 0; i < 100; ++i) {
            System.err.println(i);
        }
    }

}
