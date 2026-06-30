package com.taobao.arthas.core.command.klass100;

import org.junit.Assert;
import org.junit.Test;

public class ClassLoaderCommandUrlClassesTest {

    @Test
    public void testGuessJarNameForNestedJarUrl() {
        String url = "jar:file:/app.jar!/BOOT-INF/lib/spring-core-5.3.0.jar!/";
        Assert.assertEquals("spring-core-5.3.0.jar", ClassLoaderCommand.guessJarName(url));
    }

    @Test
    public void testGuessJarNameForFileJarUrl() {
        String url = "file:/private/tmp/math-game.jar";
        Assert.assertEquals("math-game.jar", ClassLoaderCommand.guessJarName(url));
    }

    @Test
    public void testGuessJarNameForDirectoryUrl() {
        String url = "file:/private/tmp/classes/";
        Assert.assertEquals("classes", ClassLoaderCommand.guessJarName(url));
    }

    @Test
    public void testContainsIgnoreCase() {
        Assert.assertTrue(ClassLoaderCommand.containsIgnoreCase("Spring-Core-5.3.0.jar", "spring-core"));
        Assert.assertTrue(ClassLoaderCommand.containsIgnoreCase("org.springframework.web", "SpringFramework"));
        Assert.assertFalse(ClassLoaderCommand.containsIgnoreCase("demo.MathGame", "org.springframework"));
    }
}

