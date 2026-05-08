package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ClassLoaderMetaspaceModel;
import com.taobao.arthas.core.command.model.ClassLoaderMetaspaceModel.Row;
import com.taobao.arthas.core.shell.command.CommandProcess;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

public class ClassLoaderMetaspaceViewTest {

    @Test
    public void shouldRenderDefaultColumns() {
        String output = render(false, 160);

        Assert.assertTrue(output.contains("hash"));
        Assert.assertTrue(output.contains("classes"));
        Assert.assertTrue(output.contains("chunkSize"));
        Assert.assertTrue(output.contains("blockSize"));
        Assert.assertTrue(output.contains("name"));
        Assert.assertTrue(output.contains("9301672"));
        Assert.assertTrue(output.contains("4898"));
        Assert.assertTrue(output.contains("23457792"));
        Assert.assertTrue(output.contains("23445408"));
        Assert.assertTrue(output.contains("ClassRealm[plugin>com.taobao.pandora"));
        Assert.assertFalse(output.contains("classLoaderData"));
        Assert.assertFalse(output.contains("hiddenBlockSize"));
        Assert.assertFalse(output.contains("type"));
    }

    @Test
    public void shouldRenderVerboseColumns() {
        String output = render(true, 200);

        Assert.assertTrue(output.contains("hash"));
        Assert.assertTrue(output.contains("classLoaderData"));
        Assert.assertTrue(output.contains("classes"));
        Assert.assertTrue(output.contains("chunkSize"));
        Assert.assertTrue(output.contains("blockSize"));
        Assert.assertTrue(output.contains("hiddenBlockSize"));
        Assert.assertTrue(output.contains("type"));
        Assert.assertTrue(output.contains("name"));
        Assert.assertTrue(output.contains("0x00006000026d2440"));
        Assert.assertTrue(output.contains("org.eclipse.sisu.space.CloningClassSpace$CloningClassLoader"));
    }

    @Test
    public void shouldKeepMetaspaceColumnsWhenVerboseNameAndTypeAreLong() {
        String output = render(true, 100);

        Assert.assertTrue(output.contains("hash"));
        Assert.assertTrue(output.contains("classLoaderData"));
        Assert.assertTrue(output.contains("classes"));
        Assert.assertTrue(output.contains("chunkSize"));
        Assert.assertTrue(output.contains("blockSize"));
        Assert.assertTrue(output.contains("hiddenBlockSize"));
        Assert.assertTrue(output.contains("9301672"));
        Assert.assertTrue(output.contains("0x00006000026d2440"));
        Assert.assertTrue(output.contains("4898"));
        Assert.assertTrue(output.contains("23457792"));
        Assert.assertTrue(output.contains("23445408"));
    }

    private static String render(boolean verbose, int width) {
        ClassLoaderMetaspaceModel model = new ClassLoaderMetaspaceModel()
                .setVerbose(verbose)
                .setRows(Collections.singletonList(new Row()
                        .setHash("9301672")
                        .setClassLoaderData(0x00006000026d2440L)
                        .setClassCount(4898)
                        .setChunkSize(23457792)
                        .setBlockSize(23445408)
                        .setHiddenBlockSize(0)
                        .setType("org.eclipse.sisu.space.CloningClassSpace$CloningClassLoader")
                        .setName("ClassRealm[plugin>com.taobao.pandora:pandora-boot-maven-plugin:2.2.2.2, "
                                + "parent: jdk.internal.loader.ClassLoaders$AppClassLoader@7a8c5397]")));
        CommandProcess process = Mockito.mock(CommandProcess.class);
        Mockito.when(process.width()).thenReturn(width);
        Mockito.when(process.write(Mockito.anyString())).thenReturn(process);

        new ClassLoaderMetaspaceView().draw(process, model);

        ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(process).write(outputCaptor.capture());
        return outputCaptor.getValue();
    }
}
