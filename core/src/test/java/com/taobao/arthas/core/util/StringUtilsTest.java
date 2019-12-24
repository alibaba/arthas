package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.rules.ExpectedException;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Properties;
/**
 * @author bohrqiu 2018-09-21 01:01
 * @author paulkennethkent 2019-04-08 10:29
 */
public class StringUtilsTest {
    @Rule public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testHumanReadableByteCount() {
        Assert.assertEquals(StringUtils.humanReadableByteCount(1023L), "1023 B");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L), "1.0 KiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024L), "1.0 MiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024L - 100), "1023.9 KiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024 * 1024L), "1.0 GiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024 * 1024 * 1024L), "1.0 TiB");
        Assert.assertEquals(StringUtils.humanReadableByteCount(1024L * 1024 * 1024 * 1024 * 1024), "1.0 PiB");
    }

    @Test
    public void testCause() {
        Assert.assertNull(StringUtils.cause(new Throwable(null, null)));

        final Throwable t2 = new Throwable("error message", new Throwable("error message", null));
        Assert.assertEquals(t2.getMessage(), StringUtils.cause(t2));
    }

    @Test
    public void testCommaDelimitedListToSet() {
        TreeSet set = new TreeSet();
        set.add("foo");
        Assert.assertEquals(set, StringUtils.commaDelimitedListToSet("foo"));
    }

    @Test
    public void testCommaDelimitedListToStringArray() {
        Assert.assertArrayEquals(new String[] {}, StringUtils.commaDelimitedListToStringArray(null));
        Assert.assertArrayEquals(new String[] {}, StringUtils.commaDelimitedListToStringArray(""));
        Assert.assertArrayEquals(new String[] {"/////"}, StringUtils.commaDelimitedListToStringArray("/////"));
        Assert.assertArrayEquals(new String[] {"foo", "bar", "baz"},
            StringUtils.commaDelimitedListToStringArray("foo,bar,baz"));
    }

    @Test
    public void testContainsWhitespaceInputNotNullOutputTrue() {
        Assert.assertTrue(StringUtils.containsWhitespace("foo  "));
        Assert.assertTrue(StringUtils.containsWhitespace(" "));
        Assert.assertFalse(StringUtils.containsWhitespace("!"));
        Assert.assertFalse(StringUtils.containsWhitespace(""));
        Assert.assertFalse(StringUtils.containsWhitespace(null));
    }

    @Test
    public void testCountOccurrencesOf() {
        Assert.assertEquals(0, StringUtils.countOccurrencesOf("44444444", "$$$$$$$$"));
        Assert.assertEquals(0, StringUtils.countOccurrencesOf("$", ""));
        Assert.assertEquals(0, StringUtils.countOccurrencesOf("", ""));
        Assert.assertEquals(1, StringUtils.countOccurrencesOf(";;;;;;;:::", ";;;;;;;:"));
        Assert.assertEquals(3, StringUtils.countOccurrencesOf("foofoofoo", "foo"));
    }

    @Test
    public void testDeleteAny() {
        Assert.assertEquals("", StringUtils.deleteAny("\"", "\"!!!!!!!! "));
        Assert.assertEquals("\"", StringUtils.deleteAny("\"", "$ 00000000"));
        Assert.assertEquals("!", StringUtils.deleteAny("!", ""));
        Assert.assertEquals("", StringUtils.deleteAny("", ""));
        Assert.assertEquals("barbar", StringUtils.deleteAny("foobarfoobar", "foo"));
    }

    @Test
    public void testDelete() {
        Assert.assertEquals("0", StringUtils.delete("0", ""));
        Assert.assertEquals("foo", StringUtils.delete("foobar", "bar"));
    }

    @Test
    public void testDelimitedListToStringArray() {
        Assert.assertArrayEquals(new String[] {},
            StringUtils.delimitedListToStringArray("", ">", ""));
        Assert.assertArrayEquals(new String[] {"r662"},
            StringUtils.delimitedListToStringArray("r662", ">>>>", ""));
        Assert.assertArrayEquals(new String[] {},
            StringUtils.delimitedListToStringArray("", "", ""));
        Assert.assertArrayEquals(new String[] {"foo", "br", "bz"},
            StringUtils.delimitedListToStringArray("foo>bar>baz", ">", "a"));
    }

    @Test
    public void testDelimitedListToStringArray2() {
        Assert.assertArrayEquals(new String[] {}, StringUtils.delimitedListToStringArray(null, ""));
        Assert.assertArrayEquals(new String[] {"{}~~~~~~"},
            StringUtils.delimitedListToStringArray("{}~~~~~~", null));
        Assert.assertArrayEquals(new String[] {"{"}, StringUtils.delimitedListToStringArray("{", ""));
        Assert.assertArrayEquals(new String[] {}, StringUtils.delimitedListToStringArray("", "????"));
        Assert.assertArrayEquals(new String[] {"V777VVVVV"},
            StringUtils.delimitedListToStringArray("V777VVVVV", "77777"));
        Assert.assertArrayEquals(new String[] {}, StringUtils.delimitedListToStringArray("", ""));
        Assert.assertArrayEquals(new String[] {"foo", "bar", "baz"},
            StringUtils.delimitedListToStringArray("foo-bar-baz", "-"));
    }

    @Test
    public void testEndsWithIgnoreCase() {
        Assert.assertFalse(StringUtils.endsWithIgnoreCase(null, ""));
        Assert.assertFalse(StringUtils.endsWithIgnoreCase("QPPQPPP[", "\'g\'&&\'&A&"));
        Assert.assertTrue(StringUtils.endsWithIgnoreCase("FFFFFFFFF\'", "f\'"));
        Assert.assertTrue(StringUtils.endsWithIgnoreCase("&&&&&&&&&\'", "&&&&&\'"));
    }

    @Test
    public void testHasLength() {
        Assert.assertTrue(StringUtils.hasLength("        "));
        Assert.assertTrue(StringUtils.hasLength("AAAAAAAA"));
        Assert.assertFalse(StringUtils.hasLength(""));
        Assert.assertFalse(StringUtils.hasLength(null));
    }

    @Test
    public void testHasText() {
        Assert.assertTrue(StringUtils.hasText(" !"));
        Assert.assertTrue(StringUtils.hasText("!"));
        Assert.assertFalse(StringUtils.hasText(" "));
        Assert.assertFalse(StringUtils.hasText(""));
    }

    @Test
    public void testIsBlank() {
        Assert.assertTrue(StringUtils.isBlank(""));
        Assert.assertTrue(StringUtils.isBlank(" "));
        Assert.assertFalse(StringUtils.isBlank(" (!!!!!!!!"));
    }

    @Test
    public void testIsEmpty() {
        Assert.assertFalse(StringUtils.isEmpty(-2147483647));
        Assert.assertFalse(StringUtils.isEmpty("foo"));
        Assert.assertTrue(StringUtils.isEmpty(""));
    }

    @Test
    public void testJoin() {
        Assert.assertEquals("7234", StringUtils.join(new Object[] {7, 234}, null));
        Assert.assertEquals("11", StringUtils.join(new Object[] {11}, null));
        Assert.assertEquals("", StringUtils.join(new Object[] {}, null));
        Assert.assertEquals("", StringUtils.join(new Object[] {}, "HH"));
        Assert.assertEquals("foobarbaz", StringUtils.join(new Object[] {"foo", "bar", "baz"}, ""));
    }

    @Test
    public void testLength() {
        Assert.assertEquals(0, StringUtils.length(null));
        Assert.assertEquals(0, StringUtils.length(""));
        Assert.assertEquals(3, StringUtils.length("boo"));
    }

    @Test
    public void testNormalizeClassName() {
        Assert.assertEquals("", StringUtils.normalizeClassName(""));
        Assert.assertEquals(".", StringUtils.normalizeClassName("/"));
        Assert.assertEquals("foo.bar.Baz", StringUtils.normalizeClassName("foo/bar/Baz"));
        Assert.assertEquals("foo.bar.Baz", StringUtils.normalizeClassName("foo.bar.Baz"));
    }

    @Test
    public void testObjectToString() {
        Assert.assertEquals("1", StringUtils.objectToString(1));
        Assert.assertEquals("", StringUtils.objectToString(null));
        Assert.assertEquals("{}", StringUtils.objectToString(new TreeMap()));
    }

    @Test
    public void testQuoteIfString() {
        Assert.assertEquals("\'foo\'", StringUtils.quoteIfString("foo"));
        Assert.assertEquals(1, StringUtils.quoteIfString(1));
    }

    @Test
    public void testQuote() {
        Assert.assertEquals("\'!\'", StringUtils.quote("!"));
        Assert.assertEquals("\'1234\'", StringUtils.quote("1234"));
    }

    @Test
    public void testRepeat() {
        Assert.assertNull(StringUtils.repeat(null, -1635778558));
        Assert.assertEquals("", StringUtils.repeat("", 3));
        Assert.assertEquals("\u0000\u0000", StringUtils.repeat("\u0000", 2));
        Assert.assertEquals("\u0000\u0000\u0000\u0000", StringUtils.repeat("\u0000\u0000", 2));
        Assert.assertEquals("", StringUtils.repeat("foo", 0));
        Assert.assertEquals("foo", StringUtils.repeat("foo", 1));
        Assert.assertEquals("foofoofoofoo", StringUtils.repeat("foo", 4));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8194; i++)
            sb.append('c');
        Assert.assertEquals(sb.toString(), StringUtils.repeat('c', 8194));

        thrown.expect(NegativeArraySizeException.class);
        StringUtils.repeat("\uffff\u2aa0", 1073742337);
    }

    @Test
    public void testReplace() {
        Assert.assertEquals(" ", StringUtils.replace(" ", "", "    "));
        Assert.assertEquals("", StringUtils.replace("", "", "    "));
        Assert.assertEquals("baz", StringUtils.replace("bar", "r", "z"));
    }

    @Test
    public void testSplitArrayElementsIntoProperties() {
        final String[] array = {};
        Assert.assertNull(StringUtils.splitArrayElementsIntoProperties(array, "_", "DEE"));

        final String[] array2 = {"foo=1", "bar=2"};
        final Properties prop = new Properties();
        prop.setProperty("foo", "1");
        prop.setProperty("bar", "2");
        Assert.assertEquals(prop, StringUtils.splitArrayElementsIntoProperties(array2, "=", ""));

        final String[] array3 = {"    foo=1   ", "   bar=2    "};
        final Properties prop2 = new Properties();
        prop2.setProperty("foo", "1");
        prop2.setProperty("bar", "2");
        Assert.assertEquals(prop2, StringUtils.splitArrayElementsIntoProperties(array3, "=", " "));
    }

    @Test
    public void testSplit() {
        Assert.assertNull(StringUtils.split("AAAAAAA@@", "AAAAAAAA"));
        Assert.assertNull(StringUtils.split("@", ""));
        Assert.assertNull(StringUtils.split("", "AAAAAAAA"));
        Assert.assertArrayEquals(new String[] {"", ""}, StringUtils.split("A", "A"));
        Assert.assertArrayEquals(new String[] {"foo", "foo"}, StringUtils.split("foo,foo", ","));
    }

    @Test
    public void testStartsWith() {
        Assert.assertFalse(StringUtils.startsWithIgnoreCase(null, ""));
        Assert.assertFalse(StringUtils.startsWithIgnoreCase("LHNCC", "TTsVV"));
        Assert.assertFalse(StringUtils.startsWithIgnoreCase("", "TTTT"));
        Assert.assertTrue(StringUtils.startsWithIgnoreCase("Foo", "f"));
    }

    @Test
    public void testStripEnd() {
        Assert.assertEquals("foo", StringUtils.stripEnd("foo!", "!"));
        Assert.assertEquals("  foo", StringUtils.stripEnd("  foo  ", "  "));
        Assert.assertEquals("!", StringUtils.stripEnd("!", ""));
        Assert.assertEquals("#", StringUtils.stripEnd("# ", null));
        Assert.assertEquals("", StringUtils.stripEnd("", "!!"));
        Assert.assertEquals("1234", StringUtils.stripEnd("1234.0", ".0"));
    }

    @Test
    public void testSubstringAfter() {
        Assert.assertEquals("", StringUtils.substringAfter("foo", null));
        Assert.assertEquals("!!", StringUtils.substringAfter("!!", ""));
        Assert.assertEquals("", StringUtils.substringAfter("", ""));
        Assert.assertEquals("bar", StringUtils.substringAfter("foo=bar", "="));
    }

    @Test
    public void testSubstringAfterLast() {
        Assert.assertEquals("bar", StringUtils.substringAfterLast("foo-bar", "-"));
        Assert.assertEquals("6", StringUtils.substringAfterLast("123456", "12345"));
        Assert.assertEquals("", StringUtils.substringAfterLast("foo", "foo"));
        Assert.assertEquals("", StringUtils.substringAfterLast("foo", ""));
        Assert.assertEquals("", StringUtils.substringAfterLast("", ""));
    }

    @Test
    public void testSubstringBefore() {
        Assert.assertNull(StringUtils.substringBefore(null, ""));
        Assert.assertEquals("foo", StringUtils.substringBefore("foo", "-"));
        Assert.assertEquals("", StringUtils.substringBefore("foo", "foo"));
        Assert.assertEquals("", StringUtils.substringBefore("?", ""));
        Assert.assertEquals("foo", StringUtils.substringBefore("foo, bar", ","));
    }

    @Test
    public void testSubstringBeforeLast() {
        Assert.assertEquals("foo", StringUtils.substringBeforeLast("foo", ","));
        Assert.assertEquals("", StringUtils.substringBeforeLast("foo", "foo"));
        Assert.assertEquals("", StringUtils.substringBeforeLast("", ","));
        Assert.assertEquals("foo,bar", StringUtils.substringBeforeLast("foo,bar,baz", ","));
    }

    @Test
    public void testSubstringMatch() {
        Assert.assertFalse(StringUtils.substringMatch("foo", 524290, "o"));
        Assert.assertFalse(StringUtils.substringMatch("   foo", 2, "@"));
        Assert.assertTrue(StringUtils.substringMatch("{{", 1, "{"));
        Assert.assertTrue(StringUtils.substringMatch("foo", 524290, ""));
        Assert.assertTrue(StringUtils.substringMatch("foobarbaz", 3, "bar"));
    }

    @Test
    public void testTokenizeToStringArray() {
        Assert.assertNull(StringUtils.tokenizeToStringArray(null, "?", true, false));
        Assert.assertArrayEquals(new String[] {},
            StringUtils.tokenizeToStringArray("", "\"\"", false, false));
        Assert.assertArrayEquals(new String[] {"bar", "baz", "foo  "},
            StringUtils.tokenizeToStringArray("bar,baz,foo  ,,", ",", false, true));
        Assert.assertArrayEquals(new String[] {"bar", "baz", "foo  "},
            StringUtils.tokenizeToStringArray("bar,baz,foo  ,,", ",", false, false));

        Assert.assertArrayEquals(new String[] {"bar", "baz", "foo"},
            StringUtils.tokenizeToStringArray("bar,baz,foo  ,,", ","));
    }

    @Test
    public void testToStringArray() {
        final Collection<String> collection = null;
        final ArrayList<String> arrayList = new ArrayList<String>();
        final ArrayList<String> arrayList2 = new ArrayList<String>();
        arrayList2.add("foo");

        Assert.assertNull(StringUtils.toStringArray(collection));
        Assert.assertArrayEquals(new String[] {}, StringUtils.toStringArray(arrayList));
        Assert.assertArrayEquals(new String[] {"foo"}, StringUtils.toStringArray(arrayList2));
    }

    @Test
    public void testTrimAllWhitespace() {
        Assert.assertEquals("", StringUtils.trimAllWhitespace(" "));
        Assert.assertEquals("", StringUtils.trimAllWhitespace(""));
        Assert.assertEquals("foo", StringUtils.trimAllWhitespace("foo"));
        Assert.assertEquals("foo", StringUtils.trimAllWhitespace("  foo  "));
    }

    @Test
    public void testTrimLeadingCharacter() {
        Assert.assertEquals("", StringUtils.trimLeadingCharacter("", 'a'));
        Assert.assertEquals("foo", StringUtils.trimLeadingCharacter("foo", 'a'));
        Assert.assertEquals("", StringUtils.trimLeadingCharacter("a", 'a'));
        Assert.assertEquals("foo", StringUtils.trimLeadingCharacter("afoo", 'a'));
    }

    @Test
    public void testTrimLeadingWhitespace() {
        Assert.assertEquals("", StringUtils.trimLeadingWhitespace(""));
        Assert.assertEquals("", StringUtils.trimLeadingWhitespace(" "));
        Assert.assertEquals("!", StringUtils.trimLeadingWhitespace("!"));
        Assert.assertEquals("foo  ", StringUtils.trimLeadingWhitespace("  foo  "));
    }

    @Test
    public void testTrimTrailingCharacter() {
        Assert.assertEquals("foo", StringUtils.trimTrailingCharacter("foo!", '!'));
        Assert.assertEquals("", StringUtils.trimTrailingCharacter("", '!'));
        Assert.assertEquals("", StringUtils.trimTrailingCharacter("!", '!'));
    }

    @Test
    public void testTrimTrailingWhitespace() {
        Assert.assertEquals("", StringUtils.trimTrailingWhitespace(" "));
        Assert.assertEquals("", StringUtils.trimTrailingWhitespace(""));
        Assert.assertEquals("  foo", StringUtils.trimTrailingWhitespace("  foo   "));
    }

    @Test
    public void testTrimWhitespace() {
        Assert.assertEquals("\u6d7c\u1280", StringUtils.trimWhitespace(" \u6d7c\u1280\u1680"));
        Assert.assertEquals("", StringUtils.trimWhitespace(" "));
        Assert.assertEquals("(", StringUtils.trimWhitespace("("));
        Assert.assertEquals("", StringUtils.trimWhitespace(""));
        Assert.assertEquals("foo foo", StringUtils.trimWhitespace(" foo foo "));
    }

    @Test
    public void testUncapitalize() {
        Assert.assertEquals("a", StringUtils.uncapitalize("A"));
        Assert.assertEquals("a", StringUtils.uncapitalize("a"));
        Assert.assertEquals("", StringUtils.uncapitalize(""));
    }

    @Test
    public void testCapitalize() {
        Assert.assertEquals("A", StringUtils.capitalize("a"));
        Assert.assertEquals("A", StringUtils.capitalize("A"));
        Assert.assertEquals("", StringUtils.capitalize(""));
    }

    @Test
    public void testUnqualify() {
        Assert.assertEquals("c", StringUtils.unqualify("a.b.c"));
        Assert.assertEquals("d", StringUtils.unqualify("a!b!c!d", '!'));
    }

    @Test
    public void testWrap() {
        Assert.assertEquals("\n!", StringUtils.wrap("!", 0));
        Assert.assertEquals("", StringUtils.wrap("", 1));
        Assert.assertEquals("f\no\no", StringUtils.wrap("foo", 1));
        Assert.assertEquals("f\n", StringUtils.wrap("f\n", 1));
        Assert.assertEquals("\u0001\n", StringUtils.wrap("\u0001\n", 3));
    }

    @Test
    public void testClassLoaderHash() {
        Assert.assertEquals("null", StringUtils.classLoaderHash(null));
    }
}
