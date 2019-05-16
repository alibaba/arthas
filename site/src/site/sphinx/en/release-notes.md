Release Notes
=============


v3.1.1
---

* [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.1.1](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.1.1)

v3.1.0
---

* [https://github.com/alibaba/arthas/releases/tag/3.1.0](https://github.com/alibaba/arthas/releases/tag/3.1.0)


v3.0.5
---

* [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.5](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.5)

v3.0.4
---

* [https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.4](https://github.com/alibaba/arthas/releases/tag/arthas-all-3.0.4)


v2017-11-03
----

* [improvement] add [`getstatic`](getstatic.md)
* [bug] fix Arthas class loader logs loading issues
* [improvement] introduce [OGNL](https://en.wikipedia.org/wiki/OGNL) to customize `classloader` to invoke static methods
* [improvement] optimise `termd` uppercase output performance 
* [improvement] `classloader` compile in class loader category by default
* [bug] fix `wc` counting issue
* [improvement] disable certain JDK classes e.g. `Classloader`, `Method`, `Integer` and the lik
* [improvement] quit directly when encountering incorrect [OGNL](https://en.wikipedia.org/wiki/OGNL) expression
* [bug] fix `pipe` issues
* [improvement] optimize command re-direct features using asynchronous log
* [improvement] [`trace`](trace.md) can filter JDK method calls

v2017-09-22
----

* [improvement] improve the error message when starting agent and server fails
* [bug] fix some asynchronous issues

v2017-09-11
----

* [improvement] [`async`](async.md) supported
* [improvement] optimize [`jad`](jad.md) support JDK 8 and inner class
* [bug] fix Chinese encoding issues

v2017-05-11
----

* [improvement] [`tt`](tt.md) investigating/recording level one to avoid too much performance overhead
* [bug] fix Chinese characters can not be presented issue

v2017-05-12
----

* Arthas 3.0 release :confetti_ball:

v2016-12-09
----

* [feature] [`as.sh`](https://github.com/alibaba/arthas/blob/master/bin/as.sh) support `-h` to print help info
* [bug] [#121] fix leftover temp files causing Arthas cannot start issue
* [bug] [#123] fix `attach/shutdown` repeatedly causing Arthas classloader leakage issue
* [improvement] make the help info more readable
* [bug] [#126] fix the documents links issues 
* [bug] [#122] fix the [`classloader`](classloader.md) filtering out `sun.reflect.DelegatingClassLoader` issue
* [bug] [#129] fix [`classloader`](classloader.md) presenting structure issues
* [improvement]  [#125] make the Arthas log output more readable
* [improvement]  [#96] [`sc`](sc.md) and more commands are supporting format as `com/taobao/xxx/TestClass`
* [bug] [#124] fix the negative values of [`trace`](trace.md)
* [improvement]  [#128] the output of [`tt`](tt.md) will auto-expand now
* [bug] [#130] providing more meaningful error messages when port conflicts
* [bug] [#98] fix Arthas starting issue: when updating/downloading failed, Arthas will fail to start
* [bug] [#139] fix agent attaching fails under some scenarios issues
* [improvement]  [#156] delay `jd-core-java` initialization to avoid Arthas starting failure
* [bug] avoid thread names duplicate issue
* [improvement]  [#150] filtering by total time cost in [`trace`](trace.md) 
* [bug] fix [`sc`](sc.md) `NPE` issue when searching `SystemClassloader` 
* [bug] [#180] fix attach fails issues: attaching succeed at the first time, delete the Arthas installer, re-compile and package => attaching fails


v2016-06-07
----

* [bug] fix NPE when loading `spy` as resource
* [improvement] locating the blocking thread 
* [improvement] print out thread in name order
* [improvement] specify the refreshing interval when checking topN busiest threads

v2016-04-08
----

* [feature]  specify refreshing interval and execution times in [`dashboard`](dashboard.md)
* [feature]  log the command execution result
* [feature]  speed up the booting and attaching while the first attaching is even quicker by 100% than before
* [feature]  batch supported; script supported
* [feature]  interactive mode used in Arthas 
* [feature]  inheritance relation included in class searching; global option `disable-sub-class` can be used to turn it off
* [feature]  colorful and plain text modes both supported
* [improvement]  merge `exit` and `quit` commands
* [improvement]  help info enclosed with wiki links 
* [improvement]  optimize [`watch`](watch.md) using flow for better UX
* [improvement]  add examples to [`thread`](thread.md)
* [improvement]  auto-completion ignores character case
* [improvement]  make the UI more beautiful/friendly
* [bug] fix [`trace`](trace.md) printing too much encountering loop issues
* [bug] fix [`trace`](trace.md) node twisting issues when method throwing exceptions
* [bug] fix injected/enhanced `BootstrapClassLoader` cannot locate `spy` issues

v2016-03-07
----

* [feature] checking the topN thread and related stack traces
* [bug] fix Arthas starting failure in OpenJdk issues (requiring to reinstall [as.sh](https://github.com/alibaba/arthas/blob/master/bin/as.sh))
* [improvement] optimize UX


v2016-01-18
----

* [improvement]  optimise [`jad`](jad.md); dump memory byte array in real time; using `jd-core-java` to decompile; line number presented;
* [bug] fix checking/re-producing issues when [`tt`](tt.md) is watching thread-context related methods invoking

v2016-01-08
----

* [bug] jad NPE
* [bug] watch/monitor NPE
* [bug] wrong escaping issues
* [bug] wrong statistics
* [bug] [`sc`](sc.md) checking internal structure issues

v2015-12-29
---

* Arthas 2.0 Beta :boom:！
