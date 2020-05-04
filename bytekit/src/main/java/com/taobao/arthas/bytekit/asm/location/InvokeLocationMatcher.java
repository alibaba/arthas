package com.taobao.arthas.bytekit.asm.location;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.JumpInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LabelNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.TryCatchBlock;
import com.taobao.arthas.bytekit.asm.location.Location.InvokeExceptionExitLocation;
import com.taobao.arthas.bytekit.asm.location.Location.InvokeLocation;
import com.taobao.arthas.bytekit.asm.location.filter.LocationFilter;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.MatchUtils;

/**
 * 
 * @author hengyunabc
 *
 */
public class InvokeLocationMatcher implements LocationMatcher {

    /**
     * the name of the method being invoked at the point where the trigger point
     * should be inserted. maybe null, when null, match all method invoke.
     */
    private String methodName;

    /**
     * the name of the type to which the method belongs or null if any type will do
     */
    private String owner;

    /**
     * the method signature in externalised form, maybe null.
     */
    private String desc;

    /**
     * count identifying which invocation should be taken as the trigger point. if
     * not specified as a parameter this defaults to the first invocation.
     */
    private int count;

    /**
     * flag which is false if the trigger should be inserted before the method
     * invocation is performed and true if it should be inserted after
     */
    private boolean whenComplete;

    /**
     * wildcard matcher to exclude the invoke class, such as java.* to exclude jdk
     * invoke.
     */
    private List<String> excludes = new ArrayList<String>();

    private boolean atInvokeExcpetionExit = false;

    public InvokeLocationMatcher(String owner, String methodName, String desc, int count, boolean whenComplete,
            List<String> excludes, boolean atInvokeExcpetionExit) {
        super();
        this.owner = owner;
        this.methodName = methodName;
        this.desc = desc;
        this.count = count;
        this.whenComplete = whenComplete;
        this.excludes = excludes;
        this.atInvokeExcpetionExit = atInvokeExcpetionExit;
    }

    public InvokeLocationMatcher(String owner, String methodName, String desc, int count, boolean whenComplete,
            List<String> excludes) {
        this(owner, methodName, desc, count, whenComplete, excludes, false);
    }

    public InvokeLocationMatcher(String owner, String methodName, String desc, int count, boolean whenComplete) {
        this(owner, methodName, desc, count, whenComplete, new ArrayList<String>());
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        if (this.atInvokeExcpetionExit) {
            return matchForException(methodProcessor);
        }
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();
        
        LocationFilter locationFilter = methodProcessor.getLocationFilter();
        
        LocationType locationType = whenComplete ? LocationType.INVOKE_COMPLETED : LocationType.INVOKE;

        int matchedCount = 0;
        while (insnNode != null) {
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;

                if (matchCall(methodInsnNode)) {
                    if(locationFilter.allow(methodInsnNode, locationType, this.whenComplete)) {
                        matchedCount++;
                        if (count <= 0 || count == matchedCount) {
                            InvokeLocation invokeLocation = new InvokeLocation(methodInsnNode, count, whenComplete);
                            locations.add(invokeLocation);
                        }
                    }
                }
            }
            insnNode = insnNode.getNext();
        }

        return locations;
    }

    public List<Location> matchForException(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();

        MethodNode methodNode = methodProcessor.getMethodNode();

        List<MethodInsnNode> methodInsnNodes = new ArrayList<MethodInsnNode>();
        
        LocationFilter locationFilter = methodProcessor.getLocationFilter();
        
        LocationType locationType = LocationType.INVOKE_EXCEPTION_EXIT;

        int matchedCount = 0;
        while (insnNode != null) {
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;

                if (matchCall(methodInsnNode)) {
                    if(locationFilter.allow(methodInsnNode, locationType, this.whenComplete)) {
                        matchedCount++;
                        if (count <= 0 || count == matchedCount) {
                            methodInsnNodes.add(methodInsnNode);
                        }
                    }
                }
            }
            insnNode = insnNode.getNext();
        }

        // insert try/catch
        for (MethodInsnNode methodInsnNode : methodInsnNodes) {
            TryCatchBlock tryCatchBlock = new TryCatchBlock(methodNode);

            InsnList toInsert = new InsnList();

            LabelNode gotoDest = new LabelNode();

            LabelNode startLabelNode = tryCatchBlock.getStartLabelNode();
            LabelNode endLabelNode = tryCatchBlock.getEndLabelNode();

            toInsert.add(new JumpInsnNode(Opcodes.GOTO, gotoDest));
            toInsert.add(endLabelNode);
            AsmOpUtils.throwException(toInsert);
            locations.add(new InvokeExceptionExitLocation(methodInsnNode, endLabelNode));

            toInsert.add(gotoDest);

            methodNode.instructions.insertBefore(methodInsnNode, startLabelNode);
            methodNode.instructions.insert(methodInsnNode, toInsert);

            tryCatchBlock.sort();
        }

        return locations;
    }

    private boolean matchCall(MethodInsnNode methodInsnNode) {

        if (methodName != null && !methodName.isEmpty()) {
            if (!this.methodName.equals(methodInsnNode.name)) {
                return false;
            }
        }

        if (!excludes.isEmpty()) {
            String ownerClassName = Type.getObjectType(methodInsnNode.owner).getClassName();
            for (String exclude : excludes) {
                if (MatchUtils.wildcardMatch(ownerClassName, exclude)) {
                    return false;
                }
            }
        }

        if (this.owner != null && !this.owner.equals(methodInsnNode.owner)) {
            return false;
        }

        if (this.desc != null && !desc.equals(methodInsnNode.desc)) {
            return false;
        }

        return true;

    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isWhenComplete() {
        return whenComplete;
    }

    public void setWhenComplete(boolean whenComplete) {
        this.whenComplete = whenComplete;
    }
}
