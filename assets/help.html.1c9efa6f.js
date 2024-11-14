import{_ as n,o as s,c as e,d as a}from"./app.1e03d722.js";const i={},t=a(`<h1 id="help" tabindex="-1"><a class="header-anchor" href="#help" aria-hidden="true">#</a> help</h1><p>show help message, the command can show all the commands that current Arthas server supports,or you can use the command to show the detail usage of another command.</p><div class="custom-container tip"><p class="custom-container-title">TIP</p><p>[help command] equals [command -help],both is to show the detail usage of one command.</p></div><h2 id="options" tabindex="-1"><a class="header-anchor" href="#options" aria-hidden="true">#</a> Options</h2><table><thead><tr><th style="text-align:right;">Name</th><th style="text-align:left;">Specification</th></tr></thead><tbody><tr><td style="text-align:right;"></td><td style="text-align:left;">show all the commands that current Arthas server supports</td></tr><tr><td style="text-align:right;">[name:]</td><td style="text-align:left;">show the detail usage of one command</td></tr></tbody></table><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ <span class="token builtin class-name">help</span>
 NAME         DESCRIPTION
 <span class="token builtin class-name">help</span>         Display Arthas Help
 auth         Authenticates the current session
 keymap       Display all the available keymap <span class="token keyword">for</span> the specified connection.
 sc           Search all the classes loaded by JVM
 sm           Search the method of classes loaded by JVM
 classloader  Show classloader info
 jad          Decompile class
 getstatic    Show the static field of a class
 monitor      Monitor method execution statistics, e.g. total/success/failure count, average rt, fail rate, etc.
 stack        Display the stack trace <span class="token keyword">for</span> the specified class and method
 thread       Display thread info, thread stack
 trace        Trace the execution <span class="token function">time</span> of specified method invocation.
 <span class="token function">watch</span>        Display the input/output parameter, <span class="token builtin class-name">return</span> object, and thrown exception of specified method invocation
 tt           Time Tunnel
 jvm          Display the target JVM information
 perfcounter  Display the perf counter information.
 ognl         Execute ognl expression.
 <span class="token function">mc</span>           Memory compiler, compiles <span class="token function">java</span> files into bytecode and class files <span class="token keyword">in</span> memory.
 redefine     Redefine classes. @see Instrumentation<span class="token comment">#redefineClasses(ClassDefinition...)</span>
 retransform  Retransform classes. @see Instrumentation<span class="token comment">#retransformClasses(Class...)</span>
 dashboard    Overview of target jvm&#39;s thread, memory, gc, vm, tomcat info.
 dump         Dump class byte array from JVM
 heapdump     Heap dump
 options      View and change various Arthas options
 cls          Clear the <span class="token function">screen</span>
 reset        Reset all the enhanced classes
 version      Display Arthas version
 session      Display current session information
 sysprop      Display, and change the system properties.
 sysenv       Display the system env.
 vmoption     Display, and update the vm diagnostic options.
 logger       Print logger info, and update the logger level
 <span class="token function">history</span>      Display <span class="token builtin class-name">command</span> <span class="token function">history</span>
 <span class="token function">cat</span>          Concatenate and print files
 base64       Encode and decode using Base64 representation
 <span class="token builtin class-name">echo</span>         <span class="token function">write</span> arguments to the standard output
 <span class="token builtin class-name">pwd</span>          Return working directory name
 mbean        Display the mbean information
 <span class="token function">grep</span>         <span class="token function">grep</span> <span class="token builtin class-name">command</span> <span class="token keyword">for</span> pipes.
 <span class="token function">tee</span>          <span class="token function">tee</span> <span class="token builtin class-name">command</span> <span class="token keyword">for</span> pipes.
 profiler     Async Profiler. https://github.com/jvm-profiling-tools/async-profiler
 stop         Stop/Shutdown Arthas server and <span class="token builtin class-name">exit</span> the console.


</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code> $ <span class="token builtin class-name">help</span> dashboard
  USAGE:
   dashboard <span class="token punctuation">[</span>-h<span class="token punctuation">]</span> <span class="token punctuation">[</span>-i <span class="token operator">&lt;</span>value<span class="token operator">&gt;</span><span class="token punctuation">]</span> <span class="token punctuation">[</span>-n <span class="token operator">&lt;</span>value<span class="token operator">&gt;</span><span class="token punctuation">]</span>

 SUMMARY:
   Overview of target jvm&#39;s thread, memory, gc, vm, tomcat info.

 EXAMPLES:
   dashboard
   dashboard <span class="token parameter variable">-n</span> <span class="token number">10</span>
   dashboard <span class="token parameter variable">-i</span> <span class="token number">2000</span>

 WIKI:
   https://arthas.aliyun.com/doc/dashboard

 OPTIONS:
 -h, <span class="token parameter variable">--help</span>                              this <span class="token builtin class-name">help</span>
 -i, <span class="token parameter variable">--interval</span> <span class="token operator">&lt;</span>value<span class="token operator">&gt;</span>                  The interval <span class="token punctuation">(</span>in ms<span class="token punctuation">)</span> between two executions, default is <span class="token number">5000</span> ms.
 -n, --number-of-execution <span class="token operator">&lt;</span>value<span class="token operator">&gt;</span>       The number of <span class="token builtin class-name">times</span> this <span class="token builtin class-name">command</span> will be executed.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,8),l=[t];function o(c,r){return s(),e("div",null,l)}const p=n(i,[["render",o],["__file","help.html.vue"]]);export{p as default};
