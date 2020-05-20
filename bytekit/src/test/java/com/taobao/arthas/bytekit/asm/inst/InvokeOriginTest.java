package com.taobao.arthas.bytekit.asm.inst;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;

import com.taobao.arthas.bytekit.asm.inst.impl.InstrumentImpl;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.Decompiler;
import com.taobao.arthas.bytekit.utils.VerifyUtils;

/**
 *
 * @author hengyunabc 2019-03-18
 *
 */
public class InvokeOriginTest {

    ClassNode apmClassNode;
    ClassNode originClassNode;

    ClassNode targetClassNode;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeClass() throws IOException {

    }

    @Before
    public void before() throws IOException {
        apmClassNode = AsmUtils.loadClass(InvokeOriginDemo_APM.class);
        originClassNode = AsmUtils.loadClass(InvokeOriginDemo.class);

        byte[] renameClass = AsmUtils.renameClass(AsmUtils.toBytes(apmClassNode),
                        Type.getObjectType(originClassNode.name).getClassName());

        apmClassNode = AsmUtils.toClassNode(renameClass);

        targetClassNode = AsmUtils.copy(originClassNode);
    }

    private Object replace(String methodName) throws Exception {
        System.err.println(methodName);
        for (MethodNode methodNode : apmClassNode.methods) {
            if (methodNode.name.equals(methodName)) {
                methodNode = AsmUtils.removeLineNumbers(methodNode);
                // 从原来的类里查找对应的函数
                MethodNode findMethod = AsmUtils.findMethod(originClassNode.methods, methodNode);
                if (findMethod != null) {
                    MethodNode methodNode2 = InstrumentImpl.replaceInvokeOrigin(originClassNode.name, findMethod,
                                    methodNode);

                    System.err.println(Decompiler.toString(methodNode2));

                    AsmUtils.replaceMethod(targetClassNode, methodNode2);

                } else {

                }
            }
        }

        byte[] resutlBytes = AsmUtils.toBytes(targetClassNode);

        System.err.println("=================");

        System.err.println(Decompiler.decompile(resutlBytes));

        // System.err.println(AsmUtils.toASMCode(resutlBytes));

        VerifyUtils.asmVerify(resutlBytes);
        return VerifyUtils.instanceVerity(resutlBytes);
    }

    @Test
    public void test_returnVoid() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);

        Assertions.assertThat(VerifyUtils.invoke(object, methodName)).isEqualTo(null);
    }

    @Test
    public void test_returnVoidObject() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName)).isEqualTo(null);
    }

    @Test
    public void test_returnInt() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName, 123)).isEqualTo(9998 + 123);
    }

    @Test
    public void test_returnIntToObject() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName, 123)).isEqualTo(9998 + 9998);
    }

    @Test
    public void test_returnIntToInteger() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName, 123)).isEqualTo(9998 + 9998);
    }

    @Test
    public void test_returnIntStatic() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName, 123)).isEqualTo(9998 + 9998);
    }

    @Test
    public void test_returnLong() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName)).isEqualTo(9998L + 9998);
    }

    @Test
    public void test_returnLongToObject() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName)).isEqualTo(9998L + 9998);
    }

    @Test
    public void test_returnStrArray() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName)).isEqualTo(new String[] { "abc", "xyz", "ufo" });
    }

    @Test
    public void test_returnStrArrayWithArgs() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName, 123, "sss", 777L))
                        .isEqualTo(new Object[] { "fff", "xyz" + "sss", "ufo" + 777 });
    }

    @Test
    public void test_returnStr() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName)).asString().startsWith("hello");
    }

    @Test
    public void test_returnObject() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName)).isEqualTo(object.getClass());
    }

    @Test
    public void test_recursive() throws Exception {
        String methodName = testName.getMethodName().substring("test_".length());
        Object object = replace(methodName);
        Assertions.assertThat(VerifyUtils.invoke(object, methodName, 100)).isEqualTo((100 + 1) * 100 / 2);
    }
}
