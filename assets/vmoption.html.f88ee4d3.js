import{_ as e,o as l,c as i,a as n,b as o,e as a,d as t,r}from"./app.1e03d722.js";const d={},c=n("h1",{id:"vmoption",tabindex:"-1"},[n("a",{class:"header-anchor",href:"#vmoption","aria-hidden":"true"},"#"),a(" vmoption")],-1),p={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=cn&id=command-vmoption",target:"_blank",rel:"noopener noreferrer"},u=n("code",null,"vmoption",-1),v=a("\u5728\u7EBF\u6559\u7A0B"),b=t(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B\uFF0C\u66F4\u65B0 VM \u8BCA\u65AD\u76F8\u5173\u7684\u53C2\u6570</p></div><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><h3 id="\u67E5\u770B\u6240\u6709\u7684-option" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B\u6240\u6709\u7684-option" aria-hidden="true">#</a> \u67E5\u770B\u6240\u6709\u7684 option</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@56963<span class="token punctuation">]</span>$ vmoption
 KEY                    VALUE                   ORIGIN                 WRITEABLE
---------------------------------------------------------------------------------------------
 HeapDumpBeforeFullGC   <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 HeapDumpAfterFullGC    <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 HeapDumpOnOutOfMemory  <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 Error
 HeapDumpPath                                   DEFAULT                <span class="token boolean">true</span>
 CMSAbortablePrecleanW  <span class="token number">100</span>                     DEFAULT                <span class="token boolean">true</span>
 aitMillis
 CMSWaitDuration        <span class="token number">2000</span>                    DEFAULT                <span class="token boolean">true</span>
 CMSTriggerInterval     <span class="token parameter variable">-1</span>                      DEFAULT                <span class="token boolean">true</span>
 PrintGC                <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 PrintGCDetails         <span class="token boolean">true</span>                    MANAGEMENT             <span class="token boolean">true</span>
 PrintGCDateStamps      <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 PrintGCTimeStamps      <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 PrintGCID              <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 PrintClassHistogramBe  <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 foreFullGC
 PrintClassHistogramAf  <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 terFullGC
 PrintClassHistogram    <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
 MinHeapFreeRatio       <span class="token number">0</span>                       DEFAULT                <span class="token boolean">true</span>
 MaxHeapFreeRatio       <span class="token number">100</span>                     DEFAULT                <span class="token boolean">true</span>
 PrintConcurrentLocks   <span class="token boolean">false</span>                   DEFAULT                <span class="token boolean">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u67E5\u770B\u6307\u5B9A\u7684-option" tabindex="-1"><a class="header-anchor" href="#\u67E5\u770B\u6307\u5B9A\u7684-option" aria-hidden="true">#</a> \u67E5\u770B\u6307\u5B9A\u7684 option</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ vmoption PrintGC
 KEY                 VALUE                ORIGIN              WRITEABLE
---------------------------------------------------------------------------------
 PrintGC             <span class="token boolean">false</span>                MANAGEMENT          <span class="token boolean">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="\u66F4\u65B0\u6307\u5B9A\u7684-option" tabindex="-1"><a class="header-anchor" href="#\u66F4\u65B0\u6307\u5B9A\u7684-option" aria-hidden="true">#</a> \u66F4\u65B0\u6307\u5B9A\u7684 option</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ vmoption PrintGC <span class="token boolean">true</span>
Successfully updated the vm option.
 NAME     BEFORE-VALUE  AFTER-VALUE
------------------------------------
 PrintGC  <span class="token boolean">false</span>         <span class="token boolean">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ vmoption PrintGCDetails <span class="token boolean">true</span>
Successfully updated the vm option.
 NAME            BEFORE-VALUE  AFTER-VALUE
-------------------------------------------
 PrintGCDetails  <span class="token boolean">false</span>         <span class="token boolean">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,9);function m(h,k){const s=r("ExternalLinkIcon");return l(),i("div",null,[c,n("p",null,[n("a",p,[u,v,o(s)])]),b])}const A=e(d,[["render",m],["__file","vmoption.html.vue"]]);export{A as default};
