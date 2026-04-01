package com.taobao.arthas.core.util;

import com.taobao.arthas.core.command.basic1000.OptionsCommand;
import com.taobao.arthas.core.command.klass100.JadCommand;
import com.taobao.arthas.core.command.monitor200.ThreadCommand;
import com.taobao.middleware.cli.annotations.Option;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class OptionUtilsTest {

    @Test
    void testFindOptions() {
        assertEquals(Arrays.asList("classLoaderClass", "code", "directory", "hideUnicode","lineNumber", "regex", "source-only"),
                sortedNames(OptionUtils.findOptions(JadCommand.class, null)));
        assertEquals(Collections.emptyList(),
                sortedNames(OptionUtils.findOptions(OptionsCommand.class, null)));
    }

    @Test
    void testFindNonFlagOptions() {
        assertEquals(Arrays.asList("classLoaderClass", "code", "directory", "lineNumber"),
                sortedNames(OptionUtils.findNonFlagOptions(JadCommand.class)));
        assertEquals(Arrays.asList("sample-interval", "state", "top-n-threads"),
                sortedNames(OptionUtils.findNonFlagOptions(ThreadCommand.class)));
        assertEquals(Collections.emptyList(),
                sortedNames(OptionUtils.findOptions(OptionsCommand.class, null)));
    }

    @Test
    void testContainsShortOption() {
        List<Option> options = OptionUtils.findOptions(JadCommand.class, null);
        assertTrue(OptionUtils.containsShortOption(options, "c"));
        assertTrue(OptionUtils.containsShortOption(options, "E"));
        assertTrue(OptionUtils.containsShortOption(options, "d"));
        assertFalse(OptionUtils.containsShortOption(options, "F"));
        assertFalse(OptionUtils.containsShortOption(options, null));
        assertFalse(OptionUtils.containsShortOption(options, ""));
        assertFalse(OptionUtils.containsShortOption(options, "code"));
    }

    @Test
    void testContainsLongOption() {
        List<Option> options = OptionUtils.findOptions(JadCommand.class, null);
        assertTrue(OptionUtils.containsLongOption(options, "classLoaderClass"));
        assertTrue(OptionUtils.containsLongOption(options, "code"));
        assertTrue(OptionUtils.containsLongOption(options, "directory"));
        assertTrue(OptionUtils.containsLongOption(options, "hideUnicode"));
        assertTrue(OptionUtils.containsLongOption(options, "lineNumber"));
        assertTrue(OptionUtils.containsLongOption(options, "regex"));
        assertTrue(OptionUtils.containsLongOption(options, "source-only"));
        assertFalse(OptionUtils.containsLongOption(options, null));
        assertFalse(OptionUtils.containsLongOption(options, ""));
        assertFalse(OptionUtils.containsLongOption(options, "c"));
    }

    private static List<String> sortedNames(List<Option> options) {
        return options.stream().map(Option::longName).sorted().collect(Collectors.toList());
    }
}