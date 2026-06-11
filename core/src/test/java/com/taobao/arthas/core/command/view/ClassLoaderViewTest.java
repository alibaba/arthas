package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.ClassLoaderUrlStat;
import com.taobao.arthas.core.command.klass100.ClassLoaderCommand.UrlClassStat;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.ClassLoaderModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ClassSetVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.text.util.RenderUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

public class ClassLoaderViewTest {

    @Test
    public void shouldEscapeLineBreaksInClassLoaderTable() {
        ClassLoaderVO classLoader = new ClassLoaderVO();
        classLoader.setName("TomcatEmbeddedWebappClassLoader\r\n  context: /demo");
        classLoader.setParent("jdk.internal.loader.ClassLoaders$AppClassLoader\n  parent detail");
        classLoader.setLoadedCount(12);
        classLoader.setHash("1a2b3c");

        String output = renderClassLoaders(classLoader, false);

        Assert.assertTrue(output.contains("TomcatEmbeddedWebappClassLoader\\n  context: /demo"));
        Assert.assertTrue(output.contains("jdk.internal.loader.ClassLoaders$AppClassLoader\\n  parent detail"));
    }

    @Test
    public void shouldEscapeLineBreaksInClassLoaderTree() {
        ClassLoaderVO root = new ClassLoaderVO();
        root.setName("RootClassLoader\r\n  root detail");
        root.setHash("root");

        ClassLoaderVO child = new ClassLoaderVO();
        child.setName("ChildClassLoader\n  child detail");
        child.setHash("child");
        root.addChild(child);

        String output = renderClassLoaders(root, true);

        Assert.assertTrue(output.contains("RootClassLoader\\n  root detail"));
        Assert.assertTrue(output.contains("ChildClassLoader\\n  child detail"));
    }

    @Test
    public void shouldEscapeLineBreaksInClassDetailClassLoaderTree() {
        ClassVO classInfo = new ClassVO();
        classInfo.setClassloader(new String[] {
                "TomcatEmbeddedWebappClassLoader\r\n  context: /demo",
                "jdk.internal.loader.ClassLoaders$AppClassLoader\n  parent detail"
        });

        String output = RenderUtil.render(TypeRenderUtils.drawClassLoader(classInfo), 200);

        Assert.assertTrue(output.contains("TomcatEmbeddedWebappClassLoader\\n  context: /demo"));
        Assert.assertTrue(output.contains("jdk.internal.loader.ClassLoaders$AppClassLoader\\n  parent detail"));
    }

    @Test
    public void shouldEscapeLineBreaksInUrlStatsHeader() {
        ClassLoaderVO classLoader = new ClassLoaderVO();
        classLoader.setName("TomcatEmbeddedWebappClassLoader\r\n  context: /demo");
        classLoader.setHash("urlstat");

        ClassLoaderUrlStat urlStat = new ClassLoaderUrlStat(Collections.singletonList("file:/tmp/demo.jar"),
                Collections.<String>emptyList());

        ClassLoaderModel model = new ClassLoaderModel();
        model.setUrlStats(Collections.singletonMap(classLoader, urlStat));

        String output = renderView(model);

        Assert.assertTrue(output.contains("TomcatEmbeddedWebappClassLoader\\n  context: /demo, hash:urlstat"));
    }

    @Test
    public void shouldEscapeLineBreaksInUrlClassStatsHeader() {
        ClassLoaderVO classLoader = new ClassLoaderVO();
        classLoader.setName("TomcatEmbeddedWebappClassLoader\r\n  context: /demo");
        classLoader.setHash("urlclasses");

        UrlClassStat stat = new UrlClassStat();
        stat.setUrl("file:/tmp/demo.jar");
        stat.setLoadedClassCount(3);

        ClassLoaderModel model = new ClassLoaderModel()
                .setClassLoader(classLoader)
                .setUrlClassStats(Collections.singletonList(stat))
                .setUrlClassStatsDetail(false);

        String output = renderView(model);

        Assert.assertTrue(output.contains("TomcatEmbeddedWebappClassLoader\\n  context: /demo, hash:urlclasses"));
    }

    @Test
    public void shouldEscapeLineBreaksInAllClassesHeader() {
        ClassLoaderVO classLoader = new ClassLoaderVO();
        classLoader.setName("TomcatEmbeddedWebappClassLoader\r\n  context: /demo");
        classLoader.setHash("allclasses");

        ClassSetVO classSet = new ClassSetVO(classLoader, Collections.singletonList("demo.SampleClass"));

        ClassLoaderModel model = new ClassLoaderModel().setClassSet(classSet);

        String output = renderView(model);

        Assert.assertTrue(output.contains("hash:allclasses, TomcatEmbeddedWebappClassLoader\\n  context: /demo"));
    }

    private String renderClassLoaders(ClassLoaderVO classLoader, boolean tree) {
        return render(process -> ClassLoaderView.drawClassLoaders(process, Collections.singletonList(classLoader), tree));
    }

    private String renderView(ClassLoaderModel model) {
        return render(process -> new ClassLoaderView().draw(process, model));
    }

    private String render(RenderAction action) {
        CommandProcess process = Mockito.mock(CommandProcess.class);
        StringBuilder output = new StringBuilder();
        Mockito.when(process.width()).thenReturn(200);
        Mockito.when(process.write(Mockito.anyString())).thenAnswer(invocation -> {
            output.append(invocation.getArgument(0, String.class));
            return process;
        });

        action.render(process);
        return output.toString();
    }

    private interface RenderAction {
        void render(CommandProcess process);
    }
}
