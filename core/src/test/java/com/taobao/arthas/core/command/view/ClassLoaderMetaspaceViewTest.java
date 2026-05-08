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
    public void shouldKeepMetaspaceColumnsWhenNameAndTypeAreLong() {
        ClassLoaderMetaspaceModel model = new ClassLoaderMetaspaceModel()
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
        Mockito.when(process.width()).thenReturn(100);
        Mockito.when(process.write(Mockito.anyString())).thenReturn(process);

        new ClassLoaderMetaspaceView().draw(process, model);

        ArgumentCaptor<String> outputCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(process).write(outputCaptor.capture());
        String output = outputCaptor.getValue();
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
}
