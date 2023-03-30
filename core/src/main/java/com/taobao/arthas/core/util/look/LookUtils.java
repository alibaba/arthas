package com.taobao.arthas.core.util.look;

import com.alibaba.bytekit.utils.MatchUtils;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.*;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.util.EncryptUtils;
import com.taobao.arthas.core.util.StringUtils;

import java.util.*;

/**
 * look命令工具
 * look相关的定义中需要特别注意的是LocationCode，它的形式如 abcd-1 这是用来标记一个位置用的，
 * 而它对应的明文则称之为LocationContent，形如 assign-variable: it 或者 invoke-method: java.util.Iterator#hasNext:()Z
 */
public class LookUtils {

    private static final String LOCATION_CODE_SPLITTER = "-";

    public static final String LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER = "*$*";
    private static final String LOCATION_CONTENT_VARIABLE_FORMATTER = "assign-variable: %s";
    private static final String LOCATION_CONTENT_METHOD_FORMATTER = "invoke-method: %s#%s:%s";
    private static final String LOCATION_VIEW_LINE_FORMATTER = "/*%s*/   %s   %s";


    /**
     * 根据locationCode找到对应的InsnNode
     */
    public static AbstractInsnNode findInsnNodeByLocationCode(MethodNode methodNode, String locationCode) {
        Pair<String, Integer> locationCodePair = convertToLocationCode(locationCode);
        Map<String, AbstractInsnNode> uniqMap = genLocationCodeMapNode(methodNode, locationCodePair.getFirst().length());
        return uniqMap.get(locationCode);
    }

    /**
     * 生成方法的look视图
     */
    public static String renderMethodLocation(MethodNode methodNode) {
        Object[] lineArray = renderMethodView(methodNode).toArray();
        return StringUtils.join(lineArray, "\n");
    }

    /**
     * 将传入的location(形如abcd-1)转换成 结构化的Pair(first=abcd，second=1)
     */
    public static Pair<String, Integer> convertToLocationCode(String location) {
        String[] arr = location.split(LOCATION_CODE_SPLITTER);
        return new Pair<String, Integer>(arr[0], Integer.valueOf(arr[1]));
    }

    /**
     * 判断look使用的location是否合法
     * 1.LineNumber类型的location，如12
     * 2.LocationCode类型的location，如abcd=-1
     */
    public static boolean validLocation(String locationCode) {
        if (locationCode == null || locationCode.isEmpty()) {
            return false;
        }
        if (locationCode.contains("-")) {
            String[] arr = locationCode.split(LOCATION_CODE_SPLITTER);
            if (arr.length != 2) {
                return false;
            }
            return StringUtils.isNumeric(arr[1]) && arr[1].length() < 8;
        }
        return StringUtils.isNumeric(locationCode) && locationCode.length() < 8;
    }

    /**
     * 判断是否为LocatonCode类型
     * 依据：是否包含 "-"
     */
    public static boolean isLocationCode(String location) {
        return location.contains(LOCATION_CODE_SPLITTER);
    }

    /**
     * 判断 InsnNode 类型是否是look想要的
     * 依据：因为look监测的对象是变量，而变量值一般发生在 赋值、作为参数被方法调用
     * 为什么方法调用只过滤了基础类型的box方法？
     * 1.首先已经明确这些方法不会改变变量值
     * 2.其次在插桩的时候，生成的代码里边会有这些方法，如果不排除掉，会影响LocationCode的匹配和生成
     * 3.目前要判断一个变量是否作为该方法的参数较为复杂，先不做
     */
    private static boolean matchInsnNode(AbstractInsnNode abstractInsnNode, Set<Integer> allowVariableSet) {
        if (abstractInsnNode instanceof VarInsnNode) {
            //赋值
            switch (abstractInsnNode.getOpcode()) {
                case Opcodes.ISTORE:
                case Opcodes.LSTORE:
                case Opcodes.FSTORE:
                case Opcodes.DSTORE:
                case Opcodes.ASTORE:
                    return allowVariableSet.contains(((VarInsnNode) abstractInsnNode).var);
            }
            return false;
        } else if (abstractInsnNode instanceof MethodInsnNode) {
            //box方法,及arthas内部方法
            MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
            if (methodInsnNode.owner.equals("java/arthas/SpyAPI")) return false;
            if (methodInsnNode.owner.equals("java/lang/Byte") ||
                    methodInsnNode.owner.equals("java/lang/Short") ||
                    methodInsnNode.owner.equals("java/lang/Integer") ||
                    methodInsnNode.owner.equals("java/lang/Long") ||
                    methodInsnNode.owner.equals("java/lang/Boolean") ||
                    methodInsnNode.owner.equals("java/lang/Float")
            )
                return !methodInsnNode.name.equals("<init>");
            return true;
        }
        return false;
    }

    /**
     * 向前寻找离insnNode最近的LineNumber节点并返回行号
     * 如果找不到，则返回 0
     */
    private static int findPreLineNumber(AbstractInsnNode insnNode) {
        while (insnNode != null) {
            if (insnNode instanceof LineNumberNode) {
                return ((LineNumberNode) insnNode).line;
            }
            insnNode = insnNode.getPrevious();
        }
        return 0;
    }

    /**
     * 生成InsnNode对应的LocationContent，也就是方便肉眼理解的形式
     */
    private static List<String> genLocationContentList(List<AbstractInsnNode> nodeList, Map<Integer, String> varIdxMap) {
        List<String> contentList = new LinkedList<String>();
        for (AbstractInsnNode abstractInsnNode : nodeList) {
            String content = "";
            if (abstractInsnNode instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                content = String.format(LOCATION_CONTENT_VARIABLE_FORMATTER, varIdxMap.get(varInsnNode.var));
            } else if (abstractInsnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                content = String.format(LOCATION_CONTENT_METHOD_FORMATTER, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
            }
            contentList.add(content);
        }
        return contentList;
    }

    /**
     * 生成InsnNode对应的LineNumber
     */
    private static List<Integer> genLineNumberList(List<AbstractInsnNode> nodeList) {
        List<Integer> preLineNumberList = new LinkedList<Integer>();
        for (AbstractInsnNode abstractInsnNode : nodeList) {
            int preLineNumber = findPreLineNumber(abstractInsnNode);
            preLineNumberList.add(preLineNumber);
        }
        return preLineNumberList;
    }

    /**
     * 过滤出只会被look命令关注的InsnNode
     */
    private static List<AbstractInsnNode> filterNodeList(InsnList insnList, Map<Integer, String> varIdxMap) {
        List<AbstractInsnNode> noteList = new LinkedList<AbstractInsnNode>();
        for (AbstractInsnNode abstractInsnNode : insnList) {
            if (matchInsnNode(abstractInsnNode, varIdxMap.keySet())) {
                noteList.add(abstractInsnNode);
            }
        }
        return noteList;
    }

    /**
     * 生成方法的jad视图，形如：
     * /*82* /   f0bd-1   invoke-method: java.util.ArrayList#<init>:(I)V
     * /*83* /   7cda-1   invoke-method: java.lang.Iterable#iterator:()Ljava/util/Iterator;
     * /*83* /   ad72-1   invoke-method: java.util.Iterator#hasNext:()Z
     * /*83* /   b105-1   invoke-method: java.util.Iterator#next:()Ljava/lang/Object;
     * /*84* /   11a1-1   assign-variable: it
     * /*84* /   f9e5-1   invoke-method: java.lang.Number#intValue:()I
     */
    private static List<String> renderMethodView(MethodNode methodNode) {
        List<String> printLines = new LinkedList<String>();

        Map<Integer, String> varIdxMap = new HashMap<Integer, String>();
        for (LocalVariableNode localVariable : methodNode.localVariables) {
            if (!MatchUtils.wildcardMatch(localVariable.name, LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER)) {
                varIdxMap.put(localVariable.index, localVariable.name);
            }
        }

        List<AbstractInsnNode> noteList = filterNodeList(methodNode.instructions, varIdxMap);

        List<String> contentList = genLocationContentList(noteList, varIdxMap);
        List<Integer> preLineNumberList = genLineNumberList(noteList);

        List<Pair<String, String>> contentAndCode = genLocationCode(contentList);

        for (int i = 0; i < contentAndCode.size(); i++) {
            Pair<String, String> contentCodePair = contentAndCode.get(i);
            Integer preLineNumber = preLineNumberList.get(i);
            String printLine = String.format(LOCATION_VIEW_LINE_FORMATTER, preLineNumber, contentCodePair.getSecond(), contentCodePair.getFirst());
            printLines.add(printLine);
        }

        return printLines;
    }

    /**
     * 生成LocationCode及其匹配的InsnNode
     */
    private static Map<String, AbstractInsnNode> genLocationCodeMapNode(MethodNode methodNode, int uniqLength) {
        //本地变量表
        Map<Integer, String> varIdxMap = new HashMap<Integer, String>();
        for (LocalVariableNode localVariable : methodNode.localVariables) {
            if (!MatchUtils.wildcardMatch(localVariable.name, LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER)) {
                varIdxMap.put(localVariable.index, localVariable.name);
            }
        }
        //filter node
        List<AbstractInsnNode> nodeList = new LinkedList<AbstractInsnNode>();
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            if (matchInsnNode(abstractInsnNode, varIdxMap.keySet())) {
                nodeList.add(abstractInsnNode);
            }
        }
        //拼凑出content
        List<String> contentList = genLocationContentList(nodeList, varIdxMap);
        //构建拼凑map
        Map<String, AbstractInsnNode> uniqMapNode = new HashMap<String, AbstractInsnNode>();
        Map<String, Integer> contentMapIdx = new HashMap<String, Integer>();
        for (int i = 0; i < contentList.size(); i++) {
            String c = contentList.get(i);
            Integer lastIdx = contentMapIdx.get(c);
            int curIdx = lastIdx == null ? 1 : ++lastIdx;
            contentMapIdx.put(c, curIdx);

            String md5 = EncryptUtils.md5DigestAsHex(c.getBytes()).substring(0, uniqLength);

            String locationCode = md5 + LOCATION_CODE_SPLITTER + curIdx;
            uniqMapNode.put(locationCode, nodeList.get(i));
        }
        return uniqMapNode;
    }

    /**
     * 生成LocationContent及其映射的LocationCode
     */
    private static List<Pair<String, String>> genLocationCode(List<String> locationContentList) {
        int preSize = locationContentList.size();
        List<Pair<String, String>> locationContentAndCodeList = new LinkedList<Pair<String, String>>();
        Set<String> contentSet = new HashSet<String>(locationContentList);
        //采集md5
        Map<String, String> contentMapMd5 = new HashMap<String, String>(preSize);
        for (String content : contentSet) {
            String project = EncryptUtils.md5DigestAsHex(content.getBytes());
            contentMapMd5.put(content, project);
        }
        //寻找合适长度
        int length = determineLocationCodeLength(contentMapMd5.values());
        //生成map
        Map<String, Integer> contentMapIdx = new HashMap<String, Integer>();
        for (String locationContent : locationContentList) {
            //维护idx
            Integer lastIdx = contentMapIdx.get(locationContent);
            int curIdx = lastIdx == null ? 1 : ++lastIdx;
            contentMapIdx.put(locationContent, curIdx);

            String locationCode = locationContent + LOCATION_CODE_SPLITTER + curIdx;
            if (length != -1) {
                String md5 = contentMapMd5.get(locationContent);
                locationCode = md5.substring(0, length) + LOCATION_CODE_SPLITTER + curIdx;
            }
            locationContentAndCodeList.add(new Pair<String, String>(locationContent, locationCode));
        }
        return locationContentAndCodeList;
    }

    /**
     * 获取出LocationCode的长度
     * 目前摘要算法用md5，初始长度给4为，如果有出现重复，则递增
     */
    private static int determineLocationCodeLength(Collection<String> md5List) {
        Set<String> md5Set = new HashSet<String>(md5List);
        if (md5Set.size() != md5List.size()) {
            return -1;
        }
        for (int i = 4; i < 32; i++) {
            Set<String> uniqSet = new HashSet<String>(md5Set.size());
            for (String md5 : md5Set) {
                String uniq = md5.substring(0, i);
                if (!uniqSet.add(uniq)) {
                    break;
                }
            }
            if (uniqSet.size() == md5Set.size()) {
                return i;
            }
        }
        return -1;
    }

}
