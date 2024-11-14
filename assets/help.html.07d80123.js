import{_ as e,o as i,c as n,d as s}from"./app.1e03d722.js";const a={},l=s(`<h1 id="help" tabindex="-1"><a class="header-anchor" href="#help" aria-hidden="true">#</a> help</h1><p>\u67E5\u770B\u547D\u4EE4\u5E2E\u52A9\u4FE1\u606F\uFF0C\u53EF\u4EE5\u67E5\u770B\u5F53\u524D arthas \u7248\u672C\u652F\u6301\u7684\u6307\u4EE4\uFF0C\u6216\u8005\u67E5\u770B\u5177\u4F53\u6307\u4EE4\u7684\u4F7F\u7528\u8BF4\u660E\u3002</p><div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>[help \u6307\u4EE4]\u7684\u7B49\u540C\u4E8E[\u6307\u4EE4 -help]\uFF0C\u90FD\u662F\u67E5\u770B\u5177\u4F53\u6307\u4EE4\u7684\u4F7F\u7528\u8BF4\u660E\u3002</p></div><h2 id="\u53C2\u6570\u8BF4\u660E" tabindex="-1"><a class="header-anchor" href="#\u53C2\u6570\u8BF4\u660E" aria-hidden="true">#</a> \u53C2\u6570\u8BF4\u660E</h2><table><thead><tr><th style="text-align:right;">\u53C2\u6570\u540D\u79F0</th><th style="text-align:left;">\u53C2\u6570\u8BF4\u660E</th></tr></thead><tbody><tr><td style="text-align:right;">\u4E0D\u63A5\u53C2\u6570</td><td style="text-align:left;">\u67E5\u8BE2\u5F53\u524D arthas \u7248\u672C\u652F\u6301\u7684\u6307\u4EE4\u4EE5\u53CA\u6307\u4EE4\u63CF\u8FF0</td></tr><tr><td style="text-align:right;">[name:]</td><td style="text-align:left;">\u67E5\u8BE2\u5177\u4F53\u6307\u4EE4\u7684\u4F7F\u7528\u8BF4\u660E</td></tr></tbody></table><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ help
 NAME         DESCRIPTION
 help         Display Arthas Help
 auth         Authenticates the current session
 keymap       Display all the available keymap for the specified connection.
 sc           Search all the classes loaded by JVM
 sm           Search the method of classes loaded by JVM
 classloader  Show classloader info
 jad          Decompile class
 getstatic    Show the static field of a class
 monitor      Monitor method execution statistics, e.g. total/success/failure count, average rt, fail rate, etc.
 stack        Display the stack trace for the specified class and method
 thread       Display thread info, thread stack
 trace        Trace the execution time of specified method invocation.
 watch        Display the input/output parameter, return object, and thrown exception of specified method invocation
 tt           Time Tunnel
 jvm          Display the target JVM information
 perfcounter  Display the perf counter information.
 ognl         Execute ognl expression.
 mc           Memory compiler, compiles java files into bytecode and class files in memory.
 redefine     Redefine classes. @see Instrumentation#redefineClasses(ClassDefinition...)
 retransform  Retransform classes. @see Instrumentation#retransformClasses(Class...)
 dashboard    Overview of target jvm&#39;s thread, memory, gc, vm, tomcat info.
 dump         Dump class byte array from JVM
 heapdump     Heap dump
 options      View and change various Arthas options
 cls          Clear the screen
 reset        Reset all the enhanced classes
 version      Display Arthas version
 session      Display current session information
 sysprop      Display, and change the system properties.
 sysenv       Display the system env.
 vmoption     Display, and update the vm diagnostic options.
 logger       Print logger info, and update the logger level
 history      Display command history
 cat          Concatenate and print files
 base64       Encode and decode using Base64 representation
 echo         write arguments to the standard output
 pwd          Return working directory name
 mbean        Display the mbean information
 grep         grep command for pipes.
 tee          tee command for pipes.
 profiler     Async Profiler. https://github.com/jvm-profiling-tools/async-profiler
 stop         Stop/Shutdown Arthas server and exit the console.


</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> $ help dashboard
  USAGE:
   dashboard [-h] [-i &lt;value&gt;] [-n &lt;value&gt;]

 SUMMARY:
   Overview of target jvm&#39;s thread, memory, gc, vm, tomcat info.

 EXAMPLES:
   dashboard
   dashboard -n 10
   dashboard -i 2000

 WIKI:
   https://arthas.aliyun.com/doc/dashboard

 OPTIONS:
 -h, --help                              this help
 -i, --interval &lt;value&gt;                  The interval (in ms) between two executions, default is 5000 ms.
 -n, --number-of-execution &lt;value&gt;       The number of times this command will be executed.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,8),t=[l];function d(r,c){return i(),n("div",null,t)}const o=e(a,[["render",d],["__file","help.html.vue"]]);export{o as default};
