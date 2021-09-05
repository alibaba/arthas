package com.taobao.arthas.core.util;


import com.taobao.text.util.Pair;
import org.junit.Assert;
import org.junit.Test;

public class CharSlicerTest {

    @Test
    public void testCharSlicerLinesMethod() {
        //增大value行数原始lines方法会导致StackOverflow error
        String value = "tttttttttttttttttttttttttttttttttttttttttttttttttttttt\n" +
                "tttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt\n" +
                "tttttttttttttttttttttttttt\n" +
                "ttttttttttttttttttttttttttttttttttttttttttttttttttttt\n" +
                "ttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt\n" +
                "tttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt\n" +
                "ttttttttttttttttttttttttttttttttttttttttttttt\n" +
                "tttttttttt\n" +
                "tttttttttt\n" +
                "tttttttttttttttttttttttttttttttttttttttttttttt\n";
        CharSlicer charSlicer = new CharSlicer(value);
        Pair<Integer, Integer>[] strLines = charSlicer.lines(99);
        Assert.assertEquals(10, strLines.length);
    }
}
