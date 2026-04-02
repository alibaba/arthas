package com.taobao.arthas.core.command.express;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import ognl.ClassResolver;
import ognl.MemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlRuntime;

/**
 * OGNL表达式执行器实现类
 *
 * 此类实现了Express接口，提供基于OGNL（Object-Graph Navigation Language）的表达式执行功能。
 * OGNL是一种强大的表达式语言，用于获取和设置Java对象的属性。
 *
 * 主要功能：
 * 1. 执行OGNL表达式并返回结果
 * 2. 支持绑定对象到表达式上下文
 * 3. 支持在上下文中添加变量
 * 4. 支持重置表达式上下文
 * 5. 提供安全的成员访问控制
 *
 * 使用场景：
 * - 在Arthas的ognl命令中执行表达式
 * - 动态访问和修改对象属性
 * - 调用对象方法
 * - 进行复杂的对象图导航
 *
 * 技术实现：
 * - 使用OGNL库作为表达式引擎
 * - 自定义MemberAccess实现访问控制
 * - 自定义ClassResolver实现类加载
 * - 自定义PropertyAccessor实现属性访问
 *
 * @author ralf0131 2017-01-04 14:41.
 * @author hengyunabc 2018-10-18
 */
public class OgnlExpress implements Express {
    /**
     * OGNL成员访问处理器
     * 用于控制对类成员（字段、方法、构造器）的访问权限
     * DefaultMemberAccess允许访问私有成员，这是Arthas调试功能的基础
     */
    private static final MemberAccess MEMBER_ACCESS = new DefaultMemberAccess(true);

    /**
     * 日志记录器，用于记录表达式执行过程中的错误
     */
    private static final Logger logger = LoggerFactory.getLogger(OgnlExpress.class);

    /**
     * 自定义对象属性访问器
     * 用于处理Object类型属性的访问，提供更灵活的属性访问能力
     */
    private static final ArthasObjectPropertyAccessor OBJECT_PROPERTY_ACCESSOR = new ArthasObjectPropertyAccessor();

    /**
     * 绑定的对象，作为表达式执行时的根对象（root object）
     * 表达式可以直接访问此对象的属性和方法，无需使用#前缀
     */
    private Object bindObject;

    /**
     * OGNL表达式上下文
     * 存储表达式执行所需的所有信息，包括变量、类型转换器、成员访问控制器等
     */
    private final OgnlContext context;

    /**
     * 默认构造函数
     * 使用自定义的类解析器（CustomClassResolver）创建OGNL表达式执行器
     * 自定义类解析器可以更好地控制类的加载和解析
     */
    public OgnlExpress() {
        this(CustomClassResolver.customClassResolver);
    }

    /**
     * 带类解析器的构造函数
     * 允许自定义类解析器，用于控制OGNL如何查找和加载类
     *
     * @param classResolver 自定义的类解析器，用于解析表达式中的类引用
     */
    public OgnlExpress(ClassResolver classResolver) {
        // 设置Object类的属性访问器，使用自定义的ArthasObjectPropertyAccessor
        OgnlRuntime.setPropertyAccessor(Object.class, OBJECT_PROPERTY_ACCESSOR);
        // 创建OGNL上下文，配置成员访问控制器和类解析器
        context = new OgnlContext(MEMBER_ACCESS, classResolver, null, null);
    }

    /**
     * 执行OGNL表达式并返回结果
     *
     * 这是核心方法，负责解析和执行OGNL表达式。
     * 表达式可以访问绑定的对象和上下文中的变量。
     *
     * @param express 要执行的OGNL表达式字符串
     * @return 表达式执行的结果对象
     * @throws ExpressException 如果表达式执行过程中发生错误
     */
    @Override
    public Object get(String express) throws ExpressException {
        try {
            // 使用OGNL引擎执行表达式
            // 参数说明：
            // - express: 表达式字符串
            // - context: OGNL上下文，包含变量和配置信息
            // - bindObject: 根对象，表达式的默认作用对象
            return Ognl.getValue(express, context, bindObject);
        } catch (Exception e) {
            // 记录表达式执行的错误信息
            logger.error("Error during evaluating the expression:", e);
            // 将异常包装为ExpressException抛出
            throw new ExpressException(express, e);
        }
    }

    /**
     * 执行OGNL表达式并判断结果是否为true
     *
     * 这是一个便捷方法，用于执行返回布尔值的表达式。
     * 如果表达式结果不是布尔类型，返回false。
     *
     * @param express 要执行的OGNL表达式，期望返回布尔值
     * @return 表达式结果是否为true
     * @throws ExpressException 如果表达式执行过程中发生错误
     */
    @Override
    public boolean is(String express) throws ExpressException {
        // 执行表达式获取结果
        final Object ret = get(express);
        // 判断结果是否为Boolean类型且值为true
        return ret instanceof Boolean && (Boolean) ret;
    }

    /**
     * 绑定对象作为表达式的根对象
     *
     * 绑定后，表达式可以直接访问此对象的公共成员，
     * 无需使用对象引用。这是OGNL的便捷特性。
     *
     * @param object 要绑定的对象，将作为表达式的根对象
     * @return 返回当前对象，支持链式调用
     */
    @Override
    public Express bind(Object object) {
        this.bindObject = object;
        return this;
    }

    /**
     * 在表达式上下文中绑定变量
     *
     * 绑定的变量可以在表达式中通过#name的方式访问。
     * 这是一种向表达式传递参数的方式。
     *
     * @param name 变量名，在表达式中使用#name引用
     * @param value 变量值，可以是任何Java对象
     * @return 返回当前对象，支持链式调用
     */
    @Override
    public Express bind(String name, Object value) {
        // 将变量添加到OGNL上下文中
        context.put(name, value);
        return this;
    }

    /**
     * 重置表达式上下文
     *
     * 清除上下文中的所有变量和绑定，
     * 使表达式执行器回到初始状态。
     * 这在需要重新开始一批表达式执行时很有用。
     *
     * @return 返回当前对象，支持链式调用
     */
    @Override
    public Express reset() {
        // 清除OGNL上下文中的所有内容
        context.clear();
        return this;
    }
}
