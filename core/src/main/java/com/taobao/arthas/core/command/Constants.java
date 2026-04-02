package com.taobao.arthas.core.command;

/**
 * Arthas命令常量接口
 * <p>
 * 定义了Arthas命令系统中使用的各种常量字符串，
 * 包括表达式描述、示例、文档链接等信息。
 * 这些常量主要用于生成命令的帮助文档和使用说明。
 *
 * @author ralf0131 2016-12-14 17:21.
 * @author hengyunabc 2018-12-03
 */
public interface Constants {

    /**
     * 表达式描述文档
     * <p>
     * 描述了Arthas支持的表达式类型和可用变量。
     * 在watch、trace等命令中，可以使用这些表达式来访问和操作运行时的上下文信息。
     * <p>
     * 可用的表达式变量包括：
     * <ul>
     * <li>target: 被调用的对象实例</li>
     * <li>clazz: 目标对象的Class对象</li>
     * <li>method: 被调用的构造器或方法对象</li>
     * <li>params: 方法参数数组</li>
     * <li>params[0..n]: 参数数组中的某个具体元素</li>
     * <li>returnObj: 方法返回的对象</li>
     * <li>throwExp: 方法抛出的异常对象</li>
     * <li>isReturn: 方法是否通过return结束</li>
     * <li>isThrow: 方法是否通过抛出异常结束</li>
     * <li>#cost: 方法执行的耗时（毫秒）</li>
     * </ul>
     */
    String EXPRESS_DESCRIPTION = "  The express may be one of the following expression (evaluated dynamically):\n" +
            "          target : the object\n" +
            "           clazz : the object's class\n" +
            "          method : the constructor or method\n" +
            "          params : the parameters array of method\n" +
            "    params[0..n] : the element of parameters array\n" +
            "       returnObj : the returned object of method\n" +
            "        throwExp : the throw exception of method\n" +
            "        isReturn : the method ended by return\n" +
            "         isThrow : the method ended by throwing exception\n" +
            "           #cost : the execution time in ms of method invocation";

    /**
     * 示例部分的标题
     * <p>
     * 用于在帮助文档中标记示例代码的开始位置。
     */
    String EXAMPLE = "\nEXAMPLES:\n";

    /**
     * 文档部分的标题
     * <p>
     * 用于在帮助文档中标记文档链接的开始位置。
     */
    String WIKI = "\nWIKI:\n";

    /**
     * Arthas官方文档主页
     * <p>
     * 提供Arthas的详细使用文档和API说明。
     * 用户可以通过这个链接获取更详细的信息和高级用法。
     */
    String WIKI_HOME = "  https://arthas.aliyun.com/doc/";

    /**
     * 表达式使用示例
     * <p>
     * 提供了常用表达式的使用示例，帮助用户快速理解如何使用表达式。
     * 这些示例展示了如何访问方法参数、返回值、目标对象等。
     * <p>
     * 示例包括：
     * <ul>
     * <li>params: 打印所有参数</li>
     * <li>params[0]: 打印第一个参数</li>
     * <li>'params[0]+params[1]': 计算前两个参数的和</li>
     * <li>'{params[0], target, returnObj}': 打印第一个参数、目标对象和返回值</li>
     * <li>returnObj: 打印返回值</li>
     * <li>throwExp: 打印异常对象</li>
     * <li>target: 打印目标对象</li>
     * <li>clazz: 打印目标对象的类</li>
     * <li>method: 打印方法对象</li>
     * </ul>
     */
    String EXPRESS_EXAMPLES =   "Examples:\n" +
                                "  params\n" +
                                "  params[0]\n" +
                                "  'params[0]+params[1]'\n" +
                                "  '{params[0], target, returnObj}'\n" +
                                "  returnObj\n" +
                                "  throwExp\n" +
                                "  target\n" +
                                "  clazz\n" +
                                "  method\n";

    /**
     * 条件表达式说明文档
     * <p>
     * 说明了如何在命令中使用OGNL风格的条件表达式。
     * 条件表达式用于过滤和筛选需要观察的方法调用。
     * <p>
     * 条件表达式示例：
     * <ul>
     * <li>1==1: 始终为真（总是匹配）</li>
     * <li>true: 布尔值true（总是匹配）</li>
     * <li>false: 布尔值false（从不匹配）</li>
     * <li>'params.length>=0': 参数长度大于等于0时匹配</li>
     * <li>1==2: 始终为假（从不匹配）</li>
     * <li>'#cost>100': 方法执行时间超过100ms时匹配</li>
     * </ul>
     * <p>
     * 条件表达式在watch、trace、tt等命令中特别有用，
     * 可以只捕获满足特定条件的方法调用。
     */
    String CONDITION_EXPRESS =  "Conditional expression in ognl style, for example:\n" +
                                "  TRUE  : 1==1\n" +
                                "  TRUE  : true\n" +
                                "  FALSE : false\n" +
                                "  TRUE  : 'params.length>=0'\n" +
                                "  FALSE : 1==2\n" +
                                "  '#cost>100'\n";

}
