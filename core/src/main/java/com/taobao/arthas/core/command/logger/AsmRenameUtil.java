package com.taobao.arthas.core.command.logger;

import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassVisitor;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.deps.org.objectweb.asm.commons.ClassRemapper;
import com.alibaba.deps.org.objectweb.asm.commons.SimpleRemapper;

/**
 * ASM类重命名工具类
 *
 * 该工具类使用ASM字节码操作框架来重命名Java类。
 * 主要用于在运行时动态修改类的名称，同时保持类的其他功能和结构不变。
 * 通过ASM的字节码转换能力，可以精确地修改类名及其所有引用。
 *
 * @author hengyunabc 2019-09-23
 *
 */
public class AsmRenameUtil {

    /**
     * 重命名类的字节码
     *
     * 该方法接收类的字节数组表示，将其中的类名从旧名称替换为新名称。
     * 使用ASM的ClassRemapper和SimpleRemapper来完成类名的映射和替换。
     *
     * @param bytes 类的字节数组表示
     * @param oldName 原类名（使用点分隔的格式，如"com.example.OldClass"）
     * @param newName 新类名（使用点分隔的格式，如"com.example.NewClass"）
     * @return 重命名后的类字节数组
     */
    public static byte[] renameClass(byte[] bytes, final String oldName, final String newName) {
        // 创建ClassReader对象，用于读取类的字节码
        ClassReader reader = new ClassReader(bytes);

        // 创建ClassWriter对象，用于写入修改后的字节码
        // 参数0表示不自动计算栈帧和局部变量表大小
        ClassWriter writer = new ClassWriter(reader, 0);

        // 将点分隔的类名转换为ASM内部使用的斜杠分隔格式
        // 例如："com.example.OldClass" 转换为 "com/example/OldClass"
        final String internalOldName = oldName.replace('.', '/');
        final String internalNewName = newName.replace('.', '/');

        // 注释掉的代码展示了另一种使用自定义Remapper的方式
        // 可以实现更复杂的类名映射逻辑
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

        // 创建ClassRemapper访问器，使用SimpleRemapper进行简单的类名映射
        // SimpleRemapper会将所有出现的internalOldName替换为internalNewName
        ClassVisitor visitor = new ClassRemapper(writer, new SimpleRemapper(internalOldName, internalNewName));

        // 让ClassReader接受访问器，开始处理字节码
        // 参数0表示不进行额外的处理选项
        reader.accept(visitor, 0);

        // 返回修改后的字节码数组
        return writer.toByteArray();
    }

}
