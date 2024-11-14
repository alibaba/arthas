import{_ as s,o as i,c as l,a as n,b as o,e as a,d as t,r}from"./app.1e03d722.js";const d={},c=n("h1",{id:"vmoption",tabindex:"-1"},[n("a",{class:"header-anchor",href:"#vmoption","aria-hidden":"true"},"#"),a(" vmoption")],-1),p={href:"https://arthas.aliyun.com/doc/arthas-tutorials.html?language=en&id=command-vmoption",target:"_blank",rel:"noopener noreferrer"},u=n("code",null,"vmoption",-1),v=a(" online tutorial"),b=t(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Display, and update the vm diagnostic options.</p></div><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><h3 id="view-all-options" tabindex="-1"><a class="header-anchor" href="#view-all-options" aria-hidden="true">#</a> View all options</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code><span class="token punctuation">[</span>arthas@56963<span class="token punctuation">]</span>$ vmoption
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="view-individual-option" tabindex="-1"><a class="header-anchor" href="#view-individual-option" aria-hidden="true">#</a> View individual option</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ vmoption PrintGC
 KEY                 VALUE                ORIGIN              WRITEABLE
---------------------------------------------------------------------------------
 PrintGC             <span class="token boolean">false</span>                MANAGEMENT          <span class="token boolean">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h3 id="update-individual-option" tabindex="-1"><a class="header-anchor" href="#update-individual-option" aria-hidden="true">#</a> Update individual option</h3><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ vmoption PrintGC <span class="token boolean">true</span>
Successfully updated the vm option.
 NAME     BEFORE-VALUE  AFTER-VALUE
------------------------------------
 PrintGC  <span class="token boolean">false</span>         <span class="token boolean">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ vmoption PrintGCDetails <span class="token boolean">true</span>
Successfully updated the vm option.
 NAME            BEFORE-VALUE  AFTER-VALUE
-------------------------------------------
 PrintGCDetails  <span class="token boolean">false</span>         <span class="token boolean">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,9);function m(h,k){const e=r("ExternalLinkIcon");return i(),l("div",null,[c,n("p",null,[n("a",p,[u,v,o(e)])]),b])}const A=s(d,[["render",m],["__file","vmoption.html.vue"]]);export{A as default};
