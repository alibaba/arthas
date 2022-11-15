import{_ as s,o as t,c as i,a as e,b as d,e as a,d as p,r as o}from"./app.f1782391.js";const c={},r=e("h1",{id:"heapdump",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#heapdump","aria-hidden":"true"},"#"),a(" heapdump")],-1),l={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-heapdump",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"heapdump",-1),h=a(" online tutorial"),m=p(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>dump java heap in hprof binary format, like <code>jmap</code>.</p></div><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><h3 id="dump-to-file" tabindex="-1"><a class="header-anchor" href="#dump-to-file" aria-hidden="true">#</a> Dump to file</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@58205<span class="token punctuation">]</span>$ heapdump /tmp/dump.hprof
Dumping heap to /tmp/dump.hprof<span class="token punctuation">..</span>.
Heap dump <span class="token function">file</span> created
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="dump-only-live-objects" tabindex="-1"><a class="header-anchor" href="#dump-only-live-objects" aria-hidden="true">#</a> Dump only live objects</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@58205<span class="token punctuation">]</span>$ heapdump <span class="token parameter variable">--live</span> /tmp/dump.hprof
Dumping heap to /tmp/dump.hprof<span class="token punctuation">..</span>.
Heap dump <span class="token function">file</span> created
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="dump-to-tmp-file" tabindex="-1"><a class="header-anchor" href="#dump-to-tmp-file" aria-hidden="true">#</a> Dump to tmp file</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@58205<span class="token punctuation">]</span>$ heapdump
Dumping heap to /var/folders/my/wy7c9w9j5732xbkcyt1mb4g40000gp/T/heapdump2019-09-03-16-385121018449645518991.hprof<span class="token punctuation">..</span>.
Heap dump <span class="token function">file</span> created
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,8);function v(b,f){const n=o("ExternalLinkIcon");return t(),i("div",null,[r,e("p",null,[e("a",l,[u,h,d(n)])]),m])}const g=s(c,[["render",v],["__file","heapdump.html.vue"]]);export{g as default};