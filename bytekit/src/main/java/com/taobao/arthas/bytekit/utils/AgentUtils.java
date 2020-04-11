package com.taobao.arthas.bytekit.utils;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import net.bytebuddy.agent.ByteBuddyAgent;

public class AgentUtils {

    private static class InstrumentationHolder {
        static final Instrumentation instance = ByteBuddyAgent.install();
    }

    public static void redefine(Class<?> theClass, byte[] theClassFile)
            throws ClassNotFoundException, UnmodifiableClassException {
        ClassDefinition classDefinition = new ClassDefinition(theClass, theClassFile);
        InstrumentationHolder.instance.redefineClasses(classDefinition);
    }

}
