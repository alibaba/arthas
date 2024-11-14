import{_ as t,o as i,c as s,a as e,b as d,e as n,d as l,r}from"./app.1e03d722.js";const o={},c=e("h1",{id:"tee",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#tee","aria-hidden":"true"},"#"),n(" tee")],-1),u={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-tee",target:"_blank",rel:"noopener noreferrer"},v=e("code",null,"tee",-1),m=n(" online tutorial"),p=l(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Similar to the traditional <code>tee</code> command, it is used to read standard input data and output its contents into a file.</p><p><code>tee</code> will read data from standard input device, output its content to standard output device, and save it as a file.</p></div><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> USAGE:
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,3);function h(_,b){const a=r("ExternalLinkIcon");return i(),s("div",null,[c,e("p",null,[e("a",u,[v,m,d(a)])]),p])}const g=t(o,[["render",h],["__file","tee.html.vue"]]);export{g as default};
