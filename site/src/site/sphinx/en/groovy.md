groovy
===

> Arthas support groovy scripting to allow user to use script like BTrace. It is possible to use if/for/switch/while in groovy scripting, but has more limitations compared to BTrace.

### Limitations

1. Prohibit from alternating the original logic. Like `watch` command, The major purpose of scripting is monitoring and observing.
2. Only allow to monitor at the stages of before/success/exception/finish on one method.

### Parameters

|Parameter|Explanation|
|---:|:---|
|*class-pattern*|class name pattern|
|*method-pattern*|method name pattern|
|*script-filepath*|the absolute path of the groovy script|
|[S]|match all sub classes|
|[E]|enable regex match, the default is wildcard match|

Note: the third parameter `script-filepath` must be the absolute path of the groovy script, for example `/tmp/test.groovy`. It is not recommended to use relative path, e.g. `./test.groovy`.
 
### Explanation on the important callbacks

```java
/**
 * Listeners for script to enhance the class
 */
interface ScriptListener {

    /**
     * When the script is created
     *
     * @param output Output
     */
    void create(Output output);

    /**
     * When the script is destroyed
     *
     * @param output Output
     */
    void destroy(Output output);

    /**
     * Before the method executes
     *
     * @param output Output
     * @param advice Advice
     */
    void before(Output output, Advice advice);

    /**
     * After the method returns
     *
     * @param output Output
     * @param advice Advice
     */
    void afterReturning(Output output, Advice advice);

    /**
     * After the method throws exceptions
     *
     * @param output Output
     * @param advice Advice
     */
    void afterThrowing(Output output, Advice advice);

}
```

### `Advice` parameter

`Advice` contains all information necessary for notification. Refer to [expression core parameters](advice-class.md) for more details.

### `Output` parameter

There are three methods in `Output`, used for outputting the corresponding text.

```java
/**
 * Output
 */
interface Output {

    /**
     * Output text without line break
     *
     * @param string Text to output
     * @return this
     */
    Output print(String string);

    /**
     * Output text with line break
     *
     * @param string Text to output
     * @return this
     */
    Output println(String string);

    /**
     * Finish outputting from the script
     *
     * @return this
     */
    Output finish();

}
```

### A groovy sample script to output logs

```groovy
import com.taobao.arthas.core.command.ScriptSupportCommand
import com.taobao.arthas.core.util.Advice

import static java.lang.String.format

/**
 * Output method logs
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

Run the script like this:

```bash
$ groovy com.alibaba.sample.petstore.dal.dao.ProductDao getProductById /Users/zhuyong/middleware/arthas/scripts/Logger.groovy -S
script create.
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 102 ms.
before:class=IbatisProductDao;method=getProductById;paramslen=1;[Ljava.lang.Object;@45df64fc;
returning:class=IbatisProductDao;method=getProductById;
before:class=IbatisProductDao;method=getProductById;paramslen=1;[Ljava.lang.Object;@5b0e2d00;
returning:class=IbatisProductDao;method=getProductById;
```
