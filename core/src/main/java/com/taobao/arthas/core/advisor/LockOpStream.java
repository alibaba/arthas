package com.taobao.arthas.core.advisor;

import static org.objectweb.asm.Opcodes.*;

/**
 * Arthas special opcodes stream.
 *
 * Date: 2019/5/16
 *
 * @author xuzhiyi
 */
public interface LockOpStream {

    int[] BEGIN_CODE_STREAM = new int[]{
        ACONST_NULL, ICONST_0, ICONST_1, SWAP, SWAP, POP2, POP
    };

    int[] END_CODE_STREAM = new int[]{
        ICONST_1, ACONST_NULL, ICONST_0, SWAP, SWAP, POP, POP2
    };

    int[] INS_MARK_STREAM = new int[]{
        ICONST_2, ICONST_0, ICONST_1, SWAP, SWAP, POP2, POP
    };
}
