stack
=====

Print out the full call stack trace from the starter **till** the current method.

Most of the time, we know what method being invoked but not always we know **how being invoked**.

### Parameters

|Name|Specification|
|---:|:---|
|*class-pattern*|pattern for the class name|
|*method-pattern*|pattern for the method name|
|*condition-expression*|condition expression|
|[E]|turn on regex matching while the default is wildcard matching|
|[n:]|calling times|
|#cost|time cost|

**You should know:**
1. any valid [OGNL](https://en.wikipedia.org/wiki/OGNL) expression as `"{params,returnObj}"` supported;
2. filter by time cost as `trace *StringUtils isBlank '#cost>100'`; calling stack with only time cost higher than `100ms` will be printed.
3. `#cost` can be used in `watch/stack/trace`;
4. using `#cost` in Arthas 3.0 instead of `$cost`.
5. quoting rules: if there are quotes within the expression, use another type of quotes to quote the whole expression (single `''` or double `""` quotes). 


**Advanced:**
* [Critical fields in expression](advice-class.md)
* [Special usage](https://github.com/alibaba/arthas/issues/71)
* [OGNL official guide](https://commons.apache.org/proper/commons-ognl/language-guide.html)


### Usage

```
$ stack demo.Demo addTwoLists
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 99 ms.
ts=2018-09-30 01:17:41;thread_name=Thread-3;id=c;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@659e0bfd
    @demo.Demo.testListAdd()
        at demo.Demo.lambda$main$0(Demo.java:26)
        at java.lang.Thread.run(Thread.java:745)
```

