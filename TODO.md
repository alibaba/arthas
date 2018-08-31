
* 代码还是很乱，需要继续重构
* 依赖需要清理，几个问题：
    * 所有 apache 的 common 库应当不需要
    * json 库有好几份
    * `jopt-simple` 看下能不能用 `cli` 取代
    * `cli`, `termd` 的 artifactId, version 需要想下。是不是应该直接拿进来。他们的依赖也需要仔细看一下
* termd 依赖 netty，感觉有点重，而且第一次 attach 比较慢，不确定是 netty 的问题还是 attach 的问题
* 目前 web console 依赖 termd 中自带的 term.js 和 css，需要美化，需要想下如何集成到研发门户上
* 因为现在没有 Java 客户端了，所以 batch mode 也就没有了
* `com.taobao.arthas.core.shell.session.Session` 的能力需要和以前的 session 的实现对标。其中：
    * 真的需要 textmode 吗？我觉得这个应该是 option 的事情
    * 真的需要 encoding 吗？我觉得仍然应该在 option 中定义，就算是真的需要，因为我觉得就应该是 UTF-8
    * duration 是应当展示的，session 的列表也许也应当展示
    * 需要仔细看下 session 过期是否符合预期
    * 多人协作的时候 session 原来是在多人之间共享的吗？
* 所有的命令现在实现的是 AnnotatedCommand，需要继续增强的是:
    * Help 中的格式化输出被删除。需要为 `@Description` 定义一套统一的格式
    * 命令的输入以及输出的日志 (record logger) 被删除，需要重新实现，因为现在是用 `CommandProcess` 来输出，所以，需要在 `CommandProcess` 的实现里打日志
* `com.taobao.arthas.core.GlobalOptions` 看上去好奇怪，感觉是 OptionCommand 应当做的事情
* `com.taobao.arthas.core.config.Configure` 需要清理，尤其是和 http 相关的
* 需要合并 develop 分支上后续的修复
* 代码中的 TODO/FIXME