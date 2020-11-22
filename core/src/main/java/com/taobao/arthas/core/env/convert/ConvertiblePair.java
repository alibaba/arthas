package com.taobao.arthas.core.env.convert;

/**
 * Holder for a source-to-target class pair.
 */
public final class ConvertiblePair {

    private final Class<?> sourceType;

    private final Class<?> targetType;

    /**
     * Create a new source-to-target pair.
     * 
     * @param sourceType the source type
     * @param targetType the target type
     */
    public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public Class<?> getSourceType() {
        return this.sourceType;
    }

    public Class<?> getTargetType() {
        return this.targetType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ConvertiblePair.class) {
            return false;
        }
        ConvertiblePair other = (ConvertiblePair) obj;
        return this.sourceType.equals(other.sourceType) && this.targetType.equals(other.targetType);
    }

    @Override
    public int hashCode() {
        return this.sourceType.hashCode() * 31 + this.targetType.hashCode();
    }

    @Override
    public String toString() {
        return this.sourceType.getName() + " -> " + this.targetType.getName();
    }
}