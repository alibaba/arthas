import{_ as i,o as a,c as s,a as e,b as d,e as n,d as l,r as o}from"./app.8a3762b8.js";const r={},c=e("h1",{id:"tee",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#tee","aria-hidden":"true"},"#"),n(" tee")],-1),v={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-tee",target:"_blank",rel:"noopener noreferrer"},u=e("code",null,"tee",-1),m=n(" online tutorial"),p=l(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Similar to the traditional <code>tee</code> command, it is used to read standard input data and output its contents into a file.</p><p><code>tee</code> will read data from standard input device, output its content to standard output device, and save it as a file.</p></div><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> USAGE:
   tee [-a] [-h] [file]

 SUMMARY:
   tee command for pipes.

 EXAMPLES:
  sysprop | tee /path/to/logfile | grep java
  sysprop | tee -a /path/to/logfile | grep java

 WIKI:
   https://arthas.aliyun.com/doc/tee

 OPTIONS:
 -a, --append                              Append to file
 -h, --help                                this help
 &lt;file&gt;                                    File path
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,2);function h(_,b){const t=o("ExternalLinkIcon");return a(),s("div",null,[c,e("p",null,[e("a",v,[u,m,d(t)])]),p])}var g=i(r,[["render",h],["__file","tee.html.vue"]]);export{g as default};
