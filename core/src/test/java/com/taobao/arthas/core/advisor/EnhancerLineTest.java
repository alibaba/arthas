package com.taobao.arthas.core.advisor;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.core.bytecode.TestHelper;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.util.matcher.EqualsMatcher;
import com.taobao.arthas.core.util.matcher.TrueMatcher;

import demo.MathGame;
import net.bytebuddy.agent.ByteBuddyAgent;

public class EnhancerLineTest {
    private static final String SPY_API_OWNER = "java/arthas/SpyAPI";

    @BeforeClass
    public static void beforeClass() throws Throwable {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        TestHelper.appendSpyJar(instrumentation);
        ArthasBootstrap.getInstance(instrumentation, "ip=127.0.0.1");
    }

    @Test
    public void lineEnhanceShouldInsertAtLineForMultipleLinesAndAvoidDuplicateTransform() throws Throwable {
        AdviceListener listener = Mockito.mock(AdviceListener.class);
        Enhancer enhancer = new Enhancer(listener, false, false,
                new EqualsMatcher<String>(MathGame.class.getName()), null, new TrueMatcher<String>());
        enhancer.setLineEnhanceOptions(new LineEnhanceOptions(lines(51, 57), null));

        byte[] result = transform(enhancer, MathGame.class, AsmUtils.toBytes(AsmUtils.loadClass(MathGame.class)));
        ClassNode resultClassNode1 = AsmUtils.toClassNode(result);
        MethodNode primeFactors1 = AsmUtils.findMethods(resultClassNode1.methods, "primeFactors").get(0);
        MethodNode print1 = AsmUtils.findMethods(resultClassNode1.methods, "print").get(0);

        Assertions.assertThat(countAtLine(primeFactors1)).isEqualTo(2);
        Assertions.assertThat(countAtLine(print1)).isEqualTo(0);

        byte[] result2 = transform(enhancer, MathGame.class, result);
        ClassNode resultClassNode2 = AsmUtils.toClassNode(result2);
        MethodNode primeFactors2 = AsmUtils.findMethods(resultClassNode2.methods, "primeFactors").get(0);

        Assertions.assertThat(countAtLine(primeFactors2)).isEqualTo(countAtLine(primeFactors1));
    }

    @Test
    public void lineEnhanceShouldAppendNewLineWithoutReinsertingOldLine() throws Throwable {
        byte[] original = AsmUtils.toBytes(AsmUtils.loadClass(MathGame.class));

        AdviceListener firstListener = Mockito.mock(AdviceListener.class);
        Enhancer firstEnhancer = new Enhancer(firstListener, false, false,
                new EqualsMatcher<String>(MathGame.class.getName()), null, new EqualsMatcher<String>("primeFactors"));
        firstEnhancer.setLineEnhanceOptions(new LineEnhanceOptions(lines(51), null));
        byte[] first = transform(firstEnhancer, MathGame.class, original);

        AdviceListener secondListener = Mockito.mock(AdviceListener.class);
        Enhancer secondEnhancer = new Enhancer(secondListener, false, false,
                new EqualsMatcher<String>(MathGame.class.getName()), null, new EqualsMatcher<String>("primeFactors"));
        secondEnhancer.setLineEnhanceOptions(new LineEnhanceOptions(lines(57), null));
        byte[] second = transform(secondEnhancer, MathGame.class, first);

        MethodNode primeFactors = AsmUtils.findMethods(AsmUtils.toClassNode(second).methods, "primeFactors").get(0);
        Assertions.assertThat(countAtLine(primeFactors)).isEqualTo(2);
    }

    @Test
    public void lineEnhanceShouldHandleComplexControlFlowAndWideLocalVariables() throws Throwable {
        ClassNode originalClassNode = AsmUtils.loadClass(ComplexLineCase.class);
        MethodNode complexMethod = AsmUtils.findMethods(originalClassNode.methods, "complex").get(0);
        Set<Integer> targetLines = lineNumbers(complexMethod);

        AdviceListener listener = Mockito.mock(AdviceListener.class);
        Enhancer enhancer = new Enhancer(listener, false, false,
                new EqualsMatcher<String>(ComplexLineCase.class.getName()), null, new EqualsMatcher<String>("complex"));
        enhancer.setLineEnhanceOptions(new LineEnhanceOptions(targetLines, "(I)I"));

        byte[] result = transform(enhancer, ComplexLineCase.class, AsmUtils.toBytes(originalClassNode));
        MethodNode enhancedMethod = AsmUtils.findMethods(AsmUtils.toClassNode(result).methods, "complex").get(0);

        Assertions.assertThat(countAtLine(enhancedMethod)).isGreaterThan(3);

        byte[] result2 = transform(enhancer, ComplexLineCase.class, result);
        MethodNode enhancedAgainMethod = AsmUtils.findMethods(AsmUtils.toClassNode(result2).methods, "complex").get(0);
        Assertions.assertThat(countAtLine(enhancedAgainMethod)).isEqualTo(countAtLine(enhancedMethod));
    }

    private static byte[] transform(Enhancer enhancer, Class<?> clazz, byte[] classfileBuffer) throws Exception {
        return enhancer.transform(clazz.getClassLoader(), clazz.getName(), clazz, null, classfileBuffer);
    }

    private static Set<Integer> lines(Integer... lines) {
        return new LinkedHashSet<Integer>(Arrays.asList(lines));
    }

    private static Set<Integer> lineNumbers(MethodNode methodNode) {
        Set<Integer> result = new LinkedHashSet<Integer>();
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode
                .getNext()) {
            if (insnNode instanceof LineNumberNode) {
                result.add(((LineNumberNode) insnNode).line);
            }
        }
        return result;
    }

    private static int countAtLine(MethodNode methodNode) {
        List<?> methodInsnNodes = AsmUtils.findMethodInsnNode(methodNode, SPY_API_OWNER, "atLine");
        return methodInsnNodes.size();
    }

    public static class ComplexLineCase {
        public int complex(int input) {
            long wide = input * 17L;
            double factor = wide / 3.0D;
            int result = 0;
            try {
                for (int i = 0; i < input; i++) {
                    switch (i % 3) {
                    case 0:
                        result += i;
                        break;
                    case 1:
                        result += (int) factor;
                        break;
                    default:
                        result -= (int) wide;
                        break;
                    }
                }
                return result;
            } catch (RuntimeException e) {
                return e.getMessage() == null ? -1 : e.getMessage().length();
            } finally {
                result++;
            }
        }
    }
}
