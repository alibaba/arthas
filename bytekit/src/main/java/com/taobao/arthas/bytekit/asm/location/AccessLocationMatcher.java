package com.taobao.arthas.bytekit.asm.location;

public abstract class AccessLocationMatcher implements LocationMatcher {
    protected int count;

    /**
     * flags identifying which type of access should be used to identify the
     * trigger. this is either ACCESS_READ, ACCESS_WRITE or an OR of these two
     * values
     */
    protected int flags;

    /**
     * flag which is false if the trigger should be inserted before the field
     * access is performed and true if it should be inserted after
     */
    protected boolean whenComplete;

    AccessLocationMatcher(int count, int flags, boolean whenComplete) {
        this.count = count;
        this.flags = flags;
        this.whenComplete = whenComplete;
    }
}
