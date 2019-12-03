package com.taobao.arthas.core.view;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author ralf0131 2018-07-10 10:55.
 */
public class ObjectViewTest {

    @Test
    public void testNull() {
        ObjectView objectView = new ObjectView(null, 3);
        Assert.assertEquals("null", objectView.draw());
    }

    @Test
    public void testInteger() {
        ObjectView objectView = new ObjectView(new Integer(1), 3);
        Assert.assertEquals("@Integer[1]", objectView.draw());
    }

    @Test
    public void testChar() {
        ObjectView objectView = new ObjectView(new Character('中'), 3);
        Assert.assertEquals("@Character[中]", objectView.draw());
    }

    @Test
    public void testString() {
        ObjectView objectView = new ObjectView("hello\nworld!", 3);
        Assert.assertEquals("@String[hello\\nworld!]", objectView.draw());
    }

    @Test
    public void testList() {
        List<String> data = new ArrayList<String>();
        data.add("aaa");
        data.add("bbb");
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@ArrayList[\n" +
                "    @String[aaa],\n" +
                "    @String[bbb],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testMap() {
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@LinkedHashMap[\n" +
                "    @String[key1]:@String[value1],\n" +
                "    @String[key2]:@String[value2],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testIntArray() {
        int[] data = {1,3,4,5};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@int[][\n" +
                "    @Integer[1],\n" +
                "    @Integer[3],\n" +
                "    @Integer[4],\n" +
                "    @Integer[5],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testLongArray() {
        long[] data = {1L,3L,4L,5L};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@long[][\n" +
                "    @Long[1],\n" +
                "    @Long[3],\n" +
                "    @Long[4],\n" +
                "    @Long[5],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testShortArray() {
        short[] data = {1,3,4,5};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@short[][\n" +
                "    @Short[1],\n" +
                "    @Short[3],\n" +
                "    @Short[4],\n" +
                "    @Short[5],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testFloatArray() {
        float[] data = {1.0f, 3.0f, 4.2f, 5.3f};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@float[][\n" +
                "    @Float[1.0],\n" +
                "    @Float[3.0],\n" +
                "    @Float[4.2],\n" +
                "    @Float[5.3],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testDoubleArray() {
        double[] data = {1.0d, 3.0d, 4.2d, 5.3d};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@double[][\n" +
                "    @Double[1.0],\n" +
                "    @Double[3.0],\n" +
                "    @Double[4.2],\n" +
                "    @Double[5.3],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testBooleanArray() {
        boolean[] data = {true, false, true, true};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@boolean[][\n" +
                "    @Boolean[true],\n" +
                "    @Boolean[false],\n" +
                "    @Boolean[true],\n" +
                "    @Boolean[true],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testCharArray() {
        char[] data = {'a', 'b', 'c', 'd'};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@char[][\n" +
                "    @Character[a],\n" +
                "    @Character[b],\n" +
                "    @Character[c],\n" +
                "    @Character[d],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testByteArray() {
        byte[] data = {'a', 'b', 'c', 'd'};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@byte[][\n" +
                "    @Byte[97],\n" +
                "    @Byte[98],\n" +
                "    @Byte[99],\n" +
                "    @Byte[100],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testObjectArray() {
        String[] data = {"111", "222", "333", "444"};
        ObjectView objectView = new ObjectView(data, 3);
        String expected = "@String[][\n" +
                "    @String[111],\n" +
                "    @String[222],\n" +
                "    @String[333],\n" +
                "    @String[444],\n" +
                "]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testThrowable() {
        Exception t = new Exception("test");
        ObjectView objectView = new ObjectView(t, 3);
        Assert.assertTrue(objectView.draw().startsWith("java.lang.Exception: test"));
    }

    @Test
    public void testDate() {
        Date d = new Date(1531204354961L - TimeZone.getDefault().getRawOffset()
                        + TimeZone.getTimeZone("GMT+8").getRawOffset());
        ObjectView objectView = new ObjectView(d, 3);
        String expected = "@Date[2018-07-10 14:32:34,961]";
        Assert.assertEquals(expected, objectView.draw());
    }

    @Test
    public void testNestedClass() {
        ObjectView objectView = new ObjectView(new NestedClass(100), 3);

        String expected = "@NestedClass[\n" +
                "    code=@Integer[100],\n" +
                "    c1=@NestedClass[\n" +
                "        code=@Integer[1],\n" +
                "        c1=@NestedClass[\n" +
                "            code=@Integer[1],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "        c2=@NestedClass[\n" +
                "            code=@Integer[2],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "    ],\n" +
                "    c2=@NestedClass[\n" +
                "        code=@Integer[2],\n" +
                "        c1=@NestedClass[\n" +
                "            code=@Integer[1],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "        c2=@NestedClass[\n" +
                "            code=@Integer[2],\n" +
                "            c1=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "            c2=@NestedClass[com.taobao.arthas.core.view.ObjectViewTest$NestedClass@ffffffff],\n" +
                "        ],\n" +
                "    ],\n" +
                "]";
        Assert.assertEquals(expected, replaceHashCode(objectView.draw()));
    }

    @Test
    public void testObjectTooLarge() {
        ObjectView objectView = new ObjectView(new NestedClass(100), 3, 100);
        String expected = "@NestedClass[\n" +
                "    code=@Integer[100],\n" +
                "    c1=@NestedClass[\n" +
                "        code=@Integer[1],\n" +
                "        c1=...\n" +
                "... Object size exceeds size limit: 100, try to specify -M size_limit in your command, check the help command for more.";
        Assert.assertEquals(expected, objectView.draw());
    }

    private String replaceHashCode(String input) {
        return input.replaceAll("@[0-9a-f]+", "@ffffffff");
    }


    private static class NestedClass {

        private int code;

        private static NestedClass c1 = get(1);
        private static NestedClass c2 = get(2);

        public NestedClass(int code) {
            this.code = code;
        }

        private static NestedClass get(int code) {
            return new NestedClass(code);
        }
    }

    /**
     * 显示基类属性值
     */
    @Test
    public void testObjectViewBaseFieldValue() {
        SonBean sonBean = new SonBean();
        sonBean.setI(10);
        sonBean.setJ("test");

        ObjectView objectView = new ObjectView(sonBean, 3, 100);
        Assert.assertTrue(objectView.draw().contains("i=@Integer[10]"));
    }

    private class BaseBean {
        private int i;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

    private class SonBean extends BaseBean {
        private String j;

        public String getJ() {
            return j;
        }

        public void setJ(String j) {
            this.j = j;
        }
    }
}
