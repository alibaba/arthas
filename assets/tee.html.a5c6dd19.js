import{_ as a,o as s,c as t,a as e,b as l,e as n,d,r}from"./app.1e03d722.js";const c={},o=e("h1",{id:"tee",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#tee","aria-hidden":"true"},"#"),n(" tee")],-1),v={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-tee",target:"_blank",rel:"noopener noreferrer"},m=e("code",null,"tee",-1),u=n("\u5728\u7EBF\u6559\u7A0B"),p=d(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u7C7B\u4F3C\u4F20\u7EDF\u7684<code>tee</code>\u547D\u4EE4, \u7528\u4E8E\u8BFB\u53D6\u6807\u51C6\u8F93\u5165\u7684\u6570\u636E\uFF0C\u5E76\u5C06\u5176\u5185\u5BB9\u8F93\u51FA\u6210\u6587\u4EF6\u3002</p><p>tee \u6307\u4EE4\u4F1A\u4ECE\u6807\u51C6\u8F93\u5165\u8BBE\u5907\u8BFB\u53D6\u6570\u636E\uFF0C\u5C06\u5176\u5185\u5BB9\u8F93\u51FA\u5230\u6807\u51C6\u8F93\u51FA\u8BBE\u5907\uFF0C\u540C\u65F6\u4FDD\u5B58\u6210\u6587\u4EF6\u3002</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code> USAGE:
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,3);function h(_,b){const i=r("ExternalLinkIcon");return s(),t("div",null,[o,e("p",null,[e("a",v,[m,u,l(i)])]),p])}const g=a(c,[["render",h],["__file","tee.html.vue"]]);export{g as default};
