import{_ as i,o as a,c as t,a as e,b as d,e as s,d as r,r as l}from"./app.1e03d722.js";const c={},v=e("h1",{id:"reset",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#reset","aria-hidden":"true"},"#"),s(" reset")],-1),o={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-reset",target:"_blank",rel:"noopener noreferrer"},m=e("code",null,"reset",-1),u=s("\u5728\u7EBF\u6559\u7A0B"),h=r(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u91CD\u7F6E\u589E\u5F3A\u7C7B\uFF0C\u5C06\u88AB Arthas \u589E\u5F3A\u8FC7\u7684\u7C7B\u5168\u90E8\u8FD8\u539F\uFF0CArthas \u670D\u52A1\u7AEF<code>stop</code>\u65F6\u4F1A\u91CD\u7F6E\u6240\u6709\u589E\u5F3A\u8FC7\u7684\u7C7B</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ reset -h
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="\u8FD8\u539F\u6307\u5B9A\u7C7B" tabindex="-1"><a class="header-anchor" href="#\u8FD8\u539F\u6307\u5B9A\u7C7B" aria-hidden="true">#</a> \u8FD8\u539F\u6307\u5B9A\u7C7B</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ trace Test test
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 57 ms.
\`---ts=2017-10-26 17:10:33;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    \`---[0.590102ms] Test:test()

\`---ts=2017-10-26 17:10:34;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    \`---[0.068692ms] Test:test()

$ reset Test
Affect(class-cnt:1 , method-cnt:0) cost in 11 ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="\u8FD8\u539F\u6240\u6709\u7C7B" tabindex="-1"><a class="header-anchor" href="#\u8FD8\u539F\u6240\u6709\u7C7B" aria-hidden="true">#</a> \u8FD8\u539F\u6240\u6709\u7C7B</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ trace Test test
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 15 ms.
\`---ts=2017-10-26 17:12:06;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    \`---[0.128518ms] Test:test()

$ reset
Affect(class-cnt:1 , method-cnt:0) cost in 9 ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,7);function b(p,_){const n=l("ExternalLinkIcon");return a(),t("div",null,[v,e("p",null,[e("a",o,[m,u,d(n)])]),h])}const x=i(c,[["render",b],["__file","reset.html.vue"]]);export{x as default};
