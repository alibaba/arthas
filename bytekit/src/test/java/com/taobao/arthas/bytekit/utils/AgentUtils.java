package com.taobao.arthas.bytekit.utils;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import net.bytebuddy.agent.ByteBuddyAgent;

public class AgentUtils {

    private static class InstrumentationHolder {
        static final Instrumentation instance = ByteBuddyAgent.install();
    }

    public static Instrumentation install() {
        return InstrumentationHolder.instance;

    }

    public static void redefine(Class<?> clazz, byte[] classFile)
            throws ClassNotFoundException, UnmodifiableClassException {
        ClassDefinition classDefinition = new ClassDefinition(clazz, classFile);
        InstrumentationHolder.instance.redefineClasses(classDefinition);
    }

    public static void reTransform(Class<?> clazz, byte[] classFile) throws UnmodifiableClassException {

        SimpleClassFileTransformer transformer = new SimpleClassFileTransformer(clazz.getClassLoader(), clazz.getName(),
                classFile);
        InstrumentationHolder.instance.addTransformer(transformer, true);

        InstrumentationHolder.instance.retransformClasses(clazz);
        InstrumentationHolder.instance.removeTransformer(transformer);
    }

    public static class SimpleClassFileTransformer implements ClassFileTransformer {
        private byte[] classBuffer;
        private ClassLoader classLoader;
        private String className;

        public SimpleClassFileTransformer(ClassLoader classLoader, String className, byte[] classBuffer) {
            this.classLoader = classLoader;
            this.className = className.replace('.', '/');
            this.classBuffer = classBuffer;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            if (this.classLoader == loader && className.equals(this.className)) {
                return classBuffer;
            }

            return null;

        }

    }

}
