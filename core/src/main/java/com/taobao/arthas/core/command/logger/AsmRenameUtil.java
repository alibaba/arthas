package com.taobao.arthas.core.command.logger;

import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassVisitor;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.deps.org.objectweb.asm.commons.ClassRemapper;
import com.alibaba.deps.org.objectweb.asm.commons.SimpleRemapper;

/**
 * 
 * @author hengyunabc 2019-09-23
 *
 */
public class AsmRenameUtil {

    public static byte[] renameClass(byte[] bytes, final String oldName, final String newName) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);

        final String internalOldName = oldName.replace('.', '/');
        final String internalNewName = newName.replace('.', '/');
//        ClassVisitor visitor = new ClassRemapper(writer, new Remapper() {
//
//            @Override
//            public String mapType(String internalName) {
//                if (internalName.equals(internalOldName)) {
//                    return internalNewName;
//                } else {
//                    return super.mapType(internalName);
//                }
//            }
//
//        });
        
        ClassVisitor visitor = new ClassRemapper(writer, new SimpleRemapper(internalOldName, internalNewName));
        
        
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

}
