package com.taobao.arthas.core.shell.command.internal;

import org.junit.Assert;
import org.junit.Test;

public class GrepHandlerTest {

    @Test
    public void test4grep_ABC() { // -A -B -C
        Object[][] samples = new Object[][] { { "ABC\n1\n2\n3\n4\nc", "ABC", 0, 4, "ABC\n1\n2\n3\n4" },
                        { "ABC\n1\n2\n3\n4\nABC\n5", "ABC", 2, 1, "ABC\n1\n3\n4\nABC\n5" },
                        { "ABC\n1\n2\n3\n4\na", "ABC", 2, 1, "ABC\n1" }, { "ABC\n1\n2\n3\n4\nb", "ABC", 0, 0, "ABC" },
                        { "ABC\n1\n2\n3\n4\nc", "ABC", 0, 5, "ABC\n1\n2\n3\n4\nc" },
                        { "ABC\n1\n2\n3\n4\nc", "ABC", 0, 10, "ABC\n1\n2\n3\n4\nc" },
                        { "ABC\n1\n2\n3\n4\nc", "ABC", 0, 2, "ABC\n1\n2" },
                        { "1\n2\n3\n4\nABC", "ABC", 5, 1, "1\n2\n3\n4\nABC" },
                        { "1\n2\n3\n4\nABC", "ABC", 4, 1, "1\n2\n3\n4\nABC" },
                        { "1\n2\n3\n4\nABC", "ABC", 2, 1, "3\n4\nABC" } };

        for (Object[] args : samples) {
            String word = (String) args[1];
            int beforeLines = (Integer) args[2];
            int afterLines = (Integer) args[3];
            GrepHandler handler = new GrepHandler(word, false, false, true, false, true, beforeLines, afterLines, 0);
            String input = (String) args[0];
            final String ret = handler.apply(input);
            final String expected = (String) args[4];
            Assert.assertEquals(expected, ret.substring(0, ret.length() - 1));
        }
    }

    @Test
    public void test4grep_v() {// -v
        Object[][] samples = new Object[][] { { "ABC\n1\n2\nc", "ABC", 0, 4, "1\n2\nc" },
                        { "ABC\n1\n2\n", "ABC", 0, 0, "1\n2" }, { "ABC\n1\n2\nc", "ABC", 0, 1, "1\n2\nc" } };

        for (Object[] args : samples) {
            String word = (String) args[1];
            int beforeLines = (Integer) args[2];
            int afterLines = (Integer) args[3];
            GrepHandler handler = new GrepHandler(word, false, true, true, false, true, beforeLines, afterLines, 0);
            String input = (String) args[0];
            final String ret = handler.apply(input);
            final String expected = (String) args[4];
            Assert.assertEquals(expected, ret.substring(0, ret.length() - 1));
        }
    }

    @Test
    public void test4grep_e() {// -e
        Object[][] samples = new Object[][] { { "java\n1python\n2\nc", "java|python", "java\n1python" },
                        { "java\n1python\n2\nc", "ja|py", "java\n1python" } };

        for (Object[] args : samples) {
            String word = (String) args[1];
            GrepHandler handler = new GrepHandler(word, false, false, true, false, true, 0, 0, 0);
            String input = (String) args[0];
            final String ret = handler.apply(input);
            final String expected = (String) args[2];
            Assert.assertEquals(expected, ret.substring(0, ret.length() - 1));
        }
    }

    @Test
    public void test4grep_m() {// -e
        Object[][] samples = new Object[][] { { "java\n1python\n2\nc", "java|python", "java", 1 },
                        { "java\n1python\n2\nc", "ja|py", "java\n1python", 2 },
                        { "java\n1python\n2\nc", "ja|py", "java\n1python", 3 } };

        for (Object[] args : samples) {
            String word = (String) args[1];
            int maxCount = args.length > 3 ? (Integer) args[3] : 0;
            GrepHandler handler = new GrepHandler(word, false, false, true, false, true, 0, 0, maxCount);
            String input = (String) args[0];
            final String ret = handler.apply(input);
            final String expected = (String) args[2];
            Assert.assertEquals(expected, ret.substring(0, ret.length() - 1));
        }
    }

    @Test
    public void test4grep_n() {// -n
        Object[][] samples = new Object[][] { { "java\n1\npython\n2\nc", "1:java\n3:python", "java|python" },
                        { "java\n1\npython\njava\nc", "1:java\n4:java", "java", false } };

        for (Object[] args : samples) {
            String word = (String) args[2];
            boolean regexpMode = args.length > 3 ? (Boolean) args[3] : true;
            GrepHandler handler = new GrepHandler(word, false, false, regexpMode, true, true, 0, 0, 0);
            String input = (String) args[0];
            final String ret = handler.apply(input);
            final String expected = (String) args[1];
            Assert.assertEquals(expected, ret.substring(0, ret.length() - 1));
        }
    }

}
