import{_ as s,o as t,c as i,a,b as l,e as n,d as r,r as o}from"./app.1e03d722.js";const d={},c=a("h1",{id:"sm",tabindex:"-1"},[a("a",{class:"header-anchor",href:"#sm","aria-hidden":"true"},"#"),n(" sm")],-1),p={href:"https://arthas.aliyun.com/doc/arthas-tutorials?language=en&id=command-sm",target:"_blank",rel:"noopener noreferrer"},g=a("code",null,"sm",-1),v=n(" online tutorial"),m=r(`<div class="custom-container tip"><p class="custom-container-title">TIP</p><p>Search method from the loaded classes.</p></div><p><code>sm</code> stands for search method. This command can search and show method information from all loaded classes. <code>sm</code> can only view the methods declared on the target class, that is, methods from its parent classes are invisible.</p><h2 id="options" tabindex="-1"><a class="header-anchor" href="#options" aria-hidden="true">#</a> Options</h2><table><thead><tr><th style="text-align:right;">Name</th><th style="text-align:left;">Specification</th></tr></thead><tbody><tr><td style="text-align:right;"><em>class-pattern</em></td><td style="text-align:left;">pattern for class name</td></tr><tr><td style="text-align:right;"><em>method-pattern</em></td><td style="text-align:left;">pattern for method name</td></tr><tr><td style="text-align:right;"><code>[d]</code></td><td style="text-align:left;">print the details of the method</td></tr><tr><td style="text-align:right;"><code>[E]</code></td><td style="text-align:left;">turn on regex matching while the default mode is wildcard matching</td></tr><tr><td style="text-align:right;"><code>[c:]</code></td><td style="text-align:left;">The hash code of the special class&#39;s classLoader</td></tr><tr><td style="text-align:right;"><code>[classLoaderClass:]</code></td><td style="text-align:left;">The class name of the ClassLoader that executes the expression.</td></tr><tr><td style="text-align:right;"><code>[n:]</code></td><td style="text-align:left;">Maximum number of matching classes with details (100 by default)</td></tr></tbody></table><h2 id="usage" tabindex="-1"><a class="header-anchor" href="#usage" aria-hidden="true">#</a> Usage</h2><p>View methods of <code>java.lang.String</code>:</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sm java.lang.String
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
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>View method <code>java.lang.String#toString</code> details:</p><div class="language-bash ext-sh line-numbers-mode"><pre class="language-bash"><code>$ sm <span class="token parameter variable">-d</span> java.lang.String toString
 declaring-class  java.lang.String
 method-name      toString
 modifier         public
 annotation
 parameters
 <span class="token builtin class-name">return</span>           java.lang.String
 exceptions

Affect<span class="token punctuation">(</span>row-cnt:1<span class="token punctuation">)</span> cost <span class="token keyword">in</span> <span class="token number">3</span> ms.
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,9);function u(h,b){const e=o("ExternalLinkIcon");return t(),i("div",null,[c,a("p",null,[a("a",p,[g,v,l(e)])]),m])}const S=s(d,[["render",u],["__file","sm.html.vue"]]);export{S as default};
