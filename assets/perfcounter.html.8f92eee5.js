import{_ as s,o as d,c as i,a as e,b as r,e as a,d as t,r as l}from"./app.1e03d722.js";const c={},o=e("h1",{id:"perfcounter",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#perfcounter","aria-hidden":"true"},"#"),a(" perfcounter")],-1),v={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-perfcounter",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"perfcounter",-1),m=a("\u5728\u7EBF\u6559\u7A0B"),p=t(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B\u5F53\u524D JVM \u7684 Perf Counter \u4FE1\u606F</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ perfcounter
 java.ci.totalTime                            2325637411
 java.cls.loadedClasses                       3403
 java.cls.sharedLoadedClasses                 0
 java.cls.sharedUnloadedClasses               0
 java.cls.unloadedClasses                     0
 java.property.java.version                   11.0.4
 java.property.java.vm.info                   mixed mode
 java.property.java.vm.name                   OpenJDK 64-Bit Server VM
...
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>\u53EF\u4EE5\u7528<code>-d</code>\u53C2\u6570\u6253\u5370\u66F4\u591A\u4FE1\u606F\uFF1A</p><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ perfcounter -d
 Name                                   Variability   Units        Value
---------------------------------------------------------------------------------
 java.ci.totalTime                      Monotonic     Ticks        3242526906
 java.cls.loadedClasses                 Monotonic     Events       3404
 java.cls.sharedLoadedClasses           Monotonic     Events       0
 java.cls.sharedUnloadedClasses         Monotonic     Events       0
 java.cls.unloadedClasses               Monotonic     Events       0
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="jdk9-\u4EE5\u4E0A\u7684\u5E94\u7528" tabindex="-1"><a class="header-anchor" href="#jdk9-\u4EE5\u4E0A\u7684\u5E94\u7528" aria-hidden="true">#</a> jdk9 \u4EE5\u4E0A\u7684\u5E94\u7528</h2><p>\u5982\u679C\u6CA1\u6709\u6253\u5370\u51FA\u4FE1\u606F\uFF0C\u5E94\u7528\u5728\u542F\u52A8\u65F6\uFF0C\u52A0\u4E0B\u9762\u7684\u53C2\u6570\uFF1A</p><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>--add-opens java.base/jdk.internal.perf=ALL-UNNAMED --add-exports java.base/jdk.internal.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter=ALL-UNNAMED
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div></div></div>`,8);function h(b,f){const n=l("ExternalLinkIcon");return d(),i("div",null,[o,e("p",null,[e("a",v,[u,m,r(n)])]),p])}const _=s(c,[["render",h],["__file","perfcounter.html.vue"]]);export{_ as default};
