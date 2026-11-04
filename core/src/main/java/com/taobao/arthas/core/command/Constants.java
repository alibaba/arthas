package com.taobao.arthas.core.command;

/**
 * @author ralf0131 2016-12-14 17:21.
 * @author hengyunabc 2018-12-03
 */
public interface Constants {

    /**
     * TODO improve the description
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

    String EXAMPLE = "\nEXAMPLES:\n";

    String WIKI = "\nWIKI:\n";

    String WIKI_HOME = "  https://arthas.aliyun.com/doc/";

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

    String CONDITION_EXPRESS =  "Conditional expression in ognl style, for example:\n" +
                                "  TRUE  : 1==1\n" +
                                "  TRUE  : true\n" +
                                "  FALSE : false\n" +
                                "  TRUE  : 'params.length>=0'\n" +
                                "  FALSE : 1==2\n" +
                                "  '#cost>100'\n";

}
