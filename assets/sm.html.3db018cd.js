import{_ as e,o as t,c as l,a,b as i,e as n,d as r,r as o}from"./app.1e03d722.js";const d={},c=a("h1",{id:"sm",tabindex:"-1"},[a("a",{class:"header-anchor",href:"#sm","aria-hidden":"true"},"#"),n(" sm")],-1),p={href:"https://arthas.aliyun.com/doc/arthas-tutorials?language=cn&id=command-sm",target:"_blank",rel:"noopener noreferrer"},g=a("code",null,"sm",-1),v=n("\u5728\u7EBF\u6559\u7A0B"),m=r(`<div class="custom-container tip"><p class="custom-container-title">\u63D0\u793A</p><p>\u67E5\u770B\u5DF2\u52A0\u8F7D\u7C7B\u7684\u65B9\u6CD5\u4FE1\u606F</p></div><p>\u201CSearch-Method\u201D \u7684\u7B80\u5199\uFF0C\u8FD9\u4E2A\u547D\u4EE4\u80FD\u641C\u7D22\u51FA\u6240\u6709\u5DF2\u7ECF\u52A0\u8F7D\u4E86 Class \u4FE1\u606F\u7684\u65B9\u6CD5\u4FE1\u606F\u3002</p><p><code>sm</code> \u547D\u4EE4\u53EA\u80FD\u770B\u5230\u7531\u5F53\u524D\u7C7B\u6240\u58F0\u660E (declaring) \u7684\u65B9\u6CD5\uFF0C\u7236\u7C7B\u5219\u65E0\u6CD5\u770B\u5230\u3002</p><h2 id="\u53C2\u6570\u8BF4\u660E" tabindex="-1"><a class="header-anchor" href="#\u53C2\u6570\u8BF4\u660E" aria-hidden="true">#</a> \u53C2\u6570\u8BF4\u660E</h2><table><thead><tr><th style="text-align:right;">\u53C2\u6570\u540D\u79F0</th><th style="text-align:left;">\u53C2\u6570\u8BF4\u660E</th></tr></thead><tbody><tr><td style="text-align:right;"><em>class-pattern</em></td><td style="text-align:left;">\u7C7B\u540D\u8868\u8FBE\u5F0F\u5339\u914D</td></tr><tr><td style="text-align:right;"><em>method-pattern</em></td><td style="text-align:left;">\u65B9\u6CD5\u540D\u8868\u8FBE\u5F0F\u5339\u914D</td></tr><tr><td style="text-align:right;">[d]</td><td style="text-align:left;">\u5C55\u793A\u6BCF\u4E2A\u65B9\u6CD5\u7684\u8BE6\u7EC6\u4FE1\u606F</td></tr><tr><td style="text-align:right;">[E]</td><td style="text-align:left;">\u5F00\u542F\u6B63\u5219\u8868\u8FBE\u5F0F\u5339\u914D\uFF0C\u9ED8\u8BA4\u4E3A\u901A\u914D\u7B26\u5339\u914D</td></tr><tr><td style="text-align:right;"><code>[c:]</code></td><td style="text-align:left;">\u6307\u5B9A class \u7684 ClassLoader \u7684 hashcode</td></tr><tr><td style="text-align:right;"><code>[classLoaderClass:]</code></td><td style="text-align:left;">\u6307\u5B9A\u6267\u884C\u8868\u8FBE\u5F0F\u7684 ClassLoader \u7684 class name</td></tr><tr><td style="text-align:right;"><code>[n:]</code></td><td style="text-align:left;">\u5177\u6709\u8BE6\u7EC6\u4FE1\u606F\u7684\u5339\u914D\u7C7B\u7684\u6700\u5927\u6570\u91CF\uFF08\u9ED8\u8BA4\u4E3A 100\uFF09</td></tr></tbody></table><h2 id="\u4F7F\u7528\u53C2\u8003" tabindex="-1"><a class="header-anchor" href="#\u4F7F\u7528\u53C2\u8003" aria-hidden="true">#</a> \u4F7F\u7528\u53C2\u8003</h2><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sm java.lang.String
java.lang.String-<span class="token operator">&gt;</span><span class="token operator">&lt;</span>init<span class="token operator">&gt;</span>
java.lang.String-<span class="token operator">&gt;</span>equals
java.lang.String-<span class="token operator">&gt;</span>toString
java.lang.String-<span class="token operator">&gt;</span>hashCode
java.lang.String-<span class="token operator">&gt;</span>compareTo
java.lang.String-<span class="token operator">&gt;</span>indexOf
java.lang.String-<span class="token operator">&gt;</span>valueOf
java.lang.String-<span class="token operator">&gt;</span>checkBounds
java.lang.String-<span class="token operator">&gt;</span>length
java.lang.String-<span class="token operator">&gt;</span>isEmpty
java.lang.String-<span class="token operator">&gt;</span>charAt
java.lang.String-<span class="token operator">&gt;</span>codePointAt
java.lang.String-<span class="token operator">&gt;</span>codePointBefore
java.lang.String-<span class="token operator">&gt;</span>codePointCount
java.lang.String-<span class="token operator">&gt;</span>offsetByCodePoints
java.lang.String-<span class="token operator">&gt;</span>getChars
java.lang.String-<span class="token operator">&gt;</span>getBytes
java.lang.String-<span class="token operator">&gt;</span>contentEquals
java.lang.String-<span class="token operator">&gt;</span>nonSyncContentEquals
java.lang.String-<span class="token operator">&gt;</span>equalsIgnoreCase
java.lang.String-<span class="token operator">&gt;</span>compareToIgnoreCase
java.lang.String-<span class="token operator">&gt;</span>regionMatches
java.lang.String-<span class="token operator">&gt;</span>startsWith
java.lang.String-<span class="token operator">&gt;</span>endsWith
java.lang.String-<span class="token operator">&gt;</span>indexOfSupplementary
java.lang.String-<span class="token operator">&gt;</span>lastIndexOf
java.lang.String-<span class="token operator">&gt;</span>lastIndexOfSupplementary
java.lang.String-<span class="token operator">&gt;</span>substring
java.lang.String-<span class="token operator">&gt;</span>subSequence
java.lang.String-<span class="token operator">&gt;</span>concat
java.lang.String-<span class="token operator">&gt;</span>replace
java.lang.String-<span class="token operator">&gt;</span>matches
java.lang.String-<span class="token operator">&gt;</span>contains
java.lang.String-<span class="token operator">&gt;</span>replaceFirst
java.lang.String-<span class="token operator">&gt;</span>replaceAll
java.lang.String-<span class="token operator">&gt;</span>split
java.lang.String-<span class="token operator">&gt;</span>join
java.lang.String-<span class="token operator">&gt;</span>toLowerCase
java.lang.String-<span class="token operator">&gt;</span>toUpperCase
java.lang.String-<span class="token operator">&gt;</span>trim
java.lang.String-<span class="token operator">&gt;</span>toCharArray
java.lang.String-<span class="token operator">&gt;</span>format
java.lang.String-<span class="token operator">&gt;</span>copyValueOf
java.lang.String-<span class="token operator">&gt;</span>intern
Affect<span class="token punctuation">(</span>row-cnt:44<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">1342</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sm <span class="token parameter variable">-d</span> java.lang.String toString
 declaring-class  java.lang.String
 method-name      toString
 modifier         public
 annotation
 parameters
 <span class="token builtin class-name">return</span>           java.lang.String
 exceptions

Affect<span class="token punctuation">(</span>row-cnt:1<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,8);function u(b,h){const s=o("ExternalLinkIcon");return t(),l("div",null,[c,a("p",null,[a("a",p,[g,v,i(s)])]),m])}const S=e(d,[["render",u],["__file","sm.html.vue"]]);export{S as default};
