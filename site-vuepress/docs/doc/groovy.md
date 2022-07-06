groovy
===

> Arthas 支持 groovy 脚本增强，允许像 BTrace 一样编写脚本来解决问题，可以在 groovy 脚本中进行if/for/switch/while 等控制语句，不受限制，但相比 BTrace 而言拥有更多的限制范围。

### 限制内容

1. 禁止改变原有逻辑，与 watch 等命令一样，重点保证的是监听和观察。
1. 只允许在方法的 before/success/exception/finish 四个环节进行监听。

### 参数说明

|参数名称|参数说明|
|---:|:---|
|*class-pattern*|类名表达式匹配|
|*method-pattern*|方法名表达式匹配|
|*script-filepath*|groovy 脚本的绝对路径|
|[S]|匹配所有的子类|
|[E]|开启正则表达式匹配，默认为通配符匹配|

需要说明的是，第三个输入参数是脚本的绝对路径，比如 `/tmp/test.groovy`，不建议输入相对路径，比如 `./test.groovy`
 
### 五个关键函数声明

```java
/**
 * 增强脚本监听器
 */
interface ScriptListener {

    /**
     * 脚本创建
     *
     * @param output 输出器
     */
    void create(Output output);

    /**
     * 脚本销毁
     *
     * @param output 输出器
     */
    void destroy(Output output);

    /**
     * 方法执行前
     *
     * @param output 输出器
     * @param advice 通知点
     */
    void before(Output output, Advice advice);

    /**
     * 方法正常返回
     *
     * @param output 输出器
     * @param advice 通知点
     */
    void afterReturning(Output output, Advice advice);

    /**
     * 方法异常返回
     *
     * @param output 输出器
     * @param advice 通知点
     */
    void afterThrowing(Output output, Advice advice);

}
```

### 参数 `Advice` 说明

`Advice` 参数最主要是封装了通知节点的所有信息。参考[表达式核心变量](advice-class.md)中关于该节点的描述。

### 参数 `Output` 说明

`Output` 参数只拥有三个方法，主要的工作还是输出对应的文本信息

```java
/**
 * 输出器
 */
interface Output {

    /**
     * 输出字符串(不换行)
     *
     * @param string 待输出字符串
     * @return this
     */
    Output print(String string);

    /**
     * 输出字符串(换行)
     *
     * @param string 待输出字符串
     * @return this
     */
    Output println(String string);

    /**
     * 结束当前脚本
     *
     * @return this
     */
    Output finish();

}
```

### 一个输出日志的 groovy 脚本示例

```groovy
import com.taobao.arthas.core.command.ScriptSupportCommand
import com.taobao.arthas.core.util.Advice

import static java.lang.String.format

/**
 * 输出方法日志
 */
public class Logger implements ScriptSupportCommand.ScriptListener {

    @Override
    void create(ScriptSupportCommand.Output output) {
        output.println("script create.");
    }

    @Override
    void destroy(ScriptSupportCommand.Output output) {
        output.println("script destroy.");
    }

    @Override
    void before(ScriptSupportCommand.Output output, Advice advice) {
        output.println(format("before:class=%s;method=%s;paramslen=%d;%s;",
                advice.getClazz().getSimpleName(),
                advice.getMethod().getName(),
                advice.getParams().length, advice.getParams()))
    }

    @Override
    void afterReturning(ScriptSupportCommand.Output output, Advice advice) {
        output.println(format("returning:class=%s;method=%s;",
                advice.getClazz().getSimpleName(),
                advice.getMethod().getName()))
    }

    @Override
    void afterThrowing(ScriptSupportCommand.Output output, Advice advice) {
        output.println(format("throwing:class=%s;method=%s;",
                advice.getClazz().getSimpleName(),
                advice.getMethod().getName()))
    }
}
```

使用示例：

```
$ groovy com.alibaba.sample.petstore.dal.dao.ProductDao getProductById /Users/zhuyong/middleware/arthas/scripts/Logger.groovy -S
script create.
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 102 ms.
before:class=IbatisProductDao;method=getProductById;paramslen=1;[Ljava.lang.Object;@45df64fc;
returning:class=IbatisProductDao;method=getProductById;
before:class=IbatisProductDao;method=getProductById;paramslen=1;[Ljava.lang.Object;@5b0e2d00;
returning:class=IbatisProductDao;method=getProductById;
```
