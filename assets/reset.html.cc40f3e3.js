import{_ as a,o as i,c as t,a as e,b as d,e as s,d as l,r}from"./app.1e03d722.js";const c={},v=e("h1",{id:"reset",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#reset","aria-hidden":"true"},"#"),s(" reset")],-1),o={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-reset",target:"_blank",rel:"noopener noreferrer"},m=e("code",null,"reset",-1),u=s(" online tutorial"),h=l(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Reset all classes that have been enhanced by Arthas. These enhanced classes will also be reset when Arthas server is <code>stop</code>.</p></div><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ reset -h
 USAGE:
   reset [-h] [-E] [class-pattern]

 SUMMARY:
   Reset all the enhanced classes

 EXAMPLES:
   reset
   reset *List
   reset -E .*List

 OPTIONS:
 -h, --help                                                         this help
 -E, --regex                                                        Enable regular expression to match (wildcard matching by default)
 &lt;class-pattern&gt;                                                    Path and classname of Pattern Matching
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="reset-specified-class" tabindex="-1"><a class="header-anchor" href="#reset-specified-class" aria-hidden="true">#</a> Reset specified class</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ trace Test test
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 57 ms.
\`---ts=2017-10-26 17:10:33;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    \`---[0.590102ms] Test:test()

\`---ts=2017-10-26 17:10:34;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    \`---[0.068692ms] Test:test()

$ reset Test
Affect(class-cnt:1 , method-cnt:0) cost in 11 ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="reset-all-classes" tabindex="-1"><a class="header-anchor" href="#reset-all-classes" aria-hidden="true">#</a> Reset all classes</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ trace Test test
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 15 ms.
\`---ts=2017-10-26 17:12:06;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    \`---[0.128518ms] Test:test()

$ reset
Affect(class-cnt:1 , method-cnt:0) cost in 9 ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,7);function b(p,_){const n=r("ExternalLinkIcon");return i(),t("div",null,[v,e("p",null,[e("a",o,[m,u,d(n)])]),h])}const x=a(c,[["render",b],["__file","reset.html.vue"]]);export{x as default};
