import{_ as s,o as i,c as d,a as e,b as t,e as a,d as r,r as o}from"./app.1e03d722.js";const l={},c=e("h1",{id:"perfcounter",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#perfcounter","aria-hidden":"true"},"#"),a(" perfcounter")],-1),v={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-perfcounter",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"perfcounter",-1),m=a(" online tutorial"),p=r(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Check the current JVM Perf Counter information.</p></div><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ perfcounter
 java.ci.totalTime                            2325637411
 java.cls.loadedClasses                       3403
 java.cls.sharedLoadedClasses                 0
 java.cls.sharedUnloadedClasses               0
 java.cls.unloadedClasses                     0
 java.property.java.version                   11.0.4
 java.property.java.vm.info                   mixed mode
 java.property.java.vm.name                   OpenJDK 64-Bit Server VM
...
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>Print more information with the <code>-d</code> option:</p><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ perfcounter -d
 Name                                   Variability   Units        Value
---------------------------------------------------------------------------------
 java.ci.totalTime                      Monotonic     Ticks        3242526906
 java.cls.loadedClasses                 Monotonic     Events       3404
 java.cls.sharedLoadedClasses           Monotonic     Events       0
 java.cls.sharedUnloadedClasses         Monotonic     Events       0
 java.cls.unloadedClasses               Monotonic     Events       0
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="jvm-above-jdk9" tabindex="-1"><a class="header-anchor" href="#jvm-above-jdk9" aria-hidden="true">#</a> JVM above JDK9</h2><p>If the information is not printed, when the application starts, add the following parameters:</p><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>--add-opens java.base/jdk.internal.perf=ALL-UNNAMED --add-exports java.base/jdk.internal.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter.perf=ALL-UNNAMED --add-opens java.management/sun.management.counter=ALL-UNNAMED
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div></div></div>`,8);function h(b,f){const n=o("ExternalLinkIcon");return i(),d("div",null,[c,e("p",null,[e("a",v,[u,m,t(n)])]),p])}const _=s(l,[["render",h],["__file","perfcounter.html.vue"]]);export{_ as default};
