package org.mvel2.util;

public class CompatibilityStrategy {

    public static CompatibilityEvaluator compatibilityEvaluator = new DefaultCompatibilityEvaluator();

    private CompatibilityStrategy() {
    }

    public static boolean areEqualityCompatible(Class<?> c1, Class<?> c2) {
        return compatibilityEvaluator.areEqualityCompatible(c1, c2);
    }

    public static boolean areComparisonCompatible(Class<?> c1, Class<?> c2) {
        return compatibilityEvaluator.areComparisonCompatible(c1, c2);
    }

    public static void setCompatibilityEvaluator(CompatibilityEvaluator compatibilityEvaluator) {
        CompatibilityStrategy.compatibilityEvaluator = compatibilityEvaluator;
    }

    public interface CompatibilityEvaluator {

        boolean areEqualityCompatible(Class<?> c1, Class<?> c2);

        boolean areComparisonCompatible(Class<?> c1, Class<?> c2);
    }

    public static class DefaultCompatibilityEvaluator implements CompatibilityEvaluator {

        public boolean areEqualityCompatible(Class<?> c1, Class<?> c2) {
            if (c1 == NullType.class || c2 == NullType.class) return true;
            if (c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1)) return true;
            if (isBoxedNumber(c1, false) && isBoxedNumber(c2, true)) return true;
            if (c1.isPrimitive()) return c2.isPrimitive() || arePrimitiveCompatible(c1, c2, true);
            if (c2.isPrimitive()) return arePrimitiveCompatible(c2, c1, false);
            return false;
        }

        public boolean areComparisonCompatible(Class<?> c1, Class<?> c2) {
            return areEqualityCompatible(c1, c2);
        }

        private boolean arePrimitiveCompatible(Class<?> primitive, Class<?> boxed, boolean leftFirst) {
            if (primitive == Boolean.TYPE) return boxed == Boolean.class;
            if (primitive == Integer.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Long.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Double.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Float.TYPE) return isBoxedNumber(boxed, leftFirst);
            if (primitive == Character.TYPE) return boxed == Character.class;
            if (primitive == Byte.TYPE) return boxed == Byte.class;
            if (primitive == Short.TYPE) return boxed == Short.class;
            return false;
        }

        private boolean isBoxedNumber(Class<?> c, boolean allowString) {
            return Number.class.isAssignableFrom(c) || (allowString && c == String.class);
        }
    }
}
