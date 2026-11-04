package com.taobao.arthas.compiler;

import org.junit.Assert;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.List;

/**
 * description: PackageInternalsFinderTest <br>
 * date: 2021/9/23 12:55 下午 <br>
 * author: zzq0324 <br>
 * version: 1.0 <br>
 */
public class PackageInternalsFinderTest {

    @Test
    public void testFilePathContainWhitespace() throws IOException {
        PackageInternalsFinder finder = new PackageInternalsFinder(this.getClass().getClassLoader());
        List<JavaFileObject> fileObjectList= finder.find("file/test folder");

        Assert.assertEquals(fileObjectList.size(), 0);
    }

    @Test
    public void testFilePathContainChineseCharacter() throws IOException {
        PackageInternalsFinder finder = new PackageInternalsFinder(this.getClass().getClassLoader());
        List<JavaFileObject> fileObjectList= finder.find("file/测试目录");

        Assert.assertEquals(fileObjectList.size(), 0);
    }
}
