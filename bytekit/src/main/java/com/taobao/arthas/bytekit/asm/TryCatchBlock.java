package com.taobao.arthas.bytekit.asm;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LabelNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TryCatchBlock {
    private final MethodNode methodNode;
    private final LabelNode startLabelNode = new LabelNode();
    private final LabelNode endLabelNode = new LabelNode();

    public TryCatchBlock(final MethodNode methodNode) {
        this(methodNode, Type.getType(Throwable.class).getInternalName());
    }

    public TryCatchBlock(final MethodNode methodNode, String exception) {
        this.methodNode = methodNode;

        final TryCatchBlockNode tryCatchBlockNode = new TryCatchBlockNode(this.startLabelNode, this.endLabelNode,
                this.endLabelNode, exception);
        if (this.methodNode.tryCatchBlocks == null) {
            this.methodNode.tryCatchBlocks = new ArrayList<TryCatchBlockNode>();
        }
        this.methodNode.tryCatchBlocks.add(tryCatchBlockNode);
    }

    public LabelNode getStartLabelNode() {
        return this.startLabelNode;
    }

    public LabelNode getEndLabelNode() {
        return this.endLabelNode;
    }

    public void sort() {
        if (this.methodNode.tryCatchBlocks == null) {
            return;
        }

        // Compares TryCatchBlockNodes by the length of their "try" block.
        Collections.sort(this.methodNode.tryCatchBlocks, new Comparator<TryCatchBlockNode>() {
            @Override
            public int compare(TryCatchBlockNode t1, TryCatchBlockNode t2) {
                int len1 = blockLength(t1);
                int len2 = blockLength(t2);
                return len1 - len2;
            }

            private int blockLength(TryCatchBlockNode block) {
                final int startidx = methodNode.instructions.indexOf(block.start);
                final int endidx = methodNode.instructions.indexOf(block.end);
                return endidx - startidx;
            }
        });

        // Updates the 'target' of each try catch block annotation.
        for (int i = 0; i < this.methodNode.tryCatchBlocks.size(); i++) {
            this.methodNode.tryCatchBlocks.get(i).updateIndex(i);
        }
    }
}