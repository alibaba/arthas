package com.taobao.arthas.core.util.matcher;

/**
 * 支持 lambda 合成方法的匹配器
 * @author geN 2026-06-26
 */
public class LambdaAwareMatcher implements Matcher<String> {

    private static final String LAMBDA_METHOD_PREFIX = "lambda$";

    private final Matcher<String> delegate;
    private final boolean includeLambda;

    public LambdaAwareMatcher(Matcher<String> delegate, boolean includeLambda) {
        this.delegate = delegate;
        this.includeLambda = includeLambda;
    }

    @Override
    public boolean matching(String target) {
        if (delegate.matching(target)) {
            return true;
        }
        if (!includeLambda) {
            return false;
        }
        String enclosing = parseEnclosingMethod(target);
        return enclosing != null && delegate.matching(enclosing);
    }

    /**
     * 解析 lambda 合成方法名，返回其外层方法名；非 lambda 方法名返回 {@code null}。
     */
    private static String parseEnclosingMethod(String methodName) {
        if (methodName == null || !methodName.startsWith(LAMBDA_METHOD_PREFIX)) {
            return null;
        }
        String rest = methodName.substring(LAMBDA_METHOD_PREFIX.length());
        int lastDollar = rest.lastIndexOf('$');
        // 没有索引分隔符，或外层方法名为空
        if (lastDollar <= 0) {
            return null;
        }
        String index = rest.substring(lastDollar + 1);
        if (index.isEmpty()) {
            return null;
        }
        // 索引必须是纯数字（lambda$run$0 / lambda$run$1 ...），借此排除 $deserializeLambda$ 等
        for (int i = 0; i < index.length(); i++) {
            if (!Character.isDigit(index.charAt(i))) {
                return null;
            }
        }
        return rest.substring(0, lastDollar);
    }
}
