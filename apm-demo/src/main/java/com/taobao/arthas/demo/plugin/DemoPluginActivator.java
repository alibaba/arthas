package com.taobao.arthas.demo.plugin;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.MatchUtils;
import com.taobao.arthas.plugin.Plugin;
import com.taobao.arthas.plugin.PluginActivator;
import com.taobao.arthas.plugin.PluginContext;

public class DemoPluginActivator implements PluginActivator {

    private static final Logger logger = LoggerFactory.getLogger("arthas.apm.demo");

    @Override
    public boolean enabled(PluginContext context) {
        return true;
    }

    @Override
    public void init(PluginContext context) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void start(PluginContext context) {
        Plugin plugin = context.getPlugin();
        logger.info("start apm demo, plugin name: {}", plugin.name());

        context.getInstrumentation().addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
                            throws IllegalClassFormatException {

                if (!className.equals("demo/MathGame")) {
                    return null;
                }

                DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

                List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser
                                .parse(EnterInterceptor.class);

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

                return bytes;
            }

        });

    }

    @Override
    public void stop(PluginContext context) {
        Plugin plugin = context.getPlugin();
        logger.info("start apm demo, plugin name: {}", plugin.name());
    }

}
