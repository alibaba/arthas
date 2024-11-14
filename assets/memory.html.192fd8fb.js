import{_ as o,o as r,c as i,a as e,b as s,e as a,d as t,r as d}from"./app.1e03d722.js";const l={},c=e("h1",{id:"memory",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#memory","aria-hidden":"true"},"#"),a(" memory")],-1),m=e("p",null,"\u67E5\u770B JVM \u5185\u5B58\u4FE1\u606F\u3002",-1),v=e("p",null,"\u5177\u4F53\u5B57\u6BB5\u4FE1\u606F\uFF0C\u53C2\u8003\uFF1A",-1),_={href:"https://docs.oracle.com/en/java/javase/11/docs/api/java.management/java/lang/management/MemoryMXBean.html#getHeapMemoryUsage()",target:"_blank",rel:"noopener noreferrer"},h=a("MemoryMXBean#getHeapMemoryUsage()"),M={href:"https://docs.oracle.com/en/java/javase/11/docs/api/java.management/java/lang/management/MemoryMXBean.html#getHeapMemoryUsage()",target:"_blank",rel:"noopener noreferrer"},u=a("MemoryMXBean#getNonHeapMemoryUsage()"),p=t(`<p>\u5177\u4F53\u4EE3\u7801\uFF1A</p><ul><li>https://github.com/alibaba/arthas/blob/master/core/src/main/java/com/taobao/arthas/core/command/monitor200/MemoryCommand.java</li></ul><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>$ memory
Memory                           used      total      max        usage
heap                             32M       256M       4096M      0.79%
g1_eden_space                    11M       68M        -1         16.18%
g1_old_gen                       17M       184M       4096M      0.43%
g1_survivor_space                4M        4M         -1         100.00%
nonheap                          35M       39M        -1         89.55%
codeheap_&#39;non-nmethods&#39;          1M        2M         5M         20.53%
metaspace                        26M       27M        -1         96.88%
codeheap_&#39;profiled_nmethods&#39;     4M        4M         117M       3.57%
compressed_class_space           2M        3M         1024M      0.29%
codeheap_&#39;non-profiled_nmethods&#39; 685K      2496K      120032K    0.57%
mapped                           0K        0K         -          0.00%
direct                           48M       48M        -          100.00%
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,4);function b(g,y){const n=d("ExternalLinkIcon");return r(),i("div",null,[c,m,v,e("ul",null,[e("li",null,[e("a",_,[h,s(n)])]),e("li",null,[e("a",M,[u,s(n)])])]),p])}const x=o(l,[["render",b],["__file","memory.html.vue"]]);export{x as default};
