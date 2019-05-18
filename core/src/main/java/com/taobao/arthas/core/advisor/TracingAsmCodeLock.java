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
            LockOpStream.BEGIN_CODE_STREAM,
            LockOpStream.END_CODE_STREAM,
            LockOpStream.INS_MARK_STREAM);
    }
}
