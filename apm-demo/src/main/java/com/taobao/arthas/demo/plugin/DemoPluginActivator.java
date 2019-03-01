package com.taobao.arthas.demo.plugin;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.Decompiler;
import com.taobao.arthas.bytekit.utils.MatchUtils;
import com.taobao.arthas.plugin.Plugin;
import com.taobao.arthas.plugin.PluginActivator;
import com.taobao.arthas.plugin.PluginContext;

public class DemoPluginActivator implements PluginActivator{

    @Override
    public boolean enabled(PluginContext context) {
        return true;
    }

    @Override
    public void start(PluginContext context) {

        Plugin plugin = context.getPlugin();

        System.err.println("hello" + plugin.getName());

        context.getInstrumentation().addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
                            throws IllegalClassFormatException {

                if(!className.equals("demo/MathGame")) {
                    return null;
                }

                DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

                List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser.parse(EnterInterceptor.class);

                ClassNode classNode = AsmUtils.toClassNode(classfileBuffer);

                List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
                for (MethodNode methodNode : classNode.methods) {
                    if (MatchUtils.wildcardMatch(methodNode.name, "primeFactors")) {
                        matchedMethods.add(methodNode);
                    }
                }

                for (MethodNode methodNode : matchedMethods) {
                    MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                    for (InterceptorProcessor interceptor : interceptorProcessors) {
                        try {
                            interceptor.process(methodProcessor);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                byte[] bytes = AsmUtils.toBytes(classNode);

//                try {
//                    String decompile = Decompiler.decompile(bytes);
//                    System.err.println(decompile);
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
                return bytes;
            }

        });


    }

    @Override
    public void stop(PluginContext context) {
        // TODO Auto-generated method stub

    }

}
