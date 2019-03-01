package com.taobao.arthas.bytekit.asm.inst;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.Decompiler;
import com.taobao.arthas.bytekit.utils.VerifyUtils;

public class InstDemoTest {

	@Test
	public void test() throws Exception {

		ClassNode apmClassNode = AsmUtils.loadClass(InstDemo_APM.class);

		ClassNode originClassNode = AsmUtils.loadClass(InstDemo.class);

		ClassNode targetClassNode = AsmUtils.copy(originClassNode);


		byte[] renameClass = AsmUtils.renameClass(AsmUtils.toBytes(apmClassNode), Type.getObjectType(originClassNode.name).getClassName());

		apmClassNode = AsmUtils.toClassNode(renameClass);

		for(FieldNode fieldNode : apmClassNode.fields) {
			if( fieldNode.visibleAnnotations != null) {
				for( AnnotationNode annotationNode : fieldNode.visibleAnnotations) {
					System.err.println(annotationNode.desc);
					System.err.println(annotationNode.values);

					if(Type.getType(NewField.class).equals(Type.getType(annotationNode.desc))) {
						AsmUtils.addField(targetClassNode, fieldNode);
					}

				}
			}
		}

        for (MethodNode methodNode : apmClassNode.methods) {
            methodNode = AsmUtils.removeLineNumbers(methodNode);
            if (methodNode.name.startsWith("__origin_")) {
                continue;
            } else {
                MethodNode findMethod = AsmUtils.findMethod(originClassNode.methods, methodNode);
                if (findMethod != null) {
                    // 先要替换 invokeOrigin ，要判断
                    // 从 apm 里查找 __origin_ 开头的函数，忽略
                    // 查找 非 __origin_ 开头的函数，在原来的类里查找，如果有同样签名的函数
                    // 则从函数里查找 是否有 __origin_ 的函数调用。如果有的话，则从原有的类里查找到 method，再inline掉。

                    List<MethodInsnNode> originMethodInsnNodes = AsmUtils.findMethodInsnNodeWithPrefix(methodNode,
                                    "__origin_");

                    for (MethodInsnNode methodInsnNode : originMethodInsnNodes) {
                        String toInlineMethodName = methodInsnNode.name.substring("__origin_".length());
                        MethodNode originMethodNode = AsmUtils.findMethod(originClassNode.methods, toInlineMethodName,
                                        findMethod.desc);

                        MethodNode tmpMethodNode = AsmUtils.copy(originMethodNode);
                        tmpMethodNode.name = methodInsnNode.name;

                        MethodProcessor methodProcessor = new MethodProcessor(apmClassNode.name, methodNode);
                        methodProcessor.inline(originClassNode.name, tmpMethodNode);

                        AsmUtils.replaceMethod(targetClassNode, methodProcessor.getMethodNode());

                    }

                } else {
                    // 没找到的函数，则加进去
                    AsmUtils.addMethod(targetClassNode, methodNode);
                }
            }


        }

        byte[] resutlBytes = AsmUtils.toBytes(targetClassNode);

		System.err.println(Decompiler.decompile(resutlBytes));

		System.err.println(AsmUtils.toASMCode(resutlBytes));

		FileUtils.writeByteArrayToFile(new File("/tmp/ttt/InstDemo.class"), resutlBytes);

		VerifyUtils.asmVerify(resutlBytes);
		VerifyUtils.instanceVerity(resutlBytes);
	}
}
