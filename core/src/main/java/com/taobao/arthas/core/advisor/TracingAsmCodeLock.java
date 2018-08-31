package com.taobao.arthas.core.advisor;

import org.objectweb.asm.commons.AdviceAdapter;

/**
 * 用于Tracing的代码锁
 * @author ralf0131 2016-12-28 16:46.
 */
public class TracingAsmCodeLock extends AsmCodeLock {

    public TracingAsmCodeLock(AdviceAdapter aa) {
        super(
                aa,
                new int[]{
                        ACONST_NULL, ICONST_0, ICONST_1, SWAP, SWAP, POP2, POP
                },
                new int[]{
                        ICONST_1, ACONST_NULL, ICONST_0, SWAP, SWAP, POP, POP2
                }
        );
    }
}
