package com.taobao.arthas.core.command.monitor200;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.binding.BindingContext;
import com.alibaba.bytekit.asm.binding.IntBinding;
import com.alibaba.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.BindingParserUtils;
import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.asm.location.LocationMatcher;
import com.alibaba.bytekit.asm.location.LocationType;
import com.alibaba.bytekit.asm.location.filter.LocationFilter;
import com.alibaba.bytekit.utils.AsmOpUtils;
import com.alibaba.bytekit.utils.MatchUtils;
import com.alibaba.bytekit.utils.ReflectionUtils;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.*;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.advisor.SpyInterceptors;
import com.taobao.arthas.core.util.EncryptUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.Ansi;

import java.lang.reflect.Method;
import java.util.*;

/**
 * line命令用到的一些方法集合
 */
public class LineHelper {

    private static final String LOCATION_CODE_SPLITTER = "-";
    public static final String LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER = "*$*";
    private static final String LOCATION_CONTENT_VARIABLE_FORMATTER = "assign-variable:%s";
    private static final String LOCATION_CONTENT_METHOD_FORMATTER = "invoke-method:%s#%s:%s";
    private static final String LOCATION_VIEW_LINE_SPLIT_LINE = "------------------------- lineCode location -------------------------";
    private static final String LOCATION_VIEW_LINE_HEADER = "format： /*LineNumber*/ (LineCode)-> Instruction";
    private static final String LOCATION_VIEW_LINE_FORMATTER_POINTER = "/*%-3s*/ (%s)->  ";
    private static final String LOCATION_VIEW_LINE_FORMATTER_INSTRUCTION = "                  %s";
    public static final String VARIABLE_RENAME = "-renamed-";
    public static final String EXCLUDE_VARIABLE_THIS = "this";

    /**
     * 方法退出前的LineNumber值
     */
    public static final int LINE_LOCATION_BEFORE_METHOD_EXIT = -1;

    /**
     * 判断line命令使用的line参数是否合法
     * 1.LineNumber类型，如12
     * 2.LineCode类型，如abcd-1
     */
    public static boolean validLocation(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        if (line.equals(String.valueOf(LINE_LOCATION_BEFORE_METHOD_EXIT))) return true;
        if (line.contains(LOCATION_CODE_SPLITTER)) {
            String[] arr = line.split(LOCATION_CODE_SPLITTER);
            if (arr.length != 2) {
                return false;
            }
            if (StringUtils.isBlank(arr[0])){
                return false;
            }
            return StringUtils.isNumeric(arr[1]) && arr[1].length() < 8;
        }
        return StringUtils.isNumeric(line) && line.length() < 8;
    }

    /**
     * 判定是否支持line命令
     * 有个情况是先启动了旧版本,然后再启动新版,就会导致方法找不到,进而报错,需要避免这个问题
     */
    public static boolean hasSupportLineCommand() {
        boolean hasAtLineMethod = false;
        try {
            Class clazz = Class.forName("java.arthas.SpyAPI"); // 加载不到会抛异常
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                if (declaredMethod.getName().equals("atLineCode")) {
                    hasAtLineMethod = true;
                    break;
                }
            }
        } catch (Throwable e) {
            // ignore
        }
        return hasAtLineMethod;
    }

    /**
     * 构建本地变量Map
     * @param vars 本地变量数据
     * @param varNames 本地变量名数据
     */
    public static Map<String, Object> buildVarMap(Object[] vars, String[] varNames){
        Map<String, Object> varMap = new HashMap<String, Object>(vars.length);
        for (int i = 0; i < vars.length; i++) {
            //不放入this，想要获取this，直接在表达式中获取即可
            if (LineHelper.EXCLUDE_VARIABLE_THIS.equals(varNames[i])) continue;
            String varName = LineHelper.determinedVarName(varMap.keySet(), varNames[i]);
            varMap.put(varName, vars[i]);
        }
        return varMap;
    }

    /**
     * 生成方法的lineCode视图
     */
    public static String renderMethodLineCodeView(MethodNode methodNode) {
        Object[] lineArray = renderMethodView(methodNode).toArray();
        return StringUtils.join(lineArray, "\n");
    }

    /**
     * 创建增强用的 InterceptorProcessor
     * @param line 行标识，可能是行号(LineNumber)，也可能是行的特殊标号(LineCode)
     */
    public static InterceptorProcessor createLineInterceptorProcessor(String line) {
        Method method = ReflectionUtils.findMethod(SpyInterceptors.SpyLineInterceptor.class, "atLineNumber", null);
        if (isLineCode(line)) {
            method = ReflectionUtils.findMethod(SpyInterceptors.SpyLineInterceptor.class, "atLineCode", null);
        }

        InterceptorProcessor interceptorProcessor = new InterceptorProcessor(method.getDeclaringClass().getClassLoader());

        //locationMatcher
        if (isLineCode(line)) {
            interceptorProcessor.setLocationMatcher(new LineCodeMatcher(line));
        } else {
            interceptorProcessor.setLocationMatcher(new LineNumberMatcher(line));
        }

        //interceptorMethodConfig
        InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
        interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);
        interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
        interceptorMethodConfig.setMethodName(method.getName());
        interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

        //inline
        interceptorMethodConfig.setInline(true);

        //bindings
        List<Binding> bindings = BindingParserUtils.parseBindings(method);
        LineLocationBinding lineLocationBinding = new LineLocationBinding(line);
        bindings.add(lineLocationBinding);
        interceptorMethodConfig.setBindings(bindings);

        return interceptorProcessor;
    }

    /**
     * 解析变量名
     * 处理重复的变量名
     */
    private static String determinedVarName(Set<String> nameSet, String varName) {
        String tmpVarName = varName;
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            if (nameSet.contains(tmpVarName)) {
                tmpVarName = varName + VARIABLE_RENAME + i;
            } else {
                return tmpVarName;
            }
        }
        throw new IllegalArgumentException("illegal varName:" + varName);
    }

    /**
     * 根据lineCode找到对应的InsnNode
     */
    private static AbstractInsnNode findInsnNodeByLocationCode(MethodNode methodNode, String lineCode) {
        Pair<String, Integer> lineCodePair = convertToLineCode(lineCode);
        Map<String, AbstractInsnNode> uniqMap = genLineCodeMapNode(methodNode, lineCodePair.getFirst().length());
        return uniqMap.get(lineCode);
    }

    /**
     * 将传入的LineCode(形如abcd-1)转换成 结构化的Pair(first=abcd，second=1)
     */
    private static Pair<String, Integer> convertToLineCode(String line) {
        String[] arr = line.split(LOCATION_CODE_SPLITTER);
        return new Pair<String, Integer>(arr[0], Integer.valueOf(arr[1]));
    }

    /**
     * 判断是否为LineCode类型
     */
    private static boolean isLineCode(String line) {
        if (line.equals(String.valueOf(LINE_LOCATION_BEFORE_METHOD_EXIT))) return false;
        return line.contains(LOCATION_CODE_SPLITTER);
    }

    /**
     * 判断 InsnNode 类型是否是line想要的
     * 依据：因为line监测的对象是变量，而变量值一般发生在 赋值、作为参数被方法调用
     * 为什么方法调用只过滤了基础类型的box方法？
     * 1.首先已经明确这些方法不会改变变量值
     * 2.其次在插桩的时候，生成的代码里边会有这些方法，如果不排除掉，会影响LineCode的匹配和生成
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
     * 如方法赋值：assign-variable: varName
     * 如方法调用：invoke-method: java/lang/StringBuilder#append:()V
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
     * 过滤出只会被line命令关注的InsnNode
     * 具体规则见：matchInsnNode方法注释
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
     * /*82* / (f0bd-1)->
     *                      invoke-method: java.util.ArrayList#<init>:(I)V
     * /*83* / (7cda-1)->
     *                      invoke-method: java.lang.Iterable#iterator:()Ljava/util/Iterator;
     * /*83* / (ad72-1)->
     *                      invoke-method: java.util.Iterator#hasNext:()Z
     * /*83* / (b105-1)->
     *                      invoke-method: java.util.Iterator#next:()Ljava/lang/Object;
     * /*84* / (11a1-1)->
     *                      assign-variable: it
     * /*84* / (f9e5-1)->
     *                      invoke-method: java.lang.Number#intValue:()I
     */
    private static List<String> renderMethodView(MethodNode methodNode) {
        List<String> printLines = new LinkedList<String>();

        printLines.add(LOCATION_VIEW_LINE_SPLIT_LINE);
        printLines.add(LOCATION_VIEW_LINE_HEADER);

        //读取变量表
        Map<Integer, String> varIdxMap = new HashMap<Integer, String>();
        for (LocalVariableNode localVariable : methodNode.localVariables) {
            if (!MatchUtils.wildcardMatch(localVariable.name, LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER)) {
                varIdxMap.put(localVariable.index, localVariable.name);
            }
        }

        //过滤出符合的Node
        List<AbstractInsnNode> noteList = filterNodeList(methodNode.instructions, varIdxMap);
        //渲染出肉眼友好的内容
        List<String> contentList = genLocationContentList(noteList, varIdxMap);
        //获取Node对应的行号
        List<Integer> preLineNumberList = genLineNumberList(noteList);
        //生成LineCode
        List<Pair<String, String>> contentAndCode = genLineCode(contentList);

        //渲染需要展示的内容
        for (int i = 0; i < contentAndCode.size(); i++) {
            Pair<String, String> contentCodePair = contentAndCode.get(i);
            Integer preLineNumber = preLineNumberList.get(i);
            String lineCode = Ansi.ansi().fg(Ansi.Color.GREEN).a(contentCodePair.getSecond()).reset().toString();
            String pointer = String.format(LOCATION_VIEW_LINE_FORMATTER_POINTER, preLineNumber, lineCode);
            String instruction = String.format(LOCATION_VIEW_LINE_FORMATTER_INSTRUCTION,contentCodePair.getFirst());
            printLines.add(pointer);
            printLines.add(instruction);
        }

        return printLines;
    }

    /**
     * 生成LineCode及其匹配的InsnNode
     */
    private static Map<String, AbstractInsnNode> genLineCodeMapNode(MethodNode methodNode, int uniqLength) {
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
     * 生成LocationContent及其映射的LineCode
     * 就是 content 做唯一标识，目前采用的方法是md5和排序
     */
    private static List<Pair<String, String>> genLineCode(List<String> locationContentList) {
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
        int length = determineLineCodeLength(contentMapMd5.values());
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
     * 获取出LineCode的长度
     * 目前摘要算法用md5，初始长度给4为，如果有出现重复，则递增
     */
    private static int determineLineCodeLength(Collection<String> md5List) {
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

    /**
     * 定义一个LineLocation
     */
    private static class LineLocation extends Location {

        private String location;

        public LineLocation(AbstractInsnNode insnNode, String location, boolean whenComplete) {
            super(insnNode, whenComplete);
            this.location = location;
        }

        @Override
        public LocationType getLocationType() {
            return LocationType.USER_DEFINE;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

    }

    /**
     * LineNumber匹配
     */
    private static class LineNumberMatcher implements LocationMatcher {

        /**
         * 目标行号
         * -1则代表方法的退出之前
         */
        private final Integer lineNumber;

        public LineNumberMatcher(String locationCode) {
            this.lineNumber = Integer.valueOf(locationCode);
        }

        @Override
        public List<Location> match(MethodProcessor methodProcessor) {
            List<Location> locations = new ArrayList<Location>();
            AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();
            LocationFilter locationFilter = methodProcessor.getLocationFilter();

            while (insnNode != null) {

                if (lineNumber == LINE_LOCATION_BEFORE_METHOD_EXIT) {
                    //匹配方法退出之前，可能会有多个return语句
                    if (insnNode instanceof InsnNode) {
                        InsnNode node = (InsnNode) insnNode;
                        if (matchExit(node)) {
                            //行前匹配
                            boolean filtered = !locationFilter.allow(node, LocationType.LINE, false);
                            if (!filtered){
                                Location location = new LineLocation(node, lineNumber.toString(), false);
                                locations.add(location);
                            }
                        }
                    }
                } else {
                    //匹配具体的行
                    if (insnNode instanceof LineNumberNode) {
                        LineNumberNode lineNumberNode = (LineNumberNode) insnNode;
                        if (matchLine(lineNumberNode.line)) {
                            //行前匹配
                            boolean filtered = !locationFilter.allow(lineNumberNode, LocationType.LINE, false);
                            if (filtered) break;
                            //目前因为如果直接返回lineNumberNode，增强完之后会导致行号丢失，暂时没找到原因，因此取上一个节点
                            Location location = new LineLocation(lineNumberNode.getPrevious(), lineNumber.toString(), false);
                            locations.add(location);
                            //存在一个方法内会有多个相同行号的情况，这里只取第一个
                            break;
                        }
                    }
                }
                insnNode = insnNode.getNext();
            }
            return locations;
        }

        private boolean matchLine(int line) {
            return line == lineNumber;
        }

        public boolean matchExit(InsnNode node) {
            switch (node.getOpcode()) {
                case Opcodes.RETURN: // empty stack
                case Opcodes.IRETURN: // 1 before n/a after
                case Opcodes.FRETURN: // 1 before n/a after
                case Opcodes.ARETURN: // 1 before n/a after
                case Opcodes.LRETURN: // 2 before n/a after
                case Opcodes.DRETURN: // 2 before n/a after
                    return true;
            }
            return false;
        }

    }

    /**
     * LineCode匹配
     */
    private static class LineCodeMatcher implements LocationMatcher {

        private final String lineCode;

        public LineCodeMatcher(String lineCode) {
            this.lineCode = lineCode;
        }

        @Override
        public List<Location> match(MethodProcessor methodProcessor) {
            List<Location> locations = new ArrayList<Location>();
            LocationFilter locationFilter = methodProcessor.getLocationFilter();

            AbstractInsnNode insnNode = findInsnNodeByLocationCode(methodProcessor.getMethodNode(), lineCode);
            if (insnNode == null) {
                return locations;
            }
            boolean filtered = !locationFilter.allow(insnNode, LocationType.USER_DEFINE, false);
            if (filtered) return Collections.emptyList();
            Location location = new LineLocation(insnNode, lineCode, false);
            locations.add(location);
            return locations;
        }

    }

    /**
     * LineLocation的Binding
     * 参照了 {@link IntBinding}
     */
    private static class LineLocationBinding extends Binding {

        private String value;


        public LineLocationBinding(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
            AsmOpUtils.push(instructions, value);
        }

        @Override
        public Type getType(BindingContext bindingContext) {
            return Type.getType(String.class);
        }

    }

}
