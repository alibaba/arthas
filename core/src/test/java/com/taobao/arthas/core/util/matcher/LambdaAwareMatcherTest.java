package com.taobao.arthas.core.util.matcher;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author geN 2026-06-26.
 */
public class LambdaAwareMatcherTest {

    private static LambdaAwareMatcher matcher(String pattern, boolean includeLambda) {
        return new LambdaAwareMatcher(new WildcardMatcher(pattern), includeLambda);
    }

    @Test
    public void testDelegatePassThrough() {
        // 原始方法名始终走 delegate，与 includeLambda 无关
        Assert.assertTrue(matcher("run", false).matching("run"));
        Assert.assertTrue(matcher("run", true).matching("run"));
        Assert.assertFalse(matcher("run", false).matching("other"));
        Assert.assertFalse(matcher("run", true).matching("other"));
    }

    @Test
    public void testLambdaIncludedOnlyWhenFlagOnAndEnclosingMatches() {
        // 关闭时：lambda 方法名不会被匹配
        Assert.assertFalse(matcher("run", false).matching("lambda$run$0"));
        // 开启且外层方法匹配：纳入匹配
        Assert.assertTrue(matcher("run", true).matching("lambda$run$0"));
        Assert.assertTrue(matcher("run", true).matching("lambda$run$12"));
        // 开启但外层方法不匹配：不纳入
        Assert.assertFalse(matcher("run", true).matching("lambda$other$0"));
    }

    @Test
    public void testWildcardEnclosing() {
        // 通配符外层方法同样适用
        Assert.assertTrue(matcher("ru*", true).matching("lambda$run$0"));
        Assert.assertFalse(matcher("go*", true).matching("lambda$run$0"));
    }

    @Test
    public void testRegexEnclosing() {
        LambdaAwareMatcher m = new LambdaAwareMatcher(new RegexMatcher("run|go"), true);
        Assert.assertTrue(m.matching("lambda$run$0"));
        Assert.assertTrue(m.matching("lambda$go$3"));
        Assert.assertFalse(m.matching("lambda$stop$0"));
    }

    @Test
    public void testMalformedLambdaNamesRejected() {
        // 索引非数字
        Assert.assertFalse(matcher("run", true).matching("lambda$run$x"));
        // $deserializeLambda$ 这类无数字索引的不应误判
        Assert.assertFalse(matcher("run", true).matching("lambda$deserializeLambda$"));
        // 缺少索引
        Assert.assertFalse(matcher("run", true).matching("lambda$run$"));
        // 非 lambda 方法名仍走 delegate
        Assert.assertFalse(matcher("run", true).matching("toString"));
    }

    @Test
    public void testEnclosingNameWithDollar() {
        // 外层方法名本身含 $（如外部类场景），仅以最后一个 $ 后的数字作为索引
        Assert.assertTrue(matcher("run$inner", true).matching("lambda$run$inner$0"));
        Assert.assertTrue(matcher("doIt", true).matching("lambda$doIt$7"));
    }

    @Test
    public void testNullSafety() {
        Assert.assertFalse(matcher("run", true).matching(null));
        Assert.assertFalse(matcher("run", false).matching(null));
    }

}
