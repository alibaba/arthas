package com.taobao.arthas.core.command.express;

/**
 * 表达式接口
 *
 * 该接口定义了表达式求值的核心功能，用于在Arthas命令中动态执行表达式。
 * 主要用于OGNL（Object-Graph Navigation Language）表达式的求值和绑定操作。
 *
 * <p>主要功能：</p>
 * <ul>
 * <li>表达式求值：支持复杂的对象属性访问和方法调用</li>
 * <li>布尔表达式判断：快速判断条件是否成立</li>
 * <li>对象绑定：将对象绑定到表达式上下文，便于直接访问</li>
 * <li>变量绑定：支持命名变量的绑定和使用</li>
 * <li>上下文重置：清空所有绑定，重置表达式上下文</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * 在watch、trace、tt等命令中，需要动态计算表达式的值或判断条件是否满足。
 * 例如：
 * <pre>
 * Express express = ...;
 * express.bind(someObject);  // 绑定观察对象
 * Object result = express.get("field.name");  // 获取字段值
 * boolean condition = express.is("value > 100");  // 判断条件
 * </pre>
 *
 * <p>设计模式：</p>
 * 采用流式API设计，支持链式调用，提供更好的使用体验。
 *
 * Created by vlinux on 15/5/20.
 */
public interface Express {

    /**
     * 根据表达式获取值
     *
     * 该方法用于执行任意表达式并返回计算结果。表达式可以是：
     * <ul>
     * <li>简单的属性访问：如"name"、"field.value"</li>
     * <li>方法调用：如"toString()"</li>
     * <li>复杂表达式：如"a + b * c"</li>
     * <li>OGNL表达式：如"@Math@sin(0)"</li>
     * </ul>
     *
     * @param express 要执行的表达式字符串
     *                 表达式将在当前绑定的对象上下文中求值
     * @return 表达式计算后的结果对象
     *         返回类型取决于表达式的实际类型
     * @throws ExpressException 当表达式语法错误或执行出错时抛出
     *                         例如：属性不存在、方法调用失败等
     */
    Object get(String express) throws ExpressException;

    /**
     * 根据表达式判断是与否
     *
     * 该方法用于执行布尔表达式，返回true或false。
     * 主要用于条件判断，如过滤、断言等场景。
     *
     * <p>表达式示例：</p>
     * <pre>
     * express.is("value > 100")           // 比较表达式
     * express.is("name == 'test'")        // 相等判断
     * express.is("list.isEmpty()")        // 方法调用
     * express.is("str.contains('abc')")   // 字符串包含
     * </pre>
     *
     * @param express 布尔表达式字符串
     *                表达式的计算结果应该是布尔类型
     * @return 表达式计算后的布尔值
     *         如果表达式结果不是布尔类型，会尝试转换为布尔值
     * @throws ExpressException 当表达式语法错误或执行出错时抛出
     */
    boolean is(String express) throws ExpressException;

    /**
     * 绑定对象
     *
     * 将对象绑定到表达式上下文中，使其成为表达式的根对象。
     * 绑定后，在表达式中可以直接访问该对象的属性和方法，无需指定对象名。
     *
     * <p>使用示例：</p>
     * <pre>
     * class Person {
     *     private String name = "John";
     *     private int age = 30;
     * }
     * Person p = new Person();
     * Express express = ...;
     * express.bind(p);
     * express.get("name");  // 直接访问name字段，返回"John"
     * express.get("age");   // 直接访问age字段，返回30
     * </pre>
     *
     * @param object 待绑定的对象
     *               该对象将成为表达式求值的上下文根对象
     * @return 当前Express对象，支持链式调用
     */
    Express bind(Object object);

    /**
     * 绑定变量
     *
     * 将命名变量绑定到表达式上下文中，使变量可以在表达式中使用。
     * 支持绑定任意类型的对象，包括基本类型、集合、自定义对象等。
     *
     * <p>使用场景：</p>
     * <ul>
     * <li>在条件表达式中使用外部变量</li>
     * <li>在复杂计算中传入参数</li>
     * <li>在表达式中引用常量或配置值</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>
     * Express express = ...;
     * express.bind("threshold", 100);
     * express.bind("name", "test");
     * express.is("value > threshold");  // 使用绑定的变量
     * express.get("name + ' suffix'");  // 在字符串中使用变量
     * </pre>
     *
     * @param name 变量名，用于在表达式中引用该变量
     *             应该遵循Java标识符命名规则
     * @param value 变量值，可以是任意类型的对象
     *              基本类型会自动装箱
     * @return 当前Express对象，支持链式调用
     */
    Express bind(String name, Object value);

    /**
     * 重置整个表达式上下文
     *
     * 清除所有已绑定的对象和变量，将表达式上下文恢复到初始状态。
     * 通常在需要重新设置绑定环境时调用。
     *
     * <p>重置操作包括：</p>
     * <ul>
     * <li>清除通过bind(Object)绑定的根对象</li>
     * <li>清除通过bind(String, Object)绑定的所有变量</li>
     * <li>清除表达式求值的缓存状态</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>
     * Express express = ...;
     * express.bind(obj);
     * express.bind("var", value);
     * express.get("someExpression");  // 使用绑定环境
     *
     * express.reset();  // 重置环境
     * // 现在可以重新绑定新的对象
     * express.bind(newObj);
     * </pre>
     *
     * @return 当前Express对象，支持链式调用
     */
    Express reset();


}
