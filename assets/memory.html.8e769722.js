import{_ as e,o as n,c as s,d as a}from"./app.391b0e4e.js";const i={},r=a(`<h1 id="memory" tabindex="-1"><a class="header-anchor" href="#memory" aria-hidden="true">#</a> memory</h1><p>View the JVM memory information.</p><h3 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ memory
Memory                           used      total      max        usage
heap                             32M       256M       4096M      <span class="token number">0.79</span>%
g1_eden_space                    11M       68M        -1         <span class="token number">16.18</span>%
g1_old_gen                       17M       184M       4096M      <span class="token number">0.43</span>%
g1_survivor_space                4M        4M         -1         <span class="token number">100.00</span>%
nonheap                          35M       39M        -1         <span class="token number">89.55</span>%
codeheap_<span class="token string">&#39;non-nmethods&#39;</span>          1M        2M         5M         <span class="token number">20.53</span>%
metaspace                        26M       27M        -1         <span class="token number">96.88</span>%
codeheap_<span class="token string">&#39;profiled_nmethods&#39;</span>     4M        4M         117M       <span class="token number">3.57</span>%
compressed_class_space           2M        3M         1024M      <span class="token number">0.29</span>%
codeheap_<span class="token string">&#39;non-profiled_nmethods&#39;</span> 685K      2496K      120032K    <span class="token number">0.57</span>%
mapped                           0K        0K         -          <span class="token number">0.00</span>%
direct                           48M       48M        -          <span class="token number">100.00</span>%
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,4),d=[r];function l(c,o){return n(),s("div",null,d)}var p=e(i,[["render",l],["__file","memory.html.vue"]]);export{p as default};
